# HybridMC 상세 작업 분해 (Task Breakdown)

[ROADMAP.md](ROADMAP.md)의 마일스톤을 **실제로 손대는 작은 작업 단위**로 쪼갠 문서.

## 표기 규칙 — 작업 단위마다 육하원칙

소프트웨어 작업에 맞게 매핑했다. 단독/소규모 개발 가정이라 **'누가'는 담당 모듈(어디)** 로 갈음하고,
대신 검증 가능한 **완료 기준(DoD)** 을 둔다.

| 필드 | 의미 |
|---|---|
| **무엇** (What) | 만들 산출물 |
| **왜** (Why) | 이게 왜 필요한가 |
| **어디** (Where) | 담당 모듈/패키지 |
| **어떻게** (How) | 구현 접근·핵심 타입·알고리즘 |
| **선행** (When) | 시작 전 끝나 있어야 할 작업(순서/병렬) |
| **DoD** | "끝났다"의 객관적 기준 |

작업 ID: `T<마일스톤>.<순번>` (예: `T0.3`은 M0의 3번 작업). `‖`는 다른 작업과 병렬 가능.

> 근거리(M0·M1·M2)는 풀 육하원칙으로, 먼 마일스톤(M3~M8)은 설계가 앞 단계 결과에 좌우되므로
> 작업 단위 체크리스트로 둔다. 각 마일스톤은 **착수 시점에** 이 형식으로 완전 분해한다.

---

# M0 — 기반 (부트 가능한 빈 서버)

목표: 빈 서버가 20 TPS로 부팅/종료. 선행: 없음(스캐폴드 완료).

### `T0.1` 바이트 버퍼 & 기본 코덱 · 선행: 없음
- **무엇**: `PacketBuffer` + read/write 헬퍼(bool, i8/16/32/64, f32/64, **VarInt/VarLong**, len-prefixed String, UUID, ByteArray, packed-long position).
- **왜**: 모든 프로토콜·NBT·청크 직렬화의 최하위 토대. 가장 먼저 있어야 위가 올라간다.
- **어디**: `:core` → `io.hybridmc.core.buffer`.
- **어떻게**: Netty `ByteBuf`를 자체 인터페이스로 감싼다(테스트 용이·에디션 무관). VarInt는 7비트 그룹+continuation bit.
- **DoD**: VarInt 경계값(0, 127, 128, -1, Int.MAX/MIN) 라운드트립 단위 테스트 통과.

### `T0.2` Identifier(ResourceLocation) · 선행: 없음 ‖
- **무엇**: `namespace:path` 식별자 value class + 파싱/검증.
- **왜**: 블록·아이템·엔티티·디멘션·레지스트리 키의 공통 표현.
- **어디**: `:core` → `io.hybridmc.core.Identifier`.
- **어떻게**: `@JvmInline value class`, 기본 네임스페이스 `minecraft`, 정규식 검증, 파싱 캐시.
- **DoD**: 정상/비정상 입력 파싱·검증 테스트.

### `T0.3` NBT 모델 + 직렬화(두 포맷) · 선행: T0.1
- **무엇**: 13종 태그 sealed 계층 + **Java(big-endian)** / **Bedrock(little-endian + VarInt)** 리더·라이터.
- **왜**: 청크·엔티티·아이템·레지스트리·디스크 저장이 전부 NBT 기반. 두 에디션이 바이트 순서부터 다르다.
- **어디**: `:core` → `io.hybridmc.core.nbt`.
- **어떻게**: `sealed interface NbtTag`(End/Byte/…/Compound=Map/List/IntArray/LongArray). 코덱은 `NbtFormat` 전략(JavaNetwork·JavaDisk·BedrockNetwork)으로 분리.
- **DoD**: 라운드트립(write→read 동일) + 알려진 `.nbt` 샘플 파싱 통과.

