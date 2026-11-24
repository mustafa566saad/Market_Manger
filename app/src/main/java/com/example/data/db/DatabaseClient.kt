package com.example.data.db

import android.util.Log
import com.example.data.model.ConnectionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

/**
 * Crash-Proof Singleton Database Connection Manager for Mohsen Market.
 * - Strictly isolates all network & JDBC calls inside withContext(Dispatchers.IO).
 * - Uses direct jTDS Driver instantiation on Android Runtime (ART) with prepareSQL=0
 *   to avoid DriverManager ServiceLoader crashes and sp_prepare round-trip overhead.
 * - Throttles concurrent connections via Semaphore to protect remote SQL Server.
 * - Catches all SQLException, SocketTimeoutException, and runtime Throwables into Result<T>.
 */
object DatabaseClient {

    private const val TAG = "DatabaseClient"

    // Remote SQL Server Credentials (DO NOT CHANGE)
    const val SERVER_HOST = "db79344.public.databaseasp.net"
    const val SERVER_PORT = 1433
    const val DATABASE_NAME = "db79344"
    const val USER_ID = "db79344"
    const val PASSWORD = "Ph4+@Nr5g8E-"

    const val CONNECTION_STRING =
        "Server=db79344.public.databaseasp.net; Database=db79344; User Id=db79344; Password=Ph4+@Nr5g8E-; Encrypt=True; TrustServerCertificate=True; MultipleActiveResultSets=True;"

    // jTDS URL optimized for Android Runtime (ART):
    // prepareSQL=0 avoids extra sp_prepare network round-trips and untyped subquery parameter errors.
    private const val JTDS_JDBC_URL =
        "jdbc:jtds:sqlserver://$SERVER_HOST:$SERVER_PORT/$DATABASE_NAME;ssl=request;useNTLMv2=true;prepareSQL=0;loginTimeout=10;socketTimeout=15;charset=UTF-8;"

    private const val JTDS_NO_SSL_URL =
        "jdbc:jtds:sqlserver://$SERVER_HOST:$SERVER_PORT/$DATABASE_NAME;useNTLMv2=true;prepareSQL=0;loginTimeout=10;socketTimeout=15;charset=UTF-8;"

    private const val MSSQL_JDBC_URL =
        "jdbc:sqlserver://$SERVER_HOST:$SERVER_PORT;databaseName=$DATABASE_NAME;user=$USER_ID;password=$PASSWORD;encrypt=true;trustServerCertificate=true;multipleActiveResultSets=true;loginTimeout=10;socketTimeout=15000;"

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    // Limit simultaneous open connections to prevent remote server throttling
    private val connectionSemaphore = Semaphore(permits = 4)

    @Volatile
    private var jtdsDriverInstance: Driver? = null

    init {
        try {
            val clazz = Class.forName("net.sourceforge.jtds.jdbc.Driver")
            jtdsDriverInstance = clazz.getDeclaredConstructor().newInstance() as? Driver
        } catch (t: Throwable) {
            Log.w(TAG, "jTDS driver init warning: ${t.message}")
        }
    }

    /**
     * Opens a live JDBC connection. Must only be called from an IO background thread.
     * Prioritizes direct jTDS Driver connection for 100% Android Runtime (ART) stability,
     * with automatic fallback to MSSQL JDBC driver.
     */
    fun openConnection(): Connection {
        DriverManager.setLoginTimeout(10)
        val props = Properties().apply {
            setProperty("user", USER_ID)
            setProperty("password", PASSWORD)
            setProperty("loginTimeout", "10")
            setProperty("socketTimeout", "15")
            setProperty("prepareSQL", "0")
            setProperty("charset", "UTF-8")
        }

        // 1. Primary path: Direct jTDS Driver instance (bypasses DriverManager global lock & ART classloading issues)
        val jtds = jtdsDriverInstance
        if (jtds != null) {
            try {
                val conn = jtds.connect(JTDS_JDBC_URL, props)
                if (conn != null) return conn
            } catch (sslErr: Throwable) {
                Log.w(TAG, "jTDS ssl=request failed (${sslErr.message}), trying jTDS plain/require fallback")
                try {
                    val connRequire = jtds.connect(
                        "jdbc:jtds:sqlserver://$SERVER_HOST:$SERVER_PORT/$DATABASE_NAME;ssl=require;useNTLMv2=true;prepareSQL=0;loginTimeout=10;socketTimeout=15;",
                        props
                    )
                    if (connRequire != null) return connRequire
                } catch (_: Throwable) {
                }
                try {
                    val connPlain = jtds.connect(JTDS_NO_SSL_URL, props)
                    if (connPlain != null) return connPlain
                } catch (_: Throwable) {
                }
            }
        }

        // 2. Secondary path: DriverManager with jTDS URL
        try {
            return DriverManager.getConnection(JTDS_JDBC_URL, USER_ID, PASSWORD)
        } catch (t: Throwable) {
            Log.w(TAG, "DriverManager jTDS failed (${t.message}), trying MSSQL JDBC driver")
        }

        // 3. Tertiary fallback: MSSQL JDBC Driver
        return DriverManager.getConnection(MSSQL_JDBC_URL)
    }

