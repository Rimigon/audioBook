package com.nikit.audiobook.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nikit.audiobook.domain.model.BookStatus

/** Единые для всего приложения метки статусов (Библиотека, Полки, страница книги). */
@Composable
fun statusLabel(s: BookStatus): String =
    when (s) {
        BookStatus.READING -> "Читаю"
        BookStatus.COMPLETED -> "Прочитал"
        BookStatus.DROPPED -> "Брошено"
        BookStatus.PAUSED -> "Пауза"
        BookStatus.WISHLIST -> "Хочу"
    }

/** Цвет метки статуса — чтобы статус читался одинаково везде. */
@Composable
fun statusColor(s: BookStatus): Color =
    when (s) {
        BookStatus.READING -> MaterialTheme.colorScheme.primary
        BookStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        BookStatus.DROPPED -> MaterialTheme.colorScheme.error
        BookStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        BookStatus.WISHLIST -> MaterialTheme.colorScheme.onSurfaceVariant
    }
