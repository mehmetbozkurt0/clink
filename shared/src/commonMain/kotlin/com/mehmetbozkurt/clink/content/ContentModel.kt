package com.mehmetbozkurt.clink.content

import kotlinx.serialization.Serializable

@Serializable
enum class Level { CASUAL, HARD, SPICY }

@Serializable
enum class CardType { TRUTH, DARE, PENALTY }

@Serializable
enum class Access { FREE }

/** One card. [text] maps a language code ("tr", "en") to the card's text in that language. */
@Serializable
data class Card(val id: String, val text: Map<String, String>)

@Serializable
data class Pack(
    val id: String,
    val type: CardType,
    val level: Level,
    val access: Access,
    val cards: List<Card>,
)
