package io.github.agent0876.hybridmc.core.player

import java.util.UUID

class TestPlayer(
    override val uuid: UUID = UUID.randomUUID(),
    override val username: String = "TestPlayer",
    override val edition: Edition = Edition.JAVA,
    override val ping: Int = 0,
) : HybridPlayer {
    var lastMessage: String? = null
    var disconnected = false

    override fun sendMessage(text: String) {
        lastMessage = text
    }

    override fun disconnect(reason: String) {
        disconnected = true
    }
}
