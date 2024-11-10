package com.example.inventory.ui.item

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ItemEntryViewModel(private val itemsRepository: ItemsRepository) : ViewModel() {

    var itemUiState by mutableStateOf(ItemUiState())
        private set


    fun updateUiState(itemDetails: ItemDetails) {
        itemUiState =
            ItemUiState(itemDetails = itemDetails, isEntryValid = validateInput(itemDetails))
    }


    fun saveItem() {
        if (validateInput()) {
            val updatedItemDetails = itemUiState.itemDetails.copy(
                date = if (itemUiState.itemDetails.date.isBlank()) {
                    getCurrentFormattedDate()
                } else {
                    validateAndFormatDate(itemUiState.itemDetails.date)
                }
            )
            val newItem = updatedItemDetails.toItem()
            viewModelScope.launch {
                itemsRepository.insertItem(newItem)
            }
        }
    }


    private fun getCurrentFormattedDate(): String {
        val currentDate = Date(System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("America/Mexico_City")
        return dateFormat.format(currentDate)
    }


    private fun validateAndFormatDate(dateString: String): String {
        return try {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("America/Mexico_City")
            val parsedDate = dateFormat.parse(dateString)
            parsedDate?.let {
                dateFormat.format(it)
            } ?: getCurrentFormattedDate()
        } catch (e: Exception) {
            getCurrentFormattedDate()
        }
    }


    private fun validateInput(uiState: ItemDetails = itemUiState.itemDetails): Boolean {
        return with(uiState) {
            title.isNotBlank() && descripcion.isNotBlank()
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
    val date: String = "",
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
