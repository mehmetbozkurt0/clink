package com.mehmetbozkurt.clink

import com.mehmetbozkurt.clink.content.ContentCache

/** What only the platform can provide (file storage now; Google sign-in comes next), passed in by MainActivity. */
class Platform(val contentCache: ContentCache)
