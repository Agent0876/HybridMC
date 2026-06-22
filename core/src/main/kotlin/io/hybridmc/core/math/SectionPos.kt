package io.hybridmc.core.math

/**
 * An immutable 16x16x16 chunk section coordinate (X, Y, Z) packed into a 64-bit Long.
 *
 * Packing format (Java 1.14+ standard):
 * - X: 22 bits
 * - Z: 22 bits
 * - Y: 20 bits
 */
@JvmInline
public value class SectionPos(
    public val packed: Long,
) {
    public val x: Int
        get() = (packed shr X_SHIFT).toInt()

    public val y: Int
        get() = (packed shl (64 - Y_BITS) shr (64 - Y_BITS)).toInt()

    public val z: Int
        get() = (packed shl (64 - Z_SHIFT - Z_BITS) shr (64 - Z_BITS)).toInt()

    public fun getMinBlockX(): Int = x shl 4

    public fun getMinBlockY(): Int = y shl 4

    public fun getMinBlockZ(): Int = z shl 4

    override fun toString(): String = "SectionPos(x=$x, y=$y, z=$z)"

    public companion object {
        private const val X_BITS = 22
        private const val Z_BITS = 22
        private const val Y_BITS = 20

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
        ): SectionPos {
            val packed =
                ((x.toLong() and X_MASK) shl X_SHIFT) or
                    ((y.toLong() and Y_MASK) shl Y_SHIFT) or
                    ((z.toLong() and Z_MASK) shl Z_SHIFT)
            return SectionPos(packed)
        }

        public fun fromBlockPos(pos: BlockPos): SectionPos = of(pos.x shr 4, pos.y shr 4, pos.z shr 4)

        public fun fromChunkPos(
            chunkPos: ChunkPos,
            sectionY: Int,
        ): SectionPos = of(chunkPos.x, sectionY, chunkPos.z)
    }
}
