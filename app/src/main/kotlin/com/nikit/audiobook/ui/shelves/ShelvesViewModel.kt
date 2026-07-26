package com.nikit.audiobook.ui.shelves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.repo.ShelfRepository
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Shelf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelvesViewModel
    @Inject
    constructor(
        private val shelfRepository: ShelfRepository,
        private val scanSettings: ScanSettings,
    ) : ViewModel() {
        val shelves: StateFlow<List<Shelf>> =
            shelfRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        init {
            seedPresetsIfFirstLaunch()
        }

        private fun seedPresetsIfFirstLaunch() =
            viewModelScope.launch {
                if (!scanSettings.shelvesSeeded.first()) {
                    val presets = listOf("Читаю" to 0, "Прочитал" to 1, "Брошено" to 2, "Любимое" to 3)
                    presets.forEach { (name, idx) -> shelfRepository.upsert(Shelf(name = name, sortIndex = idx)) }
                    scanSettings.markShelvesSeeded()
                }
            }

        fun addShelf(name: String) = viewModelScope.launch { shelfRepository.upsert(Shelf(name = name)) }

        fun booksOfShelf(shelfId: String): Flow<List<Book>> = shelfRepository.observeBooksOfShelf(shelfId)
    }
