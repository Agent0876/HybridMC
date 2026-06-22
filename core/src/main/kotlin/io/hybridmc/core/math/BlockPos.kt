package io.hybridmc.core.math

/**
 * An immutable absolute block coordinate packed into a 64-bit Long.
 *
 * Packing format (Java 1.13- standard):
 * - X: 28 bits
 * - Z: 28 bits
 * - Y: 8 bits
 */
@JvmInline
public value class BlockPos(
    public val packed: Long,
) {
    public val x: Int
        get() = (packed shr X_SHIFT).toInt()

    public val y: Int
        get() = (packed shl (64 - Y_BITS) shr (64 - Y_BITS)).toInt()

    public val z: Int
        get() = (packed shl (64 - Z_SHIFT - Z_BITS) shr (64 - Z_BITS)).toInt()

    public fun add(
        dx: Int,
        dy: Int,
        dz: Int,
    ): BlockPos = of(x + dx, y + dy, z + dz)

    public operator fun plus(vec: Vec3i): BlockPos = add(vec.x, vec.y, vec.z)

    public operator fun minus(vec: Vec3i): BlockPos = add(-vec.x, -vec.y, -vec.z)

    public fun relative(
        direction: Direction,
        distance: Int = 1,
    ): BlockPos = add(direction.stepX * distance, direction.stepY * distance, direction.stepZ * distance)

    public fun toVec3d(): Vec3d = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())

    override fun toString(): String = "BlockPos(x=$x, y=$y, z=$z)"

    public companion object {
        private const val X_BITS = 28
        private const val Z_BITS = 28
        private const val Y_BITS = 8

        private const val Y_SHIFT = 0
        private const val Z_SHIFT = Y_BITS
        private const val X_SHIFT = Y_BITS + Z_BITS

        private const val X_MASK = (1L shl X_BITS) - 1L
        private const val Y_MASK = (1L shl Y_BITS) - 1L
        private const val Z_MASK = (1L shl Z_BITS) - 1L

        public fun of(
            x: Int,
            y: Int,
            z: Int,
        ): BlockPos {
            val packed =
                ((x.toLong() and X_MASK) shl X_SHIFT) or
                    ((y.toLong() and Y_MASK) shl Y_SHIFT) or
                    ((z.toLong() and Z_MASK) shl Z_SHIFT)
            return BlockPos(packed)
        }

        public val ZERO: BlockPos = of(0, 0, 0)
    }
}
