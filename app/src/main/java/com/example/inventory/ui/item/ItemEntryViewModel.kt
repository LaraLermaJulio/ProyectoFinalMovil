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

import android.widget.TimePicker
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import java.sql.Time
import java.sql.Timestamp
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Date

class ItemEntryViewModel(private val itemsRepository: ItemsRepository) : ViewModel() {

    var itemUiState by mutableStateOf(ItemUiState())
        private set

    fun updateUiState(itemDetails: ItemDetails) {
        itemUiState =
            ItemUiState(itemDetails = itemDetails, isEntryValid = validateInput(itemDetails))
    }

    suspend fun saveItem() {
        if (validateInput()) {
            itemsRepository.insertItem(itemUiState.itemDetails.toItem())
        }
    }

    private fun validateInput(uiState: ItemDetails = itemUiState.itemDetails): Boolean {
        return with(uiState) {
            title.isNotBlank()  && descripcion.isNotBlank()
        }
    }
}

data class ItemUiState(
    val itemDetails: ItemDetails = ItemDetails(),
    val isEntryValid: Boolean = false
)
data class ItemDetails(
    val id: Int = 0,
    val title: String = "",
    val type: Boolean = true,
    val status: Boolean = false,
    val descripcion: String = "",
    val date: String = Timestamp(System.currentTimeMillis()).toString(),
    val photoUri: String? = null,
    val videoUri: String? = null,
    val audioUri: String? = null
)

fun ItemDetails.toItem(): Item = Item(
    id = id,
    title = title,
    descripcion = descripcion,
    type = type,
    status = status,
    date = date,
    photoUri = photoUri,
    videoUri = videoUri,
    audioUri = audioUri
)

fun Item.toItemUiState(isEntryValid: Boolean = false): ItemUiState = ItemUiState(
    itemDetails = this.toItemDetails(),
    isEntryValid = isEntryValid
)

fun Item.toItemDetails(): ItemDetails = ItemDetails(
    id = id,
    title = title,
    descripcion = descripcion,
    type = type,
    status = status,
    date = date,
    photoUri = photoUri,
    videoUri = videoUri,
    audioUri = audioUri
)

