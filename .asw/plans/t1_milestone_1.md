# t1_milestone_1

## TL;DR
Parallelized execution plan for Milestone 1 (T1) implementing both Java and Bedrock handshake & login stacks, culminating in a unified player session connection.

## Objective
Enable Java and Bedrock clients to ping the server and complete the offline/online authentication handshake flow. We split implementation into two parallel stacks (Java and Bedrock) before converging on the unified `PlayerConnection` abstraction in the `:network` module.

## Non-goals
- Do not implement actual in-game play state packets, chunk rendering, or entity spawning (handled in M2).
- Do not implement full game loops or player ticks beyond basic login and state transitions.

## Discovery
- `:protocol-java` is the Netty-based TCP stack module.
- `:protocol-bedrock` is the Netty-based UDP/RakNet stack module.
- `:network` binds both frontends into `PlayerConnection` representations.
- `:core` has packet buffers and VarInt codecs.

## Decisions
- **Parallelization**: Separate developers (or subagent lanes) can build out Java and Bedrock layers independently as they share no file paths or dependencies.
- **Protocol Versions**: Java 26.2 (Protocol version 766 or latest for Java 26.2) and Bedrock 26.2.
- **Unified abstraction**: The `:network` layer acts as the unified boundary.

## TODOs

### Java Track (Can run in parallel with Bedrock Track)

- [ ] Task T1.J1: Netty Server + Framing
  - Files:
    - [NEW] [JavaServer.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/main/kotlin/io/hybridmc/protocol/java/JavaServer.kt)
    - [NEW] [JavaServerTest.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/test/kotlin/io/hybridmc/protocol/java/JavaServerTest.kt)
  - RED: Run tests (empty or missing).
  - GREEN: Implement TCP socket acceptor and VarInt length-prefixed frame decoder/encoder.
  - Real-surface QA: Start Netty server on default port and connect via standard netcat (`nc localhost 25565`).
  - Evidence: netcat connection logs.
  - Cleanup: Shut down Netty server.
  - Commit: YES
    - Message: "feat(protocol-java): implement Netty server and framing decoder T1.J1"

- [ ] Task T1.J2: Handshake + Status (Server List Ping)
  - Files:
    - [NEW] [HandshakePacket.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/main/kotlin/io/hybridmc/protocol/java/packet/HandshakePacket.kt)
    - [NEW] [StatusPacket.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/main/kotlin/io/hybridmc/protocol/java/packet/StatusPacket.kt)
  - RED: Connection closed unexpectedly on status request.
  - GREEN: Implement Handshake status branching and respond with JSON server information and ping/pong packets.
  - Real-surface QA: Ping server from Java Edition Minecraft client multiplayer list and verify MOTD and player count are rendered correctly.
  - Evidence: Server list screenshot or log transcript.
  - Cleanup: Kill server.
  - Commit: YES
    - Message: "feat(protocol-java): implement server list ping status response T1.J2"

- [ ] Task T1.J3: Login (Offline) & Config Transition
  - Files:
    - [NEW] [LoginPacket.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/main/kotlin/io/hybridmc/protocol/java/packet/LoginPacket.kt)
  - RED: Server drops connection when trying to log in.
  - GREEN: Handle login start, generate offline UUID, send login success, and transition connection state to configuration state.
  - Real-surface QA: Connect an offline Java client and verify it proceeds past authentication to download configuration packets.
  - Evidence: Client connection log in terminal.
  - Cleanup: Kill server.
  - Commit: YES
    - Message: "feat(protocol-java): implement offline login and state transitions T1.J3"

- [ ] Task T1.J4: Packet Compression
  - Files:
    - [NEW] [CompressionHandler.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/main/kotlin/io/hybridmc/protocol/java/handler/CompressionHandler.kt)
  - RED: Log-in fails when zlib compression packet prefix is enabled.
  - GREEN: Implement set-compression packet and Netty handlers using zlib to compress packets larger than threshold.
  - Real-surface QA: Test connection from a client with compression thresholds configured (e.g. 256 bytes) and verify packets round-trip cleanly.
  - Evidence: Unit test assertion matching raw uncompressed and compressed packets.
  - Cleanup: None.
  - Commit: YES
    - Message: "feat(protocol-java): implement packet compression T1.J4"

