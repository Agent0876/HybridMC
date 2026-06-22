package io.hybridmc.core.nbt

/**
 * Defines the binary serialization format for NBT tags.
 *
 * The three official formats are:
 * - [JavaNetwork]: Big-endian, modified UTF-8 strings (max 32767 bytes), NO root compound name.
 * - [JavaDisk]: Big-endian, modified UTF-8 strings (max 32767 bytes), HAS root compound name.
 * - [BedrockNetwork]: Little-endian, VarInt arrays/strings, standard UTF-8, HAS root compound name.
 *
 * *Note: This codec does not handle gzip compression. Gzip must be handled by the caller.*
 */
public sealed class NbtFormat {
    /**
     * Used for Java Edition network packets (e.g., chunk data, block entities).
     * - Big Endian
     * - Modified UTF-8 (DataInput/Output)
     * - No root name (the payload directly follows the ID 10 byte)
     */
    public data object JavaNetwork : NbtFormat()

    /**
     * Used for Java Edition disk storage (e.g., `.dat` and `.mca` files).
     * - Big Endian
     * - Modified UTF-8
     * - Includes root name
     */
    public data object JavaDisk : NbtFormat()

    /**
     * Used for Bedrock Edition network and storage.
     * - Little Endian
     * - VarInt string/array lengths
     * - Standard UTF-8
     * - Includes root name
     */
    public data object BedrockNetwork : NbtFormat()
}
