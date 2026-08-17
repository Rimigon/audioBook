package com.nikit.audiobook.data.diag

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLoggerTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun writesAndReadsCrash() {
        val logger = CrashLogger(ctx)
        logger.write(RuntimeException("boom"))

        val logs = logger.logs()
        assertThat(logs).hasSize(1)
        val text = logger.read(logs[0])
        assertThat(text).contains("RuntimeException")
        assertThat(text).contains("boom")
        // служебная шапка с версией и устройством
        assertThat(text).contains("Версия приложения")
        assertThat(text).contains("устройство")
    }

    @Test
    fun capsAtTwentyLogs() {
        val logger = CrashLogger(ctx)
        repeat(30) { i -> logger.write(RuntimeException("boom$i")) }
        assertThat(logger.logs()).hasSize(20)
    }

    @Test
    fun clearRemovesAllLogs() {
        val logger = CrashLogger(ctx)
        logger.write(RuntimeException("x"))
        logger.write(RuntimeException("y"))
        assertThat(logger.logs()).hasSize(2)

        logger.clear()
        assertThat(logger.logs()).isEmpty()
    }
}
