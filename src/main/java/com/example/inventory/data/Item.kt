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

package com.example.inventory.data

import android.widget.TimePicker
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Entity data class represents a single row in the database.
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val descripcion: String,
    val type: Boolean, // 1 = task, 0 = note
    val status: Boolean, // 1 = finished, 0 = not finished
    val date: String,
    val photoUri: String? = null,
    val videoUri: String? = null,
    val audioUri: String? = null
)
