package io.hybridmc.core.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MathTypesTest {
    @Test
    fun `BlockPos packing and unpacking`() {
        val x = 1234567
        val y = 120
        val z = -891011

        val pos = BlockPos.of(x, y, z)
        assertEquals(x, pos.x)
        assertEquals(y, pos.y)
        assertEquals(z, pos.z)
    }

    @Test
    fun `BlockPos boundary packing`() {
        val max8 = 127
        val min8 = -128
        val max28 = (1 shl 27) - 1
        val min28 = -(1 shl 27)

        var pos = BlockPos.of(max28, max8, max28)
        assertEquals(max28, pos.x)
        assertEquals(max8, pos.y)
        assertEquals(max28, pos.z)

        pos = BlockPos.of(min28, min8, min28)
        assertEquals(min28, pos.x)
        assertEquals(min8, pos.y)
        assertEquals(min28, pos.z)
    }

    @Test
    fun `ChunkPos packing and unpacking`() {
        val x = -1000000
        val z = 500000

        val pos = ChunkPos.of(x, z)
        assertEquals(x, pos.x)
        assertEquals(z, pos.z)
    }

    @Test
    fun `ChunkPos from BlockPos`() {
        val block = BlockPos.of(-17, 64, 15)
        val chunk = ChunkPos.fromBlockPos(block)

        assertEquals(-2, chunk.x) // -17 shr 4 is -2
        assertEquals(0, chunk.z) // 15 shr 4 is 0
    }

    @Test
    fun `SectionPos packing and unpacking`() {
        val x = -1234
        val y = 567
        val z = 8901

        val pos = SectionPos.of(x, y, z)
        assertEquals(x, pos.x)
        assertEquals(y, pos.y)
        assertEquals(z, pos.z)
    }

    @Test
    fun `Rotation packing and unpacking`() {
        val yaw = 123.456f
        val pitch = -45.678f

        val rot = Rotation.of(yaw, pitch)
        assertEquals(yaw, rot.yaw)
        assertEquals(pitch, rot.pitch)
    }

    @Test
    fun `Vec3d math operations`() {
        val v1 = Vec3d(1.0, 2.0, 3.0)
        val v2 = Vec3d(-1.0, -2.0, -3.0)

        assertEquals(Vec3d(0.0, 0.0, 0.0), v1 + v2)
        assertEquals(Vec3d(2.0, 4.0, 6.0), v1 - v2)
        assertEquals(Vec3d(2.0, 4.0, 6.0), v1 * 2.0)
        assertEquals(Vec3d(-1.0, -2.0, -3.0), -v1)
        assertEquals(14.0, v1.lengthSquared())
    }

    @Test
    fun `Vec3i math operations`() {
        val v1 = Vec3i(1, 2, 3)
        val v2 = Vec3i(-1, -2, -3)

        assertEquals(Vec3i(0, 0, 0), v1 + v2)
        assertEquals(Vec3i(2, 4, 6), v1 - v2)
        assertEquals(Vec3i(2, 4, 6), v1 * 2)
        assertEquals(Vec3i(-1, -2, -3), -v1)
        assertEquals(14.0, v1.distanceToSqr(Vec3i.ZERO))
    }

    @Test
    fun `AABB intersection and containment`() {
        val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val b = AABB(0.5, 0.5, 0.5, 1.5, 1.5, 1.5)
        val c = AABB(2.0, 2.0, 2.0, 3.0, 3.0, 3.0)

        assertTrue(a.intersects(b))
        assertFalse(a.intersects(c))

        assertTrue(a.contains(Vec3d(0.5, 0.5, 0.5)))
        assertFalse(a.contains(Vec3d(1.0, 1.0, 1.0))) // Max bounds are exclusive in 'contains' logic
    }
}
