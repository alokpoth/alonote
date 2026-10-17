package com.noto.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.noto.app.data.local.dao.NoteDao
import com.noto.app.data.local.entity.Note

/**
 * Main Room Database for Noto.
 * Stores all note records locally on the device with zero cloud connectivity.
 */
@Database(
    entities = [Note::class],
    version = 4,
    exportSchema = true
)
abstract class NotoDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private const val DATABASE_NAME = "noto_notes.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN imagePaths TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN audioPaths TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderTime INTEGER DEFAULT NULL")
            }
        }

        @Volatile
        private var INSTANCE: NotoDatabase? = null

        fun getInstance(context: Context): NotoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotoDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
