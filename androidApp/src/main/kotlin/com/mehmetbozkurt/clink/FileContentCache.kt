package com.mehmetbozkurt.clink

import android.content.Context
import com.mehmetbozkurt.clink.content.ContentCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Downloaded content in the app's private files; written to a temp file first, so a crash never leaves half a file. */
class FileContentCache(context: Context) : ContentCache {
    private val file = File(context.filesDir, "content-cache.json")

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
