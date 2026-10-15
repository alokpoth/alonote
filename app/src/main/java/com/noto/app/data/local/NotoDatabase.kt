package com.noto.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.noto.app.data.local.dao.NoteDao
import com.noto.app.data.local.entity.Note

/**
 * Main Room Database for Noto.
 * Stores all note records locally on the device with zero cloud connectivity.
 */
@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = true
)
abstract class NotoDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private const val DATABASE_NAME = "noto_notes.db"

        @Volatile
        private var INSTANCE: NotoDatabase? = null

        fun getInstance(context: Context): NotoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotoDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
