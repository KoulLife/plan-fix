# 스팟 좋아요 기능 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인한 사용자가 스팟을 좋아요/좋아요 취소할 수 있게 하고, 상세 조회에 본인의 좋아요 여부(`isLiked`)를 내려준다.

**Architecture:** `users`와 `spots`를 잇는 새 테이블 `spot_likes`(다대다 이력)를 만들고, `(user_id, spot_id)` 유니크 제약으로 중복/동시성을 DB 레벨에서 막는다. `spots.like_count`는 지금처럼 비정규화 카운터로 유지하며 `view_count`와 같은 방식(원자적 UPDATE)으로 증감시킨다.

**Tech Stack:** Spring Boot 4.1 / Spring Data JPA / Spring Security(`@AuthenticationPrincipal`) / Postgres 17 / JUnit 5 + AssertJ + Mockito

**Spec:** `docs/superpowers/specs/2026-08-24-spot-likes-design.md`

## Global Constraints

- `(user_id, spot_id)`에 DB 유니크 제약을 건다 — 이게 "이미 좋아요했는가"의 유일한 진실 소스다.
- 좋아요/취소는 idempotent다: 중복 좋아요·안 한 것 취소는 에러 없이 현재 상태를 그대로 응답한다.
- `spot_likes` insert가 유니크 제약 위반(`DataIntegrityViolationException`)으로 실패하면 "이미 좋아요됨"으로 간주하고 카운트 증가를 건너뛴다 (`SocialLoginApplicationService`의 동시 로그인 처리와 같은 패턴).
- `like_count` 감소는 0 밑으로 내려가지 않는다.
- 로컬은 `ddl-auto: update`가 새 테이블을 자동 생성하지만, 운영은 `ddl-auto: validate`라 `docs/migrations/2026-08-24-create-spot-likes.sql`을 반드시 만든다.
- 통합 테스트(`@SpringBootTest`)로 실제 로컬 DB에 쓰는 테스트 클래스에는 `@Transactional`을 붙여 테스트가 끝나면 롤백되게 한다 — 로컬 DB에 가짜 데이터를 남기지 않는다.
- `SpotLikeController`의 POST/DELETE는 `SecurityConfig`에 별도 설정을 추가하지 않아도 기본 규칙(`anyRequest().authenticated()`)에 걸려 인증을 요구한다 — `/api/v1/spots/*`(GET만 permitAll)는 세그먼트 1개까지만 매치해서 `/api/v1/spots/{id}/like`(세그먼트 2개)와 겹치지 않는다.

---

### Task 1: 도메인 — SpotLikeModel

**Files:**
- Create: `backend/src/main/java/taedonghee/plan_fix/domain/spot/SpotLikeModel.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/domain/spot/SpotLikeModelTest.java`

**Interfaces:**
- Produces: `SpotLikeModel.create(Long userId, Long spotId)`, `SpotLikeModel.reconstruct(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt)`, 게터 `spotLikeId()`/`userId()`/`spotId()`/`createdAt()`

- [ ] **Step 1: Write the failing test**

```java
package taedonghee.plan_fix.domain.spot;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotLikeModelTest {

    @Test
    void userId와_spotId로_생성된다() {
        SpotLikeModel like = SpotLikeModel.create(1L, 100L);

        assertThat(like.userId()).isEqualTo(1L);
        assertThat(like.spotId()).isEqualTo(100L);
        assertThat(like.spotLikeId()).isNull();
        assertThat(like.createdAt()).isNotNull();
    }

    @Test
    void userId가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> SpotLikeModel.create(null, 100L))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void spotId가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> SpotLikeModel.create(1L, null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("spotId");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `SpotLikeModel`이 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

```java
package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * [domain] 사용자가 스팟을 좋아요한 이력 한 건. users와 spots를 잇는 다대다 관계 테이블(spot_likes)의
 * canonical 모델이다. (userId, spotId) 조합은 DB 유니크 제약으로 유일함이 보장된다.
 */
public class SpotLikeModel {

    private final Long spotLikeId;
    private final Long userId;
    private final Long spotId;
    private final OffsetDateTime createdAt;

    private SpotLikeModel(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (spotId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "spotId는 필수입니다.");
        }
        this.spotLikeId = spotLikeId;
        this.userId = userId;
        this.spotId = spotId;
        this.createdAt = createdAt;
    }

    /** 신규 좋아요 생성 */
    public static SpotLikeModel create(Long userId, Long spotId) {
        return new SpotLikeModel(null, userId, spotId, OffsetDateTime.now());
    }

    /** infrastructure의 영속 데이터 복원 */
    public static SpotLikeModel reconstruct(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt) {
        return new SpotLikeModel(spotLikeId, userId, spotId, createdAt);
    }

    public Long spotLikeId() { return spotLikeId; }
    public Long userId() { return userId; }
    public Long spotId() { return spotId; }
    public OffsetDateTime createdAt() { return createdAt; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.domain.spot.SpotLikeModelTest" --console=plain`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다 (표준 작업 방식).

---

### Task 2: 좋아요 저장소 — spot_likes 테이블, SpotLikeRepository

**Files:**
- Create: `backend/src/main/java/taedonghee/plan_fix/domain/spot/SpotLikeRepository.java`
- Create: `backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeJpaEntity.java`
- Create: `backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeJpaRepository.java`
- Create: `backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeRepositoryImpl.java`
- Create: `docs/migrations/2026-08-24-create-spot-likes.sql`
- Test: `backend/src/test/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeRepositoryImplTest.java`

**Interfaces:**
- Consumes: `SpotLikeModel.create`/`.reconstruct`(Task 1)
- Produces: `SpotLikeRepository`의 `existsByUserIdAndSpotId(Long, Long): boolean`, `save(SpotLikeModel): SpotLikeModel`(유니크 제약 위반 시 `DataIntegrityViolationException` 그대로 전파), `deleteByUserIdAndSpotId(Long, Long): boolean`(실제로 지웠으면 true)

- [ ] **Step 1: Write the failing test**

```java
package taedonghee.plan_fix.infrastructure.spot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spot_likes에 실제로 저장/조회/삭제되는지, 유니크 제약(user_id, spot_id)이 실제로 걸려 있는지 본다.
 * @Transactional로 각 테스트가 끝나면 롤백해서 로컬 DB에 가짜 데이터를 남기지 않는다.
 */
@SpringBootTest
@Transactional
class SpotLikeRepositoryImplTest {

    @Autowired
    private SpotLikeRepository spotLikeRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 좋아요를_저장하고_존재_여부를_조회한다() {
        Long userId = saveUser().getUserId();
        Long spotId = saveSpot().spotId();

        assertThat(spotLikeRepository.existsByUserIdAndSpotId(userId, spotId)).isFalse();

        spotLikeRepository.save(SpotLikeModel.create(userId, spotId));

        assertThat(spotLikeRepository.existsByUserIdAndSpotId(userId, spotId)).isTrue();
    }

    @Test
    void 같은_사용자가_같은_스팟을_두번_좋아요하면_유니크_제약에_걸린다() {
        Long userId = saveUser().getUserId();
        Long spotId = saveSpot().spotId();
        spotLikeRepository.save(SpotLikeModel.create(userId, spotId));

        assertThatThrownBy(() -> spotLikeRepository.save(SpotLikeModel.create(userId, spotId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 좋아요를_취소하면_삭제되고_true를_반환한다() {
        Long userId = saveUser().getUserId();
        Long spotId = saveSpot().spotId();
        spotLikeRepository.save(SpotLikeModel.create(userId, spotId));

        boolean deleted = spotLikeRepository.deleteByUserIdAndSpotId(userId, spotId);

        assertThat(deleted).isTrue();
        assertThat(spotLikeRepository.existsByUserIdAndSpotId(userId, spotId)).isFalse();
    }

    @Test
    void 좋아요하지_않은_것을_취소하면_false를_반환한다() {
        Long userId = saveUser().getUserId();
        Long spotId = saveSpot().spotId();

        boolean deleted = spotLikeRepository.deleteByUserIdAndSpotId(userId, spotId);

        assertThat(deleted).isFalse();
    }

    private UserModel saveUser() {
        return userRepository.save(UserModel.create("좋아요테스트유저" + System.nanoTime(), null, null));
    }

    private SpotModel saveSpot() {
        return spotRepository.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("테스트 스팟")
                .category("관광지")
                .build());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `SpotLikeRepository` 등이 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

`backend/src/main/java/taedonghee/plan_fix/domain/spot/SpotLikeRepository.java`:

```java
package taedonghee.plan_fix.domain.spot;

/**
 * [domain] 저장소 포트. domain은 이 인터페이스만 알고, 구현(JPA 등)은 infrastructure가 담당한다.
 */
public interface SpotLikeRepository {

    boolean existsByUserIdAndSpotId(Long userId, Long spotId);

    /** (user_id, spot_id) 유니크 제약 위반 시 DataIntegrityViolationException을 그대로 던진다. */
    SpotLikeModel save(SpotLikeModel like);

    /** 실제로 지운 행이 있으면 true, 원래 좋아요하지 않았으면(지울 행이 없으면) false. */
    boolean deleteByUserIdAndSpotId(Long userId, Long spotId);
}
```

`backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeJpaEntity.java`:

```java
package taedonghee.plan_fix.infrastructure.spot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * [infrastructure] SpotLike의 JPA 매핑 전용 엔티티. spots 쪽 다른 엔티티들과 같은 관례로
 * user_id/spot_id를 @ManyToOne 없이 순수 컬럼으로 둔다(infrastructure.user에 대한 의존을 만들지 않기 위함).
 */
@Entity
@Table(
        name = "spot_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_spot_likes_user_spot", columnNames = {"user_id", "spot_id"}),
        indexes = @Index(name = "idx_spot_likes_spot_id", columnList = "spot_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotLikeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spot_like_id")
    private Long spotLikeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Builder
    private SpotLikeJpaEntity(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt) {
        this.spotLikeId = spotLikeId;
        this.userId = userId;
        this.spotId = spotId;
        this.createdAt = createdAt;
    }
}
```

`backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeJpaRepository.java`:

```java
package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface SpotLikeJpaRepository extends JpaRepository<SpotLikeJpaEntity, Long> {

    Optional<SpotLikeJpaEntity> findByUserIdAndSpotId(Long userId, Long spotId);

    /** deleteBy 파생 쿼리는 Spring Data가 자동으로 트랜잭션을 걸고, 지운 행 수를 반환한다. */
    long deleteByUserIdAndSpotId(Long userId, Long spotId);
}
```

`backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotLikeRepositoryImpl.java`:

```java
package taedonghee.plan_fix.infrastructure.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;

