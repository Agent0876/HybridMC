package io.hybridmc.core.buffer

import io.hybridmc.core.Identifier

/**
 * Maximum byte count for a serialized [Identifier] in the Minecraft protocol.
 */
private const val MAX_IDENTIFIER_BYTES = 32767

/**
 * Reads an [Identifier] from this buffer (VarInt-prefixed UTF-8 string).
 *
 * @param maxBytes upper bound on the string byte count (default 32 767).
 * @throws IllegalArgumentException if the read string is not a valid identifier.
 */
public fun PacketBuffer.readIdentifier(maxBytes: Int = MAX_IDENTIFIER_BYTES): Identifier = Identifier.of(readString(maxBytes))

/**
 * Writes an [Identifier] to this buffer as a VarInt-prefixed UTF-8 string.
 */
public fun PacketBuffer.writeIdentifier(identifier: Identifier): PacketBuffer = writeString(identifier.value)
