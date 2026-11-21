package com.mehmetbozkurt.clink

import com.mehmetbozkurt.clink.account.GoogleIdTokenProvider
import com.mehmetbozkurt.clink.content.ContentCache

/** What only the platform can provide (file storage, the Google account picker), passed in by MainActivity. */
class Platform(val contentCache: ContentCache, val googleIdTokens: GoogleIdTokenProvider)
