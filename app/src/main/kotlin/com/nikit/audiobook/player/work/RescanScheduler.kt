package com.nikit.audiobook.player.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Планирует/обновляет периодический рескан. */
@Singleton
class RescanScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun schedule(intervalMinutes: Int) {
            val request = PeriodicWorkRequestBuilder<RescanWorker>(intervalMinutes.toLong(), TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "rescan",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel() {
            WorkManager.getInstance(context).cancelUniqueWork("rescan")
        }
    }
