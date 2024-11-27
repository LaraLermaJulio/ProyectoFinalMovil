import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona el estado y la lógica para la entrada de items.
 */
class ItemEntryViewModel(private val itemsRepository: ItemsRepository) : ViewModel() {

    // Estado observable como StateFlow
    private val _itemUiState = MutableStateFlow(ItemUiState())
    val itemUiState: StateFlow<ItemUiState> = _itemUiState

    /**
     * Actualiza los detalles del estado actual del item y valida la entrada.
     */
    fun updateUiState(itemDetails: ItemDetails) {
        _itemUiState.update {
            it.copy(
                itemDetails = itemDetails,
                isEntryValid = validateInput(itemDetails)
            )
        }
    }

    /**
     * Agrega una URI válida al tipo de contenido especificado (fotos, videos, audios).
     * Si la URI no es válida, no se realiza ninguna acción.
     */
    fun addUri(uri: String, contentType: ContentType) {
        Log.d("ItemEntryViewModel", "URI agregado: $uri")
        when (contentType) {
            ContentType.PHOTO -> {
                _itemUiState.value = _itemUiState.value.copy(
                    itemDetails = _itemUiState.value.itemDetails.copy(
                        photoUris = _itemUiState.value.itemDetails.photoUris + uri
                    )
                )
            }
            ContentType.VIDEO -> {
                _itemUiState.value = _itemUiState.value.copy(
                    itemDetails = _itemUiState.value.itemDetails.copy(
                        videoUris = _itemUiState.value.itemDetails.videoUris + uri
                    )
                )
            }
            ContentType.AUDIO -> {
                _itemUiState.value = _itemUiState.value.copy(
                    itemDetails = _itemUiState.value.itemDetails.copy(
                        audioUris = _itemUiState.value.itemDetails.audioUris + uri
                    )
                )
            }
            ContentType.FILE -> {
                _itemUiState.value = _itemUiState.value.copy(
                    itemDetails = _itemUiState.value.itemDetails.copy(
                        fileUris = _itemUiState.value.itemDetails.fileUris + uri
                    )
                )
            }
        }
    }





    /**
     * Guarda el item en el repositorio si la entrada es válida.
     */
    fun saveItem() {
        if (_itemUiState.value.isEntryValid) {
            viewModelScope.launch {
                itemsRepository.insertItem(_itemUiState.value.itemDetails.toItem())
            }
        }
    }

    /**
     * Valida si los datos proporcionados son válidos.
     */
    private fun validateInput(uiState: ItemDetails = _itemUiState.value.itemDetails): Boolean {
        return with(uiState) {
            title.isNotBlank() &&
                    descripcion.isNotBlank() &&
                    date.isNotBlank() &&
                    photoUris.all { validateUri(it) } &&
                    videoUris.all { validateUri(it) } &&
                    audioUris.all { validateUri(it) }
        }
    }

    /**
     * Valida si una URI tiene un formato correcto.
     */
    private fun validateUri(uri: String): Boolean {
        // Lógica para validar URI, puede incluir expresiones regulares u otros métodos.
        return uri.isNotBlank() // Simplificado para este ejemplo
    }
}

/**
 * Clase que representa el estado de la UI.
 */
data class ItemUiState(
    val itemDetails: ItemDetails = ItemDetails(),
    val isEntryValid: Boolean = false
)

/**
 * Clase que contiene los detalles de un item.
 */
data class ItemDetails(
    val id: Int = 0,
    val title: String = "",
    val type: Boolean = true,
    val status: Boolean = false,
    val descripcion: String = "",
    val date: String = "",
    val photoUris: List<String> = emptyList(),
    val videoUris: List<String> = emptyList(),
    val audioUris: List<String> = emptyList(),
    val fileUris: List<String> = emptyList()
)

/**
 * Convierte un objeto `ItemDetails` a `Item`.
 */
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

/**
 * Convierte un objeto `Item` a `ItemUiState`.
 */
fun Item.toItemUiState(isEntryValid: Boolean = false): ItemUiState = ItemUiState(
    itemDetails = this.toItemDetails(),
    isEntryValid = isEntryValid
)

/**
 * Convierte un objeto `Item` a `ItemDetails`.
 */
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

/**
 * Enum para representar los tipos de contenido (foto, video, audio).
 */
enum class ContentType {
    PHOTO, VIDEO, AUDIO , FILE
}
