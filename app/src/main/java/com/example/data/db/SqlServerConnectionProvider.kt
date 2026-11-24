package com.example.data.db

import com.example.data.model.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow
import java.sql.Connection

/**
 * [TASK 0: SQL SERVER CONNECTION PROVIDER]
 * Delegates all connection management, Dispatchers.IO isolation, and crash-proof
 * safeQuery / safeTransaction execution to [DatabaseClient].
 */
object SqlServerConnectionProvider {

    const val SERVER_HOST = DatabaseClient.SERVER_HOST
    const val SERVER_PORT = DatabaseClient.SERVER_PORT
    const val DATABASE_NAME = DatabaseClient.DATABASE_NAME
    const val USER_ID = DatabaseClient.USER_ID
    const val PASSWORD = DatabaseClient.PASSWORD

    val connectionStatus: StateFlow<ConnectionStatus> = DatabaseClient.connectionStatus
    val lastErrorMessage: StateFlow<String?> = DatabaseClient.lastErrorMessage

    fun openConnection(): Connection = DatabaseClient.openConnection()

    suspend fun <T> execute(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 800L,
        block: (Connection) -> T
    ): Result<T> = DatabaseClient.safeQuery(
        maxAttempts = maxAttempts,
        initialDelayMs = initialDelayMs,
        block = block
    )

    suspend fun <T> executeTransaction(
        isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
        maxAttempts: Int = 3,
        initialDelayMs: Long = 800L,
        block: (Connection) -> T
    ): Result<T> = DatabaseClient.safeTransaction(
        isolationLevel = isolationLevel,
        maxAttempts = maxAttempts,
        initialDelayMs = initialDelayMs,
        block = block
    )

    suspend fun testConnection(): Boolean = DatabaseClient.testConnection()
}
