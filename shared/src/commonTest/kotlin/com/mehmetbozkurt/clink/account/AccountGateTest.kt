package com.mehmetbozkurt.clink.account

import kotlin.test.Test
import kotlin.test.assertEquals

class AccountGateTest {
    @Test
    fun signedOutMustSignIn() = assertEquals(PlayGate.SignIn, playGate(null, null))

    @Test
    fun anonymousCannotHost() = assertEquals(PlayGate.SignIn, playGate(AuthUser("u", isAnonymous = true, googleName = null), null))

    @Test
    fun signedInWithoutProfileSetsUp() =
        assertEquals(PlayGate.SetUpProfile, playGate(AuthUser("u", isAnonymous = false, googleName = "M"), null))

    @Test
    fun signedInWithProfilePlays() =
        assertEquals(PlayGate.Play, playGate(AuthUser("u", isAnonymous = false, googleName = "M"), Profile("M", Avatars.ids[0])))
}
