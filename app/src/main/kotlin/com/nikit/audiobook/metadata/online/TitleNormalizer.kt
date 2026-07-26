package com.nikit.audiobook.metadata.online

/**
 * Чистка названия файла/тега для запроса к онлайн-базам метаданных.
 * Убирает мусорные токены, технические скобки, год, расширение; схлопывает пробелы.
 */
object TitleNormalizer {
    private val noiseWords =
        setOf(
            "mp3",
            "m4b",
            "m4a",
            "opus",
            "flac",
            "ogg",
            "aac",
            "wav",
            "audiobook",
            "аудиокнига",
            "аудокнига",
            "cd",
            "rip",
            "320",
            "192",
            "128",
            "kbps",
            "stereo",
        )

    fun normalize(raw: String): String {
        var s = raw.lowercase()
        // удалить расширение
        s = s.substringBeforeLast('.')
        // убрать технические скобки [...]
        s = s.replace(Regex("\\[[^\\]]*]"), " ")
        s = s.replace(Regex("\\([^)]*\\)"), " ")
        // заменить разделители на пробел
        s = s.replace(Regex("[_./\\-]"), " ")
        // убрать год 19xx/20xx
        s = s.replace(Regex("\\b(19|20)\\d{2}\\b"), " ")
        // токенизация
        val tokens =
            s
                .split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() && it !in noiseWords && !it.matches(Regex("\\d+")) }
        return tokens.joinToString(" ").trim()
    }
}
