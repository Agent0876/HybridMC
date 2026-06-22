package io.hybridmc.core.math

/**
 * An immutable Euler rotation (yaw and pitch) packed into a 64-bit Long.
 *
 * Packing format:
 * - Yaw (Float): 32 bits (higher half)
 * - Pitch (Float): 32 bits (lower half)
 */
@JvmInline
public value class Rotation(
    public val packed: Long,
) {
    public val yaw: Float
        get() = Float.fromBits((packed shr 32).toInt())

    public val pitch: Float
        get() = Float.fromBits(packed.toInt())

    override fun toString(): String = "Rotation(yaw=$yaw, pitch=$pitch)"

    public companion object {
        public fun of(
            yaw: Float,
            pitch: Float,
        ): Rotation {
            val yawBits = yaw.toRawBits().toLong()
            val pitchBits = pitch.toRawBits().toLong() and 0xFFFFFFFFL
            return Rotation((yawBits shl 32) or pitchBits)
        }

        public val ZERO: Rotation = of(0f, 0f)
    }
}
