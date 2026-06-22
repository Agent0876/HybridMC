package io.hybridmc.app

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "HybridMC starting… (target: Java/Bedrock 26.2)" }
    // M0: the boot sequence + an empty 20 TPS tick loop will live here.
    // See ROADMAP.md for the milestone plan.
    logger.info { "Scaffold only — no game loop yet. Next up: M0." }
}