/**
 * [infrastructure] domain.SpotLikeRepository 포트의 JPA 구현체(어댑터).
 */
@Repository
@RequiredArgsConstructor
public class SpotLikeRepositoryImpl implements SpotLikeRepository {

    private final SpotLikeJpaRepository spotLikeJpaRepository;

    @Override
    public boolean existsByUserIdAndSpotId(Long userId, Long spotId) {
        return spotLikeJpaRepository.findByUserIdAndSpotId(userId, spotId).isPresent();
    }

    @Override
    public SpotLikeModel save(SpotLikeModel like) {
        SpotLikeJpaEntity saved = spotLikeJpaRepository.save(SpotLikeJpaEntity.builder()
                .spotLikeId(like.spotLikeId())
                .userId(like.userId())
                .spotId(like.spotId())
                .createdAt(like.createdAt())
                .build());
        return SpotLikeModel.reconstruct(saved.getSpotLikeId(), saved.getUserId(), saved.getSpotId(), saved.getCreatedAt());
    }

    @Override
    public boolean deleteByUserIdAndSpotId(Long userId, Long spotId) {
        return spotLikeJpaRepository.deleteByUserIdAndSpotId(userId, spotId) > 0;
    }
}
```

`docs/migrations/2026-08-24-create-spot-likes.sql`:

```sql
-- users와 spots를 잇는 좋아요 이력 테이블. (user_id, spot_id) 유니크 제약이
-- "이 사용자가 이 스팟을 좋아요했는가"의 유일한 진실 소스이자 동시 요청으로 인한
-- 중복 좋아요를 DB 레벨에서 막는 안전장치다.
-- 로컬은 ddl-auto:update가 자동으로 만들어 주지만, 운영은 ddl-auto:validate라 직접 실행해야 한다.
CREATE TABLE IF NOT EXISTS spot_likes (
    spot_like_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    spot_id BIGINT NOT NULL REFERENCES spots(spot_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_spot_likes_user_spot UNIQUE (user_id, spot_id)
);
CREATE INDEX IF NOT EXISTS idx_spot_likes_spot_id ON spot_likes(spot_id);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.infrastructure.spot.SpotLikeRepositoryImplTest" --console=plain`
Expected: PASS (4 tests). 로컬은 `ddl-auto: update`라 마이그레이션 SQL을 실행하지 않아도 이 테스트가 통과한다(Hibernate가 테이블을 자동 생성).

- [ ] **Step 5: 마이그레이션을 로컬 DB에도 실행해 운영과 스키마를 맞춘다 (idempotent라 안전)**

Run: `docker exec -i docker-postgres-1 psql -U planfix -d planfix < docs/migrations/2026-08-24-create-spot-likes.sql`
Expected: `CREATE TABLE`, `CREATE INDEX` (이미 있으면 `NOTICE: relation ... already exists, skipping`)

- [ ] **Step 6: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다.

---

### Task 3: SpotRepository — like_count 원자적 증감

**Files:**
- Modify: `backend/src/main/java/taedonghee/plan_fix/domain/spot/SpotRepository.java`
- Modify: `backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotJpaRepository.java`
- Modify: `backend/src/main/java/taedonghee/plan_fix/infrastructure/spot/SpotRepositoryImpl.java`
- Modify: `backend/src/test/java/taedonghee/plan_fix/infrastructure/spot/SpotRepositoryImplTest.java`

**Interfaces:**
- Produces: `SpotRepository.incrementLikeCount(Long spotId)`, `SpotRepository.decrementLikeCount(Long spotId)` (0 밑으로 내려가지 않음)

- [ ] **Step 1: Write the failing test**

`SpotRepositoryImplTest.java`의 `조회수를_원자적으로_1_증가시킨다()` 테스트 바로 아래에 추가:

```java
    @Test
    void 좋아요수를_원자적으로_1_증가시킨다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 5, 0);

        spotRepository.incrementLikeCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.likeCount()).isEqualTo(6);
    }

    @Test
    void 좋아요수를_원자적으로_1_감소시킨다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 5, 0);

        spotRepository.decrementLikeCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.likeCount()).isEqualTo(4);
    }

    @Test
    void 좋아요수는_0_밑으로_내려가지_않는다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 0, 0);

        spotRepository.decrementLikeCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.likeCount()).isEqualTo(0);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `incrementLikeCount`/`decrementLikeCount`가 `SpotRepository`에 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

`SpotRepository.java`의 `incrementViewCount` 선언 바로 아래에 추가:

```java
	/** like_count를 DB에서 직접 +1 한다(원자적). */
	void incrementLikeCount(Long spotId);

	/** like_count를 DB에서 직접 -1 한다. 0 밑으로는 내려가지 않는다. */
	void decrementLikeCount(Long spotId);
```

`SpotJpaRepository.java`의 `incrementViewCount` 메서드 바로 아래에 추가:

```java
	/** like_count를 DB에서 직접 +1 한다. */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE SpotJpaEntity s SET s.likeCount = s.likeCount + 1 WHERE s.spotId = :spotId")
	void incrementLikeCount(@Param("spotId") Long spotId);

	/** like_count를 DB에서 직접 -1 한다. 0 밑으로 내려가지 않게 CASE로 가드한다. */
	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE SpotJpaEntity s
			SET s.likeCount = CASE WHEN s.likeCount > 0 THEN s.likeCount - 1 ELSE 0 END
			WHERE s.spotId = :spotId
			""")
	void decrementLikeCount(@Param("spotId") Long spotId);
```

`SpotRepositoryImpl.java`의 `incrementViewCount` 메서드 바로 아래에 추가:

```java
	@Override
	public void incrementLikeCount(Long spotId) {
		spotJpaRepository.incrementLikeCount(spotId);
	}

	@Override
	public void decrementLikeCount(Long spotId) {
		spotJpaRepository.decrementLikeCount(spotId);
	}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.infrastructure.spot.SpotRepositoryImplTest" --console=plain`
Expected: PASS (기존 10개 + 신규 3개 = 13개)

- [ ] **Step 5: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다.

---

### Task 4: application — SpotLikeApplicationService

**Files:**
- Create: `backend/src/main/java/taedonghee/plan_fix/application/spot/SpotLikeResult.java`
- Create: `backend/src/main/java/taedonghee/plan_fix/application/spot/SpotLikeApplicationService.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/application/spot/SpotLikeApplicationServiceTest.java`

**Interfaces:**
- Consumes: `SpotRepository.findById/incrementLikeCount/decrementLikeCount`(Task 3), `SpotLikeRepository.existsByUserIdAndSpotId/save/deleteByUserIdAndSpotId`(Task 2), `SpotModel.likeCount()`/`.status()`(기존)
- Produces: `SpotLikeApplicationService.like(Long userId, Long spotId): SpotLikeResult`, `.unlike(Long userId, Long spotId): SpotLikeResult`, `record SpotLikeResult(boolean liked, long likeCount)` — Task 6(컨트롤러)이 그대로 쓴다

- [ ] **Step 1: Write the failing test**

```java
package taedonghee.plan_fix.application.spot;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotSearchCondition;
import taedonghee.plan_fix.domain.spot.SpotSortType;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * idempotency·404·동시성(유니크 제약 위반) 처리만 보는 순수 애플리케이션 단위 테스트라 페이크 저장소를 쓴다.
 * 실제 유니크 제약 위반은 SpotLikeRepositoryImplTest가 담당한다.
 */
class SpotLikeApplicationServiceTest {

    @Test
    void 좋아요하면_liked_true와_증가된_카운트를_반환한다() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spotOf(3));

        SpotLikeResult result = fixture.service().like(1L, spot.spotId());

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(4);
        assertThat(fixture.spots.findById(spot.spotId()).orElseThrow().likeCount()).isEqualTo(4);
        assertThat(fixture.spotLikes.existsByUserIdAndSpotId(1L, spot.spotId())).isTrue();
    }

    @Test
    void 이미_좋아요한_상태에서_또_좋아요하면_카운트가_늘지_않는다() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spotOf(3));
        SpotLikeApplicationService service = fixture.service();
        service.like(1L, spot.spotId());

        SpotLikeResult result = service.like(1L, spot.spotId());

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(4);
        assertThat(fixture.spots.findById(spot.spotId()).orElseThrow().likeCount()).isEqualTo(4);
    }

    @Test
    void 동시에_들어온_좋아요_요청은_유니크_제약_위반을_이미_좋아요됨으로_처리한다() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spotOf(3));
        fixture.spotLikes.throwOnNextSave = true;

        SpotLikeResult result = fixture.service().like(1L, spot.spotId());

        assertThat(result.liked()).isTrue();
        // 경쟁에서 진 쪽이라 이 요청 스스로는 카운트를 늘리지 않는다(이긴 쪽이 이미 늘렸다고 가정)
        assertThat(result.likeCount()).isEqualTo(3);
    }

    @Test
    void 취소하면_liked_false와_감소된_카운트를_반환한다() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spotOf(3));
        SpotLikeApplicationService service = fixture.service();
        service.like(1L, spot.spotId());

        SpotLikeResult result = service.unlike(1L, spot.spotId());

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(fixture.spotLikes.existsByUserIdAndSpotId(1L, spot.spotId())).isFalse();
    }

    @Test
    void 좋아요하지_않은_것을_취소하면_카운트가_줄지_않는다() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spotOf(3));

        SpotLikeResult result = fixture.service().unlike(1L, spot.spotId());

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(3);
    }

    @Test
    void 존재하지_않는_스팟에_좋아요하면_404_예외가_발생한다() {
        SpotLikeApplicationService service = new Fixture().service();

        assertThatThrownBy(() -> service.like(1L, 999L)).isInstanceOf(CoreException.class);
    }

    @Test
    void HIDDEN_스팟에_좋아요하면_404_예외가_발생한다() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("숨김 스팟")
                .category("관광지")
                .status(SpotStatus.HIDDEN)
                .build());

        assertThatThrownBy(() -> fixture.service().like(1L, spot.spotId())).isInstanceOf(CoreException.class);
    }

    private SpotModel spotOf(long likeCount) {
        return SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("경포해수욕장")
                .category("관광지")
                .likeCount(likeCount)
                .build();
    }

    static class Fixture {
        final InMemorySpotRepository spots = new InMemorySpotRepository();
        final InMemorySpotLikeRepository spotLikes = new InMemorySpotLikeRepository();

        SpotLikeApplicationService service() {
            return new SpotLikeApplicationService(spots, spotLikes);
        }
    }

    static class InMemorySpotRepository implements SpotRepository {
        private final List<SpotModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public SpotModel save(SpotModel spot) {
            SpotModel stored = SpotModel.builder()
                    .spotId(spot.spotId() == null ? ++sequence : spot.spotId())
                    .sourceType(spot.sourceType())
                    .attributes(new SpotModel.SourceAttributes(spot.title(), spot.category(), spot.region(),
                            spot.sigungu(), spot.address(), spot.latitude(), spot.longitude(), spot.thumbnail(),
                            spot.description()))
                    .viewCount(spot.viewCount())
                    .likeCount(spot.likeCount())
                    .commentCount(spot.commentCount())
                    .status(spot.status())
                    .createdAt(spot.createdAt())
                    .build();
            saved.removeIf(s -> s.spotId().equals(stored.spotId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<SpotModel> findById(Long spotId) {
            return saved.stream().filter(s -> s.spotId().equals(spotId)).findFirst();
        }

        @Override
        public long countAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpotModel> searchActive(SpotSearchCondition condition, SpotSortType sort, int offset, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countActive(SpotSearchCondition condition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void incrementViewCount(Long spotId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void incrementLikeCount(Long spotId) {
            findById(spotId).ifPresent(spot -> save(withLikeCount(spot, spot.likeCount() + 1)));
        }

        @Override
        public void decrementLikeCount(Long spotId) {
            findById(spotId).ifPresent(spot -> save(withLikeCount(spot, Math.max(spot.likeCount() - 1, 0))));
        }

        private SpotModel withLikeCount(SpotModel spot, long likeCount) {
            return SpotModel.builder()
                    .spotId(spot.spotId())
                    .sourceType(spot.sourceType())
                    .attributes(new SpotModel.SourceAttributes(spot.title(), spot.category(), spot.region(),
                            spot.sigungu(), spot.address(), spot.latitude(), spot.longitude(), spot.thumbnail(),
                            spot.description()))
                    .viewCount(spot.viewCount())
                    .likeCount(likeCount)
                    .commentCount(spot.commentCount())
                    .status(spot.status())
                    .createdAt(spot.createdAt())
                    .build();
        }
    }

    /** throwOnNextSave로 동시성 레이스(유니크 제약 위반)를 흉내낸다. */
    static class InMemorySpotLikeRepository implements SpotLikeRepository {
        private final List<SpotLikeModel> saved = new ArrayList<>();
        private long sequence = 0;
        boolean throwOnNextSave = false;

        @Override
        public boolean existsByUserIdAndSpotId(Long userId, Long spotId) {
            return saved.stream().anyMatch(l -> l.userId().equals(userId) && l.spotId().equals(spotId));
        }

        @Override
        public SpotLikeModel save(SpotLikeModel like) {
            if (throwOnNextSave) {
                throwOnNextSave = false;
                throw new DataIntegrityViolationException("duplicate like");
            }
            SpotLikeModel stored = SpotLikeModel.reconstruct(++sequence, like.userId(), like.spotId(), like.createdAt());
            saved.add(stored);
            return stored;
        }

        @Override
        public boolean deleteByUserIdAndSpotId(Long userId, Long spotId) {
            return saved.removeIf(l -> l.userId().equals(userId) && l.spotId().equals(spotId));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `SpotLikeApplicationService`, `SpotLikeResult`가 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

`backend/src/main/java/taedonghee/plan_fix/application/spot/SpotLikeResult.java`:

```java
package taedonghee.plan_fix.application.spot;

/** [application] 좋아요/취소 결과. */
public record SpotLikeResult(boolean liked, long likeCount) {
}
```

`backend/src/main/java/taedonghee/plan_fix/application/spot/SpotLikeApplicationService.java`:

```java
package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * [application] 스팟 좋아요/좋아요 취소. 둘 다 idempotent다 — 중복 좋아요·안 한 것 취소는
 * 에러 없이 현재 상태를 그대로 응답한다.
 */
@Service
@RequiredArgsConstructor
public class SpotLikeApplicationService {

    private final SpotRepository spotRepository;
    private final SpotLikeRepository spotLikeRepository;

    @Transactional
    public SpotLikeResult like(Long userId, Long spotId) {
        SpotModel spot = getActiveSpotOrThrow(spotId);

        if (spotLikeRepository.existsByUserIdAndSpotId(userId, spotId)) {
            return new SpotLikeResult(true, spot.likeCount());
        }

        try {
            spotLikeRepository.save(SpotLikeModel.create(userId, spotId));
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 좋아요 요청끼리의 경쟁. 유니크 제약이 막았으니 이미 좋아요된 것으로 본다.
            // 카운트는 먼저 커밋된 쪽이 이미 올렸으므로 여기서는 늘리지 않는다.
            return new SpotLikeResult(true, spot.likeCount());
        }

        spotRepository.incrementLikeCount(spotId);
        return new SpotLikeResult(true, spot.likeCount() + 1);
    }

    @Transactional
    public SpotLikeResult unlike(Long userId, Long spotId) {
        SpotModel spot = getActiveSpotOrThrow(spotId);

        boolean deleted = spotLikeRepository.deleteByUserIdAndSpotId(userId, spotId);
        if (!deleted) {
            return new SpotLikeResult(false, spot.likeCount());
        }

        spotRepository.decrementLikeCount(spotId);
        return new SpotLikeResult(false, Math.max(spot.likeCount() - 1, 0));
    }

    private SpotModel getActiveSpotOrThrow(Long spotId) {
        return spotRepository.findById(spotId)
                .filter(s -> s.status() == SpotStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "spot not found. spotId=" + spotId));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.application.spot.SpotLikeApplicationServiceTest" --console=plain`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다.

---

### Task 5: SpotDetailApplicationService — isLiked 반영

**Files:**
- Modify: `backend/src/main/java/taedonghee/plan_fix/application/spot/SpotDetailResult.java`
- Modify: `backend/src/main/java/taedonghee/plan_fix/application/spot/SpotDetailApplicationService.java`
- Modify: `backend/src/test/java/taedonghee/plan_fix/application/spot/SpotDetailApplicationServiceTest.java`

**Interfaces:**
- Consumes: `SpotLikeRepository.existsByUserIdAndSpotId`(Task 2)
- Produces: `SpotDetailApplicationService.get(Long spotId, Long viewerUserId): SpotDetailResult` (시그니처 변경 — 기존 `get(Long)`은 삭제), `SpotDetailResult`에 `boolean isLiked` 필드 추가(맨 끝), `SpotDetailResult.of(spot, viewCount, images, info, isLiked)`

- [ ] **Step 1: Write the failing test**

`SpotDetailApplicationServiceTest.java`를 아래 내용으로 전체 교체한다 (기존 6개 테스트의 `.get(spotId)` 호출을 전부 `.get(spotId, null)`로 바꾸고, isLiked 관련 테스트 3개와 `InMemorySpotLikeRepository` 페이크를 추가한 버전):

```java
package taedonghee.plan_fix.application.spot;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotSearchCondition;
import taedonghee.plan_fix.domain.spot.SpotSortType;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.domain.spot.TourDataImageModel;
import taedonghee.plan_fix.domain.spot.TourDataImageRepository;
import taedonghee.plan_fix.domain.spot.TourDataInfoModel;
import taedonghee.plan_fix.domain.spot.TourDataInfoRepository;
import taedonghee.plan_fix.domain.spot.TourDataSpotModel;
import taedonghee.plan_fix.domain.spot.TourDataSpotRepository;
import taedonghee.plan_fix.support.error.CoreException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 검증·404 처리·조회수 증가 반영·TourAPI 부가 데이터 병합·좋아요 여부 반영만 보는 순수
 * 애플리케이션 단위 테스트라 페이크 저장소를 쓴다. 실제 조인/조회 SQL은 각 RepositoryImplTest가 담당한다.
 */
class SpotDetailApplicationServiceTest {

    @Test
    void 정상_조회하면_전체_필드를_돌려주고_조회수를_1_늘린다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("경포해수욕장")
                .category("관광지")
                .region("51")
                .sigungu("150")
                .address("강원특별자치도 강릉시")
                .latitude(new BigDecimal("37.8127061"))
                .longitude(new BigDecimal("128.8987999"))
                .thumbnail("thumb.jpg")
                .description("동해안의 대표 해수욕장")
                .likeCount(3)
                .commentCount(1)
                .viewCount(10)
                .build());

        SpotDetailResult result = fixture.service().get(saved.spotId(), null);

        assertThat(result.spotId()).isEqualTo(saved.spotId());
        assertThat(result.title()).isEqualTo("경포해수욕장");
        assertThat(result.category()).isEqualTo("관광지");
        assertThat(result.region()).isEqualTo("51");
        assertThat(result.sigungu()).isEqualTo("150");
        assertThat(result.address()).isEqualTo("강원특별자치도 강릉시");
        assertThat(result.latitude()).isEqualByComparingTo("37.8127061");
        assertThat(result.longitude()).isEqualByComparingTo("128.8987999");
        assertThat(result.thumbnail()).isEqualTo("thumb.jpg");
        assertThat(result.description()).isEqualTo("동해안의 대표 해수욕장");
        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(result.commentCount()).isEqualTo(1);
        // 조회 시점에 +1 된 값을 응답에 바로 반영한다
        assertThat(result.viewCount()).isEqualTo(11);
        assertThat(result.isLiked()).isFalse();
    }

    @Test
    void 조회할_때마다_저장소의_조회수를_실제로_증가시킨다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("경포해수욕장")
                .category("관광지")
                .viewCount(0)
                .build());
        SpotDetailApplicationService service = fixture.service();

        service.get(saved.spotId(), null);
        service.get(saved.spotId(), null);

        assertThat(fixture.spots.findById(saved.spotId()).orElseThrow().viewCount()).isEqualTo(2);
    }

    @Test
    void 존재하지_않으면_404_예외가_발생한다() {
        SpotDetailApplicationService service = new Fixture().service();

        assertThatThrownBy(() -> service.get(999L, null)).isInstanceOf(CoreException.class);
    }

    @Test
    void HIDDEN_상태면_404_예외가_발생한다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("숨김 스팟")
                .category("관광지")
                .status(SpotStatus.HIDDEN)
                .build());

        assertThatThrownBy(() -> fixture.service().get(saved.spotId(), null)).isInstanceOf(CoreException.class);
    }

    @Test
    void NATIVE_스팟은_TourAPI_부가정보를_붙이지_않는다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("직접등록 스팟")
                .category("관광지")
                .build());
        // NATIVE인데도 tour_data_spots에 연결이 있다면(있을 수 없는 상황이지만) 무시해야 한다는 것까지 확인
        fixture.tourDataSpots.save(TourDataSpotModel.builder()
                .contentId(1L).spotId(saved.spotId()).title("직접등록 스팟").build());

        SpotDetailResult result = fixture.service().get(saved.spotId(), null);

        assertThat(result.images()).isEmpty();
        assertThat(result.info()).isNull();
    }

    @Test
    void TOUR_API_스팟이지만_아직_수집되지_않았으면_빈값을_돌려준다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.TOUR_API)
                .title("아직 수집 전")
                .category("관광지")
                .build());
        // tourDataSpots에 연결된 행이 아예 없는 상태

        SpotDetailResult result = fixture.service().get(saved.spotId(), null);

        assertThat(result.images()).isEmpty();
        assertThat(result.info()).isNull();
    }

    @Test
    void TOUR_API_스팟이면_사진과_상세정보를_함께_붙인다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.TOUR_API)
                .title("경포해수욕장")
                .category("관광지")
                .build());
        TourDataSpotModel tourDataSpot = fixture.tourDataSpots.save(TourDataSpotModel.builder()
                .contentId(12345L)
                .spotId(saved.spotId())
                .title("경포해수욕장")
                .build());
        fixture.tourDataImages.saveAll(List.of(
                TourDataImageModel.builder()
                        .tourDataSpotId(tourDataSpot.tourDataSpotId())
                        .contentId(12345L)
                        .originalImage("http://example.com/1.jpg")
                        .build(),
                TourDataImageModel.builder()
                        .tourDataSpotId(tourDataSpot.tourDataSpotId())
                        .contentId(12345L)
                        .originalImage("http://example.com/2.jpg")
                        .build()
        ));
        fixture.tourDataInfos.save(TourDataInfoModel.builder()
                .tourDataSpotId(tourDataSpot.tourDataSpotId())
                .contentId(12345L)
                .tel("033-000-0000")
                .parkInfo("가능")
                .timeInfo("09:00~18:00")
                .restInfo("연중무휴")
                .build());

        SpotDetailResult result = fixture.service().get(saved.spotId(), null);

        assertThat(result.images()).containsExactly("http://example.com/1.jpg", "http://example.com/2.jpg");
        assertThat(result.info()).isNotNull();
        assertThat(result.info().tel()).isEqualTo("033-000-0000");
        assertThat(result.info().parkInfo()).isEqualTo("가능");
        assertThat(result.info().timeInfo()).isEqualTo("09:00~18:00");
        assertThat(result.info().restInfo()).isEqualTo("연중무휴");
        assertThat(result.info().firstMenu()).isNull();
    }

    @Test
    void 로그인한_사용자가_좋아요한_스팟이면_isLiked가_true다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("경포해수욕장")
                .category("관광지")
                .build());
        fixture.spotLikes.save(SpotLikeModel.create(10L, saved.spotId()));

        SpotDetailResult result = fixture.service().get(saved.spotId(), 10L);

        assertThat(result.isLiked()).isTrue();
    }

    @Test
    void 좋아요하지_않았으면_isLiked가_false다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("경포해수욕장")
                .category("관광지")
                .build());

        SpotDetailResult result = fixture.service().get(saved.spotId(), 10L);

        assertThat(result.isLiked()).isFalse();
    }

    @Test
    void 비로그인이면_다른_사람이_좋아요했어도_isLiked가_false다() {
        Fixture fixture = new Fixture();
        SpotModel saved = fixture.spots.save(SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("경포해수욕장")
                .category("관광지")
                .build());
        fixture.spotLikes.save(SpotLikeModel.create(10L, saved.spotId()));

        SpotDetailResult result = fixture.service().get(saved.spotId(), null);

        assertThat(result.isLiked()).isFalse();
    }

    /** 테스트 대상과 페이크 저장소 5종을 묶은 픽스처. */
    static class Fixture {
        final InMemorySpotRepository spots = new InMemorySpotRepository();
        final InMemoryTourDataSpotRepository tourDataSpots = new InMemoryTourDataSpotRepository();
        final InMemoryTourDataInfoRepository tourDataInfos = new InMemoryTourDataInfoRepository();
        final InMemoryTourDataImageRepository tourDataImages = new InMemoryTourDataImageRepository();
        final InMemorySpotLikeRepository spotLikes = new InMemorySpotLikeRepository();

        SpotDetailApplicationService service() {
            return new SpotDetailApplicationService(spots, tourDataSpots, tourDataInfos, tourDataImages, spotLikes);
        }
    }

    /** 조건에 맞는 것만 걸러 spotId 내림차순으로 돌려주는 메모리 페이크. */
    static class InMemorySpotRepository implements SpotRepository {
        private final List<SpotModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public SpotModel save(SpotModel spot) {
            SpotModel stored = SpotModel.builder()
                    .spotId(spot.spotId() == null ? ++sequence : spot.spotId())
                    .sourceType(spot.sourceType())
                    .attributes(new SpotModel.SourceAttributes(spot.title(), spot.category(), spot.region(),
                            spot.sigungu(), spot.address(), spot.latitude(), spot.longitude(), spot.thumbnail(),
                            spot.description()))
                    .viewCount(spot.viewCount())
                    .likeCount(spot.likeCount())
                    .commentCount(spot.commentCount())
                    .status(spot.status())
                    .createdAt(spot.createdAt())
                    .build();
            saved.removeIf(s -> s.spotId().equals(stored.spotId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<SpotModel> findById(Long spotId) {
            return saved.stream().filter(s -> s.spotId().equals(spotId)).findFirst();
        }

        @Override
        public long countAll() {
            return saved.size();
        }

        @Override
        public List<SpotModel> searchActive(SpotSearchCondition condition, SpotSortType sort, int offset, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countActive(SpotSearchCondition condition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void incrementViewCount(Long spotId) {
            findById(spotId).ifPresent(spot -> save(SpotModel.builder()
                    .spotId(spot.spotId())
                    .sourceType(spot.sourceType())
                    .attributes(new SpotModel.SourceAttributes(spot.title(), spot.category(), spot.region(),
                            spot.sigungu(), spot.address(), spot.latitude(), spot.longitude(), spot.thumbnail(),
                            spot.description()))
                    .viewCount(spot.viewCount() + 1)
                    .likeCount(spot.likeCount())
                    .commentCount(spot.commentCount())
                    .status(spot.status())
                    .createdAt(spot.createdAt())
                    .build()));
        }

        @Override
        public void incrementLikeCount(Long spotId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void decrementLikeCount(Long spotId) {
            throw new UnsupportedOperationException();
        }
    }

    static class InMemoryTourDataSpotRepository implements TourDataSpotRepository {
        private final List<TourDataSpotModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public TourDataSpotModel save(TourDataSpotModel tourDataSpot) {
            TourDataSpotModel stored = TourDataSpotModel.builder()
                    .tourDataSpotId(tourDataSpot.tourDataSpotId() == null ? ++sequence : tourDataSpot.tourDataSpotId())
                    .contentId(tourDataSpot.contentId())
                    .spotId(tourDataSpot.spotId())
                    .title(tourDataSpot.title())
                    .build();
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<TourDataSpotModel> findByContentId(Long contentId) {
            return saved.stream().filter(s -> s.contentId().equals(contentId)).findFirst();
        }

        @Override
        public Optional<TourDataSpotModel> findBySpotId(Long spotId) {
            return saved.stream().filter(s -> spotId.equals(s.spotId())).findFirst();
        }

        @Override
        public List<TourDataSpotModel> findByRegionAndSigungu(String reg, String sigungu) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TourDataSpotModel> findByRegionAndSigunguAndImageNotCollected(String reg, String sigungu) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TourDataSpotModel> findByRegionAndSigunguAndInfoNotCollected(String reg, String sigungu) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countAll() {
            return saved.size();
        }
    }

    static class InMemoryTourDataInfoRepository implements TourDataInfoRepository {
        private final List<TourDataInfoModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public TourDataInfoModel save(TourDataInfoModel info) {
            TourDataInfoModel stored = TourDataInfoModel.builder()
                    .tourDataInfoId(++sequence)
                    .tourDataSpotId(info.tourDataSpotId())
                    .contentId(info.contentId())
                    .category(info.category())
                    .firstMenu(info.firstMenu())
                    .treatMenu(info.treatMenu())
                    .tel(info.tel())
                    .parkInfo(info.parkInfo())
                    .timeInfo(info.timeInfo())
                    .restInfo(info.restInfo())
                    .lcnsno(info.lcnsno())
                    .build();
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<TourDataInfoModel> findByContentId(Long contentId) {
            return saved.stream().filter(i -> i.contentId().equals(contentId)).findFirst();
        }

        @Override
        public long countAll() {
            return saved.size();
        }
    }

    static class InMemoryTourDataImageRepository implements TourDataImageRepository {
        private final List<TourDataImageModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public TourDataImageModel save(TourDataImageModel image) {
            TourDataImageModel stored = TourDataImageModel.builder()
                    .tourDataImageId(++sequence)
                    .tourDataSpotId(image.tourDataSpotId())
                    .contentId(image.contentId())
                    .imageName(image.imageName())
                    .originalImage(image.originalImage())
                    .smallImage(image.smallImage())
                    .build();
            saved.add(stored);
            return stored;
        }

        @Override
        public void saveAll(List<TourDataImageModel> images) {
            images.forEach(this::save);
        }

        @Override
        public List<TourDataImageModel> findByTourDataSpotId(Long tourDataSpotId) {
            return saved.stream().filter(i -> i.tourDataSpotId().equals(tourDataSpotId)).toList();
        }

        @Override
        public void deleteByTourDataSpotId(Long tourDataSpotId) {
            saved.removeIf(i -> i.tourDataSpotId().equals(tourDataSpotId));
        }

        @Override
        public long countAll() {
            return saved.size();
        }
    }

    static class InMemorySpotLikeRepository implements SpotLikeRepository {
        private final List<SpotLikeModel> saved = new ArrayList<>();

        @Override
        public boolean existsByUserIdAndSpotId(Long userId, Long spotId) {
            return saved.stream().anyMatch(l -> l.userId().equals(userId) && l.spotId().equals(spotId));
        }

        @Override
        public SpotLikeModel save(SpotLikeModel like) {
            saved.add(like);
            return like;
        }

        @Override
        public boolean deleteByUserIdAndSpotId(Long userId, Long spotId) {
            return saved.removeIf(l -> l.userId().equals(userId) && l.spotId().equals(spotId));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `get(Long, Long)`, `isLiked()`가 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

`SpotDetailResult.java`를 전체 교체:

```java
package taedonghee.plan_fix.application.spot;

import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.TourDataInfoModel;

import java.math.BigDecimal;
import java.util.List;

/**
 * [application] 스팟 상세 조회 결과. 공개 상세라 서비스 소유 필드 중 status/source는 담지 않는다.
 *
 * images/info는 TourAPI로 수집된 스팟일 때만 채워진다. 원본(NATIVE 등)이거나 아직
 * detailIntro2/detailImage2를 수집하지 않은 TourAPI 스팟이면 각각 빈 리스트/null이다.
 *
 * isLiked는 조회한 사람(viewerUserId) 기준이다. 비로그인이면 항상 false다.
 */
public record SpotDetailResult(
        Long spotId,
        String title,
        String category,
        String region,
        String sigungu,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnail,
        String description,
        long viewCount,
        long likeCount,
        long commentCount,
        List<String> images,
        TourInfo info,
        boolean isLiked
) {

    /**
     * viewCount를 별도로 받는 이유: 조회 시점에 저장소의 view_count를 +1 시키지만,
     * 그 갱신된 값을 다시 읽어오지 않고 이미 들고 있는 spot에 +1 한 값을 그대로 응답에 반영하기 위함이다.
     */
    public static SpotDetailResult of(
            SpotModel spot, long viewCount, List<String> images, TourInfo info, boolean isLiked) {
        return new SpotDetailResult(
                spot.spotId(),
                spot.title(),
                spot.category(),
                spot.region(),
                spot.sigungu(),
                spot.address(),
                spot.latitude(),
                spot.longitude(),
                spot.thumbnail(),
                spot.description(),
                viewCount,
                spot.likeCount(),
                spot.commentCount(),
                images,
                info,
                isLiked
        );
    }

    /**
     * detailIntro2 결과. contentTypeId마다 실제 값이 들어오는 필드가 달라
     * (예: firstMenu/treatMenu/lcnsno는 음식점만) 값이 없는 필드는 null로 내려간다.
     */
    public record TourInfo(
            String tel,
            String parkInfo,
            String timeInfo,
            String restInfo,
            String firstMenu,
            String treatMenu,
            String lcnsno
    ) {

        public static TourInfo from(TourDataInfoModel info) {
            return new TourInfo(
                    info.tel(),
                    info.parkInfo(),
                    info.timeInfo(),
                    info.restInfo(),
                    info.firstMenu(),
                    info.treatMenu(),
                    info.lcnsno()
            );
        }
    }
}
```

`SpotDetailApplicationService.java`를 전체 교체:

```java
package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.domain.spot.TourDataImageModel;
import taedonghee.plan_fix.domain.spot.TourDataImageRepository;
import taedonghee.plan_fix.domain.spot.TourDataInfoRepository;
import taedonghee.plan_fix.domain.spot.TourDataSpotModel;
import taedonghee.plan_fix.domain.spot.TourDataSpotRepository;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.List;
import java.util.Optional;

/**
 * [application] 공개 스팟 상세 조회. 조회할 때마다 view_count를 1 늘린다.
 *
 * sourceType이 TOUR_API인 스팟은 tour_data_spots를 역참조해 tour_data_info(부가 정보)와
 * tour_data_images(사진)를 함께 붙여 응답한다. 아직 그 단계까지 수집되지 않았거나
 * TourAPI 소스가 아니면 images는 빈 리스트, info는 null로 내려간다 — 에러가 아니다.
 *
 * viewerUserId는 조회하는 사람의 로그인 여부에 따라 null일 수 있다(비로그인).
 * null이면 isLiked는 항상 false다.
 */
@Service
@RequiredArgsConstructor
public class SpotDetailApplicationService {

    private final SpotRepository spotRepository;
    private final TourDataSpotRepository tourDataSpotRepository;
    private final TourDataInfoRepository tourDataInfoRepository;
    private final TourDataImageRepository tourDataImageRepository;
    private final SpotLikeRepository spotLikeRepository;

    @Transactional
    public SpotDetailResult get(Long spotId, Long viewerUserId) {
        SpotModel spot = spotRepository.findById(spotId)
                .filter(s -> s.status() == SpotStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "spot not found. spotId=" + spotId));

        spotRepository.incrementViewCount(spotId);

        List<String> images = List.of();
        SpotDetailResult.TourInfo info = null;

        if (spot.sourceType() == SpotSourceType.TOUR_API) {
            Optional<TourDataSpotModel> tourDataSpot = tourDataSpotRepository.findBySpotId(spotId);
            if (tourDataSpot.isPresent()) {
                TourDataSpotModel source = tourDataSpot.get();
                images = tourDataImageRepository.findByTourDataSpotId(source.tourDataSpotId()).stream()
                        .map(TourDataImageModel::originalImage)
                        .toList();
                info = tourDataInfoRepository.findByContentId(source.contentId())
                        .map(SpotDetailResult.TourInfo::from)
                        .orElse(null);
            }
        }

        boolean isLiked = viewerUserId != null && spotLikeRepository.existsByUserIdAndSpotId(viewerUserId, spotId);

        return SpotDetailResult.of(spot, spot.viewCount() + 1, images, info, isLiked);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.application.spot.SpotDetailApplicationServiceTest" --console=plain`
Expected: PASS (10 tests)

- [ ] **Step 5: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다.

---

### Task 6: interfaces — SpotLikeController

**Files:**
- Create: `backend/src/main/java/taedonghee/plan_fix/interfaces/api/spot/SpotLikeResponse.java`
- Create: `backend/src/main/java/taedonghee/plan_fix/interfaces/api/spot/SpotLikeController.java`
- Test: `backend/src/test/java/taedonghee/plan_fix/interfaces/api/spot/SpotLikeControllerTest.java`

**Interfaces:**
- Consumes: `SpotLikeApplicationService.like/unlike`(Task 4), `taedonghee.plan_fix.infrastructure.security.AuthenticatedUser`(기존, `id()`/`username()`/`role()`)
- Produces: `POST/DELETE /api/v1/spots/{spotId}/like` → `SpotLikeResponse(boolean liked, long likeCount)`

- [ ] **Step 1: Write the failing test**

```java
package taedonghee.plan_fix.interfaces.api.spot;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.spot.SpotLikeApplicationService;
import taedonghee.plan_fix.application.spot.SpotLikeResult;
import taedonghee.plan_fix.domain.user.UserRole;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpotLikeControllerTest {

    private final SpotLikeApplicationService spotLikeApplicationService = mock(SpotLikeApplicationService.class);
    private final SpotLikeController controller = new SpotLikeController(spotLikeApplicationService);
    private final AuthenticatedUser principal = new AuthenticatedUser(10L, "길동", UserRole.USER);

    @Test
    void 좋아요_요청은_principal_id로_서비스를_호출하고_결과를_그대로_응답한다() {
        when(spotLikeApplicationService.like(10L, 1L)).thenReturn(new SpotLikeResult(true, 4));

        ResponseEntity<SpotLikeResponse> response = controller.like(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().liked()).isTrue();
        assertThat(response.getBody().likeCount()).isEqualTo(4);
    }

    @Test
    void 좋아요_취소_요청은_principal_id로_서비스를_호출하고_결과를_그대로_응답한다() {
        when(spotLikeApplicationService.unlike(10L, 1L)).thenReturn(new SpotLikeResult(false, 3));

        ResponseEntity<SpotLikeResponse> response = controller.unlike(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().liked()).isFalse();
        assertThat(response.getBody().likeCount()).isEqualTo(3);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `SpotLikeController`, `SpotLikeResponse`가 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

`backend/src/main/java/taedonghee/plan_fix/interfaces/api/spot/SpotLikeResponse.java`:

```java
package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotLikeResult;

/** [interfaces] 좋아요/취소 응답 DTO. */
public record SpotLikeResponse(boolean liked, long likeCount) {

    public static SpotLikeResponse from(SpotLikeResult result) {
        return new SpotLikeResponse(result.liked(), result.likeCount());
    }
}
```

`backend/src/main/java/taedonghee/plan_fix/interfaces/api/spot/SpotLikeController.java`:

```java
package taedonghee.plan_fix.interfaces.api.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.spot.SpotLikeApplicationService;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

/**
 * [interfaces] 스팟 좋아요/좋아요 취소 API. 로그인이 필요하다 — SecurityConfig가 스팟 GET만
 * permitAll이라 이 경로(POST/DELETE)는 기본 규칙(anyRequest().authenticated())에 걸려
 * 별도 설정 없이 인증을 요구한다.
 */
@RestController
@RequestMapping("/api/v1/spots/{spotId}/like")
@RequiredArgsConstructor
public class SpotLikeController {

    private final SpotLikeApplicationService spotLikeApplicationService;

    /** 이미 좋아요한 상태면 조용히 무시하고 현재 상태를 그대로 응답한다(idempotent). */
    @PostMapping
    public ResponseEntity<SpotLikeResponse> like(
            @PathVariable Long spotId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SpotLikeResponse.from(spotLikeApplicationService.like(principal.id(), spotId)));
    }

    /** 좋아요하지 않은 상태에서 호출해도 조용히 무시하고 현재 상태를 그대로 응답한다(idempotent). */
    @DeleteMapping
    public ResponseEntity<SpotLikeResponse> unlike(
            @PathVariable Long spotId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SpotLikeResponse.from(spotLikeApplicationService.unlike(principal.id(), spotId)));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.interfaces.api.spot.SpotLikeControllerTest" --console=plain`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다.

---

### Task 7: SpotController/SpotDetailResponse — isLiked 노출 및 viewer 식별

**Files:**
- Modify: `backend/src/main/java/taedonghee/plan_fix/interfaces/api/spot/SpotDetailResponse.java`
- Modify: `backend/src/main/java/taedonghee/plan_fix/interfaces/api/spot/SpotController.java`
- Modify: `backend/src/test/java/taedonghee/plan_fix/interfaces/api/spot/SpotControllerTest.java`

**Interfaces:**
- Consumes: `SpotDetailApplicationService.get(Long, Long)`(Task 5), `taedonghee.plan_fix.infrastructure.security.AuthenticatedUser`(기존)
- Produces: `GET /api/v1/spots/{spotId}` 응답에 `isLiked` 포함, 로그인 상태면 `AuthenticatedUser.id()`를 viewerUserId로 전달

- [ ] **Step 1: Write the failing test**

`SpotControllerTest.java`를 아래 내용으로 전체 교체 (기존 `spotDetailApplicationService.get(1L)`류 호출과 `SpotDetailResult` 생성자 호출에 `isLiked`/viewerUserId를 반영하고, `principal` 관련 테스트 2개를 추가한 버전):

```java
package taedonghee.plan_fix.interfaces.api.spot;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.spot.SpotDetailApplicationService;
import taedonghee.plan_fix.application.spot.SpotDetailResult;
import taedonghee.plan_fix.application.spot.SpotListApplicationService;
import taedonghee.plan_fix.application.spot.SpotListQuery;
import taedonghee.plan_fix.application.spot.SpotListResult;
import taedonghee.plan_fix.domain.user.UserRole;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpotControllerTest {

    private final SpotListApplicationService spotListApplicationService = mock(SpotListApplicationService.class);
    private final SpotDetailApplicationService spotDetailApplicationService = mock(SpotDetailApplicationService.class);
    private final SpotController controller =
            new SpotController(spotListApplicationService, spotDetailApplicationService);

    @Test
    void 조회_결과를_응답_DTO로_변환한다() {
        SpotListResult result = new SpotListResult(
                List.of(new SpotListResult.Item(1L, "정동진", "관광지", "51", "150", "thumb.jpg")),
                0, 20, 1);
        when(spotListApplicationService.list(new SpotListQuery("관광지", "51", "150", "popular", 0, 20)))
                .thenReturn(result);

        ResponseEntity<SpotResponse> response = controller.list("관광지", "51", "150", "popular", 0, 20);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SpotResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offset()).isEqualTo(0);
        assertThat(body.size()).isEqualTo(20);
        assertThat(body.totalCount()).isEqualTo(1);
        assertThat(body.items()).hasSize(1);
        SpotResponse.Item item = body.items().getFirst();
        assertThat(item.spotId()).isEqualTo(1L);
        assertThat(item.title()).isEqualTo("정동진");
        assertThat(item.category()).isEqualTo("관광지");
        assertThat(item.region()).isEqualTo("51");
        assertThat(item.sigungu()).isEqualTo("150");
        assertThat(item.thumbnail()).isEqualTo("thumb.jpg");
    }

    @Test
    void 필터_파라미터가_없으면_null_query로_넘긴다() {
        when(spotListApplicationService.list(new SpotListQuery(null, null, null, null, 0, 20)))
                .thenReturn(new SpotListResult(List.of(), 0, 20, 0));

        controller.list(null, null, null, null, 0, 20);

        verify(spotListApplicationService).list(eq(new SpotListQuery(null, null, null, null, 0, 20)));
    }

    @Test
    void 상세_조회_결과를_응답_DTO로_변환한다() {
        SpotDetailResult.TourInfo tourInfo = new SpotDetailResult.TourInfo(
                "033-000-0000", "가능", "09:00~18:00", "연중무휴", null, null, null);
        SpotDetailResult result = new SpotDetailResult(1L, "정동진", "관광지", "51", "150",
                "강원특별자치도 강릉시", new BigDecimal("37.1"), new BigDecimal("129.0"), "thumb.jpg",
                "동해안의 대표 해변", 11, 3, 1,
                List.of("http://example.com/1.jpg"), tourInfo, true);
        when(spotDetailApplicationService.get(1L, null)).thenReturn(result);

        ResponseEntity<SpotDetailResponse> response = controller.get(1L, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SpotDetailResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.spotId()).isEqualTo(1L);
        assertThat(body.title()).isEqualTo("정동진");
        assertThat(body.category()).isEqualTo("관광지");
        assertThat(body.region()).isEqualTo("51");
        assertThat(body.sigungu()).isEqualTo("150");
        assertThat(body.address()).isEqualTo("강원특별자치도 강릉시");
        assertThat(body.latitude()).isEqualByComparingTo("37.1");
        assertThat(body.longitude()).isEqualByComparingTo("129.0");
        assertThat(body.thumbnail()).isEqualTo("thumb.jpg");
        assertThat(body.description()).isEqualTo("동해안의 대표 해변");
        assertThat(body.viewCount()).isEqualTo(11);
        assertThat(body.likeCount()).isEqualTo(3);
        assertThat(body.commentCount()).isEqualTo(1);
        assertThat(body.images()).containsExactly("http://example.com/1.jpg");
        assertThat(body.info()).isNotNull();
        assertThat(body.info().tel()).isEqualTo("033-000-0000");
        assertThat(body.isLiked()).isTrue();
    }

    @Test
    void TourAPI_부가정보가_없으면_images는_빈리스트_info는_null이다() {
        SpotDetailResult result = new SpotDetailResult(2L, "직접등록 스팟", "관광지", null, null,
                null, null, null, null, null, 0, 0, 0, List.of(), null, false);
        when(spotDetailApplicationService.get(2L, null)).thenReturn(result);

        ResponseEntity<SpotDetailResponse> response = controller.get(2L, null);

        SpotDetailResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.images()).isEmpty();
        assertThat(body.info()).isNull();
        assertThat(body.isLiked()).isFalse();
    }

    @Test
    void 로그인한_사용자면_principal_id를_viewerUserId로_넘긴다() {
        AuthenticatedUser principal = new AuthenticatedUser(10L, "길동", UserRole.USER);
        SpotDetailResult result = new SpotDetailResult(1L, "정동진", "관광지", null, null,
                null, null, null, null, null, 0, 0, 0, List.of(), null, true);
        when(spotDetailApplicationService.get(1L, 10L)).thenReturn(result);

        ResponseEntity<SpotDetailResponse> response = controller.get(1L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLiked()).isTrue();
        verify(spotDetailApplicationService).get(1L, 10L);
    }

    @Test
    void 존재하지_않는_스팟이면_예외가_그대로_전파된다() {
        when(spotDetailApplicationService.get(999L, null))
                .thenThrow(new CoreException(ErrorType.NOT_FOUND, "spot not found. spotId=999"));

        assertThatThrownBy(() -> controller.get(999L, null)).isInstanceOf(CoreException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew compileTestJava --console=plain`
Expected: FAIL — `controller.get(Long, AuthenticatedUser)`, `SpotDetailResponse.isLiked()`가 없어 컴파일 에러

- [ ] **Step 3: Write minimal implementation**

`SpotDetailResponse.java`를 전체 교체:

```java
package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotDetailResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * [interfaces] 공개 스팟 상세 조회 응답 DTO.
 * images/info는 TourAPI로 수집된 스팟일 때만 채워지고, 그 외에는 각각 빈 리스트/null이다.
 * isLiked는 요청한 사람(비로그인이면 false) 기준이다.
 */
public record SpotDetailResponse(
        Long spotId,
        String title,
        String category,
        String region,
        String sigungu,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnail,
        String description,
        long viewCount,
        long likeCount,
        long commentCount,
        List<String> images,
        TourInfo info,
        boolean isLiked
) {

    public static SpotDetailResponse from(SpotDetailResult result) {
        return new SpotDetailResponse(
                result.spotId(),
                result.title(),
                result.category(),
                result.region(),
                result.sigungu(),
                result.address(),
                result.latitude(),
                result.longitude(),
                result.thumbnail(),
                result.description(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.images(),
                result.info() == null ? null : TourInfo.from(result.info()),
                result.isLiked()
        );
    }

    /** detailIntro2 결과. 값이 없는 필드는 null로 내려간다(예: firstMenu/treatMenu/lcnsno는 음식점만). */
    public record TourInfo(
            String tel,
            String parkInfo,
            String timeInfo,
            String restInfo,
            String firstMenu,
            String treatMenu,
            String lcnsno
    ) {

        public static TourInfo from(SpotDetailResult.TourInfo info) {
            return new TourInfo(
                    info.tel(),
                    info.parkInfo(),
                    info.timeInfo(),
                    info.restInfo(),
                    info.firstMenu(),
                    info.treatMenu(),
                    info.lcnsno()
            );
        }
    }
}
```

`SpotController.java`를 전체 교체:

```java
package taedonghee.plan_fix.interfaces.api.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.spot.SpotDetailApplicationService;
import taedonghee.plan_fix.application.spot.SpotListApplicationService;
import taedonghee.plan_fix.application.spot.SpotListQuery;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

/**
 * [interfaces] 공개 스팟 조회 API. 컨트롤러는 HTTP 변환만 담당하고, 처리는 application에 위임한다.
 */
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotListApplicationService spotListApplicationService;
    private final SpotDetailApplicationService spotDetailApplicationService;

    /** 예: GET /api/v1/spots?category=관광지&region=51&sigungu=150&sort=popular&offset=0&size=20 */
    @GetMapping
    public ResponseEntity<SpotResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int size
    ) {
        SpotListQuery query = new SpotListQuery(category, region, sigungu, sort, offset, size);
        return ResponseEntity.ok(SpotResponse.from(spotListApplicationService.list(query)));
    }

    /**
     * 예: GET /api/v1/spots/1. 존재하지 않거나 HIDDEN이면 GlobalExceptionHandler가 404로 변환한다.
     * 이 경로는 permitAll이지만 JwtAuthenticationFilter는 모든 요청에서 실행되므로, 유효한
     * access_token 쿠키가 있으면 principal이 채워져 본인의 좋아요 여부(isLiked)를 알 수 있다.
     * 비로그인이면 principal이 null이라 viewerUserId도 null이 되고, isLiked는 항상 false다.
     */
    @GetMapping("/{spotId}")
    public ResponseEntity<SpotDetailResponse> get(
            @PathVariable Long spotId,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        Long viewerUserId = principal == null ? null : principal.id();
        return ResponseEntity.ok(SpotDetailResponse.from(spotDetailApplicationService.get(spotId, viewerUserId)));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --tests "taedonghee.plan_fix.interfaces.api.spot.SpotControllerTest" --console=plain`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

사용자가 명시적으로 요청하기 전에는 커밋하지 않는다.

---

### Task 8: 전체 검증

**Files:** 없음(검증만)

- [ ] **Step 1: 전체 백엔드 테스트 실행**

Run: `cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew test --console=plain`
Expected: BUILD SUCCESSFUL, 전부 통과 (기존 78개 + 이번에 추가한 테스트 — Task 1(3) + Task 2(4) + Task 3(3) + Task 4(7) + Task 5(신규 3, 기존 7개 수정) + Task 6(2) + Task 7(신규 2, 기존 4개 수정) = 총 94개 전후)

- [ ] **Step 2: SecurityConfig가 실제로 좋아요 경로를 인증 요구하는지 확인 — 백엔드 기동 후 curl**

```bash
lsof -ti:8080 | xargs -r kill -9
cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew bootRun &
# 기동 대기 후:
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/v1/spots/1/like   # 인증 없음 → 401 또는 403 기대
```

Expected: 인증 없이 호출하면 401/403 (기존 앱 전역 정책과 동일)

- [ ] **Step 3: 이미 있는 실 스팟과 실 사용자로 좋아요 흐름을 curl로 확인 (새 가짜 데이터는 만들지 않는다)**

이미 로그인된 세션(쿠키)이 있다면 그걸로, 없다면 자체 로그인 후 `access_token` 쿠키로 확인한다.
로컬 DB에서 이미 존재하는 spotId를 하나 골라(`SELECT spot_id FROM spots WHERE status='ACTIVE' LIMIT 1`)
좋아요 → 상세 조회(`isLiked: true`, `likeCount` 증가) → 취소 → 상세 조회(`isLiked: false`, `likeCount` 원복) 순서로 확인한다.

- [ ] **Step 4: 사용자에게 결과 보고, 커밋 여부 확인**

사용자가 명시적으로 커밋을 요청하기 전에는 커밋하지 않는다.
