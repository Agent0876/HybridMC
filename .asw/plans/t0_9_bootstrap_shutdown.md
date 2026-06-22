# t0_9_bootstrap_shutdown

## TL;DR
Implement bootstrap orchestration (initializing, registering, and freezing registries) and bulletproof graceful shutdown in `:registry`, `:server`, and `:app` for task `T0.9`.

## Objective
- Provide a `RegistryManager` in `:registry` to track and freeze registries.
- Register `RegistryManager` in `ServiceRegistry` and trigger `freezeAll()` during `Server.start()`.
- Secure graceful shutdown in `Main.kt` shutdown hook by stopping the server and waiting (`join()`) on the main thread to ensure all cleanups in `finally` blocks finish before the JVM halts.

## Non-goals
- Do not define concrete game blocks or items (these will be added in M2).
- Do not implement custom signal handling libraries beyond standard JVM shutdown hooks.

## Discovery
- `registry/src/main/kotlin/io/hybridmc/registry/Registry.kt` implements the freezeable registry.
- `server/src/main/kotlin/io/hybridmc/server/Server.kt` handles the tick loop start/stop lifecycle.
- `app/src/main/kotlin/io/hybridmc/app/Main.kt` runs the entry point and shutdown hook.

## Decisions
- **Registry manager**: Implement `RegistryManager` in `:registry` as a central registrar container, register it in `ServiceRegistry`, and call `freezeAll()` after starting subsystems in `Server.start()`.
- **Hook thread coordination**: Use `mainThread.join()` in the JVM shutdown hook thread to block JVM termination until the main thread finishes cleanups and exits.
- **Fail-safe freeze**: Log warnings instead of crashing the server if an empty registry is frozen (or ensure standard handling).

## TODOs

- [ ] Task 1: Create `RegistryManager` and register/freeze integration
  - Files:
    - [NEW] [RegistryManager.kt](file:///Users/shinseungmin/HybridMC/registry/src/main/kotlin/io/hybridmc/registry/RegistryManager.kt)
    - [NEW] [RegistryManagerTest.kt](file:///Users/shinseungmin/HybridMC/registry/src/test/kotlin/io/hybridmc/registry/RegistryManagerTest.kt)
    - [MODIFY] [Server.kt](file:///Users/shinseungmin/HybridMC/server/src/main/kotlin/io/hybridmc/server/Server.kt)
  - RED: Run tests (empty or unresolved).
  - GREEN: Implement `RegistryManager` with sequential registry creation, `freezeAll` propagation, and integrate call inside `Server.start()`. Test registry creation, registration limits, and freeze enforcement.
  - Real-surface QA: Run `./gradlew :registry:test` and `./gradlew :server:test`.
  - Evidence: Test success logs.
  - Cleanup: None.
  - Commit: YES
    - Message: "feat(registry): implement RegistryManager and freeze integration"
    - Files: `registry/src/main/kotlin/io/hybridmc/registry/RegistryManager.kt`, `registry/src/test/kotlin/io/hybridmc/registry/RegistryManagerTest.kt`, `server/src/main/kotlin/io/hybridmc/server/Server.kt`

- [ ] Task 2: Implement bulletproof graceful shutdown in Main.kt
  - Files:
    - [MODIFY] [Main.kt](file:///Users/shinseungmin/HybridMC/app/src/main/kotlin/io/hybridmc/app/Main.kt)
  - RED: Hook runs concurrently and might exit before loop cleanup.
  - GREEN: Update `Main.kt` to coordinate the hook thread with the main thread using `mainThread.join()`.
  - Real-surface QA: Boot the server via `./gradlew :app:run`, trigger `Ctrl+C` (SIGINT), and verify in the logs that subsystems stop and "HybridMC shutdown complete" prints *before* the process exits.
  - Evidence: Log output transcript.
  - Cleanup: None.
  - Commit: YES
    - Message: "feat(app): secure graceful shutdown with hook thread synchronization"
    - Files: `app/src/main/kotlin/io/hybridmc/app/Main.kt`

## Parallel Execution Waves
Task 2 depends on the compilation of Task 1, so work is serialized.

## Dependency Matrix
| Task | Depends on | Blocks | Can parallelize with |
|---|---|---|---|
| 1 | none | 2 | none |
| 2 | 1 | none | none |

## Final Verification Wave
- [ ] Run `./gradlew spotlessApply` to format all code.
- [ ] Run `./gradlew build` to ensure all quality gates and architecture-tests pass.
- [ ] Manual test of boot and graceful shutdown verification via Ctrl+C.

Next: `start-work t0_9_bootstrap_shutdown`
