package io.hybridmc.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Test

/**
 * Enforces the module dependency direction as a test, so an illegal upward import fails CI
 * instead of silently rotting the architecture.
 */
class LayeringTest {
    @Test
    fun `core must not depend on any other internal module`() {
        val violations =
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.packagee?.name?.startsWith("io.hybridmc.core") == true }
                .flatMap { file ->
                    file.imports
                        .map { it.name }
                        .filter { it.startsWith("io.hybridmc.") && !it.startsWith("io.hybridmc.core") }
                        .map { "${file.name}: illegal import $it" }
                }

        check(violations.isEmpty()) {
            "`:core` must not depend on other modules:\n" + violations.joinToString("\n")
        }
    }

    @Test
    fun `server must not depend on concrete domain modules`() {
        val forbidden =
            listOf("world", "worldgen", "entity", "game", "registry")
                .map { "io.hybridmc.$it" }

        val violations =
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.packagee?.name?.startsWith("io.hybridmc.server") == true }
                .flatMap { file ->
                    file.imports
                        .map { it.name }
                        .filter { import -> forbidden.any { import.startsWith(it) } }
                        .map { "${file.name}: illegal import $it" }
                }

        check(violations.isEmpty()) {
            "`:server` must depend on the Subsystem SPI, not concrete domains:\n" +
                violations.joinToString("\n")
        }
    }
}