    /**
     * Crash-proof generic query wrapper that:
     * - Strictly executes on Dispatchers.IO.
     * - Retries transient network drops with exponential backoff.
     * - Catches SQLException, SocketTimeoutException, and any runtime Throwable.
     * - Never crashes the app; always returns Result<T>.
     */
    suspend fun <T> safeQuery(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 800L,
        block: (Connection) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        connectionSemaphore.withPermit {
            var currentDelay = initialDelayMs
            var lastError: Throwable? = null

            for (attempt in 1..maxAttempts) {
                var conn: Connection? = null
                try {
                    if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
                        _connectionStatus.value =
                            if (attempt > 1) ConnectionStatus.RETRYING else ConnectionStatus.CONNECTING
                    }
                    conn = openConnection()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _lastErrorMessage.value = null

                    val data = block(conn)
                    return@withPermit Result.success(data)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (timeoutEx: SocketTimeoutException) {
                    lastError = timeoutEx
                    Log.w(TAG, "SQL SocketTimeout (attempt $attempt/$maxAttempts): ${timeoutEx.message}")
                    _lastErrorMessage.value = "انتهت مهلة الاتصال بالسيرفر. يرجى التحقق من الإنترنت والمحاولة مرة أخرى."
                } catch (sqlEx: SQLException) {
                    lastError = sqlEx
                    Log.w(TAG, "SQLException (attempt $attempt/$maxAttempts): ${sqlEx.message}")
                    _lastErrorMessage.value = translateSqlErrorToArabic(sqlEx)
                } catch (t: Throwable) {
                    lastError = t
                    Log.e(TAG, "Unexpected database error (attempt $attempt/$maxAttempts): ${t.message}", t)
                    _lastErrorMessage.value = translateThrowableToArabic(t)
                } finally {
                    try {
                        conn?.close()
                    } catch (_: Throwable) {
                    }
                }

                if (attempt == maxAttempts) {
                    _connectionStatus.value = ConnectionStatus.ERROR
                } else {
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(4000L)
                }
            }

            val arabicMsg = _lastErrorMessage.value ?: "تعذر الاتصال بقاعدة البيانات. تحقق من اتصال الإنترنت."
            Result.failure(SQLException(arabicMsg, lastError))
        }
    }

    /**
     * Crash-proof atomic transaction wrapper running strictly on Dispatchers.IO.
     * Commits on success or rolls back on any error without crashing the app.
     */
    suspend fun <T> safeTransaction(
        isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
        maxAttempts: Int = 3,
        initialDelayMs: Long = 800L,
        block: (Connection) -> T
    ): Result<T> = safeQuery(maxAttempts = maxAttempts, initialDelayMs = initialDelayMs) { conn ->
        val origAutoCommit = try {
            conn.autoCommit
        } catch (_: Throwable) {
            true
        }
        try {
            conn.autoCommit = false
            try {
                conn.transactionIsolation = isolationLevel
            } catch (_: Throwable) {
            }
            val res = block(conn)
            conn.commit()
            res
        } catch (t: Throwable) {
            try {
                conn.rollback()
            } catch (rbErr: Throwable) {
                Log.w(TAG, "Rollback warning: ${rbErr.message}")
            }
            throw t
        } finally {
            try {
                conn.autoCommit = origAutoCommit
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun testConnection(): Boolean {
        return safeQuery(maxAttempts = 2) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT 1").use { rs -> rs.next() }
            }
        }.getOrDefault(false)
    }

    private fun translateSqlErrorToArabic(ex: SQLException): String {
        val msg = ex.message?.lowercase() ?: ""
        return when {
            msg.contains("timeout") || msg.contains("timed out") ->
                "انتهت مهلة الاتصال بسيرفر قاعدة البيانات. اضغط إعادة المحاولة 🔄"
            msg.contains("network") || msg.contains("host") || msg.contains("refused") || msg.contains("socket") ->
                "تعذر الوصول إلى سيرفر محسن ماركت. تأكد من اتصالك بالإنترنت."
            msg.contains("login failed") ->
                "خطأ في بيانات المصادقة مع سيرفر قاعدة البيانات."
            else ->
                "حدث خطأ في قاعدة البيانات: ${ex.localizedMessage ?: "يرجى المحاولة مرة أخرى"}"
        }
    }

    private fun translateThrowableToArabic(t: Throwable): String {
        val msg = t.message ?: ""
        return if (msg.any { it in '\u0600'..'\u06FF' }) {
            msg // Already Arabic (e.g. validation message)
        } else {
            "تعذر إتمام العملية حالياً (${t.javaClass.simpleName}). يرجى المحاولة مرة أخرى 🔄"
        }
    }
}
