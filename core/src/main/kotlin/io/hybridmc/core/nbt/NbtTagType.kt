package io.hybridmc.core.nbt

/**
 * Represents the 13 standard NBT tag types defined by the Minecraft protocol.
 */
public enum class NbtTagType(
    public val id: Int,
) {
    /** 0: Marker for the end of a compound tag. */
    END(0),

    /** 1: A single signed byte (8 bits). */
    BYTE(1),

    /** 2: A single signed short (16 bits). */
    SHORT(2),

    /** 3: A single signed int (32 bits). */
    INT(3),

    /** 4: A single signed long (64 bits). */
    LONG(4),

    /** 5: A single IEEE-754 single-precision float (32 bits). */
    FLOAT(5),

    /** 6: A single IEEE-754 double-precision float (64 bits). */
    DOUBLE(6),

    /** 7: An array of bytes. */
    BYTE_ARRAY(7),

    /** 8: A UTF-8 string. */
    STRING(8),

    /** 9: A list of unnamed tags, all of the same type. */
    LIST(9),

    /** 10: A map of named tags. */
    COMPOUND(10),

    /** 11: An array of ints. */
    INT_ARRAY(11),

    /** 12: An array of longs. */
    LONG_ARRAY(12),
    ;

    public companion object {
        private val BY_ID = entries.associateBy { it.id }

        /**
         * Retrieves the [NbtTagType] corresponding to the given ID.
         *
         * @throws IllegalArgumentException if the ID is not between 0 and 12.
         */
        public fun fromId(id: Int): NbtTagType = BY_ID[id] ?: throw IllegalArgumentException("Unknown NBT tag type ID: $id")
    }
}
