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

    /**
     * Holds current item ui state
     */
    var itemUiState by mutableStateOf(ItemUiState())
        private set

    private val itemId: Int = checkNotNull(savedStateHandle[ItemEditDestination.itemIdArg])

    init {
        viewModelScope.launch {
            itemUiState = itemsRepository.getItemStream(itemId)
                .filterNotNull()
                .first()
                .toItemUiState(true)
        }
    }

    /**
     * Update the item in the [ItemsRepository]'s data source
     */
    suspend fun updateItem() {
        if (validateInput(itemUiState.itemDetails)) {
            itemsRepository.updateItem(itemUiState.itemDetails.toItem())
        }
    }
    suspend fun updateItem(uri: String, type: ContentType) {
        val updatedDetails = when (type) {
            ContentType.PHOTO -> itemUiState.itemDetails.copy(photoUris = itemUiState.itemDetails.photoUris + uri)
            ContentType.VIDEO -> itemUiState.itemDetails.copy(videoUris = itemUiState.itemDetails.videoUris + uri)
            ContentType.AUDIO -> itemUiState.itemDetails.copy(audioUris = itemUiState.itemDetails.audioUris + uri)
        }

        // Actualizar el ítem con los nuevos detalles
        if (validateInput(updatedDetails)) {
            itemsRepository.updateItem(updatedDetails.toItem())
            reloadItem()
        }
    }

    private suspend fun reloadItem() {
        itemUiState = itemsRepository.getItemStream(itemId)
            .filterNotNull()
            .first()
            .toItemUiState(true)
    }

    private fun validateUri(uri: String): Boolean {
        return uri.isNotBlank()
    }


    /**
     * Updates the [itemUiState] with the value provided in the argument. This method also triggers
     * a validation for input values.
     */
    fun updateUiState(itemDetails: ItemDetails) {
        itemUiState =
            ItemUiState(itemDetails = itemDetails, isEntryValid = validateInput(itemDetails))
    }

    private fun validateInput(uiState: ItemDetails = itemUiState.itemDetails): Boolean {
        return with(uiState) {
            title.isNotBlank() && descripcion.isNotBlank()
        }
    }
}