### `T0.4` 수학 타입 · 선행: 없음 ‖
- **무엇**: `Vec3d`, `Vec3i`, `BlockPos`, `ChunkPos`, `SectionPos`, `Direction`(6면), `AABB`, `Rotation`(yaw/pitch).
- **왜**: 위치·충돌·청크 좌표 변환의 공통 어휘.
- **어디**: `:core` → `io.hybridmc.core.math`.
- **어떻게**: 불변 value 타입 위주. `BlockPos ↔ long` 패킹, 블록→청크→섹션 좌표 변환 헬퍼.
- **DoD**: 좌표 변환·패킹 왕복 테스트.

### `T0.5` 레지스트리 프레임워크 · 선행: T0.2
- **무엇**: `Registry<T>`(Identifier ↔ T ↔ int rawId), freeze(동결) 개념.
- **왜**: 블록·아이템·엔티티 정의의 단일 저장소이자 네트워크 rawId 매핑의 기반.
- **어디**: `:registry` → `io.hybridmc.registry`.
- **어떻게**: 등록 단계 → freeze → 불변. int id는 데이터 기반으로 결정적 부여(에디션 매핑의 출발점).
- **DoD**: 등록/조회 + freeze 후 변경 거부 테스트.

### `T0.6` 이벤트 버스 · 선행: 없음 ‖
- **무엇**: 동기 이벤트 디스패처(타입 구독, 우선순위, 취소 가능).
- **왜**: 코어/게임/플러그인 간 느슨한 결합. 플러그인 API의 기초.
- **어디**: `:core` → `io.hybridmc.core.event`.
- **어떻게**: 타입별 핸들러 리스트 + 우선순위 정렬, `Cancellable`. 리플렉션 없는 명시적 등록.
- **DoD**: 우선순위 호출 순서·취소 전파 테스트.

### `T0.7` 스케줄러 & 틱 루프 · 선행: T0.6
- **무엇**: 고정 **20 TPS** 결정론적 메인 루프 + 태스크 스케줄러(delay/repeat).
- **왜**: 모든 시뮬레이션의 심장박동.
- **어디**: 스케줄러는 `:core`, 루프 소유는 `:server`.
- **어떻게**: 50ms 목표, 누적시간 기반 catch-up(상한 N틱), 과부하 시 TPS 경고. 단일 게임 스레드 가정.
- **DoD**: TPS 측정 + 인위적 지연 시 catch-up 동작 테스트.

### `T0.8` 설정 로딩 · 선행: 없음 ‖
- **무엇**: 서버 설정(포트, online-mode, 뷰 거리, 게임모드 등) 로드·검증.
- **왜**: 부팅 파라미터.
- **어디**: `:server` → `io.hybridmc.server.config`.
- **어떻게**: TOML(kotlinx-serialization) 또는 properties. 누락 시 기본값·검증.
- **DoD**: 누락/기본값/잘못된 값 처리 테스트.

### `T0.9` 부트스트랩 & graceful shutdown · 선행: T0.5, T0.7
- **무엇**: `main` → 서버 객체 생성 → 레지스트리 freeze → 틱 루프 시작 → SIGINT 시 정상 종료.
- **왜**: 실행 가능한 골격 완성(= M0 DoD).
- **어디**: `:app` + `:server`.
- **어떻게**: 종료 훅에서 진행 중 틱 마무리 후 리소스 정리.
- **DoD**: `./gradlew :app:run`이 20 TPS로 돌고 Ctrl+C에 깔끔히 종료.

---

# M1 — 핸드셰이크 & 로그인 (양 에디션)

목표: 자바·베드락 둘 다 서버목록 핑 + 로그인 성공(조인 직전)까지. 선행: M0.
자바 트랙(`T1.J*`)과 베드락 트랙(`T1.B*`)은 **서로 병렬**, 마지막에 `T1.U1`로 합류.

### `T1.J1` Netty 서버 + 프레이밍 (자바) · 선행: T0.1
- **무엇**: TCP 리스너, VarInt 길이-프리픽스 프레이밍, 상태별 패킷 인코더/디코더 골격.
- **왜**: 자바 클라이언트 연결 입구.
- **어디**: `:protocol-java`.
- **어떻게**: Netty `ServerBootstrap`, VarInt 기반 frame decoder 자체 구현, `ConnectionState`별 패킷 ID↔클래스 레지스트리.
- **DoD**: 원시 연결 수립 + 프레이밍 단위 테스트.

