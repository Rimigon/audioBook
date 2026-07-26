package com.nikit.audiobook.ui.shelves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.repo.ShelfRepository
import com.nikit.audiobook.domain.model.Shelf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelvesViewModel
    @Inject
    constructor(
        private val shelfRepository: ShelfRepository,
    ) : ViewModel() {
        val shelves: StateFlow<List<Shelf>> =
            shelfRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun addShelf(name: String) =
            viewModelScope.launch {
                shelfRepository.upsert(Shelf(name = name))
            }
    }
