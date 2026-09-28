package com.laul.lauwell.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MeasurementEntity::class], version = 1, exportSchema = true)
abstract class LauWellDatabase : RoomDatabase() {
    abstract fun measurements(): MeasurementDao

    companion object {
        fun create(context: Context): LauWellDatabase =
            Room.databaseBuilder(context, LauWellDatabase::class.java, "lauwell.db").build()
    }
}
