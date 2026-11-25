package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.game.cuppong.ThrowInput
import com.mehmetbozkurt.clink.game.spinthebottle.SpinAction
import com.mehmetbozkurt.clink.game.truthordare.TodAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What a member sends to the host (`lobbies/{id}/actions`): a game action, or Spin the Bottle's typed question/answer. */
@Serializable
sealed interface OnlineAction {
    @Serializable @SerialName("tod")
    data class Tod(val action: TodAction) : OnlineAction

    @Serializable @SerialName("spin")
    data class Spin(val action: SpinAction) : OnlineAction

    /** Spec §7.2: online, the asker types the question; it shows as a large card on every phone. */
    @Serializable @SerialName("ask")
    data class AskQuestion(val text: String) : OnlineAction

    /** The answerer's typed answer; answering ends the round like "Answered" does locally. */
    @Serializable @SerialName("answer")
    data class AnswerQuestion(val text: String) : OnlineAction

    /** Cup Pong: only the flick input is sent; the host computes the result (spec §7.3). */
    @Serializable @SerialName("throw")
    data class Throw(val input: ThrowInput) : OnlineAction
}

// "@type", not "type": TodPhase.ShowingCard already has a property called type.
internal val OnlineJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "@type"
}

fun encodeAction(action: OnlineAction): String = OnlineJson.encodeToString(OnlineAction.serializer(), action)

fun decodeAction(json: String): OnlineAction = OnlineJson.decodeFromString(OnlineAction.serializer(), json)
