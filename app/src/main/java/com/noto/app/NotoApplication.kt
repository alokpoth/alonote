package com.noto.app

import android.app.Application
import com.noto.app.data.local.NotoDatabase
import com.noto.app.data.repository.NoteRepository
import com.noto.app.data.repository.NoteRepositoryImpl

/**
 * Custom Application class for Noto.
 * Initializes the local Room database and exposes the repository.
 */
class NotoApplication : Application() {

    lateinit var database: NotoDatabase
        private set

    lateinit var noteRepository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = NotoDatabase.getInstance(this)
        noteRepository = NoteRepositoryImpl(database.noteDao())
    }
}