- [ ] Task T1.J5: Online-mode Auth + Encryption
  - Files:
    - [NEW] [EncryptionHandler.kt](file:///Users/shinseungmin/HybridMC/protocol-java/src/main/kotlin/io/hybridmc/protocol/java/handler/EncryptionHandler.kt)
  - RED: Online-mode connection fails or payload is unencrypted.
  - GREEN: Implement ECDH key exchange, AES/CFB8 Netty framing encryptor/decryptor, and join Mojang session server authentication.
  - Real-surface QA: Connect using an online-mode regular Minecraft account client and successfully complete encryption handshake.
  - Evidence: Secure authentication logs showing handshake success.
  - Cleanup: Kill server.
  - Commit: YES
    - Message: "feat(protocol-java): implement online-mode authentication and encryption T1.J5"

### Bedrock Track (Can run in parallel with Java Track)

- [ ] Task T1.B1: RakNet UDP Connection Layer
  - Files:
    - [NEW] [RakNetServer.kt](file:///Users/shinseungmin/HybridMC/protocol-bedrock/src/main/kotlin/io/hybridmc/protocol/bedrock/raknet/RakNetServer.kt)
    - [NEW] [RakNetTest.kt](file:///Users/shinseungmin/HybridMC/protocol-bedrock/src/test/kotlin/io/hybridmc/protocol/bedrock/raknet/RakNetTest.kt)
  - RED: UDP packets are ignored or handshake drops.
  - GREEN: Implement RakNet connection bootstrap, MTU negotiation, packet ordering, and frame assembly/resend logic.
  - Real-surface QA: Query Bedrock server list ping from Bedrock client and verify UDP pong is received.
  - Evidence: Bedrock client server list response logs.
  - Cleanup: Kill server.
  - Commit: YES
    - Message: "feat(protocol-bedrock): implement RakNet connection protocol T1.B1"

- [ ] Task T1.B2: Packet Batching & Compression
  - Files:
    - [NEW] [BedrockPacketCodec.kt](file:///Users/shinseungmin/HybridMC/protocol-bedrock/src/main/kotlin/io/hybridmc/protocol/bedrock/codec/BedrockPacketCodec.kt)
  - RED: Inbound batch wrapper (0xFE) is not decoded.
  - GREEN: Implement Zlib/Snappy batch envelope extractor and compression wrappers.
  - Real-surface QA: Capture client UDP packets and assert successful zlib envelope parsing in unit tests.
  - Evidence: Codec test execution.
  - Cleanup: None.
  - Commit: YES
    - Message: "feat(protocol-bedrock): implement game packet batching and compression T1.B2"

- [ ] Task T1.B3: Login Chain & Encryption
  - Files:
    - [NEW] [BedrockLoginHandler.kt](file:///Users/shinseungmin/HybridMC/protocol-bedrock/src/main/kotlin/io/hybridmc/protocol/bedrock/handler/BedrockLoginHandler.kt)
  - RED: Login token chains (JWT) are not authenticated.
  - GREEN: Verify client login token chains, exchange ECDH session key, activate AES encryption, and reply with resource-pack handshake success.
  - Real-surface QA: Connect Bedrock client and complete the login process up to spawn initiation.
  - Evidence: Terminal login sequence logs.
  - Cleanup: Kill server.
  - Commit: YES
    - Message: "feat(protocol-bedrock): implement Bedrock login chain validation and encryption T1.B3"

### Unified Convergence Wave (Runs after Java & Bedrock Tracks are completed)

- [ ] Task T1.U1: Unified PlayerConnection Abstraction
  - Files:
    - [NEW] [PlayerConnection.kt](file:///Users/shinseungmin/HybridMC/network/src/main/kotlin/io/hybridmc/network/PlayerConnection.kt)
    - [MODIFY] [Main.kt](file:///Users/shinseungmin/HybridMC/app/src/main/kotlin/io/hybridmc/app/Main.kt)
  - RED: Domain logic cannot register or communicate with connections generic to connection protocols.
  - GREEN: Implement interface `PlayerConnection` in `:network` module representing generic client operations. Bind both Java and Bedrock stack session states to it. Dispatches a unified `PlayerLoginEvent` when either completes login.
  - Real-surface QA: Launch server, connect both a Java client and a Bedrock client, and verify that the console outputs unified player join logs from the EventBus.
  - Evidence: Server log output matching both client connections.
  - Cleanup: Close server.
  - Commit: YES
    - Message: "feat(network): implement unified PlayerConnection abstraction T1.U1"

## Parallel Execution Waves

```text
Wave 1 (Parallel):
- Java Track: T1.J1 -> T1.J2 -> T1.J3 -> T1.J4 -> T1.J5 (in protocol-java)
- Bedrock Track: T1.B1 -> T1.B2 -> T1.B3 (in protocol-bedrock)

Wave 2 (Convergence):
- Convergence Track: T1.U1 (in network / app)
```

## Dependency Matrix

| Task | Depends on | Blocks | Can parallelize with |
|---|---|---|---|
| T1.J1 | none | T1.J2 | T1.B1 |
| T1.J2 | T1.J1 | T1.J3 | T1.B2 |
| T1.J3 | T1.J2 | T1.J4, T1.J5, T1.U1 | T1.B3 |
| T1.J4 | T1.J3 | none | T1.B3 |
| T1.J5 | T1.J3 | none | T1.B3 |
| T1.B1 | none | T1.B2 | T1.J1, T1.J2 |
| T1.B2 | T1.B1 | T1.B3 | T1.J3, T1.J4 |
| T1.B3 | T1.B2 | T1.U1 | T1.J5 |
| T1.U1 | T1.J3, T1.B3 | none | none |

## Final Verification Wave
- [ ] Run `./gradlew spotlessApply` to format all modules.
- [ ] Run `./gradlew build` to verify all tests and architecture layering rules.
- [ ] Verify both Java and Bedrock clients can login side-by-side.

Next: `start-work t1_milestone_1`
