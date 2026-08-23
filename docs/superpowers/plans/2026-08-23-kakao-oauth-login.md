# 카카오 OAuth 로그인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 카카오 계정으로 PlanFix에 로그인할 수 있게 하고, 자체 로그인과 동일한 HttpOnly 쿠키 세션으로 통일한다.

**Architecture:** Spring Security의 `oauth2-client`를 쓰지 않고 `RestClient`로 카카오 API를 직접 호출한다. `SecurityConfig`가 STATELESS라 OAuth의 `state`와 PKCE `code_verifier`는 5분짜리 HttpOnly 쿠키(`oauth_tx`)에 담아 왕복시킨다. 인증이 끝나면 자체 JWT를 발급해 `access_token` 쿠키에 심고 프론트로 302한다. 계층은 기존 구조(domain / application / infrastructure / interfaces)를 그대로 따른다.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, PostgreSQL 17, JUnit 5, AssertJ, `MockRestServiceServer`

**Spec:** `docs/superpowers/specs/2026-08-23-kakao-oauth-login-design.md`

## Global Constraints

- Java 21 (`build.gradle`의 toolchain). 터미널에서 Gradle 실행 시 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`가 필요하다. 기본 `JAVA_HOME`이 JDK 11이라 없으면 빌드가 즉시 실패한다.
- 새 라이브러리 의존성을 추가하지 않는다. JWT를 직접 구현한 기존 방침(`JwtTokenProvider`)을 따른다. HTTP 호출은 `spring-boot-starter-webmvc`에 포함된 `RestClient`를 쓴다.
- 모든 public 메서드와 클래스에 한 줄짜리 한글 Javadoc을 단다. 기존 코드 전부가 이 규칙을 지키고 있다.
- 테스트 메서드명은 한글 스네이크케이스로 쓴다(예: `void 미인증_이메일은_기존_계정에_연결하지_않는다()`). 단언은 AssertJ `assertThat`을 쓴다.
- 도메인 모델은 불변이다. 변경 메서드는 새 인스턴스를 반환한다.
- 에러는 `CoreException(ErrorType.X, "메시지")`로 던진다. `ErrorType`에 있는 값만 쓴다: `INTERNAL_ERROR`, `BAD_REQUEST`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`.
- 비밀값(`client-id`, `client-secret`)은 `application-secret.yml`에만 둔다. 이 파일은 gitignore 대상이며 **절대 커밋하지 않는다**. 테스트에도 실제 키를 넣지 않는다.
- `backend/src/main/resources/application-local.yml`은 커밋하지 않는다. 작업 시작 시점에 TourAPI 키가 하드코딩된 미커밋 변경이 들어 있다. `git add .`를 쓰지 말고 파일을 명시해서 add한다.
- 쿠키 이름은 `access_token`, `oauth_tx` 두 개로 고정한다.
- 실제 카카오 서버를 호출하는 테스트를 작성하지 않는다.

---

## File Structure

**신규**

| 파일 | 책임 |
|---|---|
| `domain/user/SocialProvider.java` | 소셜 provider enum |
| `domain/user/SocialAccountModel.java` | 소셜 계정 연결 도메인 모델 |
| `domain/user/SocialAccountRepository.java` | 소셜 계정 저장소 포트 |
| `infrastructure/user/SocialAccountRepositoryImpl.java` | 포트의 JPA 어댑터 |
| `infrastructure/oauth/KakaoOAuthProperties.java` | `oauth.kakao.*` 설정 바인딩 |
| `infrastructure/oauth/KakaoOAuthClient.java` | 인가 URL 생성 / 토큰 교환 / 사용자 조회 |
| `infrastructure/oauth/KakaoUser.java` | 카카오 사용자 정보 (내부 표현) |
| `infrastructure/oauth/PkceGenerator.java` | state / code_verifier / code_challenge 생성 |
| `infrastructure/oauth/OAuthTransaction.java` | state+code_verifier의 쿠키 직렬화 |
| `infrastructure/security/CookieFactory.java` | 쿠키 속성 일원화 |
| `application/auth/SocialLoginApplicationService.java` | 계정 조회/연결/생성 알고리즘 |

**수정**

| 파일 | 변경 |
|---|---|
| `domain/user/UserModel.java` | `name` 추가, `username` 검증 교체 |
| `domain/user/UserRepository.java` | `findByEmail` 추가 |
| `infrastructure/user/UserJpaEntity.java` | `name` 컬럼 |
| `infrastructure/user/UserJpaRepository.java` | `findByEmail` |
| `infrastructure/user/UserRepositoryImpl.java` | `name` 매핑, `findByEmail` |
| `infrastructure/user/UserSocialAccountJpaRepository.java` | 조회 메서드 |
| `infrastructure/security/JwtAuthenticationFilter.java` | 쿠키 우선 + 헤더 fallback |
| `infrastructure/security/SecurityConfig.java` | CORS, 신규 경로 permitAll |
| `application/user/UserCommand.java`, `UserResult.java`, `UserApplicationService.java` | `name` 전달 |
| `application/auth/AuthResult.java`, `AuthApplicationService.java` | 사용자 정보 포함 |
| `interfaces/api/user/UserRequest.java`, `UserResponse.java` | `name` |
| `interfaces/api/auth/AuthController.java`, `AuthResponse.java` | 쿠키 발급, 로그아웃, 카카오 엔드포인트 |
| `resources/application.yaml` | `oauth.kakao.*`, `app.*` |
| `resources/application-prod.yml` | 운영 환경변수 |
| `frontend/src/services/auth.ts` | 경로·타입 정정 |
| `frontend/src/App.tsx` | 목업 라우트 제거 |
| `frontend/.env.example` | `/api/v1` |

**삭제**: `frontend/src/pages/kakao-login-page.tsx`

---

### Task 1: UserModel에 name 도입, username 검증 교체

`username`의 한글 전용 검증을 신설 `name`으로 옮기고, `username`은 닉네임 규칙(길이 + 공백)으로 바꾼다. 도메인만 건드리며 아직 컴파일이 깨진다 — 호출부는 Task 2에서 고친다.

**Files:**
- Modify: `backend/src/main/java/taedonghee/plan_fix/domain/user/UserModel.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/domain/user/UserModelTest.java` (신규)

