package com.nikit.audiobook.player.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikit.audiobook.data.saf.ScanFacade
import com.nikit.audiobook.data.saf.ScanSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Периодический рескан выбранной папки. Читает treeUri из настроек и сканирует. */
@HiltWorker
class RescanWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val scanFacade: ScanFacade,
        private val scanSettings: ScanSettings,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val treeUri = scanSettings.treeUri.first() ?: return Result.success()
            return runCatching {
                scanFacade.scanNow(android.net.Uri.parse(treeUri))
                Result.success()
            }.getOrElse { Result.retry() }
        }
    }
