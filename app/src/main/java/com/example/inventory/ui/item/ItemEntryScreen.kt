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

import ItemDetails
import ItemEntryViewModel
import ItemUiState
import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ParseException
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.InventoryTopAppBar
import com.example.inventory.R
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.InventoryTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File


object ItemEntryDestination : NavigationDestination {
    override val route = "item_entry"
    override val titleRes = R.string.item_entry_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEntryScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateBack: Boolean = true,
    viewModel: ItemEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var audioFilePath by remember { mutableStateOf("") }

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Si el permiso es concedido, intentar grabar
                if (isRecording) {
                    audioFilePath = startRecording(context).also {
                        recorder = MediaRecorder().apply {
                            try {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                setOutputFile(it)
                                prepare()
                                start()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error al iniciar la grabación", Toast.LENGTH_SHORT).show()
                            }
                        }
                        isRecording = true
                    }
                }
            } else {
                Toast.makeText(context, "Permiso denegado para grabar audio", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val itemUiState = viewModel.itemUiState.collectAsState().value

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.forEach { uri ->
            viewModel.addUri(uri.toString(), ContentType.PHOTO)
        }
    }

    // Launcher para capturar fotos desde la cámara
    val photoCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToFile(context, it)
            viewModel.addUri(uri.toString(), ContentType.PHOTO)
        }
    }
    // Definir videoFile en el alcance adecuado
    val videoFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
        "video_${System.currentTimeMillis()}.mp4"
    )
    // Launcher para grabar video
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            // Si la grabación fue exitosa, obtener el video grabado y agregarlo
            val videoUri = FileProvider.getUriForFile(
                context,
                "com.example.inventory.provider",
                videoFile
            )
            viewModel.addUri(videoUri.toString(), ContentType.VIDEO)
            Toast.makeText(context, "Video guardado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al grabar el video", Toast.LENGTH_SHORT).show()
        }
    }


    val videoUri = FileProvider.getUriForFile(
        context,
        "com.example.inventory.provider",
        videoFile
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            InventoryTopAppBar(
                title = stringResource(ItemEntryDestination.titleRes),
                canNavigateBack = canNavigateBack,
                navigateUp = onNavigateUp
            )
        },
        bottomBar = {
            BottomActionButtons(
                onAddPhotoClick = { imageLauncher.launch("image/*") },
                onAddPhotoFromCameraClick = {
                    // Solicitar permiso para usar la cámara
                    val cameraPermissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                        photoCameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onAddVideoClick = {
                    // Solicitar permisos de grabación de video si no están concedidos
                    val videoPermissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    val storagePermissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    if (videoPermissionCheck == PackageManager.PERMISSION_GRANTED && storagePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                        videoLauncher.launch(videoUri)
                    } else {
                        // Solicitar permisos
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                },
                onRecordAudioClick = {
                    if (isRecording) {
                        try {
                            recorder?.apply {
                                stop()
                                reset()
                                release()
                            }
                            recorder = null
                            viewModel.addUri(audioFilePath, ContentType.AUDIO)
                            isRecording = false
                            Toast.makeText(context, "Grabación guardada", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error al detener la grabación", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val audioPermissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (audioPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                            try {
                                audioFilePath = startRecording(context).also {
                                    recorder = MediaRecorder().apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                        setOutputFile(it)
                                        prepare()
                                        start()
                                    }
                                    isRecording = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error al iniciar la grabación", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                isRecording = isRecording
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            ItemEntryBody(
                itemUiState = itemUiState,
                onItemValueChange = viewModel::updateUiState,
                onSaveClick = {
                    coroutineScope.launch {
                        try {
                            viewModel.saveItem()
                            navigateBack()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error al guardar el ítem", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            MultimediaSection(
                title = stringResource(R.string.photo),
                uris = itemUiState.itemDetails.photoUris
            )

            MultimediaSection(
                title = stringResource(R.string.video),
                uris = itemUiState.itemDetails.videoUris
            )

            MultimediaSection(
                title = stringResource(R.string.audio),
                uris = itemUiState.itemDetails.audioUris
            )
        }
    }
}


fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}


private fun startRecording(context: android.content.Context): String {
    val file = File(context.filesDir, "recording_${System.currentTimeMillis()}.3gp")
    return file.absolutePath
}


@Composable
fun MultimediaSection(title: String, uris: List<String>) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium))
    )
    if (uris.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small)),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium))
        ) {
            items(uris) { uri ->
                if (title == stringResource(R.string.audio)) {
                    AudioItem(uri = uri)
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun AudioItem(uri: String) {
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember {
        MediaPlayer()
    }

    IconButton(
        onClick = {
            if (isPlaying) {
                mediaPlayer.stop()
                isPlaying = false
            } else {
                try {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(uri)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    isPlaying = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        },
        modifier = Modifier
            .size(80.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause Audio" else "Play Audio"
        )
    }


    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }
}



@Composable
fun ItemEntryBody(
    itemUiState: ItemUiState,
    onItemValueChange: (ItemDetails) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(dimensionResource(id = R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_large))
    ) {
        ItemInputForm(
            itemDetails = itemUiState.itemDetails,
            onValueChange = onItemValueChange,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSaveClick,
            enabled = itemUiState.isEntryValid,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.save_action))
        }
    }
}


private fun startRecording(recorder: MediaRecorder, context: android.content.Context): String {
    val file = File(context.filesDir, "recording_${System.currentTimeMillis()}.3gp")
    val audioFilePath = file.absolutePath
    recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        setOutputFile(audioFilePath)
        prepare()
        start()
    }
    return audioFilePath
}

