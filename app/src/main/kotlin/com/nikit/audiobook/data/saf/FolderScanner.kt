package com.nikit.audiobook.data.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.nikit.audiobook.domain.model.AudioFileRef

/** Обходит SAF-дерево [treeUri] и строит список [FsNode] для [BookClassifier]. */
class FolderScanner(
    private val context: Context,
) {
    fun buildTree(treeUri: Uri): List<FsNode> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return listOf(root.toFsNode())
    }

    private fun DocumentFile.toFsNode(): FsNode =
        if (isDirectory) {
            val children = listFiles().map { it.toFsNode() }
            FsNode.Dir(name ?: "", uri.toString(), children)
        } else {
            FsNode.File(name ?: "", AudioFileRef(uri.toString(), name ?: "", type))
        }
}
