package com.nikit.audiobook.data.saf

import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.domain.model.AudioFileRef
import org.junit.Test

class BookClassifierTest {
    private fun file(
        name: String,
        uri: String = "uri://$name",
    ) = FsNode.File(name, AudioFileRef(uri, name))

    private fun dir(
        name: String,
        vararg children: FsNode,
    ) = FsNode.Dir(name, "uri://$name", children.toList())

    @Test
    fun folderWithMp3s_isFolderBook() {
        val tree =
            listOf(
                dir("Дюна", file("01.mp3"), file("02.mp3"), FsNode.File("cover.jpg", AudioFileRef("uri://cover.jpg", "cover.jpg"))),
            )
        val books = BookClassifier.classify(tree)
        assertThat(books).hasSize(1)
        val b = books.single()
        assertThat(b.type).isEqualTo(com.nikit.audiobook.domain.model.FileType.FOLDER)
        assertThat(b.files.map { it.name }).containsExactly("01.mp3", "02.mp3").inOrder()
        assertThat(b.title).isEqualTo("Дюна")
    }

    @Test
    fun naturalOrder_chapters() {
        val tree = listOf(dir("B", file("ch1.mp3"), file("ch10.mp3"), file("ch2.mp3")))
        val books = BookClassifier.classify(tree)
        assertThat(books.single().files.map { it.name })
            .containsExactly("ch1.mp3", "ch2.mp3", "ch10.mp3")
            .inOrder()
    }

    @Test
    fun rootLevelLooseFiles_singleOrM4b() {
        val tree =
            listOf(
                file("book.mp3"),
                file("lecture.m4b"),
                FsNode.File("notes.txt", AudioFileRef("uri://notes.txt", "notes.txt")),
            )
        val books = BookClassifier.classify(tree)
        assertThat(books).hasSize(2)
        val mp3 = books.first { it.type == com.nikit.audiobook.domain.model.FileType.SINGLE_FILE }
        val m4b = books.first { it.type == com.nikit.audiobook.domain.model.FileType.M4B }
        assertThat(mp3.title).isEqualTo("book")
        assertThat(m4b.title).isEqualTo("lecture")
    }

    @Test
    fun dirWithSubdirs_recurses() {
        val tree =
            listOf(
                dir("root", dir("A", file("a.mp3")), dir("B", file("b.mp3"))),
            )
        val books = BookClassifier.classify(tree)
        assertThat(books.map { it.title }).containsExactly("A", "B")
    }

    @Test
    fun folderWithAudioAndSubdirs_treatsAsFolder_ignoresSubdirs() {
        val tree =
            listOf(
                dir("X", file("x1.mp3"), dir("inner", file("y.mp3"))),
            )
        val books = BookClassifier.classify(tree)
        assertThat(books).hasSize(1)
        assertThat(books.single().files.map { it.name }).containsExactly("x1.mp3")
    }

    @Test
    fun ignoresNonAudioFilesInDir() {
        val tree = listOf(dir("Y", file("cover.png"), FsNode.File("cue.cue", AudioFileRef("u", "cue.cue"))))
        // нет аудио, нет подпапок → ничего
        assertThat(BookClassifier.classify(tree)).isEmpty()
    }
}
