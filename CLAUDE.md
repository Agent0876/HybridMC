# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./gradlew build

# Run the server
./gradlew :server-app:run

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :server-core:test

# Run a single test class
./gradlew :server-core:test --tests "io.github.agent0876.hybridmc.core.config.ConfigLoaderTest"

# Run a single test by name
./gradlew :server-core:test --tests "io.github.agent0876.hybridmc.core.config.ConfigLoaderTest.creates default config files when missing"

# Compile without running tests
./gradlew compileKotlin
```

The project targets JVM 25 with Kotlin. Gradle configuration cache and parallel builds are enabled (`gradle.properties`).

## Architecture

HybridMC bridges Java Edition (JE) and Bedrock Edition (BE) Minecraft clients into a single server. The design is a hub-and-spoke: `HybridServer` (core) owns shared state; edition-specific modules connect to it via the `ServerLifecycle` interface.

### Module structure

- **`server-core`** — shared types and infrastructure: `HybridServer`, `PlayerRegistry`, `GameWorld`, `ConfigLoader`/`HybridConfig`. No edition-specific code.
- **`server-java`** — Java Edition: raw Netty 5 TCP pipeline (`JavaEditionServer` → `JavaPacketHandler` → `JavaPlayerSession`). Implements protocol 776 (Minecraft 1.21.x). Packet framing is hand-rolled VarInt length-prefixed.
- **`server-bedrock`** — Bedrock Edition: RakNetty UDP transport (`BedrockEditionServer` → `BedrockPacketHandler` → `BedrockPlayerSession`). MOTD is built by `MotdBuilder` and sent in UNCONNECTED_PONG.
- **`server-app`** — entry point only. `Main.kt` calls `ConfigLoader.load()`, constructs edition servers from config, installs them into `HybridServer`, and calls `server.start()`.

### Startup flow

```
Main.kt
  └─ ConfigLoader.load()          → HybridConfig
  └─ HybridServer()               → shared PlayerRegistry + GameWorld
  └─ install(JavaEditionServer)   ─┐
  └─ install(BedrockEditionServer) ─┤ both launched concurrently via coroutines
  └─ server.start()              ──┘ blocks on SupervisorJob until shutdown
```

### Config system

Two files, priority: **defaults < `server.properties` < `hybrid.yml`**

- **`server.properties`**: common settings shared by both editions — `max-players`, `gamemode`, `difficulty`, `level-name`, `level-seed`, `force-gamemode`, `online-mode`, `view-distance`, `player-idle-timeout`. `server-ip`/`server-port` apply to Java edition only.
- **`hybrid.yml`**: edition-specific settings — `host`, `port`, and all protocol-level options (motd, bedrock-specific keys like `server-portv6`, `max-connections`, etc.).

`ConfigLoader.load()` pipeline:
1. `defaultEditions()` — hardcoded Kotlin defaults
2. `mergeServerProperties()` — applies `server.properties` values onto Java edition
3. `parseEditions()` → `deepMerge()` — overlays `hybrid.yml`

`parseEditions()` returns `Map<String, EditionOverride>` where `enabled`/`host`/`port` are **nullable** — null means "absent in yml, keep base value." `deepMerge()` uses `?:` to preserve the base when the yml field is absent. This is intentional: it prevents a yml `host: 0.0.0.0` default from overwriting a `server-ip` set in `server.properties`.

### SnakeYAML type coercion

SnakeYAML parses unquoted YAML values by type: `false` → `Boolean`, `19132` → `Int`, `"text"` → `String`. Code that reads from `EditionConfig.options` must cast accordingly:
- Use `as? Number)?.toInt()` for numeric options, not `as? Int`
- Use `?.toString()` instead of `as? String` for fields that users might write as unquoted numbers (e.g. `seed: 12345`)

### PlayerRegistry

Shared across both editions. Both `JavaPlayerSession` and `BedrockPlayerSession` call `registry.join()`/`registry.leave()` after their handshake. `onlineCount` and `all()` reflect combined players from both editions.
