package com.mehmetbozkurt.clink

import android.content.Context
import com.mehmetbozkurt.clink.prefs.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local preferences in the no-backup folder, so Auto Backup cannot restore "tour seen" flags after a reinstall;
 * written to a temp file first, so a crash never leaves half a file.
 */
class FileLocalStore(context: Context) : LocalStore {
    private val file = File(context.noBackupFilesDir, "local-prefs.json")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (file.exists()) file.readText() else null
    }

    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
        Unit
    }
}
