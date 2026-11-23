package com.mehmetbozkurt.clink.online

import kotlin.random.Random

/** The 6-character room code (spec §5.2), the fallback when an invite link doesn't open the app. */
object RoomCode {
    const val LENGTH = 6

    /** Letters and digits people can read aloud without mixing up: no 0/O, 1/I/L. */
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(random: Random): String = (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    /** What someone typed → a valid code: case, spaces and dashes don't matter; anything else is null. */
    fun normalize(input: String): String? {
        val code = input.uppercase().filterNot { it.isWhitespace() || it == '-' }
        return code.takeIf { it.length == LENGTH && it.all { c -> c in ALPHABET } }
    }
}

private const val INVITE_PREFIX = "https://clink-game.com/join/"

fun inviteLink(code: String): String = INVITE_PREFIX + code

/** The code in an invite link, or null for any other link. */
fun codeFromLink(url: String): String? {
    if (!url.startsWith(INVITE_PREFIX, ignoreCase = true)) return null
    return RoomCode.normalize(url.substring(INVITE_PREFIX.length).substringBefore('?').substringBefore('/'))
}