**Interfaces:**
- Consumes: 없음
- Produces:
  - `UserModel.create(String username, String name, String email)`
  - `UserModel.reconstruct(Long userId, String username, String name, String email, UserRole role, UserStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt)`
  - `UserModel.updateProfile(String username, String name, String email)`
  - `String UserModel.getName()`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/taedonghee/plan_fix/domain/user/UserModelTest.java`:

```java
package taedonghee.plan_fix.domain.user;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserModelTest {

    @Test
    void 소셜_가입은_name_없이_생성된다() {
        UserModel user = UserModel.create("hong gildong", null, null);

        assertThat(user.getUsername()).isEqualTo("hong gildong");
        assertThat(user.getName()).isNull();
        assertThat(user.getEmail()).isNull();
    }

    @Test
    void username은_한글_영문_숫자_이모지를_모두_허용한다() {
        assertThat(UserModel.create("길동", null, null).getUsername()).isEqualTo("길동");
        assertThat(UserModel.create("gildong99", null, null).getUsername()).isEqualTo("gildong99");
        assertThat(UserModel.create("여행가 🧳", null, null).getUsername()).isEqualTo("여행가 🧳");
    }

    @Test
    void username은_앞뒤_공백을_허용하지_않는다() {
        assertThatThrownBy(() -> UserModel.create(" 길동", null, null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("username");
    }

    @Test
    void username은_연속_공백을_허용하지_않는다() {
        assertThatThrownBy(() -> UserModel.create("홍  길동", null, null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("username");
    }

    @Test
    void username은_개행을_허용하지_않는다() {
        assertThatThrownBy(() -> UserModel.create("홍\n길동", null, null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("username");
    }

    @Test
    void username은_2자_미만이거나_30자를_넘을_수_없다() {
        assertThatThrownBy(() -> UserModel.create("가", null, null))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> UserModel.create("가".repeat(31), null, null))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void name은_한글만_허용한다() {
        assertThat(UserModel.create("gildong", "홍길동", null).getName()).isEqualTo("홍길동");

        assertThatThrownBy(() -> UserModel.create("gildong", "Hong", null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("name");
    }

    @Test
    void 프로필_수정은_username과_name을_함께_바꾼다() {
        UserModel user = UserModel.create("gildong", null, null);

        UserModel updated = user.updateProfile("길동이", "홍길동", "a@b.com");

        assertThat(updated.getUsername()).isEqualTo("길동이");
        assertThat(updated.getName()).isEqualTo("홍길동");
        assertThat(updated.getEmail()).isEqualTo("a@b.com");
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && ./gradlew test --tests '*UserModelTest*'
```

Expected: 컴파일 실패. `create(String, String, String)` 및 `getName()`이 없다.

- [ ] **Step 3: UserModel 수정**

`UserModel.java`의 상수·필드·생성자·검증을 아래로 교체한다. `userId`, `role`, `status`, `createdAt`, `updatedAt`, `withdraw()`, 기존 `validateEmail`과 게터들은 그대로 둔다.

```java
    private static final int USERNAME_MIN_LENGTH = 2;
    private static final int USERNAME_MAX_LENGTH = 30;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 30;
    private static final int EMAIL_MAX_LENGTH = 255;

    // 실명 형식 검증: 한글만 허용
    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣]+$");

    // 닉네임에 제어문자(개행·탭 포함) 사용 여부 확인
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile(".*\\p{Cntrl}.*");

    private final Long userId;
    private final String username;
    private final String name;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private UserModel(
            Long userId,
            String username,
            String name,
            String email,
            UserRole role,
            UserStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateUsername(username);
        validateName(name);
        validateEmail(email);

        this.userId = userId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role == null ? UserRole.USER : role;
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 사용자 생성
     */
    public static UserModel create(String username, String name, String email) {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserModel(null, username, name, email, UserRole.USER, UserStatus.ACTIVE, now, now);
    }

    /**
     * 저장된 사용자 정보를 기반으로 UserModel 복원
     */
    public static UserModel reconstruct(
            Long userId,
            String username,
            String name,
            String email,
            UserRole role,
            UserStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new UserModel(userId, username, name, email, role, status, createdAt, updatedAt);
    }

    /**
     * 사용자 프로필 수정
     */
    public UserModel updateProfile(String username, String name, String email) {
        if (status == UserStatus.WITHDRAWN) {
            throw new CoreException(ErrorType.CONFLICT, "탈퇴한 사용자는 수정할 수 없습니다. userId=" + userId);
        }
        return new UserModel(userId, username, name, email, role, status, createdAt, OffsetDateTime.now());
    }

    /**
     * username 필수값, 길이, 공백 및 제어문자 검증
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 필수입니다.");
        }
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 2자 이상 30자 이하여야 합니다.");
        }
        if (!username.equals(username.strip())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 앞뒤에 공백을 사용할 수 없습니다.");
        }
        if (username.contains("  ")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 연속된 공백을 사용할 수 없습니다.");
        }
        if (CONTROL_CHAR_PATTERN.matcher(username).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username에는 개행이나 제어문자를 사용할 수 없습니다.");
        }
    }

    /**
     * name 형식 및 길이 검증 (소셜 가입자는 null 허용)
     */
    private void validateName(String name) {
        if (name == null) {
            return;
        }
        if (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "name은 2자 이상 30자 이하여야 합니다.");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "name은 한글만 사용할 수 있습니다.");
        }
    }

    public String getName() {
        return name;
    }
```

주의: `CONTROL_CHAR_PATTERN.matcher(...).matches()`는 `.`이 기본적으로 개행에 매칭되지 않으므로 `"홍\n길동"`에 대해 false를 반환한다. `Pattern.DOTALL`을 켜야 한다. 상수를 다음으로 쓴다:

```java
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile(".*\\p{Cntrl}.*", Pattern.DOTALL);
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd backend && ./gradlew test --tests '*UserModelTest*'
```

Expected: 8개 테스트 PASS. (다른 파일은 아직 컴파일이 깨져 있을 수 있으나 `--tests` 필터로 이 클래스만 돌린다. 전체 컴파일이 막히면 Task 2까지 진행 후 함께 확인한다.)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/domain/user/UserModel.java \
        backend/src/test/java/taedonghee/plan_fix/domain/user/UserModelTest.java
git commit -m "feat: UserModel에 name 추가하고 username을 닉네임 규칙으로 변경"
```

---

### Task 2: name을 영속·API 계층까지 반영하고 DB 백필

Task 1로 깨진 호출부를 모두 고치고, 기존 1행을 백필한다.

**Files:**
- Modify: `infrastructure/user/UserJpaEntity.java`, `infrastructure/user/UserRepositoryImpl.java`, `application/user/UserCommand.java`, `application/user/UserResult.java`, `application/user/UserApplicationService.java`, `interfaces/api/user/UserRequest.java`, `interfaces/api/user/UserResponse.java`
- Create: `docs/migrations/2026-08-23-add-users-name.sql`

**Interfaces:**
- Consumes: Task 1의 `UserModel.create/reconstruct/updateProfile/getName`
- Produces:
  - `UserCommand.Create(String username, String name, String email, String loginId, String password)`
  - `UserCommand.Update(String username, String name, String email)`
  - `UserResult(Long userId, String username, String name, String email, UserRole role, UserStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt)`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/taedonghee/plan_fix/application/user/UserApplicationServiceTest.java` (신규). `UserApplicationService`는 포트에만 의존하므로 스텁 구현으로 테스트한다.

```java
package taedonghee.plan_fix.application.user;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.domain.user.PasswordEncryptor;
import taedonghee.plan_fix.domain.user.UserCredentialModel;
import taedonghee.plan_fix.domain.user.UserCredentialRepository;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserApplicationServiceTest {

    @Test
    void 자체_가입은_username을_loginId로_초기화하고_name을_저장한다() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        UserApplicationService service = new UserApplicationService(
                users, new StubCredentialRepository(), new PlainPasswordEncryptor());

        UserResult result = service.create(
                new UserCommand.Create("gildong01", "홍길동", "a@b.com", "gildong01", "pass1234"));

        assertThat(result.username()).isEqualTo("gildong01");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    static class InMemoryUserRepository implements UserRepository {
        private final List<UserModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public UserModel save(UserModel user) {
            UserModel stored = UserModel.reconstruct(
                    user.getUserId() == null ? ++sequence : user.getUserId(),
                    user.getUsername(), user.getName(), user.getEmail(),
                    user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<UserModel> findByUserId(Long userId) {
            return saved.stream().filter(u -> u.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<UserModel> findByEmail(String email) {
            return saved.stream().filter(u -> email.equals(u.getEmail())).findFirst();
        }

        @Override
        public List<UserModel> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public boolean existsByUsername(String username) {
            return saved.stream().anyMatch(u -> u.getUsername().equals(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return saved.stream().anyMatch(u -> email.equals(u.getEmail()));
        }
    }

    static class StubCredentialRepository implements UserCredentialRepository {
        @Override
        public UserCredentialModel save(UserCredentialModel credential) {
            return credential;
        }

        @Override
        public Optional<UserCredentialModel> findByLoginId(String loginId) {
            return Optional.empty();
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return false;
        }
    }

    static class PlainPasswordEncryptor implements PasswordEncryptor {
        @Override
        public String encrypt(String rawPassword) {
            return rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encryptedPassword) {
            return rawPassword.equals(encryptedPassword);
        }
    }
}
```

위 스텁의 시그니처는 `UserCredentialRepository`(`save` / `findByLoginId` / `existsByLoginId`)와 `PasswordEncryptor`(`encrypt` / `matches`)의 실제 정의와 일치함을 확인했다. 그대로 쓰면 된다.

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*UserApplicationServiceTest*'
```

Expected: 컴파일 실패. `UserCommand.Create`의 인자 수가 다르고 `UserResult.name()`, `UserRepository.findByEmail`이 없다.

- [ ] **Step 3: 구현**

`UserJpaEntity.java` — `username` 아래에 컬럼과 빌더 파라미터를 추가한다.

```java
    @Column(length = 30)
    private String name;
```

빌더 생성자 시그니처에 `String name`을 넣고 `this.name = name;`을 추가한다.

`UserRepository.java` — 포트에 조회 메서드를 추가한다.

```java
    /**
     * email 기반 사용자 단건 조회
     */
    Optional<UserModel> findByEmail(String email);
```

`UserJpaRepository.java`에 다음을 추가한다.

```java
    Optional<UserJpaEntity> findByEmail(String email);
```

`UserRepositoryImpl.java` — `toEntity`에 `.name(user.getName())`, `toDomain`의 `reconstruct` 인자에 `entity.getName()`을 `username` 다음 위치에 넣는다. 그리고 다음을 추가한다.

```java
    /**
     * email 기반 사용자 단건 조회 처리
     */
    @Override
    public Optional<UserModel> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toDomain);
    }
```

`UserCommand.java` — 두 레코드에 `name`을 추가한다.

```java
    public record Create(String username, String name, String email, String loginId, String password) {
    }

    public record Update(String username, String name, String email) {
    }
```

`UserResult.java` — `username` 다음에 `String name`을 넣고 `from`에 `user.getName()`을 추가한다.

`UserApplicationService.java` — `create`와 `update`를 고친다.

```java
    @Transactional
    public UserResult create(UserCommand.Create command) {
        UserCredentialModel.validateLoginId(command.loginId()); // 로그인 아이디 형식 검증
        UserCredentialModel.validateRawPassword(command.password()); // 비밀번호 형식 검증

        // 자체 가입은 닉네임 입력란이 없으므로 loginId를 username 초기값으로 사용한다
        String username = command.username() == null ? command.loginId() : command.username();
        UserModel newUser = UserModel.create(username, command.name(), command.email());

        validateUniqueUsername(username);
        validateUniqueEmail(command.email());
        validateUniqueLoginId(command.loginId());

        UserModel savedUser = userRepository.save(newUser);
        String encryptedPassword = passwordEncryptor.encrypt(command.password());
        userCredentialRepository.save(
                UserCredentialModel.create(savedUser.getUserId(), command.loginId(), encryptedPassword));

        return UserResult.from(savedUser);
    }
```

`update`는 `user.updateProfile(command.username(), command.name(), command.email())`로 바꾼다.

`UserRequest.java` — `Create`에 `name`을, `Update`에 `name`을 추가하고 `toCommand()`를 그에 맞춘다. `Create.toCommand()`는 `new UserCommand.Create(username, name, email, loginId, password)`가 된다.

`UserResponse.java` — `username` 다음에 `String name`을 넣고 `from`에 `result.name()`을 같은 위치에 추가한다.

```java
public record UserResponse(
        Long userId,
        String username,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * application 결과값 변환
     */
    public static UserResponse from(UserResult result) {
        return new UserResponse(
                result.userId(),
                result.username(),
                result.name(),
                result.email(),
                result.role(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
```

마이그레이션 SQL을 `docs/migrations/2026-08-23-add-users-name.sql`로 만든다.

```sql
-- users에 실명 컬럼 추가. 기존 username에 들어 있던 한글 실명을 name으로 복사한다.
-- 로컬은 ddl-auto:update가 컬럼만 만들고 백필은 하지 않으므로 이 UPDATE를 직접 실행해야 한다.
-- 운영은 ddl-auto:validate이므로 ALTER까지 직접 실행한다.
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(30);
UPDATE users SET name = username WHERE name IS NULL;
```

- [ ] **Step 4: 테스트 통과 확인 및 백필 실행**

```bash
cd backend && ./gradlew test
```

Expected: 전체 테스트 PASS (컴파일 복구됨).

앱을 한 번 띄워 `name` 컬럼을 만든 뒤 백필한다. 순서가 중요하다 — 백필 전에는 기존 행의 `name`이 null이지만 `name`은 nullable이라 조회는 정상 동작한다.

```bash
cd backend && ./gradlew bootRun    # 컬럼 생성 후 Ctrl+C
docker exec -i docker-postgres-1 psql -U planfix -d planfix < ../docs/migrations/2026-08-23-add-users-name.sql
docker exec docker-postgres-1 psql -U planfix -d planfix -c "select user_id, username, name from users;"
```

Expected: `user_id=1`이 `username='장동익'`, `name='장동익'`.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/domain/user/UserRepository.java \
        backend/src/main/java/taedonghee/plan_fix/infrastructure/user/ \
        backend/src/main/java/taedonghee/plan_fix/application/user/ \
        backend/src/main/java/taedonghee/plan_fix/interfaces/api/user/ \
        backend/src/test/java/taedonghee/plan_fix/application/user/ \
        docs/migrations/2026-08-23-add-users-name.sql
git commit -m "feat: name 컬럼을 영속·API 계층에 반영하고 마이그레이션 추가"
```

---

### Task 3: 쿠키 발급 유틸과 JwtAuthenticationFilter 쿠키 인증

**Files:**
- Create: `infrastructure/security/CookieFactory.java`
- Modify: `infrastructure/security/JwtAuthenticationFilter.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/infrastructure/security/CookieFactoryTest.java`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `CookieFactory.ACCESS_TOKEN_COOKIE` = `"access_token"`, `CookieFactory.OAUTH_TX_COOKIE` = `"oauth_tx"`
  - `ResponseCookie CookieFactory.accessToken(String token, long maxAgeSeconds)`
  - `ResponseCookie CookieFactory.expiredAccessToken()`
  - `ResponseCookie CookieFactory.oauthTx(String value)`
  - `ResponseCookie CookieFactory.expiredOauthTx()`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package taedonghee.plan_fix.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieFactoryTest {

    private final CookieFactory cookieFactory = new CookieFactory(false, "Lax");

    @Test
    void access_token_쿠키는_HttpOnly이며_전체_경로에_걸린다() {
        ResponseCookie cookie = cookieFactory.accessToken("jwt-value", 3600);

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("jwt-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void 만료용_access_token_쿠키는_MaxAge가_0이다() {
        assertThat(cookieFactory.expiredAccessToken().getMaxAge().getSeconds()).isZero();
    }

    @Test
    void oauth_tx_쿠키는_인증_경로로_한정되고_5분간_유효하다() {
        ResponseCookie cookie = cookieFactory.oauthTx("tx-value");

        assertThat(cookie.getName()).isEqualTo("oauth_tx");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(300);
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void 운영_설정에서는_Secure가_켜진다() {
        assertThat(new CookieFactory(true, "None").accessToken("v", 60).isSecure()).isTrue();
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*CookieFactoryTest*'
```

Expected: 컴파일 실패. `CookieFactory`가 없다.

- [ ] **Step 3: 구현**

`infrastructure/security/CookieFactory.java`:

```java
package taedonghee.plan_fix.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 인증 관련 쿠키 속성을 한곳에서 만드는 팩토리
 */
@Component
public class CookieFactory {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String OAUTH_TX_COOKIE = "oauth_tx";

    private static final String ROOT_PATH = "/";
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final long OAUTH_TX_MAX_AGE_SECONDS = 300;

    private final boolean secure;
    private final String sameSite;

    public CookieFactory(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /**
     * 세션 access token 쿠키 생성
     */
    public ResponseCookie accessToken(String token, long maxAgeSeconds) {
        return base(ACCESS_TOKEN_COOKIE, token, ROOT_PATH, maxAgeSeconds).build();
    }

    /**
     * 로그아웃용 만료된 access token 쿠키 생성
     */
    public ResponseCookie expiredAccessToken() {
        return base(ACCESS_TOKEN_COOKIE, "", ROOT_PATH, 0).build();
    }

    /**
     * OAuth state/code_verifier 보관 쿠키 생성
     */
    public ResponseCookie oauthTx(String value) {
        return base(OAUTH_TX_COOKIE, value, AUTH_PATH, OAUTH_TX_MAX_AGE_SECONDS).build();
    }

    /**
     * 콜백 처리 후 제거용 만료된 oauth_tx 쿠키 생성
     */
    public ResponseCookie expiredOauthTx() {
        return base(OAUTH_TX_COOKIE, "", AUTH_PATH, 0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
    }
}
```

`application.yaml`에 설정을 추가한다(`security:` 블록 위).

```yaml
app:
  frontend-base-url: ${FRONTEND_BASE_URL:http://localhost:3000}
  cookie:
    secure: ${COOKIE_SECURE:false}
    same-site: ${COOKIE_SAME_SITE:Lax}
```

`JwtAuthenticationFilter.java`의 `resolveToken`을 교체하고 import에 `jakarta.servlet.http.Cookie`를 추가한다.

```java
    /**
     * 쿠키 우선, Authorization 헤더 fallback으로 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String fromCookie = resolveFromCookie(request);
        if (fromCookie != null) {
            return fromCookie;
        }
        return resolveFromHeader(request);
    }

    /**
     * access_token 쿠키에서 토큰 추출
     */
    private String resolveFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CookieFactory.ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveFromHeader(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd backend && ./gradlew test
```

Expected: 전체 PASS.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/infrastructure/security/ \
        backend/src/test/java/taedonghee/plan_fix/infrastructure/security/ \
        backend/src/main/resources/application.yaml
git commit -m "feat: 인증 쿠키 팩토리 추가하고 JWT 필터가 쿠키를 읽도록 변경"
```

---

### Task 4: 자체 로그인 쿠키 전환, 로그아웃, CORS

**Files:**
- Modify: `application/auth/AuthResult.java`, `application/auth/AuthApplicationService.java`, `interfaces/api/auth/AuthResponse.java`, `interfaces/api/auth/AuthController.java`, `infrastructure/security/SecurityConfig.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/interfaces/api/auth/AuthControllerTest.java`

**Interfaces:**
- Consumes: Task 3의 `CookieFactory`
- Produces:
  - `AuthResult(String accessToken, long expiresIn, Long userId, String username, String email)`
  - `AuthResult AuthResult.of(AuthToken token, UserModel user)`
  - `AuthResponse(AuthResponse.User user)` / `AuthResponse.User(Long id, String username, String email)`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package taedonghee.plan_fix.interfaces.api.auth;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.application.auth.AuthResult;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void 응답에는_토큰이_없고_사용자_정보만_담긴다() {
        AuthResponse response = AuthResponse.from(
                new AuthResult("jwt-value", 3600, 1L, "gildong", "a@b.com"));

        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().username()).isEqualTo("gildong");
        assertThat(response.user().email()).isEqualTo("a@b.com");
    }
}
```

파일명은 `AuthResponseTest.java`로 만든다.

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*AuthResponseTest*'
```

Expected: 컴파일 실패. `AuthResult` 생성자 인자 수가 다르고 `AuthResponse.user()`가 없다.

- [ ] **Step 3: 구현**

`AuthResult.java`:

```java
package taedonghee.plan_fix.application.auth;

import taedonghee.plan_fix.domain.user.UserModel;

/**
 * 인증 결과 DTO
 */
public record AuthResult(
        String accessToken,
        long expiresIn,
        Long userId,
        String username,
        String email
) {

    /**
     * 발급 토큰과 사용자 정보 결합
     */
    public static AuthResult of(AuthToken token, UserModel user) {
        return new AuthResult(
                token.accessToken(), token.expiresIn(), user.getUserId(), user.getUsername(), user.getEmail());
    }
}
```

`AuthApplicationService.login`의 마지막 줄을 바꾼다.

```java
        return AuthResult.of(authTokenProvider.create(user), user);
```

`AuthResponse.java`:

```java
package taedonghee.plan_fix.interfaces.api.auth;

import taedonghee.plan_fix.application.auth.AuthResult;

/**
 * 인증 API 응답 DTO
 */
public record AuthResponse(User user) {

    /**
     * 응답에 담기는 사용자 정보
     */
    public record User(Long id, String username, String email) {
    }

    /**
     * application 결과값 변환
     */
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(new User(result.userId(), result.username(), result.email()));
    }
}
```

`AuthController.java`:

```java
package taedonghee.plan_fix.interfaces.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.auth.AuthApplicationService;
import taedonghee.plan_fix.application.auth.AuthResult;
import taedonghee.plan_fix.infrastructure.security.CookieFactory;

/**
 * 인증 API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final CookieFactory cookieFactory;

    /**
     * 자체 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest.Login request) {
        AuthResult result = authApplicationService.login(request.toCommand());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.accessToken(result.accessToken(), result.expiresIn()).toString())
                .body(AuthResponse.from(result));
    }

    /**
     * 로그아웃 API
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredAccessToken().toString())
                .build();
    }
}
```

`SecurityConfig.java` — CORS를 켜고 로그아웃·카카오 경로를 permitAll에 넣는다. import에 `org.springframework.web.cors.CorsConfiguration`, `org.springframework.web.cors.CorsConfigurationSource`, `org.springframework.web.cors.UrlBasedCorsConfigurationSource`, `org.springframework.beans.factory.annotation.Value`, `java.util.List`를 추가한다.

```java
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    /**
     * URL 접근 권한 및 JWT 필터 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/kakao", "/api/v1/auth/kakao/callback").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 쿠키 인증을 위한 CORS 설정
     * allowCredentials(true)는 allowedOrigins("*")와 함께 쓸 수 없으므로 origin을 명시한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendBaseUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
```

`@RequiredArgsConstructor`와 `@Value` 필드를 함께 쓰면 Lombok이 `frontendBaseUrl`을 생성자에 넣지 않으므로 필드 주입으로 동작한다. 의도한 것이다.

- [ ] **Step 4: 테스트 통과 확인 및 수동 검증**

```bash
cd backend && ./gradlew test
```

Expected: 전체 PASS.

앱을 띄우고 쿠키가 실제로 내려오는지 확인한다.

```bash
cd backend && ./gradlew bootRun    # 별도 터미널
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"<기존 계정 loginId>","password":"<비밀번호>"}'
```

Expected: 응답 헤더에 `Set-Cookie: access_token=...; Path=/; HttpOnly; SameSite=Lax`, 본문은 `{"user":{...}}`이고 `accessToken`이 없다.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/application/auth/ \
        backend/src/main/java/taedonghee/plan_fix/interfaces/api/auth/ \
        backend/src/main/java/taedonghee/plan_fix/infrastructure/security/SecurityConfig.java \
        backend/src/test/java/taedonghee/plan_fix/interfaces/api/auth/
git commit -m "feat: 자체 로그인을 쿠키 세션으로 전환하고 로그아웃·CORS 추가"
```

---

### Task 5: 소셜 계정 도메인과 저장소 어댑터

**Files:**
- Create: `domain/user/SocialProvider.java`, `domain/user/SocialAccountModel.java`, `domain/user/SocialAccountRepository.java`, `infrastructure/user/SocialAccountRepositoryImpl.java`
- Modify: `infrastructure/user/UserSocialAccountJpaRepository.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/domain/user/SocialAccountModelTest.java`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `enum SocialProvider { KAKAO }`
  - `SocialAccountModel.create(Long userId, SocialProvider provider, String providerUserId, String providerEmail)`
  - `SocialAccountModel.reconstruct(Long socialAccountId, Long userId, SocialProvider provider, String providerUserId, String providerEmail, OffsetDateTime createdAt, OffsetDateTime updatedAt)`
  - 게터: `getSocialAccountId()`, `getUserId()`, `getProvider()`, `getProviderUserId()`, `getProviderEmail()`, `getCreatedAt()`, `getUpdatedAt()`
  - `Optional<SocialAccountModel> SocialAccountRepository.findByProviderAndProviderUserId(SocialProvider provider, String providerUserId)`
  - `SocialAccountModel SocialAccountRepository.save(SocialAccountModel account)`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package taedonghee.plan_fix.domain.user;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialAccountModelTest {

    @Test
    void 소셜_계정은_userId와_provider_식별자로_생성된다() {
        SocialAccountModel account = SocialAccountModel.create(1L, SocialProvider.KAKAO, "4321", "a@b.com");

        assertThat(account.getUserId()).isEqualTo(1L);
        assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(account.getProviderUserId()).isEqualTo("4321");
        assertThat(account.getProviderEmail()).isEqualTo("a@b.com");
    }

    @Test
    void provider_이메일은_없어도_된다() {
        assertThat(SocialAccountModel.create(1L, SocialProvider.KAKAO, "4321", null).getProviderEmail()).isNull();
    }

    @Test
    void userId가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> SocialAccountModel.create(null, SocialProvider.KAKAO, "4321", null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void provider_식별자가_비어_있으면_생성할_수_없다() {
        assertThatThrownBy(() -> SocialAccountModel.create(1L, SocialProvider.KAKAO, " ", null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("providerUserId");
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*SocialAccountModelTest*'
```

Expected: 컴파일 실패.

- [ ] **Step 3: 구현**

`domain/user/SocialProvider.java`:

```java
package taedonghee.plan_fix.domain.user;

/**
 * 소셜 로그인 provider
 */
public enum SocialProvider {
    KAKAO
}
```

`domain/user/SocialAccountModel.java`:

```java
package taedonghee.plan_fix.domain.user;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * 소셜 계정 연결 Model
 */
public class SocialAccountModel {

    private static final int PROVIDER_USER_ID_MAX_LENGTH = 255;

    private final Long socialAccountId;
    private final Long userId;
    private final SocialProvider provider;
    private final String providerUserId;
    private final String providerEmail;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private SocialAccountModel(
            Long socialAccountId,
            Long userId,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (provider == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "provider는 필수입니다.");
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "providerUserId는 필수입니다.");
        }
        if (providerUserId.length() > PROVIDER_USER_ID_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "providerUserId는 255자 이하여야 합니다.");
        }

        this.socialAccountId = socialAccountId;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 소셜 계정 연결 생성
     */
    public static SocialAccountModel create(
            Long userId, SocialProvider provider, String providerUserId, String providerEmail) {
        OffsetDateTime now = OffsetDateTime.now();
        return new SocialAccountModel(null, userId, provider, providerUserId, providerEmail, now, now);
    }

    /**
     * 저장된 소셜 계정 정보를 기반으로 Model 복원
     */
    public static SocialAccountModel reconstruct(
            Long socialAccountId,
            Long userId,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new SocialAccountModel(
                socialAccountId, userId, provider, providerUserId, providerEmail, createdAt, updatedAt);
    }

    public Long getSocialAccountId() {
        return socialAccountId;
    }

    public Long getUserId() {
        return userId;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

`domain/user/SocialAccountRepository.java`:

```java
package taedonghee.plan_fix.domain.user;

import java.util.Optional;

/**
 * 소셜 계정 연결 Repository
 */
public interface SocialAccountRepository {

    /**
     * provider와 provider 식별자 기반 소셜 계정 단건 조회
     */
    Optional<SocialAccountModel> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    /**
     * 소셜 계정 연결 저장
     */
    SocialAccountModel save(SocialAccountModel account);
}
```

`infrastructure/user/UserSocialAccountJpaRepository.java`에 조회 메서드를 추가한다.

```java
    Optional<UserSocialAccountJpaEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
```

`import java.util.Optional;`도 함께 추가한다.

`infrastructure/user/SocialAccountRepositoryImpl.java`:

```java
package taedonghee.plan_fix.infrastructure.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.user.SocialAccountModel;
import taedonghee.plan_fix.domain.user.SocialAccountRepository;
import taedonghee.plan_fix.domain.user.SocialProvider;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.Optional;

/**
 * SocialAccountRepository JPA 구현체
 */
@Repository
@RequiredArgsConstructor
public class SocialAccountRepositoryImpl implements SocialAccountRepository {

    private final UserSocialAccountJpaRepository userSocialAccountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    /**
     * provider와 provider 식별자 기반 소셜 계정 단건 조회 처리
     */
    @Override
    public Optional<SocialAccountModel> findByProviderAndProviderUserId(
            SocialProvider provider, String providerUserId) {
        return userSocialAccountJpaRepository
                .findByProviderAndProviderUserId(provider.name(), providerUserId)
                .map(this::toDomain);
    }

    /**
     * 소셜 계정 연결 저장 처리
     */
    @Override
    public SocialAccountModel save(SocialAccountModel account) {
        UserJpaEntity user = userJpaRepository.findById(account.getUserId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND, "User not found. userId=" + account.getUserId()));

        UserSocialAccountJpaEntity entity = UserSocialAccountJpaEntity.builder()
                .id(account.getSocialAccountId())
                .user(user)
                .provider(account.getProvider().name())
                .providerUserId(account.getProviderUserId())
                .providerEmail(account.getProviderEmail())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();

        return toDomain(userSocialAccountJpaRepository.save(entity));
    }

    /**
     * JPA 엔티티를 도메인 모델로 변환
     */
    private SocialAccountModel toDomain(UserSocialAccountJpaEntity entity) {
        return SocialAccountModel.reconstruct(
                entity.getId(),
                entity.getUser().getId(),
                SocialProvider.valueOf(entity.getProvider()),
                entity.getProviderUserId(),
                entity.getProviderEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd backend && ./gradlew test
```

Expected: 전체 PASS.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/domain/user/SocialProvider.java \
        backend/src/main/java/taedonghee/plan_fix/domain/user/SocialAccountModel.java \
        backend/src/main/java/taedonghee/plan_fix/domain/user/SocialAccountRepository.java \
        backend/src/main/java/taedonghee/plan_fix/infrastructure/user/SocialAccountRepositoryImpl.java \
        backend/src/main/java/taedonghee/plan_fix/infrastructure/user/UserSocialAccountJpaRepository.java \
        backend/src/test/java/taedonghee/plan_fix/domain/user/SocialAccountModelTest.java
git commit -m "feat: 소셜 계정 도메인 모델과 저장소 어댑터 추가"
```

---

### Task 6: 카카오 OAuth 클라이언트와 PKCE

**Files:**
- Create: `infrastructure/oauth/KakaoOAuthProperties.java`, `infrastructure/oauth/KakaoUser.java`, `infrastructure/oauth/PkceGenerator.java`, `infrastructure/oauth/OAuthTransaction.java`, `infrastructure/oauth/KakaoOAuthClient.java`
- Modify: `resources/application.yaml`, `resources/application-prod.yml`
- Test: `backend/src/test/java/taedonghee/plan_fix/infrastructure/oauth/PkceGeneratorTest.java`, `OAuthTransactionTest.java`, `KakaoOAuthClientTest.java`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `KakaoUser(String id, String nickname, String email, boolean emailVerified)`
  - `String PkceGenerator.generateState()`, `String PkceGenerator.generateCodeVerifier()`, `String PkceGenerator.codeChallenge(String codeVerifier)`
  - `OAuthTransaction(String state, String codeVerifier)`, `String encode()`, `static Optional<OAuthTransaction> decode(String value)`
  - `String KakaoOAuthClient.buildAuthorizeUrl(String state, String codeChallenge)`
  - `String KakaoOAuthClient.exchangeCodeForAccessToken(String code, String codeVerifier)`
  - `KakaoUser KakaoOAuthClient.fetchUser(String kakaoAccessToken)`

- [ ] **Step 1: 실패하는 테스트 작성**

`PkceGeneratorTest.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceGeneratorTest {

    private final PkceGenerator generator = new PkceGenerator();

    @Test
    void state는_호출마다_달라진다() {
        assertThat(generator.generateState()).isNotEqualTo(generator.generateState());
    }

    @Test
    void code_challenge는_RFC7636_예시와_일치한다() {
        // RFC 7636 Appendix B
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        assertThat(generator.codeChallenge(verifier)).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void code_verifier는_URL_안전_문자만_포함한다() {
        assertThat(generator.generateCodeVerifier()).matches("^[A-Za-z0-9_-]+$");
    }
}
```

`OAuthTransactionTest.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthTransactionTest {

    @Test
    void 인코딩한_값을_다시_디코딩하면_원래대로_돌아온다() {
        OAuthTransaction original = new OAuthTransaction("state-value", "verifier-value");

        OAuthTransaction decoded = OAuthTransaction.decode(original.encode()).orElseThrow();

        assertThat(decoded.state()).isEqualTo("state-value");
        assertThat(decoded.codeVerifier()).isEqualTo("verifier-value");
    }

    @Test
    void 형식이_깨진_값은_비어_있는_결과를_준다() {
        assertThat(OAuthTransaction.decode("깨진값")).isEmpty();
        assertThat(OAuthTransaction.decode(null)).isEmpty();
        assertThat(OAuthTransaction.decode("")).isEmpty();
    }
}
```

`KakaoOAuthClientTest.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoOAuthClientTest {

    private static final KakaoOAuthProperties PROPERTIES = new KakaoOAuthProperties(
            "https://kauth.example/oauth/authorize",
            "https://kauth.example/oauth/token",
            "https://kapi.example/v2/user/me",
            "http://localhost:8080/api/v1/auth/kakao/callback",
            "test-client-id",
            "test-client-secret"
    );

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private KakaoOAuthClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoOAuthClient(PROPERTIES, builder);
    }

    @Test
    void 인가_URL에_PKCE와_state가_포함된다() {
        String url = client.buildAuthorizeUrl("state-value", "challenge-value");

        assertThat(url).startsWith("https://kauth.example/oauth/authorize?");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("state=state-value");
        assertThat(url).contains("code_challenge=challenge-value");
        assertThat(url).contains("code_challenge_method=S256");
        assertThat(url).contains("scope=profile_nickname,account_email");
        assertThat(url).contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fauth%2Fkakao%2Fcallback");
    }

    @Test
    void 토큰_교환은_access_token을_돌려준다() {
        server.expect(requestTo("https://kauth.example/oauth/token"))
                .andRespond(withSuccess("{\"access_token\":\"kakao-token\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.exchangeCodeForAccessToken("auth-code", "verifier")).isEqualTo("kakao-token");
        server.verify();
    }

    @Test
    void 사용자_조회는_닉네임과_인증된_이메일을_읽는다() {
        server.expect(requestTo("https://kapi.example/v2/user/me"))
                .andExpect(header("Authorization", "Bearer kakao-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 1234567890,
                          "kakao_account": {
                            "email": "a@b.com",
                            "is_email_verified": true,
                            "profile": { "nickname": "홍길동" }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoUser user = client.fetchUser("kakao-token");

        assertThat(user.id()).isEqualTo("1234567890");
        assertThat(user.nickname()).isEqualTo("홍길동");
        assertThat(user.email()).isEqualTo("a@b.com");
        assertThat(user.emailVerified()).isTrue();
        server.verify();
    }

    @Test
    void 이메일_동의를_받지_못하면_이메일은_비어_있다() {
        server.expect(requestTo("https://kapi.example/v2/user/me"))
                .andRespond(withSuccess("""
                        {
                          "id": 1234567890,
                          "kakao_account": { "profile": { "nickname": "길동" } }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoUser user = client.fetchUser("kakao-token");

        assertThat(user.email()).isNull();
        assertThat(user.emailVerified()).isFalse();
        server.verify();
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*oauth*'
```

Expected: 컴파일 실패.

- [ ] **Step 3: 구현**

`infrastructure/oauth/KakaoOAuthProperties.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * oauth.kakao.* 설정 바인딩
 */
@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
        String authorizeUri,
        String tokenUri,
        String userInfoUri,
        String redirectUri,
        String clientId,
        String clientSecret
) {
}
```

등록을 위한 추가 작업은 없다. `PlanFixApplication`에 이미 `@ConfigurationPropertiesScan`이 붙어 있어(`TourApiProperties`가 같은 방식으로 등록된다) `@ConfigurationProperties`만 달면 자동으로 잡힌다.

`infrastructure/oauth/KakaoUser.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

/**
 * 카카오 사용자 정보
 */
public record KakaoUser(String id, String nickname, String email, boolean emailVerified) {
}
```

`infrastructure/oauth/PkceGenerator.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import org.springframework.stereotype.Component;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * OAuth state 및 PKCE 값 생성기
 */
@Component
public class PkceGenerator {

    private static final int STATE_BYTES = 32;
    private static final int CODE_VERIFIER_BYTES = 48;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * CSRF 방지용 state 생성
     */
    public String generateState() {
        return randomUrlSafe(STATE_BYTES);
    }

    /**
     * PKCE code_verifier 생성
     */
    public String generateCodeVerifier() {
        return randomUrlSafe(CODE_VERIFIER_BYTES);
    }

    /**
     * code_verifier의 S256 code_challenge 계산
     */
    public String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return encoder.encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "SHA-256을 사용할 수 없습니다.");
        }
    }

    private String randomUrlSafe(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
```

`infrastructure/oauth/OAuthTransaction.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * state와 code_verifier의 쿠키 직렬화 형식
 */
public record OAuthTransaction(String state, String codeVerifier) {

    private static final String SEPARATOR = ".";

    /**
     * 쿠키에 담을 문자열로 인코딩
     */
    public String encode() {
        return encodePart(state) + SEPARATOR + encodePart(codeVerifier);
    }

    /**
     * 쿠키 문자열 디코딩. 형식이 깨졌으면 빈 결과를 반환한다.
     */
    public static Optional<OAuthTransaction> decode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.split("\\" + SEPARATOR);
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(new OAuthTransaction(decodePart(parts[0]), decodePart(parts[1])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String encodePart(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
```

`infrastructure/oauth/KakaoOAuthClient.java`:

```java
package taedonghee.plan_fix.infrastructure.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * 카카오 OAuth API 호출 어댑터
 */
@Component
public class KakaoOAuthClient {

    private static final String SCOPE = "profile_nickname,account_email";

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    public KakaoOAuthClient(KakaoOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    /**
     * 카카오 인가 화면 URL 생성
     */
    public String buildAuthorizeUrl(String state, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(properties.authorizeUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("scope", SCOPE)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode()
                .toUriString();
    }

    /**
     * 인가 코드를 카카오 access token으로 교환
     */
    public String exchangeCodeForAccessToken(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
            form.add("client_secret", properties.clientSecret());
        }

        JsonNode body = post(properties.tokenUri(), form);
        JsonNode accessToken = body.get("access_token");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "카카오 토큰 응답에 access_token이 없습니다.");
        }
        return accessToken.asText();
    }

    /**
     * 카카오 access token으로 사용자 정보 조회
     */
    public KakaoUser fetchUser(String kakaoAccessToken) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(properties.userInfoUri())
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "카카오 사용자 조회에 실패했습니다.");
        }
        if (body == null || body.get("id") == null) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "카카오 사용자 응답이 비어 있습니다.");
        }

        JsonNode account = body.path("kakao_account");
        JsonNode email = account.get("email");
        boolean emailVerified = account.path("is_email_verified").asBoolean(false);
        JsonNode nickname = account.path("profile").get("nickname");

        return new KakaoUser(
                body.get("id").asText(),
                nickname == null ? null : nickname.asText(),
                email == null ? null : email.asText(),
                emailVerified
        );
    }

    private JsonNode post(String uri, MultiValueMap<String, String> form) {
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "카카오 토큰 교환에 실패했습니다.");
        }
    }
}
```

`application.yaml`에 비밀이 아닌 설정을 추가한다.

```yaml
oauth:
  kakao:
    authorize-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
    redirect-uri: ${KAKAO_REDIRECT_URI:http://localhost:8080/api/v1/auth/kakao/callback}
```

`application-prod.yml`에 운영 값을 추가한다.

```yaml
oauth:
  kakao:
    client-id: ${KAKAO_REST_API_KEY}
    client-secret: ${KAKAO_CLIENT_SECRET}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd backend && ./gradlew test
```

Expected: 전체 PASS. `code_challenge` 테스트가 RFC 7636 예시값과 정확히 맞아야 한다 — 틀리면 Base64 URL 인코딩에 패딩이 남아 있는 것이다.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/infrastructure/oauth/ \
        backend/src/test/java/taedonghee/plan_fix/infrastructure/oauth/ \
        backend/src/main/resources/application.yaml \
        backend/src/main/resources/application-prod.yml
git commit -m "feat: 카카오 OAuth 클라이언트와 PKCE 생성기 추가"
```

---

### Task 7: 계정 조회·연결·생성 애플리케이션 서비스

스펙의 계정 연결 알고리즘을 구현한다. 이 태스크가 보안상 가장 민감하다.

**Files:**
- Create: `application/auth/SocialLoginApplicationService.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/application/auth/SocialLoginApplicationServiceTest.java`

**Interfaces:**
- Consumes: Task 5의 `SocialAccountRepository`, `SocialAccountModel`, `SocialProvider`; Task 6의 `KakaoOAuthClient`, `KakaoUser`; Task 4의 `AuthResult.of`; `UserRepository.findByEmail`
- Produces: `AuthResult SocialLoginApplicationService.loginWithKakao(String code, String codeVerifier)`

- [ ] **Step 1: 실패하는 테스트 작성**

`UserApplicationServiceTest`의 `InMemoryUserRepository`를 재사용하지 않고 이 테스트 파일 안에 별도 스텁을 둔다(태스크 간 결합을 피한다).

```java
package taedonghee.plan_fix.application.auth;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.domain.user.SocialAccountModel;
import taedonghee.plan_fix.domain.user.SocialAccountRepository;
import taedonghee.plan_fix.domain.user.SocialProvider;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.KakaoUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialLoginApplicationServiceTest {

    @Test
    void 이미_연결된_소셜_계정은_기존_사용자로_로그인한다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "홍길동", "a@b.com", true));
        UserModel existing = fixture.users.save(UserModel.create("기존닉", null, "a@b.com"));
        fixture.socialAccounts.save(
                SocialAccountModel.create(existing.getUserId(), SocialProvider.KAKAO, "kakao-1", "a@b.com"));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(result.userId()).isEqualTo(existing.getUserId());
        assertThat(result.username()).isEqualTo("기존닉");
        assertThat(fixture.users.findAll()).hasSize(1);
    }

    @Test
    void 인증된_이메일이_같으면_기존_계정에_연결한다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "카카오닉", "a@b.com", true));
        UserModel existing = fixture.users.save(UserModel.create("기존닉", "홍길동", "a@b.com"));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(result.userId()).isEqualTo(existing.getUserId());
        assertThat(fixture.users.findAll()).hasSize(1);
        assertThat(fixture.socialAccounts
                .findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1")).isPresent();
    }

    @Test
    void 미인증_이메일은_기존_계정에_연결하지_않고_새_계정을_만든다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "카카오닉", "a@b.com", false));
        fixture.users.save(UserModel.create("기존닉", "홍길동", "a@b.com"));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(fixture.users.findAll()).hasSize(2);
        assertThat(result.username()).isEqualTo("카카오닉");
        assertThat(result.email()).isNull();
    }

    @Test
    void 신규_가입은_카카오_닉네임을_username으로_쓰고_name은_비운다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "카카오닉", "a@b.com", true));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        UserModel created = fixture.users.findByUserId(result.userId()).orElseThrow();
        assertThat(created.getUsername()).isEqualTo("카카오닉");
        assertThat(created.getName()).isNull();
        assertThat(created.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void 닉네임이_중복되면_숫자를_붙여_유니크하게_만든다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "홍길동", null, false));
        fixture.users.save(UserModel.create("홍길동", null, null));
        fixture.users.save(UserModel.create("홍길동2", null, null));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(result.username()).isEqualTo("홍길동3");
    }

    /**
     * 테스트 대상과 스텁 저장소를 묶은 픽스처
     */
    static class Fixture {
        final InMemoryUserRepository users = new InMemoryUserRepository();
        final InMemorySocialAccountRepository socialAccounts = new InMemorySocialAccountRepository();
        final KakaoOAuthClient kakaoOAuthClient = mock(KakaoOAuthClient.class);

        Fixture(KakaoUser kakaoUser) {
            when(kakaoOAuthClient.exchangeCodeForAccessToken(anyString(), anyString())).thenReturn("kakao-token");
            when(kakaoOAuthClient.fetchUser("kakao-token")).thenReturn(kakaoUser);
        }

        SocialLoginApplicationService service() {
            return new SocialLoginApplicationService(
                    kakaoOAuthClient, users, socialAccounts, new FixedAuthTokenProvider());
        }
    }

    static class FixedAuthTokenProvider implements AuthTokenProvider {
        @Override
        public AuthToken create(UserModel user) {
            return new AuthToken("jwt-" + user.getUserId(), "Bearer", 3600);
        }
    }

    static class InMemoryUserRepository implements UserRepository {
        private final List<UserModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public UserModel save(UserModel user) {
            UserModel stored = UserModel.reconstruct(
                    user.getUserId() == null ? ++sequence : user.getUserId(),
                    user.getUsername(), user.getName(), user.getEmail(),
                    user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
            saved.removeIf(u -> u.getUserId().equals(stored.getUserId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<UserModel> findByUserId(Long userId) {
            return saved.stream().filter(u -> u.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<UserModel> findByEmail(String email) {
            return saved.stream().filter(u -> email != null && email.equals(u.getEmail())).findFirst();
        }

        @Override
        public List<UserModel> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public boolean existsByUsername(String username) {
            return saved.stream().anyMatch(u -> u.getUsername().equals(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return saved.stream().anyMatch(u -> email != null && email.equals(u.getEmail()));
        }
    }

    static class InMemorySocialAccountRepository implements SocialAccountRepository {
        private final List<SocialAccountModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public Optional<SocialAccountModel> findByProviderAndProviderUserId(
                SocialProvider provider, String providerUserId) {
            return saved.stream()
                    .filter(a -> a.getProvider() == provider && a.getProviderUserId().equals(providerUserId))
                    .findFirst();
        }

        @Override
        public SocialAccountModel save(SocialAccountModel account) {
            SocialAccountModel stored = SocialAccountModel.reconstruct(
                    ++sequence, account.getUserId(), account.getProvider(),
                    account.getProviderUserId(), account.getProviderEmail(),
                    account.getCreatedAt(), account.getUpdatedAt());
            saved.add(stored);
            return stored;
        }
    }
}
```

Mockito는 `spring-boot-starter-*-test`에 포함되어 있다. 없으면 `KakaoOAuthClient`를 상속한 수동 스텁으로 바꾼다(`KakaoOAuthClient`는 final이 아니다).

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*SocialLoginApplicationServiceTest*'
```

Expected: 컴파일 실패. `SocialLoginApplicationService`가 없다.

- [ ] **Step 3: 구현**

```java
package taedonghee.plan_fix.application.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.user.SocialAccountModel;
import taedonghee.plan_fix.domain.user.SocialAccountRepository;
import taedonghee.plan_fix.domain.user.SocialProvider;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;
import taedonghee.plan_fix.domain.user.UserStatus;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.KakaoUser;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.Optional;

/**
 * 소셜 로그인 Application Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginApplicationService {

    private static final int USERNAME_MAX_LENGTH = 30;
    private static final int MAX_USERNAME_ATTEMPTS = 1000;
    private static final String FALLBACK_NICKNAME = "여행자";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthTokenProvider authTokenProvider;

    /**
     * 카카오 인가 코드로 로그인 처리
     */
    @Transactional
    public AuthResult loginWithKakao(String code, String codeVerifier) {
        String kakaoAccessToken = kakaoOAuthClient.exchangeCodeForAccessToken(code, codeVerifier);
        KakaoUser kakaoUser = kakaoOAuthClient.fetchUser(kakaoAccessToken);

        UserModel user = resolveUser(kakaoUser);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CoreException(ErrorType.FORBIDDEN, "Inactive user cannot login. userId=" + user.getUserId());
        }

        return AuthResult.of(authTokenProvider.create(user), user);
    }

    /**
     * 소셜 계정 조회 → 이메일 연결 → 신규 생성 순으로 사용자 확정
     */
    private UserModel resolveUser(KakaoUser kakaoUser) {
        Optional<SocialAccountModel> linked = socialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.KAKAO, kakaoUser.id());
        if (linked.isPresent()) {
            return userRepository.findByUserId(linked.get().getUserId())
                    .orElseThrow(() -> new CoreException(
                            ErrorType.NOT_FOUND, "User not found. userId=" + linked.get().getUserId()));
        }

        try {
            return linkOrCreate(kakaoUser);
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자의 동시 첫 로그인. 유니크 제약이 막았으므로 이미 만들어진 쪽을 쓴다.
            return socialAccountRepository
                    .findByProviderAndProviderUserId(SocialProvider.KAKAO, kakaoUser.id())
                    .flatMap(account -> userRepository.findByUserId(account.getUserId()))
                    .orElseThrow(() -> new CoreException(ErrorType.CONFLICT, "소셜 계정 연결에 실패했습니다."));
        }
    }

    /**
     * 인증된 이메일이면 기존 계정에 연결하고, 아니면 신규 사용자를 만든다
     */
    private UserModel linkOrCreate(KakaoUser kakaoUser) {
        String verifiedEmail = kakaoUser.emailVerified() ? kakaoUser.email() : null;

        if (verifiedEmail != null) {
            Optional<UserModel> byEmail = userRepository.findByEmail(verifiedEmail);
            if (byEmail.isPresent()) {
                UserModel existing = byEmail.get();
                socialAccountRepository.save(SocialAccountModel.create(
                        existing.getUserId(), SocialProvider.KAKAO, kakaoUser.id(), kakaoUser.email()));
                return existing;
            }
        }

        UserModel created = userRepository.save(
                UserModel.create(uniqueUsername(kakaoUser.nickname()), null, verifiedEmail));
        socialAccountRepository.save(SocialAccountModel.create(
                created.getUserId(), SocialProvider.KAKAO, kakaoUser.id(), kakaoUser.email()));
        return created;
    }

    /**
     * 닉네임이 이미 쓰이고 있으면 뒤에 숫자를 붙여 유니크하게 만든다
     */
    private String uniqueUsername(String nickname) {
        String base = normalizeNickname(nickname);
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        for (int suffix = 2; suffix < MAX_USERNAME_ATTEMPTS; suffix++) {
            String candidate = truncateForSuffix(base, suffix) + suffix;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new CoreException(ErrorType.CONFLICT, "사용 가능한 username을 찾지 못했습니다.");
    }

    /**
     * 카카오 닉네임을 username 규칙(2~30자, 공백 정리)에 맞게 다듬는다
     */
    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return FALLBACK_NICKNAME;
        }
        String normalized = nickname.strip().replaceAll("\\s+", " ");
        if (normalized.length() > USERNAME_MAX_LENGTH) {
            normalized = normalized.substring(0, USERNAME_MAX_LENGTH).strip();
        }
        if (normalized.length() < 2) {
            return FALLBACK_NICKNAME;
        }
        return normalized;
    }

    /**
     * 숫자 접미를 붙여도 30자를 넘지 않도록 앞부분을 자른다
     */
    private String truncateForSuffix(String base, int suffix) {
        int room = USERNAME_MAX_LENGTH - String.valueOf(suffix).length();
        return base.length() <= room ? base : base.substring(0, room).strip();
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd backend && ./gradlew test
```

Expected: 전체 PASS. 특히 `미인증_이메일은_기존_계정에_연결하지_않고_새_계정을_만든다`가 통과해야 한다 — 이게 계정 탈취를 막는 테스트다.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/application/auth/SocialLoginApplicationService.java \
        backend/src/test/java/taedonghee/plan_fix/application/auth/SocialLoginApplicationServiceTest.java
git commit -m "feat: 카카오 계정 조회·연결·생성 로직 추가"
```

---

### Task 8: 인가 시작·콜백 엔드포인트

**Files:**
- Create: `interfaces/api/auth/KakaoAuthController.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/interfaces/api/auth/KakaoAuthControllerTest.java`

**Interfaces:**
- Consumes: Task 3의 `CookieFactory`, Task 6의 `KakaoOAuthClient`·`PkceGenerator`·`OAuthTransaction`, Task 7의 `SocialLoginApplicationService`
- Produces: `GET /api/v1/auth/kakao`, `GET /api/v1/auth/kakao/callback`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package taedonghee.plan_fix.interfaces.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.auth.AuthResult;
import taedonghee.plan_fix.application.auth.SocialLoginApplicationService;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.OAuthTransaction;
import taedonghee.plan_fix.infrastructure.oauth.PkceGenerator;
import taedonghee.plan_fix.infrastructure.security.CookieFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KakaoAuthControllerTest {

    private final KakaoOAuthClient kakaoOAuthClient = mock(KakaoOAuthClient.class);
    private final SocialLoginApplicationService socialLoginApplicationService =
            mock(SocialLoginApplicationService.class);
    private final CookieFactory cookieFactory = new CookieFactory(false, "Lax");
    private final KakaoAuthController controller = new KakaoAuthController(
            kakaoOAuthClient, new PkceGenerator(), socialLoginApplicationService,
            cookieFactory, "http://localhost:3000");

    @Test
    void 로그인_시작은_카카오로_302하고_oauth_tx_쿠키를_심는다() {
        when(kakaoOAuthClient.buildAuthorizeUrl(anyString(), anyString()))
                .thenReturn("https://kauth.kakao.com/oauth/authorize?x=1");

        ResponseEntity<Void> response = controller.start();

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("https://kauth.kakao.com/oauth/authorize?x=1");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).startsWith("oauth_tx=");
    }

    @Test
    void 사용자가_동의를_거부하면_denied로_리다이렉트한다() {
        ResponseEntity<Void> response = controller.callback(null, null, "access_denied", null);

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/login?error=denied");
    }

    @Test
    void oauth_tx_쿠키가_없으면_invalid_state로_리다이렉트한다() {
        ResponseEntity<Void> response = controller.callback("code", "state", null, null);

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/login?error=invalid_state");
    }

    @Test
    void state가_다르면_invalid_state로_리다이렉트한다() {
        String cookie = new OAuthTransaction("real-state", "verifier").encode();

        ResponseEntity<Void> response = controller.callback("code", "attacker-state", null, cookie);

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/login?error=invalid_state");
    }

    @Test
    void 성공하면_access_token_쿠키를_심고_main으로_리다이렉트한다() {
        String cookie = new OAuthTransaction("state", "verifier").encode();
        when(socialLoginApplicationService.loginWithKakao("code", "verifier"))
                .thenReturn(new AuthResult("jwt-value", 3600, 1L, "길동", "a@b.com"));

        ResponseEntity<Void> response = controller.callback("code", "state", null, cookie);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/main");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anyMatch(value -> value.startsWith("access_token=jwt-value"));
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

```bash
cd backend && ./gradlew test --tests '*KakaoAuthControllerTest*'
```

Expected: 컴파일 실패.

- [ ] **Step 3: 구현**

```java
package taedonghee.plan_fix.interfaces.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.auth.AuthResult;
import taedonghee.plan_fix.application.auth.SocialLoginApplicationService;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.OAuthTransaction;
import taedonghee.plan_fix.infrastructure.oauth.PkceGenerator;
import taedonghee.plan_fix.infrastructure.security.CookieFactory;
import taedonghee.plan_fix.support.error.CoreException;

import java.net.URI;
import java.util.Optional;

/**
 * 카카오 로그인 API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
public class KakaoAuthController {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthController.class);

    private final KakaoOAuthClient kakaoOAuthClient;
    private final PkceGenerator pkceGenerator;
    private final SocialLoginApplicationService socialLoginApplicationService;
    private final CookieFactory cookieFactory;
    private final String frontendBaseUrl;

    public KakaoAuthController(
            KakaoOAuthClient kakaoOAuthClient,
            PkceGenerator pkceGenerator,
            SocialLoginApplicationService socialLoginApplicationService,
            CookieFactory cookieFactory,
            @Value("${app.frontend-base-url}") String frontendBaseUrl
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.pkceGenerator = pkceGenerator;
        this.socialLoginApplicationService = socialLoginApplicationService;
        this.cookieFactory = cookieFactory;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * 카카오 인가 화면으로 리다이렉트
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> start() {
        String state = pkceGenerator.generateState();
        String codeVerifier = pkceGenerator.generateCodeVerifier();
        String authorizeUrl = kakaoOAuthClient.buildAuthorizeUrl(state, pkceGenerator.codeChallenge(codeVerifier));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.oauthTx(new OAuthTransaction(state, codeVerifier).encode()).toString())
                .build();
    }

    /**
     * 카카오 콜백 처리 후 프론트로 리다이렉트
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = CookieFactory.OAUTH_TX_COOKIE, required = false) String oauthTx
    ) {
        if (error != null) {
            log.warn("카카오 인가 거부 또는 실패. error={}", error);
            return failure("denied");
        }

        Optional<OAuthTransaction> transaction = OAuthTransaction.decode(oauthTx);
        if (transaction.isEmpty() || state == null || !transaction.get().state().equals(state)) {
            return failure("invalid_state");
        }
        if (code == null || code.isBlank()) {
            return failure("invalid_state");
        }

        try {
            AuthResult result = socialLoginApplicationService
                    .loginWithKakao(code, transaction.get().codeVerifier());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBaseUrl + "/main"))
                    .header(HttpHeaders.SET_COOKIE,
                            cookieFactory.accessToken(result.accessToken(), result.expiresIn()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredOauthTx().toString())
                    .build();
        } catch (CoreException e) {
            log.warn("카카오 로그인 실패. type={} message={}", e.getErrorType(), e.getMessage());
            return failure(switch (e.getErrorType()) {
                case UNAUTHORIZED -> "kakao_config";
                case INTERNAL_ERROR -> "kakao_unavailable";
                default -> "unknown";
            });
        } catch (RuntimeException e) {
            log.error("카카오 로그인 처리 중 예상치 못한 오류", e);
            return failure("unknown");
        }
    }

    /**
     * 실패 사유를 쿼리로 붙여 로그인 화면으로 리다이렉트하고 oauth_tx를 제거
     */
    private ResponseEntity<Void> failure(String errorCode) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendBaseUrl + "/login?error=" + errorCode))
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredOauthTx().toString())
                .build();
    }
}
```

`CoreException`은 `@Getter`가 붙은 `errorType` 필드를 가지므로 `getErrorType()`이 이미 존재한다. 추가 작업은 없다.

- [ ] **Step 4: 테스트 통과 확인 및 실제 카카오 로그인**

```bash
cd backend && ./gradlew test
```

Expected: 전체 PASS.

이제 진짜로 동작하는지 본다. `application-secret.yml`에 키가 채워져 있고 카카오 디벨로퍼스에 Redirect URI가 등록되어 있어야 한다.

```bash
cd backend && ./gradlew bootRun
```

브라우저에서 `http://localhost:8080/api/v1/auth/kakao`를 연다.

Expected: 카카오 로그인 화면 → 동의 → `http://localhost:3000/main`으로 리다이렉트(프론트가 안 떠 있으면 연결 실패 화면이지만 URL은 맞아야 한다). DB 확인:

```bash
docker exec docker-postgres-1 psql -U planfix -d planfix \
  -c "select u.user_id, u.username, u.name, u.email, s.provider, s.provider_user_id
      from users u join user_social_accounts s on s.user_id = u.user_id;"
```

Expected: 카카오 닉네임이 `username`, `name`은 null인 행이 보인다.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/taedonghee/plan_fix/interfaces/api/auth/KakaoAuthController.java \
        backend/src/test/java/taedonghee/plan_fix/interfaces/api/auth/KakaoAuthControllerTest.java
git commit -m "feat: 카카오 인가 시작·콜백 엔드포인트 추가"
```

---

### Task 9: 프론트엔드 연결과 목업 제거

**Files:**
- Modify: `frontend/src/services/auth.ts`, `frontend/src/App.tsx`, `frontend/.env.example`, `frontend/src/pages/login-page.tsx`
- Delete: `frontend/src/pages/kakao-login-page.tsx`
- Test: `frontend/src/App.test.tsx` (기존 테스트가 목업 라우트를 참조하면 수정)

**Interfaces:**
- Consumes: Task 4·8의 엔드포인트
- Produces: 없음

- [ ] **Step 1: 현재 참조 확인**

```bash
cd frontend && grep -rn "kakao-login-page\|KakaoLoginPage\|/login/kakao\|startKakaoSignIn" src
```

Expected: `App.tsx`, `App.test.tsx`, `login-page.tsx` 또는 `login-form.tsx`에서 참조가 나온다. 이 목록을 다 고쳐야 한다.

- [ ] **Step 2: 테스트 실행해 기준선 확보**

```bash
cd frontend && CI=true npm test
```

Expected: 현재 상태의 PASS/FAIL을 기록해둔다. 수정 후 비교 기준이 된다.

- [ ] **Step 3: 수정**

`frontend/.env.example`:

```
# Backend API root. Do not place secrets in REACT_APP_* variables.
REACT_APP_API_BASE_URL=http://localhost:8080/api/v1
```

`frontend/src/services/auth.ts` — 백엔드 계약에 맞춘다. `signIn`은 `loginId`를 보내고 응답에는 토큰이 없다.

```ts
export type LoginRequest = {
  loginId: string;
  password: string;
};

export type LoginResponse = {
  user: {
    id: number;
    username: string;
    email: string | null;
  };
};

const apiBaseUrl = process.env.REACT_APP_API_BASE_URL?.replace(/\/$/, "");

export function isAuthApiConfigured() {
  return Boolean(apiBaseUrl);
}

export function startKakaoSignIn() {
  if (!apiBaseUrl) {
    throw new Error("REACT_APP_API_BASE_URL이 설정되지 않았습니다.");
  }

  window.location.assign(`${apiBaseUrl}/auth/kakao`);
}

export async function signIn(payload: LoginRequest): Promise<LoginResponse> {
  if (!apiBaseUrl) {
    throw new Error("REACT_APP_API_BASE_URL이 설정되지 않았습니다.");
  }

  const response = await fetch(`${apiBaseUrl}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null;
    throw new Error(body?.message ?? "로그인할 수 없습니다. 아이디와 비밀번호를 확인해 주세요.");
  }

  return (await response.json()) as LoginResponse;
}

export async function signOut(): Promise<void> {
  if (!apiBaseUrl) {
    return;
  }

  await fetch(`${apiBaseUrl}/auth/logout`, { method: "POST", credentials: "include" });
}
```

`signIn`의 호출부가 `{email, password, rememberMe}`를 넘기고 있으면 `{loginId, password}`로 바꾼다. 호출부는 Step 1의 grep 결과로 찾는다.

`frontend/src/App.tsx` — `KakaoLoginPage` import와 `<Route path="/login/kakao" ... />` 줄을 삭제한다.

`frontend/src/pages/kakao-login-page.tsx` 삭제:

```bash
cd frontend && rm src/pages/kakao-login-page.tsx
```

로그인 화면의 카카오 버튼이 `/login/kakao`로 navigate 하고 있으면 `startKakaoSignIn()` 호출로 바꾼다.

로그인 화면에서 에러 쿼리를 읽어 메시지를 띄운다. `login-page.tsx`에 추가한다.

```tsx
import { useSearchParams } from "react-router-dom";

const KAKAO_ERROR_MESSAGES: Record<string, string> = {
  denied: "카카오 로그인을 취소했습니다.",
  invalid_state: "로그인 요청이 만료되었습니다. 다시 시도해 주세요.",
  kakao_config: "카카오 로그인 설정에 문제가 있습니다. 잠시 후 다시 시도해 주세요.",
  kakao_unavailable: "카카오 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.",
  unknown: "로그인 중 문제가 발생했습니다. 다시 시도해 주세요.",
};

// 컴포넌트 안에서
const [searchParams] = useSearchParams();
const kakaoError = searchParams.get("error");
const kakaoErrorMessage = kakaoError ? (KAKAO_ERROR_MESSAGES[kakaoError] ?? KAKAO_ERROR_MESSAGES.unknown) : null;
```

그리고 렌더 트리에 조건부로 넣는다.

```tsx
{kakaoErrorMessage ? (
  <p role="alert" className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
    {kakaoErrorMessage}
  </p>
) : null}
```

`App.test.tsx`가 삭제한 라우트를 참조하면 해당 테스트를 제거하거나 `/login`을 보도록 고친다.

- [ ] **Step 4: 전체 흐름 검증**

```bash
cd frontend && CI=true npm test
cd frontend && npx tsc --noEmit
```

Expected: 테스트 PASS, 타입 에러 없음.

백엔드와 프론트를 동시에 띄우고 브라우저에서 확인한다.

```bash
cd backend && ./gradlew bootRun     # 터미널 1
cd frontend && npm start            # 터미널 2
```

`http://localhost:3000/login`에서 카카오 버튼을 누른다.

Expected: 카카오 로그인 → 동의 → `http://localhost:3000/main`. 개발자도구 Application > Cookies에 `access_token`이 HttpOnly로 보인다. 새로고침해도 로그인이 유지된다.

- [ ] **Step 5: 커밋**

```bash
git add frontend/src frontend/.env.example
git commit -m "feat: 프론트엔드를 실제 카카오 로그인에 연결하고 목업 페이지 제거"
```

---

## Self-Review

**1. 스펙 커버리지**

| 스펙 항목 | 태스크 |
|---|---|
| 사용자 필드 정리 (loginId / username / name) | 1, 2 |
| 인증 흐름, PKCE, state 쿠키 | 6, 8 |
| 계정 연결 알고리즘, `isEmailVerified` 검사, 동시성 | 7 |
| 세션 쿠키, 헤더 fallback, 로그아웃, CORS | 3, 4 |
| 엔드포인트 5종 | 4, 8 |
| 에러 처리 매핑 | 8 |
| 설정 (`oauth.kakao.*`, `app.*`) | 3, 6 |
| 마이그레이션 | 2 |
| 신설 파일 목록 | 3, 5, 6, 7 |
| 테스트 (도메인/애플리케이션/인프라/통합) | 1, 5, 6, 7, 8 |
| 프론트 변경 3종 + 목업 삭제 | 9 |

`CookieFactory`가 스펙의 `infrastructure/security/CookieFactory.java`와 일치한다. 스펙의 `infrastructure/oauth/KakaoUserResponse.java`는 계획에서 `KakaoUser.java`로 이름을 바꿨다 — 응답 DTO가 아니라 파싱 결과 표현이므로 이 이름이 정확하다.

**2. 플레이스홀더 스캔**

계획 작성 중 미확정으로 남겼던 네 지점(`UserResponse` 형태, `@ConfigurationPropertiesScan` 등록 방식, `CoreException.getErrorType()` 존재 여부, 테스트 스텁의 포트 시그니처)은 실제 파일을 열어 확인한 뒤 전부 확정 내용으로 교체했다.

남은 유일한 미확정 지점은 Task 9의 프론트엔드다. `login-page.tsx`의 렌더 트리 구조와 카카오 버튼의 현재 동작은 파일마다 다르므로, Step 1의 grep으로 참조 위치를 먼저 확보한 뒤 수정한다. 삽입할 코드와 판단 기준은 모두 적어두었다.

**3. 타입 일관성**

- `UserModel.create(username, name, email)` — Task 1에서 정의, Task 2·7에서 동일 순서로 사용. 확인함.
- `UserModel.reconstruct(userId, username, name, email, role, status, createdAt, updatedAt)` — Task 1 정의, Task 2의 `UserRepositoryImpl`과 Task 2·7의 테스트 스텁이 동일 순서. 확인함.
- `AuthResult(accessToken, expiresIn, userId, username, email)` — Task 4 정의, Task 7·8에서 동일. `AuthToken.expiresIn()`이 `long`이고 `CookieFactory.accessToken(String, long)`도 `long`이라 일치.
- `SocialAccountModel.create(userId, provider, providerUserId, providerEmail)` — Task 5 정의, Task 7에서 동일.
- `KakaoUser(id, nickname, email, emailVerified)` — Task 6 정의, Task 7에서 동일.
- `CookieFactory.OAUTH_TX_COOKIE` — Task 3 정의, Task 8의 `@CookieValue`에서 사용.
- Task 7의 `linkOrCreate`는 기존 계정 연결 시 `kakaoUser.email()`(원본)을 `providerEmail`로 저장하고, 신규 사용자의 `users.email`에는 `verifiedEmail`(인증된 것만)을 넣는다. 의도한 구분이다 — `provider_email`은 카카오가 준 사실의 기록이고, `users.email`은 우리가 신뢰하는 값이다.
