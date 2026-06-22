# AGENTS.md

이 파일은 AI 코딩 에이전트(Gemini, Codex 등)가 이 저장소에서 작업할 때 참고하는 가이드다.

---

## 프로젝트 개요

**HybridMC**는 마인크래프트 Java Edition + Bedrock Edition을 **단일 서버에서 네이티브로 동시 지원**하는 클린룸 서버 구동기다. Mojang 서버 코드를 일절 사용하지 않고 처음부터 Kotlin/JVM으로 구현한다.

### 확정된 핵심 결정

| 축 | 결정 |
|---|---|
| 크로스플레이 | 네이티브 듀얼 스택 — 하나의 통합 코어 위에 Java·Bedrock 프로토콜 둘 다 네이티브 구현 |
| 타겟 버전 | Java·Bedrock **26.2** 단일 고정 (다버전 추상화 없음) |
| 플러그인 API | 독자 API 신규 설계 (Bukkit/Spigot 호환 아님) |
| MVP 범위 | 바닐라 서바이벌 파리티 — 수직 슬라이스 마일스톤(M0–M8)으로 분할 |

### 현재 상태

빌드 스캐폴드가 완성되어 모든 quality gate를 통과하지만, **게임 로직은 거의 없다.** DI 골격(`:core` 서비스 SPI, `:server`, `:app` 구성 루트)만 존재한다. 다음 목표는 **M0** (부트 가능한 빈 서버).

---

## 아키텍처 — canonical 레지스트리 + 에디션 매핑

핵심 엔지니어링 원칙: **단일 진실원본(통합 내부 모델) → 에디션별 프론트엔드 직렬화**.

Java와 Bedrock은 와이어 레벨에서 거의 다른 게임이다(TCP vs RakNet/UDP, global state ID vs runtime ID, 섹션 vs subchunk 등). 모든 것을 하나의 canonical 모델로 수렴시키고, 각 프로토콜 모듈에서 에디션 포맷으로 변환한다.

매 기능은 **"통합 모델 구현 → Java 직렬화 → Bedrock 직렬화"** 3겹 순서로 구현한다.

### 클린룸 원칙

> **절대 준수**: Mojang 서버 코드, 디컴파일 코드를 참조하거나 사용하지 않는다.  
> 블록·아이템·엔티티 정의와 매핑 데이터는 공개 데이터셋(`minecraft-data`(PrismarineJS), Geyser/Cloudburst 매핑 테이블, 공식 data report)에서 소싱하여 **빌드 타임 파이프라인**으로 레지스트리를 생성한다.

### 의존성 역전 규칙 (CI가 강제)

- `:server`는 구체 도메인 모듈(`:world`, `:game` 등)을 **절대 직접 import하지 않는다**. `:core`의 `Subsystem`/`ServiceRegistry` SPI를 통해서만 구동한다.
- `:app`(구성 루트)만 모든 도메인 모듈에 의존하고 subsystem을 등록한다.
- `:architecture-tests`(Konsist)가 이 규칙을 빌드에서 강제한다 — 위반 시 빌드 실패.

---

## 모듈 구조

```
build-logic/          included build — 컨벤션 플러그인(빌드 설정의 단일 원천)
:core                 수학(Vec3d/BlockPos/AABB), NBT, 바이트 버퍼/VarInt 코덱, 이벤트 버스, 스케줄러, Subsystem/ServiceRegistry DI SPI
:registry             canonical 블록스테이트/아이템/엔티티 레지스트리 + 에디션 매핑 + 데이터 파이프라인 (kotlinx-serialization)
:protocol-java        Java 26.2 패킷 코덱 + 상태 머신 (Netty/TCP)
:protocol-bedrock     Bedrock 26.2 RakNet + 패킷 코덱 + 로그인 체인 (Netty/UDP)
:network              통합 PlayerConnection 추상화; 양 프론트엔드 바인딩
:world / :worldgen    월드/청크/블록스테이트 모델 + 청크 IO; 지형 생성
:entity / :game       엔티티 컴포넌트 시스템; 크래프팅, 전투, 레드스톤, 유체, 물리
:api                  ★ 공개 플러그인 API — published, explicitApi, ABI 추적
:server               라이프사이클 + 틱 루프; :core (Subsystem SPI), :api, :network에만 의존
:plugin-host          플러그인 로더/클래스로딩/격리/생명주기
:app                  구성 루트 + 실행 진입점; 모든 도메인을 연결하는 유일한 모듈
:architecture-tests   Konsist 테스트 — 모듈 의존 방향 규칙 강제
```

