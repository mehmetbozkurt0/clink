package com.mehmetbozkurt.clink.game.cuppong

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A throw aimed at [target] from the rest spot: the yaw points at it (shifted [sideOffset] cm to the right), and the
 * speed is the one whose arc comes back down through rim height at the target's distance, times [speedFactor].
 */
fun aimAt(target: Vec3, speedFactor: Float = 1f, sideOffset: Float = 0f): ThrowInput {
    val rest = CupPongPhysics.REST
    val dx = target.x + sideOffset - rest.x
    val dy = target.y - rest.y
    val yaw = atan2(dx, dy) * 180f / PI.toFloat()
    return ThrowInput(yaw, speedToReach(hypot(dx, dy)) * speedFactor)
}

private fun speedToReach(distance: Float): Float {
    val pitch = CupPongPhysics.PITCH_DEGREES * PI.toFloat() / 180f
    val rise = CupPongPhysics.CUP_HEIGHT - CupPongPhysics.REST.z
    var low = 100f
    var high = 1000f
    repeat(50) {
        val v = (low + high) / 2f
        val vz = v * sin(pitch)
        val time = (vz + sqrt(vz * vz - 2f * CupPongPhysics.GRAVITY * rise)) / CupPongPhysics.GRAVITY
        if (v * cos(pitch) * time < distance) low = v else high = v
    }
    return (low + high) / 2f
}
