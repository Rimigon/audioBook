package com.nikit.audiobook.data.saf

import android.net.Uri

/**
 * Человекочитаемый путь из SAF tree/document URI для отображения в настройках.
 *
 * SAF tree-URI выглядит как
 * `content://com.android.externalstorage.documents/tree/primary%3AAudiobooks%2F%D0%9A%D0%BD%D0%B8%D0%B3%D0%B0`
 * — сегмент после `/tree/` это percent-закодированный document id. Декодируем его,
 * чтобы пользователь видел нормальные русские буквы и слэши вместо `%3A`/`%D0%BA`.
 *
 * Парсим прямо из строкового представления, не полагаясь на [Uri.getLastPathSegment]
 * (его поведение с декодированием зависит от версии Android/Robolectric).
 */
fun Uri.humanPath(): String {
    val full = toString()
    // Берём подстроку после маркера дерева SAF.
    val marker = "/tree/"
    val start = full.indexOf(marker)
    val encoded =
        if (start >= 0) {
            // До конца строки либо до начала query/fragment.
            val rest = full.substring(start + marker.length)
            rest.substringBefore('?').substringBefore('#')
        } else {
            // Не tree-URI — пробуем последний сегмент пути.
            full.substringAfterLast('/', missingDelimiterValue = full)
        }
    val decoded = Uri.decode(encoded).ifBlank { full }
    return when {
        decoded.startsWith("primary:") -> "Внутр. память / " + decoded.removePrefix("primary:")
        decoded.contains(':') -> decoded.replace(":", " / ")
        else -> decoded
    }
}