@Composable
fun BottomActionButtons(
    onAddPhotoClick: () -> Unit,
    onAddVideoClick: () -> Unit,
    onRecordAudioClick: () -> Unit,
    onAddPhotoFromCameraClick: () -> Unit,
    isRecording: Boolean
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onAddPhotoClick) {
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = stringResource(R.string.add_photo),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onAddPhotoFromCameraClick) {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = stringResource(R.string.add_photo),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onAddVideoClick) {
                Icon(
                    Icons.Filled.Camera,
                    contentDescription = stringResource(R.string.add_video),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onRecordAudioClick) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.record_audio),
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}


private fun handleAudioRecording(
    context: android.content.Context,
    recorder: MediaRecorder,
    isRecording: Boolean,
    setIsRecording: (Boolean) -> Unit,
    setAudioFilePath: (String) -> Unit
) {
    if (isRecording) {
        try {
            recorder.stop()
            recorder.reset()
            setIsRecording(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val filePath = startRecording(recorder, context)
            setAudioFilePath(filePath)
            setIsRecording(true)
        } else {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }
}


@Composable
fun ItemInputForm(
    itemDetails: ItemDetails,
    modifier: Modifier = Modifier,
    onValueChange: (ItemDetails) -> Unit = {},
    enabled: Boolean = true
) {
    val dateText = remember { mutableStateOf(itemDetails.date?.toString() ?: "") }
    val calendar = Calendar.getInstance()
    val context = LocalContext.current

    val isLargeScreen = LocalContext.current.resources.configuration.screenWidthDp >= 600

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        if (isLargeScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
            ) {
                OutlinedTextField(
                    value = itemDetails.title,
                    onValueChange = { onValueChange(itemDetails.copy(title = it)) },
                    label = { Text(stringResource(R.string.title)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    singleLine = true
                )
                OutlinedTextField(
                    value = itemDetails.descripcion,
                    onValueChange = { onValueChange(itemDetails.copy(descripcion = it)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = { Text(stringResource(R.string.description)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    maxLines = 10
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
            ) {
                OutlinedTextField(
                    value = dateText.value,
                    onValueChange = { newValue ->
                        dateText.value = newValue
                    },
                    label = { Text(stringResource(R.string.save_action)) },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)
                                    dateText.value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
                                    onValueChange(itemDetails.copy(date = calendar.time.toString()))
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date"
                            )
                        }
                    }
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
            ) {
                OutlinedTextField(
                    value = itemDetails.title,
                    onValueChange = { onValueChange(itemDetails.copy(title = it)) },
                    label = { Text(stringResource(R.string.title)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true
                )
                OutlinedTextField(
                    value = itemDetails.descripcion,
                    onValueChange = { onValueChange(itemDetails.copy(descripcion = it)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = { Text(stringResource(R.string.description)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    maxLines = 10
                )
                OutlinedTextField(
                    value = dateText.value,
                    onValueChange = { newValue ->
                        dateText.value = newValue
                    },
                    label = { Text(stringResource(R.string.save_action)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)
                                    dateText.value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
                                    onValueChange(itemDetails.copy(date = calendar.time.toString()))
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date"
                            )
                        }
                    }
                )
            }
        }
    }
}