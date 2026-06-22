package io.hybridmc.core.math

/**
 * An immutable chunk coordinate (X, Z) packed into a 64-bit Long.
 *
 * Packing format:
 * - X: 32 bits (higher half)
 * - Z: 32 bits (lower half)
 */
@JvmInline
public value class ChunkPos(
    public val packed: Long,
) {
    public val x: Int
        get() = (packed shr 32).toInt()

    public val z: Int
        get() = packed.toInt()

    public fun getMinBlockX(): Int = x shl 4

    public fun getMinBlockZ(): Int = z shl 4

    public fun getMaxBlockX(): Int = (x shl 4) or 15

    public fun getMaxBlockZ(): Int = (z shl 4) or 15

    override fun toString(): String = "ChunkPos(x=$x, z=$z)"

    public companion object {
        public fun of(
            x: Int,
            z: Int,
        ): ChunkPos = ChunkPos((x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL))

        /** Gets the chunk pos from a block coordinate. */
        public fun fromBlockPos(pos: BlockPos): ChunkPos = of(pos.x shr 4, pos.z shr 4)
    }
}
