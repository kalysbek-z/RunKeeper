package com.example.runkeeper.database

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class Run(
    var image: Bitmap? = null,
    var timestamp: Long = 0L, //date of run
    var speed: Float = 0f,
    var distance: Float = 0f,
    var time: Long = 0L
) {
    @PrimaryKey(autoGenerate = false)
    var id: Int? = null
}