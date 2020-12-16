package com.example.runkeeper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Run::class],
    version = 1
)
@TypeConverters(ImageConverter::class)
abstract class RunDatabase : RoomDatabase() {

    abstract fun getRunDao(): RunDao
}