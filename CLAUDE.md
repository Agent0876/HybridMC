# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

This repository is a freshly-generated Gradle scaffold (`gradle init`, "general purpose build"). As of now it contains **only build infrastructure — no source code, subprojects, plugins, or dependencies have been added yet.** Expect to create the project structure from scratch.

Concretely:
- `settings.gradle.kts` declares `rootProject.name = "HybridMC"` and **no** `include(...)` subprojects.
- `build.gradle.kts` applies **no** plugins and defines no tasks — `./gradlew build` currently does nothing meaningful.
- `gradle/libs.versions.toml` (the version catalog) is empty.

## Commands

Use the Gradle wrapper (`./gradlew`), which pins Gradle **9.5.0** — do not rely on a system Gradle.

```bash
./gradlew tasks            # list available tasks (use --all to see everything)
./gradlew build            # assemble + test (no-op until plugins/source exist)
./gradlew test             # run tests (requires a JVM plugin to be applied first)
./gradlew check            # run all verification tasks
./gradlew projects         # list subprojects
```

Running a single test (once a JVM/test plugin and tests exist):
```bash
./gradlew test --tests "fully.qualified.ClassName"
./gradlew test --tests "fully.qualified.ClassName.methodName"
./gradlew :<subproject>:test --tests "..."   # scope to one subproject
```

## Conventions for extending this build

When adding code, follow the structure this scaffold is set up for:

- **Dependencies and versions go through the version catalog**, not hardcoded strings. Declare versions/libraries/plugins in `gradle/libs.versions.toml`, then reference them in build scripts as `libs.<alias>` / `alias(libs.plugins.<alias>)`. This is the intended pattern for this project.
- **Build scripts are Kotlin DSL** (`.gradle.kts`), not Groovy. Match that for any new build files.
- **New modules are added as subprojects**: create the module directory with its own `build.gradle.kts`, then register it with `include("module-name")` in `settings.gradle.kts`.
- `gradle.properties` enables the **configuration cache, parallel execution, and the build cache**. Keep build logic configuration-cache compatible (avoid reading mutable state at execution time, don't reference `Project` from task actions).

## Notes

- `gradlew` must use LF line endings and `*.bat` must use CRLF (enforced by `.gitattributes`); don't normalize these.
- `.gradle/`, `build/`, and `.kotlin/` are gitignored build output — never commit them.
