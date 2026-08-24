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

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp")

private val PREFERRED_COVER = setOf("cover.jpg", "cover.png", "folder.jpg", "folder.png", "обложка.jpg", "обложка.png")

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
                val subdirs = node.children.filterIsInstance<FsNode.Dir>()
                when {
                    subdirs.isNotEmpty() -> {
                        // В папке есть подпапки — это каталог книг, а не сама книга.
                        // Рекурсивно обрабатываем подпапки как книги, а прямые аудиофайлы
                        // (например .m4b в корне наблюдения) — как отдельные книги.
                        // Иначе одиночный m4b в корне «съел» бы все книги-подпапки.
                        subdirs.forEach { visit(it, out) }
                        audioChildren.forEach { f -> out.add(singleDescriptor(f.name, f.ref)) }
                    }

                    audioChildren.isNotEmpty() -> {
                        val sorted = audioChildren.sortedWith(NaturalOrder)
                        out.add(
                            BookDescriptor(
                                title = node.name,
                                type = FileType.FOLDER,
                                files = sorted.map { it.ref },
                                sourceUri = node.uri,
                                sourceKind = SourceKind.LOCAL_FOLDER,
                                coverImage = node.children.firstImage(),
                            ),
                        )
                    }
                }
            }

            is FsNode.File -> {
                if (isAudioFile(node.name)) out.add(singleDescriptor(node.name, node.ref))
            }
        }
    }

    private fun singleDescriptor(
        name: String,
        ref: AudioFileRef,
    ): BookDescriptor {
        val type = if (name.lowercase().endsWith(".m4b")) FileType.M4B else FileType.SINGLE_FILE
        return BookDescriptor(
            title = stripExt(name),
            type = type,
            files = listOf(ref),
            sourceUri = ref.uri,
            sourceKind = SourceKind.LOCAL_FILE,
        )
    }

    /** Лучшая картинка-обложка среди файлов папки (сначала с каноничным именем, иначе первый jpg/png/webp). */
    private fun List<FsNode>.firstImage(): AudioFileRef? {
        val images =
            filterIsInstance<FsNode.File>().filter {
                it.name.lowercase() in PREFERRED_COVER ||
                    it.name.substringAfterLast('.', "").lowercase() in IMAGE_EXT
            }
        if (images.isEmpty()) return null
        val preferred = images.firstOrNull { it.name.lowercase() in PREFERRED_COVER }
        return (preferred ?: images.first()).ref
    }
}

/** Natural-order компаратор (ch2 < ch10) для файлов. */
private object NaturalOrder : Comparator<FsNode.File> {
    override fun compare(
        a: FsNode.File,
        b: FsNode.File,
    ): Int = naturalCompare(a.name, b.name)
}
