package com.mehmetbozkurt.clink.content

class ContentLibrary(private val packs: List<Pack>) {
    private val cardsById: Map<String, Card> = packs.flatMap { it.cards }.associateBy { it.id }

    /** All card ids of [type] and [level] across packs, in file order. */
    fun cardIds(type: CardType, level: Level): List<String> =
        packs.filter { it.type == type && it.level == level }.flatMap { pack -> pack.cards.map { it.id } }

    /** Text in [language], falling back to English, then to any available text. */
    fun text(cardId: String, language: String): String {
        val card = requireNotNull(cardsById[cardId]) { "Unknown card id: $cardId" }
        return card.text[language] ?: card.text["en"] ?: card.text.values.first()
    }
}
