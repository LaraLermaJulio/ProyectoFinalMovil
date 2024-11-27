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
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ParseException
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
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
import com.google.android.exoplayer2.ExoPlayer
import java.io.File
import com.google.android.exoplayer2.MediaItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ui.PlayerView
import com.example.inventory.ui.item.MultimediaSection
import java.util.Date


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


    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Permiso de notificaciones otorgado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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

    val videoFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
        "video_${System.currentTimeMillis()}.mp4"
    )

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            val videoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                videoFile
            )
            viewModel.addUri(videoUri.toString(), ContentType.VIDEO)
            Toast.makeText(context, "Video guardado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al grabar el video", Toast.LENGTH_SHORT).show()
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val cameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraPermissionGranted && audioPermissionGranted) {
                videoLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile))
            } else {
                Toast.makeText(context, "Permisos denegados para grabar video", Toast.LENGTH_SHORT).show()
            }
        }
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
                    val cameraPermissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                        photoCameraLauncher.launch(null)
                    } else {
                        videoPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                },
                onAddVideoClick = {
                    val cameraPermissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    val audioPermissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    )

                    if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED && audioPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                        videoLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile))
                    } else {
                        videoPermissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
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
                            videoPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
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

public fun startRecording(context: android.content.Context): String {
    val file = File(context.filesDir, "recording_${System.currentTimeMillis()}.3gp")
    return file.absolutePath
}



@Composable
fun ImageItem(uri: String) {

    var showFullScreenDialog by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .clickable { showFullScreenDialog = true }
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }


    if (showFullScreenDialog) {
        Dialog(
            onDismissRequest = {
                showFullScreenDialog = false
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )


                IconButton(
                    onClick = { showFullScreenDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Fullscreen Image",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
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
                when (title) {
                    stringResource(R.string.audio) -> {
                        AudioItem(uri = uri)
                    }
                    stringResource(R.string.video) -> {
                        VideoItem(uri = uri)
                    }
                    else -> {
                        ImageItem(uri = uri)
                    }
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



@Composable
fun VideoItem(uri: String) {
    val context = LocalContext.current
    val videoUri = Uri.parse(uri)


    var showFullScreenDialog by remember { mutableStateOf(false) }


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }


    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.matchParentSize()
        )


        IconButton(
            onClick = { showFullScreenDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(50))
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Expand Video",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }


    if (showFullScreenDialog) {
        Dialog(
            onDismissRequest = {
                showFullScreenDialog = false
                exoPlayer.pause()
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )


                IconButton(
                    onClick = {
                        showFullScreenDialog = false
                        exoPlayer.pause()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Fullscreen Video",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
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
    val alarms = remember { mutableStateListOf<Pair<Long, String>>() }
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
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)

                                    val timePickerDialog = TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            calendar.set(Calendar.MINUTE, minute)

                                            dateText.value = SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm",
                                                Locale.getDefault()
                                            ).format(calendar.time)
                                            onValueChange(itemDetails.copy(date = calendar.time.toString()))
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true // formato de 24 horas
                                    )
                                    timePickerDialog.show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            datePickerDialog.show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date and Time"
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

                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            calendar.set(Calendar.MINUTE, minute)
                                            calendar.set(Calendar.SECOND, 0)

                                            dateText.value = SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm:ss",
                                                Locale.getDefault()
                                            ).format(calendar.time)

                                            onValueChange(itemDetails.copy(date = calendar.time.toString()))

                                            // Configurar la alarma y agregarla a la lista
                                            setAlarm(context, calendar.timeInMillis, itemDetails.title)
                                            alarms.add(calendar.timeInMillis to itemDetails.title)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date and Time"
                            )
                        }
                    }
                )

                alarms.forEachIndexed { index, (time, title) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(time)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row {
                            // Botón para editar
                            IconButton(onClick = {
                                // Abre un diálogo para editar la alarma
                                editAlarm(context, alarms, index)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Alarm"
                                )
                            }

                            // Botón para eliminar
                            IconButton(onClick = {
                                alarms.removeAt(index)
                                cancelAlarm(context, title.hashCode())
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Alarm"
                                )
                            }
                        }
                    }
                }



            }

        }

    }

}

@SuppressLint("ScheduleExactAlarm")
fun setAlarm(context: Context, triggerAtMillis: Long, itemTitle: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        action = "com.example.SET_ALARM"
        putExtra("item_title", itemTitle)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        itemTitle.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Programar la alarma
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

    // Guardar alarma completa en SharedPreferences (fecha y hora en milisegundos)
    val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putLong(itemTitle, triggerAtMillis) // Guardamos con el título como clave
        apply()
    }
}

fun editAlarm(context: Context, alarms: MutableList<Pair<Long, String>>, index: Int) {
    val currentAlarm = alarms[index]
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentAlarm.first
    }

    // Mostrar DatePickerDialog
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Mostrar TimePickerDialog
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    // Actualizar la alarma en la lista
                    val newTime = calendar.timeInMillis
                    val title = currentAlarm.second
                    alarms[index] = newTime to title

                    // Cancelar la alarma anterior y programar la nueva
                    cancelAlarm(context, title.hashCode())
                    setAlarm(context, newTime, title)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun cancelAlarm(context: Context, alarmId: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        action = "com.example.SET_ALARM"
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarmId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)

    val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        remove(alarmId.toString())
        apply()
    }
}