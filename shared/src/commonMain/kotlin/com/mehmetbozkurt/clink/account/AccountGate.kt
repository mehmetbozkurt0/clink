package com.mehmetbozkurt.clink.account

/** Where Play leads (spec §4: the host must have an account, in both modes). */
sealed interface PlayGate {
    data object SignIn : PlayGate
    data object SetUpProfile : PlayGate
    data object Play : PlayGate
}

/** Anonymous guests can't host, so they sign in like a signed-out user; a missing profile is set up first. */
fun playGate(user: AuthUser?, profile: Profile?): PlayGate = when {
    user == null || user.isAnonymous -> PlayGate.SignIn
    profile == null -> PlayGate.SetUpProfile
    else -> PlayGate.Play
}