---

## 빌드 명령

Gradle wrapper 사용 (`./gradlew`), **Gradle 9.5.0** 고정.

```bash
# 서버 실행
./gradlew :app:run

# 전체 빌드 + quality gate (spotless, 테스트, apiCheck, kover)
./gradlew build

# 코드 포맷 자동 적용 (커밋 전 필수)
./gradlew spotlessApply

# 단일 테스트 클래스 실행
./gradlew :<module>:test --tests "FQCN"

# 플러그인 API ABI 베이스라인 재생성 (의도적 API 변경 후)
./gradlew :api:apiDump

# 플러그인 API ABI 드리프트 검증
./gradlew :api:apiCheck

# 커버리지 리포트
./gradlew koverHtmlReport

# 모듈 트리 확인
./gradlew projects
```

---

## 코딩 컨벤션 — 반드시 따를 것

### 빌드 설정

1. **빌드 설정은 `build-logic` 컨벤션 플러그인에만 둔다.** 개별 모듈 `build.gradle.kts`에 Kotlin/테스트/품질 설정을 직접 추가하지 않는다. 변경이 필요하면 컨벤션 플러그인을 수정한다.
2. 컨벤션 플러그인 종류:
   - `hybridmc.kotlin-library` — 라이브러리 + `explicitApi()`
   - `hybridmc.kotlin-application` — 애플리케이션
   - `hybridmc.published-library` — 라이브러리 + `maven-publish` (`:api`용)
   - `hybridmc.kotlin-serialization` — kotlinx-serialization 오버레이 (`:registry`용)
3. **`explicitApi()`가 모든 라이브러리 모듈에 적용된다** — public 선언에는 명시적 가시성과 반환 타입이 필수다.
4. **버전은 `gradle/libs.versions.toml`에서만 관리한다.** build-logic 클래스패스의 Gradle 플러그인 버전도 포함.
5. **저장소(repositories)는 `settings.gradle.kts`에서 중앙 선언(`FAIL_ON_PROJECT_REPOS`)** — 모듈 build script에 `repositories {}` 블록 추가 금지.

### `:api` 모듈 (플러그인 API)

- `:api`는 ABI 추적 대상이다. public surface를 변경하면 반드시:
  1. `./gradlew :api:apiDump` 실행
  2. 생성된 `api/api.api` 파일 커밋
  - 이를 빠뜨리면 `apiCheck`(CI)가 실패한다.

### Configuration Cache

- `gradle.properties`에서 configuration cache, parallel, build cache가 활성화되어 있다.
- 빌드 로직과 컨벤션 플러그인은 **configuration-cache 호환**이어야 한다 — 실행 시점에 mutable state를 읽거나 `Project`를 참조하지 않는다.

### 코드 스타일

- **포맷/린트**: Spotless + ktlint. 커밋 전 `./gradlew spotlessApply` 실행.
- **detekt 미적용**: detekt 최신 안정판(1.23.8)이 Kotlin 2.4.0을 지원하지 않아 의도적으로 제외됨. ktlint가 린트 게이트 역할. detekt가 K2/2.4를 지원하면 `hybridmc.kotlin-common`에 추가 예정.
- **로깅**: `io.github.oshai.kotlinlogging.KotlinLogging` (SLF4J 위). `:app`이 런타임에 logback 제공.
- **줄 끝**: `gradlew`는 LF, `*.bat`는 CRLF (`.gitattributes`로 관리).
- **gitignore**: `.gradle/`, `build/`, `.kotlin/`.

