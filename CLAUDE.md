# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**HybridMC** is a clean-room Minecraft server launcher ("구동기") that aims to support **Java Edition and Bedrock Edition natively and simultaneously** (true cross-play, not a translation proxy), plus a custom plugin API. **No Minecraft server code is used** — it is implemented from scratch.

Fixed product decisions (see [ROADMAP.md](ROADMAP.md) and [TASKS.md](TASKS.md)):
- **Cross-play**: native dual-stack — one unified core, both protocols implemented natively.
- **Target version**: Java & Bedrock **26.2**, single pinned version.
- **Plugin API**: custom new API (not Bukkit/Spigot compatible).
- **MVP goal**: full vanilla survival parity, delivered as vertical-slice milestones (M0–M8).

Current state: the enterprise build skeleton exists and passes all quality gates, but **almost no game logic is written yet** — only the DI scaffolding (`:core` service SPI, a thin `:server`, the `:app` composition root). Build out milestone by milestone per TASKS.md (next up: **M0**).

## Architectural spine

The central engineering artifact is a **canonical registry + per-edition mapping layer** (`:registry`). Java and Bedrock diverge heavily at the wire level; everything converges on one internal model and each network frontend serializes to its edition. Every feature is **unified model → Java serialization → Bedrock serialization**. Source mapping **data** from public datasets (`minecraft-data`, Geyser/Cloudburst tables) via a build-time pipeline — never decompiled Mojang code.

## Module structure

```
build-logic/        included build holding the convention plugins (the single source of build config)
:core               math, NBT, codecs, event bus, scheduler, + the Subsystem/ServiceRegistry DI SPI
:registry           canonical block/item/entity registry + edition mapping + data pipeline (kotlinx-serialization)
:protocol-java      Java 26.2 packet codecs + state machine (Netty/TCP)
:protocol-bedrock   Bedrock 26.2 RakNet + packet codecs + login chain (Netty/UDP)
:network            unified PlayerConnection abstraction; binds both frontends
:world / :worldgen  world/chunk/block-state model + chunk IO; terrain generation
:entity / :game     entity component system; crafting, combat, redstone, fluids, physics
:api                public plugin API — published, explicitApi, ABI-tracked
:server             lifecycle + tick loop; depends ONLY on :core (Subsystem SPI), :api, :network
:plugin-host        plugin loader/classloading/isolation/lifecycle
:app                composition root + runnable entry point; the only module that wires concrete domains
:architecture-tests Konsist tests that enforce the module dependency direction
```

**Dependency inversion is enforced, not just intended:** `:server` must never import concrete domain modules (`:world`, `:game`, …) — it drives them through the `Subsystem`/`ServiceRegistry` SPI in `:core`. The composition root `:app` is the only place that depends on every domain and registers their subsystems. `architecture-tests` fails the build if this is violated.

## Commands

Use the wrapper (`./gradlew`), pinned to Gradle **9.5.0**.

```bash
./gradlew :app:run              # build everything and run the server entry point
./gradlew build                 # assemble + all quality gates (spotlessCheck, tests, apiCheck, kover)
./gradlew spotlessApply         # auto-format Kotlin (ktlint) — run before committing
./gradlew :<module>:test --tests "FQCN"        # single test class
./gradlew :api:apiDump          # regenerate the public-API ABI baseline after an intentional API change
./gradlew :api:apiCheck         # verify the plugin API ABI hasn't drifted
./gradlew koverHtmlReport       # aggregated coverage report
./gradlew projects              # list the module tree
```

## Conventions

- **All build config lives in `build-logic` convention plugins** — modules just apply one:
  `hybridmc.kotlin-library` (library + `explicitApi()`), `hybridmc.kotlin-application` (app), `hybridmc.published-library` (library + `maven-publish`, used by `:api`), and `hybridmc.kotlin-serialization` (overlay for `:registry`). Don't reintroduce per-module Kotlin/test/quality config — change the convention plugin instead.
- **explicitApi() is on for every library module** — public declarations need explicit visibility and return types.
- **Version catalog `gradle/libs.versions.toml` is the only place for versions**, including the Gradle-plugin artifacts that build-logic puts on its classpath. Repositories are declared centrally in `settings.gradle.kts` (`FAIL_ON_PROJECT_REPOS`) — do not add `repositories {}` to module build scripts.
- **`:api` is a published, ABI-tracked contract.** Any intentional change to its public surface requires `./gradlew :api:apiDump` and committing the updated `api/api.api`; otherwise `apiCheck` (and CI) fails.
- **`gradle.properties` enables the configuration cache, parallel execution, and the build cache** (plus raised `jvmargs`). Keep build logic and convention plugins configuration-cache-compatible — don't read mutable state or reference `Project` at execution time. `:app:run` is verified CC-clean.
- **Quality gates** (wired via convention plugins + `.github/workflows/ci.yml`): spotless/ktlint (format+lint), kover (coverage, aggregated at root), binary-compatibility-validator (`:api` only), Konsist (`:architecture-tests`).
- **detekt is intentionally absent**: its latest stable (1.23.8) does not support Kotlin 2.4.0. ktlint (via spotless) is the lint gate until detekt ships K2/2.4 support; re-enable by adding it back to `hybridmc.kotlin-common`.
- Logging via `io.github.oshai.kotlinlogging.KotlinLogging` over SLF4J; `:app` provides logback at runtime.
- `gradlew` stays LF and `*.bat` CRLF (`.gitattributes`); `.gradle/`, `build/`, `.kotlin/` are gitignored.
