package io.hybridmc.core.nbt

/**
 * Base interface for all NBT tags.
 *
 * NBT tags are deeply immutable once created. For mutations, use the builder DSL.
 */
public sealed interface NbtTag {
    public val type: NbtTagType
    public val value: Any
}

// ── Primitive tags ──────────────────────────────────────────

@JvmInline
public value class NbtByte(
    override val value: Byte,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.BYTE
}

@JvmInline
public value class NbtShort(
    override val value: Short,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.SHORT
}

@JvmInline
public value class NbtInt(
    override val value: Int,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.INT
}

@JvmInline
public value class NbtLong(
    override val value: Long,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.LONG
}

@JvmInline
public value class NbtFloat(
    override val value: Float,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.FLOAT
}

@JvmInline
public value class NbtDouble(
    override val value: Double,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.DOUBLE
}

@JvmInline
public value class NbtString(
    override val value: String,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.STRING
}

// ── Array tags ──────────────────────────────────────────────

public class NbtByteArray(
    override val value: ByteArray,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.BYTE_ARRAY

    override fun equals(other: Any?): Boolean = this === other || (other is NbtByteArray && value.contentEquals(other.value))

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "NbtByteArray(size=${value.size})"
}

public class NbtIntArray(
    override val value: IntArray,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.INT_ARRAY

    override fun equals(other: Any?): Boolean = this === other || (other is NbtIntArray && value.contentEquals(other.value))

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "NbtIntArray(size=${value.size})"
}

public class NbtLongArray(
    override val value: LongArray,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.LONG_ARRAY

    override fun equals(other: Any?): Boolean = this === other || (other is NbtLongArray && value.contentEquals(other.value))

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "NbtLongArray(size=${value.size})"
}

// ── Collection tags ─────────────────────────────────────────

/**
 * An immutable list of unnamed NBT tags, all of the same [elementType].
 */
public class NbtList<out T : NbtTag>(
    public override val value: List<T>,
    public val elementType: NbtTagType,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.LIST

    init {
        if (value.isNotEmpty()) {
            require(value.all { it.type == elementType }) {
                "All elements in NbtList must match the elementType $elementType"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NbtList<*>
        if (elementType != other.elementType) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        var result = elementType.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String = "NbtList(type=$elementType, size=${value.size})"
}

/**
 * An immutable map of named NBT tags.
 */
public class NbtCompound(
    public override val value: Map<String, NbtTag>,
) : NbtTag {
    override val type: NbtTagType get() = NbtTagType.COMPOUND

    /** Retrieves the tag associated with [key], or null if it doesn't exist. */
    public operator fun get(key: String): NbtTag? = value[key]

    // Convenience typed getters
    public fun getByte(key: String): Byte? = (value[key] as? NbtByte)?.value

    public fun getShort(key: String): Short? = (value[key] as? NbtShort)?.value

    public fun getInt(key: String): Int? = (value[key] as? NbtInt)?.value

    public fun getLong(key: String): Long? = (value[key] as? NbtLong)?.value

    public fun getFloat(key: String): Float? = (value[key] as? NbtFloat)?.value

    public fun getDouble(key: String): Double? = (value[key] as? NbtDouble)?.value

    public fun getString(key: String): String? = (value[key] as? NbtString)?.value

    public fun getByteArray(key: String): ByteArray? = (value[key] as? NbtByteArray)?.value

    public fun getIntArray(key: String): IntArray? = (value[key] as? NbtIntArray)?.value

    public fun getLongArray(key: String): LongArray? = (value[key] as? NbtLongArray)?.value

    @Suppress("UNCHECKED_CAST")
    public fun <T : NbtTag> getList(key: String): NbtList<T>? = value[key] as? NbtList<T>

    public fun getCompound(key: String): NbtCompound? = value[key] as? NbtCompound

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NbtCompound
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "NbtCompound(size=${value.size})"
}

/**
 * A marker tag used to indicate the end of a compound tag stream.
 * It is rarely instantiated directly and has no payload.
 */
public object NbtEnd : NbtTag {
    override val type: NbtTagType get() = NbtTagType.END
    override val value: Unit = Unit
}
