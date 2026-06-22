# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**HybridMC** is a clean-room Minecraft server launcher ("구동기") that aims to support **Java Edition and Bedrock Edition natively and simultaneously** (true cross-play, not a translation proxy), plus a custom plugin API. **No Minecraft server code is used** — it is implemented from scratch.

Fixed product decisions (see [ROADMAP.md](ROADMAP.md) for the full plan):
- **Cross-play**: native dual-stack — one unified core, both protocols implemented natively.
- **Target version**: Java & Bedrock **26.2**, single pinned version (no multi-version abstraction).
- **Plugin API**: custom new API (not Bukkit/Spigot compatible).
- **MVP goal**: full vanilla survival parity, delivered as vertical-slice milestones (M0–M8).

Current state: the Gradle multi-module skeleton exists and builds, but **almost no game logic is written yet** — only a placeholder `app/src/main/kotlin/io/hybridmc/app/Main.kt`. Expect to build domains out milestone by milestone per ROADMAP.md (next up: **M0**).

## The architectural spine

The central engineering artifact is a **canonical registry + per-edition mapping layer** (`:registry`). Java and Bedrock diverge heavily at the wire level (block-state IDs vs runtime IDs, TCP+VarInt vs RakNet/UDP, chunk section vs subchunk formats, entity metadata keys, UI models). Everything converges on a single internal model; each network frontend serializes to its edition. Every feature is built as three layers: **unified model → Java serialization → Bedrock serialization**.

Clean-room constraint: source registry/mapping **data** from public datasets (e.g. `minecraft-data`, Geyser/Cloudburst mapping tables, public data reports) via a build-time pipeline in `:registry` — never decompiled Mojang code.

## Module structure (`settings.gradle.kts`)

```
:core             math, NBT, buffer/VarInt codecs, event bus, scheduler   (+ fastutil)
:registry         canonical block/item/entity registry + edition mapping + data pipeline (kotlinx-serialization)
:protocol-java    Java 26.2 packet codecs + state machine                 (Netty/TCP)
:protocol-bedrock Bedrock 26.2 RakNet + packet codecs + login chain        (Netty/UDP)
:network          unified PlayerConnection abstraction; binds both frontends
:world            world/chunk/block-state model, chunk IO, lighting       (+ fastutil)
:worldgen         noise terrain, biomes, ores, structures
:entity           entity component system, metadata sync, AI/pathfinding
:game             crafting/smelting, combat, redstone, fluids, physics
:api              public plugin API (interfaces only — keep deps minimal)
:server           main tick loop, player management, commands, dimensions (depends on all domains)
:plugin-host      plugin loader/classloading/isolation/lifecycle
:app              runnable entry point (the launcher); `application` plugin, logback runtime
```

Dependency direction flows downward toward `:core`. `:server` aggregates the domain modules; `:app` is the only runnable module.

## Commands

Use the wrapper (`./gradlew`), which pins Gradle **9.5.0**.

```bash
./gradlew :app:run              # build everything and run the server entry point
./gradlew build                 # assemble + test all modules
./gradlew :<module>:build       # build a single module, e.g. :protocol-java:build
./gradlew test                  # run all tests
./gradlew :<module>:test --tests "fully.qualified.ClassName"         # single test class
./gradlew :<module>:test --tests "fully.qualified.ClassName.method"  # single test method
./gradlew projects              # list the module tree
```

## Conventions

- **Kotlin DSL** build scripts, **JDK 21 toolchain** (auto-provisioned via the foojay resolver in `settings.gradle.kts`).
- **All dependencies go through the version catalog** `gradle/libs.versions.toml` — reference as `libs.<alias>` / `libs.bundles.netty` / `alias(libs.plugins.<alias>)`. Don't hardcode version strings in build scripts. When adding a library, add it to the catalog first.
- Each module is a self-contained build script that applies `alias(libs.plugins.kotlin.jvm)` and repeats the common dependency block (coroutines + kotlin-logging + slf4j + JUnit5). This repetition is intentional for now; extracting a `build-logic` convention plugin is a deferred cleanup (fits M0/M8).
- **Configuration cache, parallel, and build cache are on** (`gradle.properties`) — keep build logic configuration-cache compatible. `:app:run` is verified CC-clean.
- Logging via `io.github.oshai.kotlinlogging.KotlinLogging` (kotlin-logging) over SLF4J; `:app` provides logback at runtime.
- `gradlew` must stay LF and `*.bat` CRLF (`.gitattributes`); `.gradle/`, `build/`, `.kotlin/` are gitignored — never commit them.
