# t0_8_server_config

## TL;DR
Implement the server configuration loading and validation system (`T0.8`) using standard Java `Properties` in `:server` → `io.hybridmc.server.config.ServerConfig`.

## Objective
Enable the server to read, write, and validate configuration settings (port, online-mode, view distance, game mode, etc.) from/to `server.properties` at boot. Register the config in the `ServiceRegistry` so other components can retrieve it.

## Non-goals
- Do not add external configuration format libraries (e.g. TOML/YAML kotlinx-serialization) to avoid unnecessary dependencies.
- Do not change network components or implement port binding logic (handled in M1).

## Discovery
- `server/src/main/kotlin/io/hybridmc/server/Server.kt` is the main server container.
- `app/src/main/kotlin/io/hybridmc/app/Main.kt` is the entry point.
- There are currently no configuration classes or properties loaders in the project.

## Decisions
- **Properties Format**: Use standard Java `server.properties` layout for ease of editing and familiarity.
- **Validation Rules**:
  - `server-ip`: Any valid string (default: `"0.0.0.0"`).
  - `server-port`: Integer in `1..65535` (default: `25565`).
  - `bedrock-port`: Integer in `1..65535` (default: `19132`).
  - `online-mode`: Boolean (default: `true`).
  - `view-distance`: Integer in `2..32` (default: `10`).
  - `gamemode`: String, one of `"survival"`, `"creative"`, `"adventure"`, `"spectator"` (default: `"survival"`).
  - `motd`: Any string (default: `"A HybridMC Minecraft Server"`).
- **Default Generation**: If `server.properties` doesn't exist, write a default commented file to disk automatically.
- **Error Handling**: On invalid inputs, log warnings and fall back to default values rather than crashing.

## TODOs

- [ ] Task 1: Create `ServerConfig` model and parser
  - Files:
    - [NEW] [ServerConfig.kt](file:///Users/shinseungmin/HybridMC/server/src/main/kotlin/io/hybridmc/server/config/ServerConfig.kt)
    - [NEW] [ServerConfigTest.kt](file:///Users/shinseungmin/HybridMC/server/src/test/kotlin/io/hybridmc/server/config/ServerConfigTest.kt)
  - RED: Run tests (which will be absent or empty initially).
  - GREEN: Implement properties parser, validation, fallback to defaults, default file generation, and fully test all behaviors.
  - Real-surface QA: Run JUnit tests for `ServerConfigTest` directly via `./gradlew :server:test --tests "io.hybridmc.server.config.ServerConfigTest"`.
  - Evidence: Terminal test output.
  - Cleanup: Remove any temporary test files generated under target.
  - Commit: YES
    - Message: "feat(server): implement T0.8 server configuration loading and validation"
    - Files: `server/src/main/kotlin/io/hybridmc/server/config/ServerConfig.kt`, `server/src/test/kotlin/io/hybridmc/server/config/ServerConfigTest.kt`

- [ ] Task 2: Integrate `ServerConfig` into Application Bootstrap
  - Files:
    - [MODIFY] [Main.kt](file:///Users/shinseungmin/HybridMC/app/src/main/kotlin/io/hybridmc/app/Main.kt)
  - RED: `Main.kt` does not attempt to load `server.properties` or register a configuration.
  - GREEN: Update `Main.kt` to load `server.properties` (generating it if missing) and register `ServerConfig` in the `ServiceRegistry`.
  - Real-surface QA: Execute `./gradlew :app:run`. Check that a default `server.properties` is generated and that the log displays target version and server start.
  - Evidence: Log statements and generated file contents.
  - Cleanup: Delete the generated `server.properties` file from workspace root.
  - Commit: YES
    - Message: "feat(app): integrate server configuration into boot pipeline"
    - Files: `app/src/main/kotlin/io/hybridmc/app/Main.kt`

## Parallel Execution Waves
Since Task 2 directly depends on Task 1's classes, execution is serialized.

## Dependency Matrix
| Task | Depends on | Blocks | Can parallelize with |
|---|---|---|---|
| 1 | none | 2 | none |
| 2 | 1 | none | none |

## Final Verification Wave
- [ ] Run `./gradlew spotlessApply` to format all code.
- [ ] Run `./gradlew build` to ensure all tests (including architecture layers) pass.
- [ ] Verify that starting the server generates a valid `server.properties` file.

Next: `start-work t0_8_server_config`
