package com.mehmetbozkurt.clink.content

import com.mehmetbozkurt.clink.resources.Res

/** Loads the baseline content shipped inside the app, so the first local game works offline. */
suspend fun loadBundledContent(): ContentLibrary =
    ContentLibrary(ContentParser.parse(Res.readBytes("files/content/baseline.json").decodeToString()))
