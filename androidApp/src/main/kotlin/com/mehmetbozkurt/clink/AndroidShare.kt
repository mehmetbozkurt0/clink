package com.mehmetbozkurt.clink

import android.content.Context
import android.content.Intent

/** Opens Android's share sheet with [text] (the lobby's invite message). */
fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null))
}
