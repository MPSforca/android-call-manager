package com.matheus.callmanager.core.di

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test

class DispatchersModuleTest {
    @Test
    fun `provides the IO dispatcher`() {
        assertSame(Dispatchers.IO, DispatchersModule.provideIoDispatcher())
    }
}