### `T1.J2` Handshake + Status(서버목록 핑) · 선행: T1.J1
- **무엇**: handshake 파싱 → status 분기, status request/response + ping/pong.
- **왜**: 자바 클라 멀티플레이 목록에 버전/MOTD/인원 표시.
- **어디**: `:protocol-java`.
- **어떻게**: 26.2 프로토콜 번호로 status JSON 구성.
- **DoD**: 실제 자바 클라이언트 서버목록에 정상 핑/MOTD 표시.

### `T1.J3` Login(오프라인) + Configuration 진입 · 선행: T1.J2
- **무엇**: login start → login success → configuration 상태 진입.
- **왜**: 플레이어 식별·세션 수립.
- **어디**: `:protocol-java`.
- **어떻게**: 먼저 offline-mode(오프라인 UUID 해시). 온라인 인증/암호화는 `T1.J5`로 분리.
- **DoD**: offline 자바 클라가 login success까지 도달.

### `T1.J4` 패킷 압축 · 선행: T1.J3
- **무엇**: set-compression 임계값, zlib 압축 패킷 경계 처리.
- **왜**: 청크 등 대용량 패킷에 필수.
- **어디**: `:protocol-java`.
- **DoD**: 임계값 초과 패킷 압축/해제 라운드트립.

### `T1.J5` online-mode 인증 + 암호화 (옵션) · 선행: T1.J3
- **무엇**: Mojang sessionserver join, AES/CFB8 암호화.
- **왜**: 정품 인증 서버 운영용.
- **어디**: `:protocol-java`.
- **DoD**: 정품 계정 접속 성공. (offline 동작 확인 후 진행)

### `T1.B1` RakNet(UDP) 연결 계층 (베드락) · 선행: T0.1
- **무엇**: unconnected ping/pong, open-connection req/reply 1·2, connection request, ACK/NAK, **신뢰성·순서·분할 재조립**.
- **왜**: 베드락 전송 토대. M1에서 가장 큰 작업.
- **어디**: `:protocol-bedrock` → `raknet`.
- **어떻게**: Netty UDP datagram + RakNet 상태머신, MTU 협상, datagram 시퀀스/재전송.
- **DoD**: 베드락 클라 서버목록 표시 + RakNet 핸드셰이크 완료.

### `T1.B2` 패킷 batch + 압축 · 선행: T1.B1
- **무엇**: 게임패킷 batch(`0xFE`) wrapper, zlib/snappy 압축, 인코더/디코더.
- **왜**: 모든 베드락 게임패킷이 batch로 도착.
- **어디**: `:protocol-bedrock`.
- **DoD**: batch 디코드/인코드 라운드트립.

### `T1.B3` 로그인 체인 + 암호화 · 선행: T1.B2
- **무엇**: login JWT chain 파싱, ECDH 키교환 → AES 암호화, network settings, resource-pack 핸드셰이크(빈 팩), play status.
- **왜**: 베드락 세션 수립.
- **어디**: `:protocol-bedrock`.
- **어떻게**: 먼저 offline(자체 인증). resource pack은 빈 응답으로 스킵.
- **DoD**: offline 베드락 클라가 join 직전까지 진행.

### `T1.U1` 통합 세션 추상화(PlayerConnection) · 선행: T1.J3, T1.B3
- **무엇**: 에디션 무관 연결 인터페이스(send/disconnect/주소/edition/state) + 로그인 완료 이벤트.
- **왜**: 상위(`:network`/`:server`)가 에디션을 몰라도 되게 만든다.
- **어디**: `:network`.
- **어떻게**: 양 프론트엔드가 구현, 공통 `LoginEvent`로 핸드셰이크 결과 정규화.
- **DoD**: 두 에디션 로그인이 **동일 콜백**으로 수렴.

