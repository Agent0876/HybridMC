package io.hybridmc.core.math

import kotlin.math.sqrt

/**
 * An immutable 3D vector using 32-bit integer coordinates.
 */
public data class Vec3i(
    public val x: Int,
    public val y: Int,
    public val z: Int,
) {
    public operator fun plus(other: Vec3i): Vec3i = Vec3i(x + other.x, y + other.y, z + other.z)

    public operator fun minus(other: Vec3i): Vec3i = Vec3i(x - other.x, y - other.y, z - other.z)

    public operator fun times(scalar: Int): Vec3i = Vec3i(x * scalar, y * scalar, z * scalar)

    public operator fun unaryMinus(): Vec3i = Vec3i(-x, -y, -z)

    public fun distanceToSqr(other: Vec3i): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }

    public fun distanceTo(other: Vec3i): Double = sqrt(distanceToSqr(other))

    public companion object {
        public val ZERO: Vec3i = Vec3i(0, 0, 0)
    }
}
