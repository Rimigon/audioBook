package com.nikit.audiobook.data.saf

import com.nikit.audiobook.domain.model.AudioFileRef
import com.nikit.audiobook.domain.model.BookDescriptor
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind

/** Узел файлового дерева, не зависящий от Android. */
sealed class FsNode {
    abstract val name: String

    data class Dir(
        override val name: String,
        val uri: String,
        val children: List<FsNode>,
    ) : FsNode()

    data class File(
        override val name: String,
        val ref: AudioFileRef,
    ) : FsNode()
}

private val AUDIO_EXT = setOf("mp3", "m4b", "m4a", "opus", "flac", "ogg", "aac", "wav")

fun isAudioFile(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in AUDIO_EXT
}

fun stripExt(name: String): String = name.substringBeforeLast('.')

/** Natural-order сравнение строк (ch2 < ch10). Public для переиспользования. */
fun naturalCompare(
    a: String,
    b: String,
): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var endI = i
            while (endI < a.length && a[endI].isDigit()) endI++
            var endJ = j
            while (endJ < b.length && b[endJ].isDigit()) endJ++
            val na = a.substring(i, endI).toLong()
            val nb = b.substring(j, endJ).toLong()
            if (na != nb) return na.compareTo(nb)
            i = endI
            j = endJ
        } else {
            if (ca != cb) return ca.compareTo(cb)
            i++
            j++
        }
    }
    return a.length - b.length
}

/**
 * Чистая эвристика классификации книг по структуре дерева.
 * - Папка с ≥1 аудиофайлом → книга FOLDER (аудиофайлы = главы, natural-order).
 *   Не-аудио дети (обложки, txt, cue) игнорируются. Подпапки внутри такой папки игнорируются.
 * - Папка без аудиофайлов, но с подпапками → рекурсия в подпапки.
 * - Одиночный аудиофайл вне книги-папки → M4B (если .m4b) иначе SINGLE_FILE.
 */
object BookClassifier {
    fun classify(roots: List<FsNode>): List<BookDescriptor> {
        val out = ArrayList<BookDescriptor>()
        for (root in roots) visit(root, out)
        return out
    }

    private fun visit(
        node: FsNode,
        out: MutableList<BookDescriptor>,
    ) {
        when (node) {
            is FsNode.Dir -> {
                val audioChildren =
                    node.children
                        .filterIsInstance<FsNode.File>()
                        .filter { isAudioFile(it.name) }
                if (audioChildren.isNotEmpty()) {
                    val sorted = audioChildren.sortedWith(NaturalOrder)
                    out.add(
                        BookDescriptor(
                            title = node.name,
                            type = FileType.FOLDER,
                            files = sorted.map { it.ref },
                            sourceUri = node.uri,
                            sourceKind = SourceKind.LOCAL_FOLDER,
                        ),
                    )
                } else {
                    for (c in node.children) if (c is FsNode.Dir) visit(c, out)
                }
            }

            is FsNode.File -> {
                if (isAudioFile(node.name)) {
                    val type = if (node.name.lowercase().endsWith(".m4b")) FileType.M4B else FileType.SINGLE_FILE
                    out.add(
                        BookDescriptor(
                            title = stripExt(node.name),
                            type = type,
                            files = listOf(node.ref),
                            sourceUri = node.ref.uri,
                            sourceKind = SourceKind.LOCAL_FILE,
                        ),
                    )
                }
            }
        }
    }
}

/** Natural-order компаратор (ch2 < ch10) для файлов. */
private object NaturalOrder : Comparator<FsNode.File> {
    override fun compare(
        a: FsNode.File,
        b: FsNode.File,
    ): Int = naturalCompare(a.name, b.name)
}
