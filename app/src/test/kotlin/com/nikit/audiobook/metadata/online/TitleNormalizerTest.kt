package com.nikit.audiobook.metadata.online

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TitleNormalizerTest {
    @Test fun stripsYearBracketsAndExt() {
        assertThat(TitleNormalizer.normalize("Дюна (1965) [m4b]")).isEqualTo("дюна")
    }

    @Test fun stripsNoiseAndSeparators() {
        assertThat(TitleNormalizer.normalize("Author - Book mp3 320")).isEqualTo("author book")
    }

    @Test fun keepsCyrillic() {
        assertThat(TitleNormalizer.normalize("Мастер и Маргарита")).isEqualTo("мастер и маргарита")
    }

    @Test fun removesAudiobookWord() {
        assertThat(TitleNormalizer.normalize("Аудиокнига - Война и мир")).isEqualTo("война и мир")
    }

    @Test fun pureNumbersRemoved() {
        assertThat(TitleNormalizer.normalize("Chapter 12 - Title")).isEqualTo("chapter title")
    }
}