### 테스트

- JUnit 5 (`junit-jupiter`) 사용.
- 커버리지는 Kover로 측정, 루트에서 전 모듈 집계.

---

## Quality Gate (CI가 실행)

CI(`.github/workflows/ci.yml`)가 push/PR마다 실행하는 게이트:

| 게이트 | 도구 | 범위 |
|---|---|---|
| 포맷/린트 | Spotless + ktlint | 전 모듈 |
| 테스트 | JUnit 5 | 전 모듈 |
| ABI 호환 | binary-compatibility-validator | `:api`만 |
| 커버리지 | Kover | 전 모듈 (루트 집계) |
| 아키텍처 | Konsist | `:architecture-tests` |

**모든 게이트를 통과해야 PR 머지 가능.**

---

## 기술 스택

| 항목 | 기술 |
|---|---|
| 언어/런타임 | Kotlin/JVM, JDK 21 toolchain |
| 네트워크 | Java = Netty(TCP), Bedrock = RakNet(Netty UDP) — 둘 다 Netty 파이프라인으로 통일 |
| 동시성 | 코루틴 기반 IO. 게임 틱은 결정론적 단일 스레드 + 비동기 IO (영역 기반 멀티스레딩은 M8) |
| 직렬화/저장 | 자체 NBT 구현, 월드 저장은 자체 포맷 (Anvil 호환 비권장) |
| 빌드 | Gradle 9.5.0, Kotlin DSL, configuration cache 활성 |
| 산출물 | `:app` 모듈의 단일 실행 jar |

---

## 마일스톤 로드맵 (요약)

| 마일스톤 | 목표 | 완료 기준 (DoD) |
|---|---|---|
| **M0** ← 현재 | 부트 가능한 빈 서버 | `./gradlew :app:run` → 20 TPS 빈 틱 루프 |
| M1 | 핸드셰이크 & 로그인 (양 에디션) | Java·Bedrock 서버 핑 + 로그인 |
| M2 ★ | 최소 인게임 (크로스플레이 검증) | 양 에디션 유저가 같은 월드에서 이동·채팅 |
| M3 | 블록/월드 상호작용 | 크리에이티브 건축 동기화 |
| M4 | 엔티티 & 전투 기초 | 몹 없는 PvP, 아이템 드랍 |
| M5 | 월드 생성 | 탐험 가능한 절차적 월드 |
| M6 | 몹 & 생존 루프 | 생존 루프 완성 |
| M7 | 시뮬레이션 심화 | 바닐라 파리티 |
| M8 | 운영·성능·도구 | 운영 가능한 서버 |

상세 작업 분해는 [TASKS.md](TASKS.md), 전략 개요는 [ROADMAP.md](ROADMAP.md) 참고.

---

## 에이전트를 위한 작업 체크리스트

코드를 작성하거나 수정할 때 아래를 반드시 확인:

- [ ] 변경 대상 모듈이 올바른 컨벤션 플러그인을 적용하고 있는가?
- [ ] public 선언에 명시적 가시성(`public`/`internal`)과 반환 타입이 있는가? (`explicitApi()`)
- [ ] `:server`에서 구체 도메인 모듈을 직접 import하지 않았는가?
- [ ] `:api` public surface를 변경했다면 `apiDump` + `api.api` 파일 커밋?
- [ ] `./gradlew spotlessApply` 실행 후 포맷이 정리되었는가?
- [ ] `./gradlew build`가 통과하는가?
- [ ] Mojang 디컴파일 코드를 참조하지 않았는가? (클린룸 원칙)
- [ ] 모듈 build script에 `repositories {}` 블록을 추가하지 않았는가?
- [ ] 버전을 `libs.versions.toml` 외부에서 하드코딩하지 않았는가?
