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
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaRecorder
import android.net.ParseException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    val recorder = remember { MediaRecorder() }
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var audioFilePath by remember { mutableStateOf("") }

    // Launchers para imágenes y videos
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let { viewModel.updateUiState(viewModel.itemUiState.itemDetails.copy(photoUris = it.map { it.toString() })) }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let { viewModel.updateUiState(viewModel.itemUiState.itemDetails.copy(videoUris = it.map { it.toString() })) }
    }

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
                onAddVideoClick = { videoLauncher.launch("video/*") },
                onRecordAudioClick = {
                    handleAudioRecording(
                        context = context,
                        recorder = recorder,
                        isRecording = isRecording,
                        setIsRecording = { isRecording = it },
                        setAudioFilePath = { filePath ->
                            audioFilePath = filePath
                            viewModel.updateUiState(
                                viewModel.itemUiState.itemDetails.copy(audioUris = listOf(filePath))
                            )
                        }
                    )
                },
                isRecording = isRecording
            )
        }
    ) { innerPadding ->
        ItemEntryBody(
            itemUiState = viewModel.itemUiState,
            onItemValueChange = viewModel::updateUiState,
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.saveItem()
                    navigateBack()
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        )
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
    isRecording: Boolean
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onAddPhotoClick) {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = stringResource(R.string.add_photo),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onAddVideoClick) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = stringResource(R.string.add_video),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onRecordAudioClick) {
                Icon(
                    imageVector = Icons.Filled.Mic,
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