---

# M2 — 최소 인게임 진입 ★크로스플레이 검증★

목표: 자바·베드락 유저가 같은 평면 월드에서 서로를 보고/움직이고/채팅. 선행: M1.

### `T2.1` 블록스테이트 canonical 모델 + 최소 매핑 · 선행: T0.5
- **무엇**: 통합 블록스테이트(블록+프로퍼티) → 자바 global state id / 베드락 runtime id 매핑(공기·돌·풀 등 최소셋).
- **왜**: 청크를 양 에디션 와이어로 직렬화하려면 매핑이 선행되어야 한다.
- **어디**: canonical은 `:registry`, 매핑 테이블은 빌드타임 데이터 파이프라인.
- **어떻게**: 공개 데이터셋에서 생성. 최소셋 먼저, 전체는 `T3` 매핑 작업에서.
- **DoD**: 같은 블록이 양 클라에서 동일 블록으로 렌더.

### `T2.2` 월드/청크 인메모리 모델 · 선행: T0.4
- **무엇**: `Chunk`(16×16×높이), `ChunkSection`(16³ paletted), Heightmap, `World` 컨테이너.
- **왜**: 표시할 지형 데이터 구조.
- **어디**: `:world`.
- **어떻게**: 섹션별 팔레트 + bit-packed storage(fastutil). 생성은 일단 슈퍼플랫.
- **DoD**: 평면 청크 생성/블록 조회 테스트.

### `T2.3` 청크 직렬화 — 자바 섹션 · 선행: T2.1, T2.2
- **무엇**: 자바 chunk-data 패킷(섹션·팔레트·heightmap·라이팅 더미).
- **어디**: `:protocol-java`.
- **DoD**: 자바 클라가 평면 월드 렌더.

### `T2.4` 청크 직렬화 — 베드락 subchunk · 선행: T2.1, T2.2
- **무엇**: LevelChunk + SubChunkRequest 흐름, 베드락 subchunk 포맷, runtime id 매핑 적용.
- **어디**: `:protocol-bedrock`.
- **DoD**: 베드락 클라가 평면 월드 렌더.

### `T2.5` 플레이어 스폰 & 초기 상태 · 선행: T2.3, T2.4
- **무엇**: join/start-game 패킷, 스폰 위치, 게임모드, 좌표 동기화, 청크 게시 반경.
- **어디**: 양 protocol + `:server`.
- **DoD**: 양 클라가 같은 좌표에 스폰.

### `T2.6` 이동 동기화 · 선행: T2.5
- **무엇**: 클라 이동 패킷 수신 → 서버 위치 갱신 → 타 플레이어에 스폰/이동/회전 broadcast.
- **왜**: 서로를 보고 움직이는 핵심.
- **어디**: `:server` + 양 protocol(엔티티 패킷 차이 흡수).
- **어떻게**: 다른 플레이어를 각 에디션의 player entity로 스폰, 메타데이터 최소.
- **DoD**: 자바↔베드락 유저가 서로의 아바타 움직임을 본다.

### `T2.7` 채팅 브로드캐스트 · 선행: T2.5
- **무엇**: 채팅 수신 → 전체 전송(에디션별 text 패킷).
- **어디**: `:server` + 양 protocol.
- **DoD**: 한쪽 채팅이 양쪽에 표시.

> **M2 DoD ★ = 프로젝트 가설 증명**: 자바 유저와 베드락 유저가 같은 평면 월드에서 서로를 보고, 움직이고, 채팅한다.

---

# M3~M8 — 작업 단위 체크리스트 (착수 시 풀 육하원칙 분해)

