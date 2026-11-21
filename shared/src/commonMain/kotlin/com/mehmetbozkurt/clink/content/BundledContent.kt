package com.mehmetbozkurt.clink.content

import com.mehmetbozkurt.clink.resources.Res

/** The baseline content shipped inside the app, so the first local game works offline. */
suspend fun loadBundledPacks(): List<Pack> =
    ContentParser.parse(Res.readBytes("files/content/baseline.json").decodeToString())
