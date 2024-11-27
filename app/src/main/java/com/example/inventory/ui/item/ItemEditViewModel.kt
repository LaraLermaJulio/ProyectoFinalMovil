/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory.ui.item

import ContentType
import ItemDetails
import ItemUiState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.ItemsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import toItem
import toItemUiState

/**
 * ViewModel to retrieve and update an item from the [ItemsRepository]'s data source.
 */
class ItemEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemsRepository: ItemsRepository
) : ViewModel() {

    private val _itemUiState = MutableStateFlow(ItemUiState())
    val itemUiState: StateFlow<ItemUiState> = _itemUiState.asStateFlow()

    private val itemId: Int = checkNotNull(savedStateHandle[ItemEditDestination.itemIdArg])

    init {
        viewModelScope.launch {
            _itemUiState.value = itemsRepository.getItemStream(itemId)
                .filterNotNull()
                .first()
                .toItemUiState(true)
        }
    }

    /**
     * Update the item in the [ItemsRepository]'s data source
     */
    suspend fun updateItem() {
        if (validateInput(_itemUiState.value.itemDetails)) {
            itemsRepository.updateItem(_itemUiState.value.itemDetails.toItem())
        }
    }

    suspend fun updateItem(uri: String, type: ContentType) {
        val updatedDetails = when (type) {
            ContentType.PHOTO -> _itemUiState.value.itemDetails.copy(photoUris = _itemUiState.value.itemDetails.photoUris + uri)
            ContentType.VIDEO -> _itemUiState.value.itemDetails.copy(videoUris = _itemUiState.value.itemDetails.videoUris + uri)
            ContentType.AUDIO -> _itemUiState.value.itemDetails.copy(audioUris = _itemUiState.value.itemDetails.audioUris + uri)
        }

        if (validateInput(updatedDetails)) {
            itemsRepository.updateItem(updatedDetails.toItem())
            reloadItem()
        }
    }

    private suspend fun reloadItem() {
        _itemUiState.value = itemsRepository.getItemStream(itemId)
            .filterNotNull()
            .first()
            .toItemUiState(true)
    }

    fun updateUiState(itemDetails: ItemDetails) {
        _itemUiState.value = ItemUiState(itemDetails = itemDetails, isEntryValid = validateInput(itemDetails))
    }

    private fun validateInput(uiState: ItemDetails = _itemUiState.value.itemDetails): Boolean {
        return with(uiState) {
            title.isNotBlank() && descripcion.isNotBlank()
        }
    }
}