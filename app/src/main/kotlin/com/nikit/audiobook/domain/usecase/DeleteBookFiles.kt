package com.nikit.audiobook.domain.usecase

import com.nikit.audiobook.data.repo.BookRepository
import javax.inject.Inject

class DeleteBookFiles
    @Inject
    constructor(
        private val repo: BookRepository,
    ) {
        suspend operator fun invoke(bookId: String) = repo.markFilesDeleted(bookId)
    }
