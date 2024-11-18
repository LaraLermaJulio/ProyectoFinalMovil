import android.widget.TimePicker
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            title.isNotBlank() &&
                    descripcion.isNotBlank() &&
                    date.isNotBlank() &&
                    (photoUris.isEmpty() || photoUris.all { it.isNotBlank() }) &&
                    (videoUris.isEmpty() || videoUris.all { it.isNotBlank() }) &&
                    (audioUris.isEmpty() || audioUris.all { it.isNotBlank() })
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
    val date: String = "", // Fecha inicializada explícitamente
    val photoUris: List<String> = emptyList(),
    val videoUris: List<String> = emptyList(),
    val audioUris: List<String> = emptyList()
)

fun ItemDetails.toItem(): Item = Item(
    id = id,
    title = title,
    descripcion = descripcion,
    type = type,
    status = status,
    date = date,
    photoUris = photoUris,
    videoUris = videoUris,
    audioUris = audioUris
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
    photoUris = photoUris,
    videoUris = videoUris,
    audioUris = audioUris
)

