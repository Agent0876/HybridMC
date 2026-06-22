package io.hybridmc.core.math

import kotlin.math.sqrt

/**
 * An immutable 3D vector using double-precision floating-point coordinates.
 */
public data class Vec3d(
    public val x: Double,
    public val y: Double,
    public val z: Double,
) {
    public operator fun plus(other: Vec3d): Vec3d = Vec3d(x + other.x, y + other.y, z + other.z)

    public operator fun minus(other: Vec3d): Vec3d = Vec3d(x - other.x, y - other.y, z - other.z)

    public operator fun times(scalar: Double): Vec3d = Vec3d(x * scalar, y * scalar, z * scalar)

    public operator fun unaryMinus(): Vec3d = Vec3d(-x, -y, -z)

    public fun lengthSquared(): Double = x * x + y * y + z * z

    public fun length(): Double = sqrt(lengthSquared())

    public fun distanceToSqr(other: Vec3d): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }

    public fun distanceTo(other: Vec3d): Double = sqrt(distanceToSqr(other))

    public fun normalize(): Vec3d {
        val len = length()
        if (len < 1.0E-4) return ZERO
        return Vec3d(x / len, y / len, z / len)
    }

    public fun dotProduct(other: Vec3d): Double = x * other.x + y * other.y + z * other.z

    public fun crossProduct(other: Vec3d): Vec3d =
        Vec3d(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x,
        )

    public companion object {
        public val ZERO: Vec3d = Vec3d(0.0, 0.0, 0.0)
    }
}
