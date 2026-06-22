package io.hybridmc.core.nbt

// ── Compound Builder ────────────────────────────────────────

public class NbtCompoundBuilder
    @PublishedApi
    internal constructor() {
        private val entries = mutableMapOf<String, NbtTag>()

        public fun put(
            key: String,
            tag: NbtTag,
        ) {
            entries[key] = tag
        }

        public fun putByte(
            key: String,
            value: Byte,
        ) {
            put(key, NbtByte(value))
        }

        public fun putShort(
            key: String,
            value: Short,
        ) {
            put(key, NbtShort(value))
        }

        public fun putInt(
            key: String,
            value: Int,
        ) {
            put(key, NbtInt(value))
        }

        public fun putLong(
            key: String,
            value: Long,
        ) {
            put(key, NbtLong(value))
        }

        public fun putFloat(
            key: String,
            value: Float,
        ) {
            put(key, NbtFloat(value))
        }

        public fun putDouble(
            key: String,
            value: Double,
        ) {
            put(key, NbtDouble(value))
        }

        public fun putString(
            key: String,
            value: String,
        ) {
            put(key, NbtString(value))
        }

        public fun putByteArray(
            key: String,
            value: ByteArray,
        ) {
            put(key, NbtByteArray(value))
        }

        public fun putIntArray(
            key: String,
            value: IntArray,
        ) {
            put(key, NbtIntArray(value))
        }

        public fun putLongArray(
            key: String,
            value: LongArray,
        ) {
            put(key, NbtLongArray(value))
        }

        public inline fun putCompound(
            key: String,
            block: NbtCompoundBuilder.() -> Unit,
        ) {
            put(key, nbtCompound(block))
        }

        public inline fun <T : NbtTag> putList(
            key: String,
            elementType: NbtTagType,
            block: NbtListBuilder<T>.() -> Unit,
        ) {
            put(key, nbtList(elementType, block))
        }

        // Default primitive list convenience
        public inline fun putCompoundList(
            key: String,
            block: NbtListBuilder<NbtCompound>.() -> Unit,
        ) {
            putList(key, NbtTagType.COMPOUND, block)
        }

        @PublishedApi
        internal fun build(): NbtCompound = NbtCompound(entries.toMap())
    }

// ── List Builder ────────────────────────────────────────────

public class NbtListBuilder<T : NbtTag>
    @PublishedApi
    internal constructor(
        @PublishedApi
        internal val elementType: NbtTagType,
    ) {
        private val elements = mutableListOf<T>()

        public fun add(tag: T) {
            require(tag.type == elementType) {
                "Cannot add tag of type ${tag.type} to a list of type $elementType"
            }
            elements.add(tag)
        }

        // Since we know the list element type, we could provide specific adders,
        // but the generic add() covers everything. We add a compound specific helper:
        @Suppress("UNCHECKED_CAST")
        public inline fun addCompound(block: NbtCompoundBuilder.() -> Unit) {
            require(elementType == NbtTagType.COMPOUND) { "This list does not hold compounds" }
            add(nbtCompound(block) as T)
        }

        @PublishedApi
        internal fun build(): NbtList<T> = NbtList(elements.toList(), elementType)
    }

// ── Top-level DSL entries ───────────────────────────────────

public inline fun nbtCompound(block: NbtCompoundBuilder.() -> Unit): NbtCompound = NbtCompoundBuilder().apply(block).build()

public inline fun <T : NbtTag> nbtList(
    elementType: NbtTagType,
    block: NbtListBuilder<T>.() -> Unit,
): NbtList<T> = NbtListBuilder<T>(elementType).apply(block).build()

public inline fun nbtCompoundList(block: NbtListBuilder<NbtCompound>.() -> Unit): NbtList<NbtCompound> = nbtList(NbtTagType.COMPOUND, block)
