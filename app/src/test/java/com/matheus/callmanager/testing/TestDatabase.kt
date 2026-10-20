package com.matheus.callmanager.testing

import android.content.Context
import androidx.room.Room
import com.matheus.callmanager.core.database.CallManagerDatabase
import java.util.concurrent.Executor

/**
 * In-memory database whose queries and transactions run inline on the calling thread.
 *
 * Room otherwise moves suspend DAO calls and Flow re-queries onto its own thread pool, which
 * `waitForIdle()` and test dispatchers can't see, so a test could assert before a write landed.
 */
fun inMemoryTestDatabase(context: Context): CallManagerDatabase {
    val direct = Executor { it.run() }
    return Room
        .inMemoryDatabaseBuilder(context, CallManagerDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(direct)
        .setTransactionExecutor(direct)
        .build()
}
