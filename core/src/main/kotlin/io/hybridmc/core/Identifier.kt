package io.hybridmc.core

/**
 * A Minecraft resource identifier in `namespace:path` format.
 *
 * This is the universal key type for blocks, items, entities, dimensions,
 * and all registry entries. The internal representation always stores the
 * full `"namespace:path"` form.
 *
 * Use [Identifier.of] for creation with validation (throws on invalid input)
 * or [Identifier.parse] for nullable parsing.
 *
 * **Valid characters:**
 * - **namespace**: `[a-z0-9._-]`
 * - **path**: `[a-z0-9._-/]`
 */
@JvmInline
public value class Identifier private constructor(
    /** The full `"namespace:path"` string. */
    public val value: String,
) {
    /** The namespace portion (before the `:`). */
    public val namespace: String
        get() = value.substringBefore(SEPARATOR)

    /** The path portion (after the `:`). */
    public val path: String
        get() = value.substringAfter(SEPARATOR)

    override fun toString(): String = value

    public companion object {
        /** The default namespace assumed when parsing a string without a colon. */
        public const val DEFAULT_NAMESPACE: String = "minecraft"

        private const val SEPARATOR = ':'
        private val VALID_NAMESPACE = Regex("[a-z0-9._-]+")
        private val VALID_PATH = Regex("[a-z0-9._/-]+")

        // ── Common identifiers ──────────────────────────────────

        /** `minecraft:air` — the empty block. */
        public val AIR: Identifier = Identifier("minecraft:air")

        /** `minecraft:stone` */
        public val STONE: Identifier = Identifier("minecraft:stone")

        /** `minecraft:grass_block` */
        public val GRASS_BLOCK: Identifier = Identifier("minecraft:grass_block")

        /** `minecraft:dirt` */
        public val DIRT: Identifier = Identifier("minecraft:dirt")

        /** `minecraft:bedrock` */
        public val BEDROCK: Identifier = Identifier("minecraft:bedrock")

        // ── Factory methods ─────────────────────────────────────

        /**
         * Creates an [Identifier] from the given string.
         *
         * If no namespace separator `:` is present, [DEFAULT_NAMESPACE] is assumed.
         *
         * @param input e.g. `"minecraft:stone"` or `"stone"`.
         * @throws IllegalArgumentException if [input] is not a valid identifier.
         */
        public fun of(input: String): Identifier =
            parse(input)
                ?: throw IllegalArgumentException("Invalid identifier: '$input'")

        /**
         * Creates an [Identifier] from explicit [namespace] and [path].
         *
         * @throws IllegalArgumentException if either component contains invalid characters.
         */
        public fun of(
            namespace: String,
            path: String,
        ): Identifier {
            require(VALID_NAMESPACE.matches(namespace)) {
                "Invalid namespace: '$namespace' (allowed: [a-z0-9._-])"
            }
            require(VALID_PATH.matches(path)) {
                "Invalid path: '$path' (allowed: [a-z0-9._-/])"
            }
            return Identifier("$namespace$SEPARATOR$path")
        }

        /**
         * Parses [input] into an [Identifier], returning `null` if it is invalid.
         *
         * If no `:` is present, [DEFAULT_NAMESPACE] is assumed.
         */
        public fun parse(input: String): Identifier? {
            val colonIdx = input.indexOf(SEPARATOR)
            val namespace: String
            val path: String
            if (colonIdx < 0) {
                namespace = DEFAULT_NAMESPACE
                path = input
            } else {
                namespace = input.substring(0, colonIdx)
                path = input.substring(colonIdx + 1)
            }
            if (!VALID_NAMESPACE.matches(namespace) || !VALID_PATH.matches(path)) {
                return null
            }
            return Identifier("$namespace$SEPARATOR$path")
        }
    }
}
