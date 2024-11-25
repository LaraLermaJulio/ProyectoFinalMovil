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

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.InventoryTopAppBar
import com.example.inventory.R
import com.example.inventory.data.Item
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ItemEditDestination : NavigationDestination {
    override val route = "item_edit"
    override val titleRes = R.string.edit_item_title
    const val itemIdArg = "itemId"
    val routeWithArgs = "$route/{$itemIdArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var audioFilePath by remember { mutableStateOf("") }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.forEach { uri ->
            coroutineScope.launch {
                viewModel.updateItem(uri.toString(), ContentType.PHOTO)
            }
        }
    }

    // Launcher para capturar fotos desde la cámara
    val photoCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToFile(context, it)
            coroutineScope.launch {
                viewModel.updateItem(uri.toString(), ContentType.PHOTO)
            }
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
            val videoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                videoFile
            )
            coroutineScope.launch {
                viewModel.updateItem(videoUri.toString(), ContentType.VIDEO)
            }

            Toast.makeText(context, "Video guardado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al grabar el video", Toast.LENGTH_SHORT).show()
        }
    }

    // Definir un launcher específico para permisos múltiples de video (cámara y audio)
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val cameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?: false
            val audioPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraPermissionGranted && audioPermissionGranted) {
                // Permisos otorgados, proceder con la grabación del video
                videoLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile))
            } else {
                Toast.makeText(context, "Permisos denegados para grabar video", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            InventoryTopAppBar(
                title = stringResource(ItemEditDestination.titleRes),
                canNavigateBack = true,
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
                            coroutineScope.launch {
                                viewModel.updateItem(audioFilePath, ContentType.AUDIO)
                            }

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
        },
        modifier = modifier
    ) { innerPadding ->
        ItemEntryBody(
            itemUiState = viewModel.itemUiState,
            onItemValueChange = viewModel::updateUiState,
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.updateItem()
                    navigateBack()
                }
            },
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
                .verticalScroll(rememberScrollState())
        )
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

private fun handleRecording(
    isRecording: Boolean,
    recorder: MediaRecorder,
    context: android.content.Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (isRecording) {
        onStop()
    } else {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            onStart()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
