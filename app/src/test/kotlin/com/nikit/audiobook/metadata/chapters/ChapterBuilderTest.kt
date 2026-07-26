package com.nikit.audiobook.metadata.chapters

import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.domain.model.AudioFileRef
import org.junit.Test

class ChapterBuilderTest {
    private fun ref(name: String) = AudioFileRef("uri://$name", name)

    @Test
    fun ordersNaturalAndStripsExt() {
        val chapters =
            ChapterBuilder.fromFiles(
                "b1",
                listOf(ref("ch10.mp3"), ref("ch2.mp3"), ref("ch1.mp3")),
            )
        assertThat(chapters.map { it.title }).containsExactly("ch1", "ch2", "ch10").inOrder()
        assertThat(chapters.map { it.index }).containsExactly(0, 1, 2).inOrder()
        assertThat(chapters.map { it.filePath })
            .containsExactly(
                "uri://ch1.mp3",
                "uri://ch2.mp3",
                "uri://ch10.mp3",
            ).inOrder()
    }

    @Test
    fun allChaptersBelongToBook() {
        val chapters = ChapterBuilder.fromFiles("b1", listOf(ref("a.mp3"), ref("b.mp3")))
        assertThat(chapters.all { it.bookId == "b1" }).isTrue()
    }

    @Test
    fun singleCreatesOneVirtualChapter() {
        val ch = ChapterBuilder.single("b1", "Дюна", "uri://dune.m4b", 3600_000L)
        assertThat(ch.index).isEqualTo(0)
        assertThat(ch.title).isEqualTo("Дюна")
        assertThat(ch.endMs).isEqualTo(3600_000L)
        assertThat(ch.filePath).isEqualTo("uri://dune.m4b")
    }
}
