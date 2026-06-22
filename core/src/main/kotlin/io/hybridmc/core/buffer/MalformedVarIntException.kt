package io.hybridmc.core.buffer

/**
 * Thrown when a VarInt or VarLong encoding is malformed —
 * typically because the continuation bits exceed the maximum allowed byte count
 * (5 for VarInt, 10 for VarLong).
 */
public class MalformedVarIntException : RuntimeException {
    public constructor() : super("VarInt/VarLong encoding is malformed")
    public constructor(message: String) : super(message)
}
