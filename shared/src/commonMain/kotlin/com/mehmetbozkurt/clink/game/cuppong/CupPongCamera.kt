package com.mehmetbozkurt.clink.game.cuppong

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/** A camera behind the thrower looking down the table (spec §5.4): projects world points onto a canvas of this size. */
class CupPongCamera(width: Float, height: Float) {
    private val eye = Vec3(0f, 40f, 85f)
    private val forward = (Vec3(0f, 215f, 0f) - eye).normalized()
    private val right = (forward cross Vec3(0f, 0f, 1f)).normalized()
    private val up = right cross forward
    // 2 × width (the approved framing); a short canvas (16:9 phones) shrinks it so the resting ball stays on screen.
    val focal = minOf(width * 2.0f, height * 1.2f)
    private val centerX = width / 2f
    private val centerY = height * 0.3f

    /** Screen x, screen y and depth; a world length L at depth d is L × [focal] / d pixels long. */
    fun project(p: Vec3): Triple<Float, Float, Float> {
        val d = p - eye
        val depth = d dot forward
        return Triple(centerX + focal * (d dot right) / depth, centerY - focal * (d dot up) / depth, depth)
    }

    /** Where the ray through a screen point meets the table plane (z = 0); null at or above the horizon. */
    fun onTable(screenX: Float, screenY: Float): Vec3? {
        val dir = forward + right * ((screenX - centerX) / focal) + up * ((centerY - screenY) / focal)
        if (dir.z >= -0.0001f) return null
        return eye + dir * (-eye.z / dir.z)
    }
}

/**
 * A flick → a throw (spec §5.3). The direction is the finger's movement across the table: the release point and where
 * the finger would be 0.06 s later are both cast onto the table. The flick speed (300…3000 dp/s) sets the launch speed.
 * Not a throw: too slow, moving down, or more than [CupPongPhysics.MAX_YAW] off straight.
 */
fun flickToThrow(
    camera: CupPongCamera,
    releaseX: Float,
    releaseY: Float,
    velocityX: Float,
    velocityY: Float,
    speedDpPerSecond: Float,
): ThrowInput? {
    if (velocityY >= 0f || speedDpPerSecond < MIN_FLICK) return null
    val from = camera.onTable(releaseX, releaseY) ?: return null
    // A very hard flick's look-ahead point can pass the horizon: look less far ahead until it lands on the table.
    val to = generateSequence(LOOK_AHEAD_SECONDS) { it / 2f }.take(5)
        .firstNotNullOfOrNull { camera.onTable(releaseX + velocityX * it, releaseY + velocityY * it) } ?: return null
    val yaw = atan2(to.x - from.x, to.y - from.y) * 180f / PI.toFloat()
    if (abs(yaw) > CupPongPhysics.MAX_YAW) return null
    val t = ((speedDpPerSecond - MIN_FLICK) / (MAX_FLICK - MIN_FLICK)).coerceIn(0f, 1f)
    return ThrowInput(yaw, CupPongPhysics.MIN_SPEED + t * (CupPongPhysics.MAX_SPEED - CupPongPhysics.MIN_SPEED))
}

private const val MIN_FLICK = 300f
private const val MAX_FLICK = 3000f
private const val LOOK_AHEAD_SECONDS = 0.06f
