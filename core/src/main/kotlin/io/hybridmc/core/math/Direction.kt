package io.hybridmc.core.math

/**
 * The 6 cardinal directions (and their axes) in Minecraft.
 */
public enum class Direction(
    public val id: Int,
    public val idOpposite: Int,
    public val axis: Axis,
    public val axisDirection: AxisDirection,
    public val stepX: Int,
    public val stepY: Int,
    public val stepZ: Int,
) {
    DOWN(0, 1, Axis.Y, AxisDirection.NEGATIVE, 0, -1, 0),
    UP(1, 0, Axis.Y, AxisDirection.POSITIVE, 0, 1, 0),
    NORTH(2, 3, Axis.Z, AxisDirection.NEGATIVE, 0, 0, -1),
    SOUTH(3, 2, Axis.Z, AxisDirection.POSITIVE, 0, 0, 1),
    WEST(4, 5, Axis.X, AxisDirection.NEGATIVE, -1, 0, 0),
    EAST(5, 4, Axis.X, AxisDirection.POSITIVE, 1, 0, 0),
    ;

    public val opposite: Direction
        get() = BY_ID.getValue(idOpposite)

    public companion object {
        private val BY_ID = entries.associateBy { it.id }

        public fun fromId(id: Int): Direction = BY_ID[id] ?: throw IllegalArgumentException("Unknown Direction ID: $id")
    }

    public enum class Axis { X, Y, Z }

    public enum class AxisDirection { POSITIVE, NEGATIVE }
}
