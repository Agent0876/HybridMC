package io.github.agent0876.hybridmc.core.player

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerRegistryTest {

    @Test
    fun `starts empty`() {
        val registry = PlayerRegistry()
        assertEquals(0, registry.onlineCount)
        assertTrue(registry.all().isEmpty())
    }

    @Test
    fun `join adds player`() {
        val registry = PlayerRegistry()
        val player = TestPlayer()

        registry.join(player)

        assertEquals(1, registry.onlineCount)
        assertNotNull(registry.find(player.uuid))
        assertEquals(player, registry.all().single())
    }

    @Test
    fun `join broadcasts join message to existing players`() {
        val registry = PlayerRegistry()
        val existing = TestPlayer(username = "Existing")
        val joiner = TestPlayer(username = "Newbie")

        registry.join(existing)
        registry.join(joiner)

        assertEquals("§eNewbie joined the game.", existing.lastMessage)
    }

    @Test
    fun `leave removes player`() {
        val registry = PlayerRegistry()
        val player = TestPlayer()
        registry.join(player)

        registry.leave(player)

        assertEquals(0, registry.onlineCount)
        assertNull(registry.find(player.uuid))
    }

    @Test
    fun `leave broadcasts leave message`() {
        val registry = PlayerRegistry()
        val staying = TestPlayer(username = "Staying")
        val leaving = TestPlayer(username = "Leaver")
        registry.join(staying)
        registry.join(leaving)

        registry.leave(leaving)

        assertEquals("§eLeaver left the game.", staying.lastMessage)
    }

    @Test
    fun `leave unknown player is no-op`() {
        val registry = PlayerRegistry()
        val player = TestPlayer()
        registry.join(player)

        val unknown = TestPlayer()
        registry.leave(unknown)

        assertEquals(1, registry.onlineCount)
    }

    @Test
    fun `find returns null for unknown uuid`() {
        val registry = PlayerRegistry()
        assertNull(registry.find(UUID.randomUUID()))
    }

    @Test
    fun `all returns snapshot`() {
        val registry = PlayerRegistry()
        val a = TestPlayer()
        val b = TestPlayer()
        registry.join(a)
        registry.join(b)

        val snapshot = registry.all()
        assertEquals(2, snapshot.size)

        registry.leave(a)
        assertEquals(2, snapshot.size, "snapshot should not reflect later mutations")
    }

    @Test
    fun `broadcast sends to every player`() {
        val registry = PlayerRegistry()
        val a = TestPlayer(username = "A")
        val b = TestPlayer(username = "B")
        registry.join(a)
        registry.join(b)

        registry.broadcast("hello")

        assertEquals("hello", a.lastMessage)
        assertEquals("hello", b.lastMessage)
    }

    @Test
    fun `multiple joins with different uuids`() {
        val registry = PlayerRegistry()
        val players = (1..10).map { TestPlayer() }

        players.forEach { registry.join(it) }

        assertEquals(10, registry.onlineCount)
        assertEquals(10, registry.all().size)
    }

    @Test
    fun `rejoining same uuid does not duplicate`() {
        val registry = PlayerRegistry()
        val uuid = UUID.randomUUID()
        val a = TestPlayer(uuid = uuid, username = "A")
        val b = TestPlayer(uuid = uuid, username = "B")

        registry.join(a)
        registry.join(b)

        assertEquals(1, registry.onlineCount, "same uuid should overwrite")
        assertEquals("B", registry.find(uuid)?.username)
    }
}
