package io.hybridmc.core.math

/**
 * An immutable 3D Axis-Aligned Bounding Box.
 */
public data class AABB(
    public val minX: Double,
    public val minY: Double,
    public val minZ: Double,
    public val maxX: Double,
    public val maxY: Double,
    public val maxZ: Double,
) {
    public constructor(min: Vec3d, max: Vec3d) : this(
        min.x,
        min.y,
        min.z,
        max.x,
        max.y,
        max.z,
    )

    public fun intersects(other: AABB): Boolean =
        this.minX < other.maxX && this.maxX > other.minX &&
            this.minY < other.maxY && this.maxY > other.minY &&
            this.minZ < other.maxZ && this.maxZ > other.minZ

    public fun contains(vec: Vec3d): Boolean =
        vec.x >= this.minX && vec.x < this.maxX &&
            vec.y >= this.minY && vec.y < this.maxY &&
            vec.z >= this.minZ && vec.z < this.maxZ

    public fun expand(
        x: Double,
        y: Double,
        z: Double,
    ): AABB {
        var nx0 = this.minX
        var ny0 = this.minY
        var nz0 = this.minZ
        var nx1 = this.maxX
        var ny1 = this.maxY
        var nz1 = this.maxZ

        if (x < 0.0) {
            nx0 += x
        } else if (x > 0.0) {
            nx1 += x
        }
        if (y < 0.0) {
            ny0 += y
        } else if (y > 0.0) {
            ny1 += y
        }
        if (z < 0.0) {
            nz0 += z
        } else if (z > 0.0) {
            nz1 += z
        }

        return AABB(nx0, ny0, nz0, nx1, ny1, nz1)
    }

    public fun grow(
        x: Double,
        y: Double,
        z: Double,
    ): AABB =
        AABB(
            minX - x,
            minY - y,
            minZ - z,
            maxX + x,
            maxY + y,
            maxZ + z,
        )

    public fun grow(value: Double): AABB = grow(value, value, value)

    public fun offset(
        x: Double,
        y: Double,
        z: Double,
    ): AABB =
        AABB(
            minX + x,
            minY + y,
            minZ + z,
            maxX + x,
            maxY + y,
            maxZ + z,
        )

    public fun offset(vec: Vec3d): AABB = offset(vec.x, vec.y, vec.z)

    public fun center(): Vec3d =
        Vec3d(
            minX + (maxX - minX) * 0.5,
            minY + (maxY - minY) * 0.5,
            minZ + (maxZ - minZ) * 0.5,
        )

    public companion object {
        public fun of(blockPos: BlockPos): AABB =
            AABB(
                blockPos.x.toDouble(),
                blockPos.y.toDouble(),
                blockPos.z.toDouble(),
                (blockPos.x + 1).toDouble(),
                (blockPos.y + 1).toDouble(),
                (blockPos.z + 1).toDouble(),
            )
    }
}
