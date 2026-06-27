package io.github.agent0876.hybridmc.core.player

import java.util.UUID

class TestPlayer(
    override val uuid: UUID = UUID.randomUUID(),
    override val username: String = "TestPlayer",
    override val edition: Edition = Edition.JAVA,
    override val ping: Int = 0,
) : HybridPlayer {
    override val entityId: Int = 0
    override var x: Double = 0.0
    override var y: Double = 100.0
    override var z: Double = 0.0
    override var yaw: Float = 0.0f
    override var pitch: Float = 0.0f

    var lastMessage: String? = null
    var disconnected = false

    override fun sendMessage(message: String) {
        lastMessage = message
    }

    override fun disconnect(reason: String) {
        disconnected = true
    }

    override fun spawnPlayer(target: HybridPlayer) {}
    override fun removePlayer(target: HybridPlayer) {}
    override fun movePlayer(target: HybridPlayer) {}
}
