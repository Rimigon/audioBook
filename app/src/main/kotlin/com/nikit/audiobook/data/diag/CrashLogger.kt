package com.nikit.audiobook.data.diag

import android.content.Context
import android.os.Build
import com.nikit.audiobook.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Лёгкий локальный журнал вылетов без внешних сервисов (Firebase и т.п.).
 *
 * Ставит глобальный [Thread.UncaughtExceptionHandler], который пишет стектрейс
 * в файл `filesDir/crash_logs/crash_<timestamp>.txt` (с версией приложения,
 * версией Android и моделью устройства), а затем передаёт исключение дальше
 * по цепочке — приложение ведёт себя как раньше.
 *
 * Хранится не более [maxLogs] последних файлов.
 */
@Singleton
class CrashLogger
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val dir: File get() = File(context.filesDir, "crash_logs")
        private val maxLogs = 20

        /** Регистрирует обработчик. Вызывается из Application.onCreate. */
        fun install() {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching { write(throwable) }
                previous?.uncaughtException(thread, throwable)
            }
        }

        /** Пишет стектрейс в файл. Отдельно от [install], чтобы тестировать без краша. */
        fun write(throwable: Throwable) {
            val d = dir
            d.mkdirs()
            val stamp =
                SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) +
                    "_" +
                    (System.nanoTime() % 1_000_000L) // уникальность при нескольких крашах в одну мс
            val header =
                buildString {
                    appendLine("Время: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine("Версия приложения: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android API: ${Build.VERSION.SDK_INT} · устройство: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine()
                }
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            File(d, "crash_$stamp.txt").writeText(header + sw.toString())
            trimTo(maxLogs)
        }

        /** Файлы крашей, от новых к старым. */
        fun logs(): List<File> =
            dir
                .listFiles { f -> f.isFile && f.extension == "txt" }
                ?.sortedByDescending { it.lastModified() }
                ?.toList()
                ?: emptyList()

        fun read(file: File): String = runCatching { file.readText() }.getOrDefault("")

        fun clear() {
            logs().forEach { it.delete() }
        }

        private fun trimTo(max: Int) {
            logs().drop(max).forEach { it.delete() }
        }
    }