## M3 — 블록/월드 상호작용 · 선행: M2
- [ ] 전체 블록·아이템 매핑 테이블 완성(데이터 파이프라인 확장) — `:registry`
- [ ] 블록 설치/파괴: 클라 의도 → 서버 권위 검증 → 이웃 broadcast, 디깅 진행도 — `:server`/`:game`
- [ ] 인벤토리/창 시스템(에디션별 window 차이 흡수), 핫바, 아이템 스택 모델+NBT — `:game`/양 protocol
- [ ] 블록 엔티티(상자·화로 등) 모델 + 동기화 — `:world`/`:game`
- [ ] 청크 영속화(자체 region 포맷) + 비동기 로드/언로드/저장 — `:world`
- [ ] 광원(블록/스카이 라이트) 계산·전파 — `:world`
- **DoD**: 크리에이티브 수준 건축이 양 에디션에서 동기화.

## M4 — 엔티티 & 전투 기초 · 선행: M3
- [ ] 엔티티 컴포넌트/타입 레지스트리, 엔티티 추적(뷰어 관리) — `:entity`
- [ ] 메타데이터 동기화(자바 index ↔ 베드락 actor-data 매핑) — `:entity`/양 protocol
- [ ] 아이템 엔티티 드랍/픽업/머지 — `:entity`/`:game`
- [ ] 속성(체력/이동속도), 데미지/넉백/사망/리스폰 — `:game`
- [ ] 기본 근접 전투(쿨다운 **canonical 정책 결정**), 투사체 기초 — `:game`
- **DoD**: 몹 없는 PvP + 아이템 드랍/픽업.

## M5 — 월드 생성 · 선행: M3
- [ ] 자체 노이즈(Perlin/Simplex) + 시드 시스템 — `:worldgen`
- [ ] 바이옴 분포, 높이맵 지형, 표면 규칙 — `:worldgen`
- [ ] 광물/광맥 배치, 동굴(노이즈/캐리어) — `:worldgen`
- [ ] 초기 구조물(나무 등) + 비동기 청크 생성 파이프라인 — `:worldgen`/`:world`
- **DoD**: 탐험 가능한 절차적 월드.

## M6 — 몹 & 생존 루프 · 선행: M4, M5
- [ ] 몹 스폰 규칙(밝기/디멘션/캡)·디스폰 — `:game`/`:entity`
- [ ] 경로탐색(A*/점프), goal 기반 AI 스케줄러, 시야/타게팅 — `:entity`
- [ ] 패시브/적대 몹 세트, 낮밤·수면 — `:game`/`:entity`
- [ ] 크래프팅/제련/인챈트/양조 레시피 + 경험치 — `:game`
- [ ] 네더/엔드 디멘션 + 포탈 — `:server`/`:world`
- **DoD**: 처음부터 끝까지 생존 루프 성립.

## M7 — 시뮬레이션 심화(= 바닐라 파리티 본진) · 선행: M6
- [ ] 레드스톤(신호/컴포넌트/업데이트 순서) — `:game`
- [ ] 유체(물/용암 확산·소스), 블록 업데이트 큐/random tick — `:game`/`:world`
- [ ] 농사/번식/성장, 날씨/번개, 통계·업적 — `:game`
- [ ] 파리티 디테일 롱테일(사실상 무한 반복)
- **DoD**: 핵심 바닐라 메커니즘 재현, 양 에디션 동작.

## M8 — 운영·성능·도구 · 선행: M2+ (지속)
- [ ] 콘솔/명령 파서(자체 brigadier류) + 권한 시스템 — `:server`
- [ ] 메트릭/프로파일링, 청크 비동기 IO 최적화 — `:server`/`:world`
- [ ] 영역기반 멀티스레딩 검토, 설정 핫리로드, 백업 — `:server`
- **DoD**: 운영 가능한 서버.

## 병렬 트랙 — 플러그인 API · 선행: M2부터 지속
- [ ] M2~M7 동안 내부 인터페이스를 `:api`에 "공개 가능한 형태"로 노출(이벤트/명령/스케줄러/월드·엔티티·플레이어 핸들/인벤토리)
- [ ] 에디션 차이 추상화(예: `player.sendForm(...)` — 베드락 네이티브 Form, 자바 폴백)
- [ ] M8에서 로더/클래스로딩/격리/권한 정식화 — `:plugin-host`
