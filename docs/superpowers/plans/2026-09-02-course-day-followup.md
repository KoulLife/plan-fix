# 직접 코스 생성 — 후속 처리 (이어서 작업할 것)

작성일: 2026-09-02
관련 문서: [설계](../specs/2026-09-02-manual-course-builder-design.md) · [최초 구현 계획](2026-09-02-manual-course-builder.md)

기능 구현은 커밋했지만 **검증 과정에서 발견한 문제 4건이 미해결 상태**다. 이 문서는 그
문제들과 남은 작업을 담는다. 이어서 작업할 때 이 문서부터 읽으면 된다.

---

## 지금 상태

| 항목 | 상태 |
|---|---|
| 브랜치 | `feature/course-day` (`main`에서 분기) |
| 커밋 | `b715977 feat: 직접 코스 생성(Day별 여행 일정) 구현` — 62개 파일 |
| 푸시 | **안 함** |
| DB 마이그레이션 | ✅ **적용 완료** (아래 참조) |
| 프론트 테스트 | ✅ 17 suites / 160 tests 통과 |
| 프론트 타입체크 | ✅ 통과 |
| 백엔드 테스트 | ❌ 138개 중 **7개 실패** (이슈 #3) |
| 메인/인기 장소 페이지 | ❌ **깨져 있음** (이슈 #1) |

### DB 마이그레이션은 이미 끝났다 — 다시 돌리지 않아도 된다

`docs/migrations/2026-09-02-add-course-days.sql`을 대상 DB(`34.22.77.44:5432/planfix`)에
적용 완료했다. 적용 후 확인한 `course_spots` 상태:

```
[constraint] course_spots_pkey                    -> PRIMARY KEY (course_spot_id)
[constraint] uk_course_spots_course_day_sequence  -> UNIQUE (course_id, day_number, sequence)
```

- `uk_course_spots_course_sequence (course_id, sequence)` — **제거됨** ✅
- `uk_course_spots_course_spot (course_id, spot_id)` — **제거됨** ✅
- 컬럼 `course_spots.day_number`, `courses.start_date`, `courses.end_date` — 존재 ✅
- `course_spots` 행 수 0 (데이터 영향 없음)

> **교훈:** `ddl-auto: update`는 컬럼 추가는 해줬지만 **제약 삭제는 하나도 하지 않았다.**
> Hibernate의 schema update는 추가 전용이다. 앞으로도 제약·인덱스 변경이 있으면
> 마이그레이션 SQL을 직접 실행해야 한다.

### 실행 환경에 관한 중요한 사실

**로컬 기동도 `local`이 아니라 기본값인 `prod` 프로필로 돌아간다.**

- `application.yaml:11` — `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:prod}`
- `SPRING_PROFILES_ACTIVE` 환경변수가 설정되어 있지 않음
- `application-secret.yml`이 `DB_HOST`/`DB_PORT`/`DB_DATABASE`/`DB_USERNAME`/`DB_PASSWORD`/
  `JWT_SECRET`/`KAKAO_*`/`TOUR_API_SERVICE_KEY`를 모두 공급 → prod 프로필이 요구하는 값이 다 채워짐
- 로컬 `localhost:5432`는 **닫혀 있다.** 즉 `application-local.yml`의 datasource는 쓰이지 않는다

**따라서 `application-local.yml`에 무엇을 써도 지금 실행에는 반영되지 않는다.**
설정을 추가할 일이 있으면 `application.yaml`(공통)이나 `application-prod.yml`에 넣어야 한다.
(그래서 swagger `with-credentials`도 `application.yaml`에 넣었다.)

그리고 **개발 중 붙는 DB가 곧 prod 프로필이 붙는 원격 DB**다. 스키마 변경 시 주의할 것.

---

## 이슈 목록

### 🔴 #1 — `GET /api/v1/spots`가 keyword 없이 호출되면 500

**증상.** 메인 페이지와 인기 장소 페이지가 지금 깨져 있다.

실측 결과:

| 요청 | 결과 |
|---|---|
| `/api/v1/spots?size=1` | **500** |
| `/api/v1/spots?size=1&keyword=강릉` | 200 |
| `/api/v1/spots?size=1&sort=popular` | **500** |
| `/api/v1/spots?size=1&sort=popular&keyword=강릉` | 200 |

**원인.** `SpotJpaRepository`의 세 쿼리(`searchActiveByLatest` `:24`,
`searchActiveByPopular` `:47`, `countActive` `:67`)에 들어간 조건:

```
AND (:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
```

`:category`/`:region`/`:sigungu`는 `s.category = :category`처럼 **타입이 있는 컬럼과 직접
비교**하므로 Postgres가 varchar로 추론한다. 반면 `:keyword`는 `IS NULL`과 `CONCAT()`
안에서만 쓰여 **어디서도 타입을 추론할 수 없고**, Postgres가
`could not determine data type of parameter`로 거절한다.

**수정 방향.** 둘 중 하나.

1. **명시적 캐스팅** — 세 쿼리 모두, `:keyword`가 등장하는 **두 곳 다** 캐스팅한다.
   Hibernate가 named 파라미터를 등장 횟수만큼 JDBC 플레이스홀더로 펼치므로 한쪽만
   캐스팅하면 다른 쪽이 그대로 타입 미추론으로 남는다.

   ```
   AND (CAST(:keyword AS String) IS NULL
        OR LOWER(s.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
   ```

2. **LIKE 패턴을 애플리케이션에서 만들기** — `SpotRepositoryImpl`에서
   `keyword == null ? null : "%" + keyword.toLowerCase() + "%"`로 변환해 넘기고,
   JPQL은 `LOWER(s.title) LIKE :keywordPattern` 형태로 단순화한다.
   이쪽이 쿼리는 깔끔해지지만 `IS NULL` 분기는 여전히 캐스팅이 필요할 수 있다.

**검증.** 위 표의 4가지 요청이 전부 200이 되는지 확인한다. 그리고
`SpotRepositoryImplTest`에 **keyword가 null일 때 전체가 조회된다**는 케이스를 반드시
추가한다 — 지금 테스트에 이 케이스가 없어서 놓쳤다.

---

### 🟠 #2 — 코스 저장 성공 후 "실패"로 표시되고 재시도 시 코스가 중복 생성됨

**위치.** `frontend/src/pages/course-create-page.tsx:270`

```ts
const result = await createCourse(payload);
sessionStorage.removeItem(DRAFT_STORAGE_KEY);   // ← try 블록 안, 무방비
navigate(`/courses/${result.courseId}`, { replace: true });
```

**문제.** draft **읽기**(`:83-95`)와 **쓰기**(`:109-111`)는 try/catch로 감쌌는데
`removeItem`만 빠져 있다. 프라이빗 모드나 사이트 데이터 차단 환경에서 `sessionStorage`
접근이 throw하면:

1. 서버에는 코스가 **이미 생성된 상태**
2. catch가 `"코스 저장에 실패했습니다"`를 띄움
3. `navigate`는 실행되지 않음
4. 사용자가 다시 저장 → **같은 코스가 하나 더 생성됨**

**수정 방향.** `removeItem`을 자체 try/catch로 감싸거나 `navigate` 뒤로 옮긴다.

**검증.** `course-create-page.test.tsx`에 `sessionStorage.removeItem`이 throw하도록
mock한 상태에서도 저장이 성공 처리되고 상세 페이지로 이동하는지 확인한다.

---

### 🟠 #3 — `@SpringBootTest` 7건이 JUnit 버전 충돌로 전부 실패

**증상.** `cd backend && ./gradlew test` 실행 시:

```
PlanFixApplicationTests, TourDataImagePersisterTest, TourDataSpotCollectApplicationServiceTest,
BoardRepositoryImplTest, SpotLikeRepositoryImplTest, SpotRepositoryImplTest,
TourDataSpotRepositoryImplTest
→ 전부 initializationError
   java.lang.NoSuchMethodError: 'java.lang.Object
   org.junit.jupiter.api.extension.ExtensionContext$Store.computeIfAbsent(
       java.lang.Object, java.util.function.Function, java.lang.Class)'
   at SpringExtension.java:466
```

**원인.** 이 기능과 무관한 선행 문제다. `build.gradle`에 langchain4j/langgraph4j BOM을
import하면서 JUnit 버전이 밀렸다.

- `spring-test:7.0.8` (Spring Boot 4)은 **JUnit Jupiter 6.x** API를 기대
- 실제 classpath에는 **`junit-jupiter-api:5.11.3`** (`junit-platform-commons:1.11.3`)
- `SpringExtension`이 호출하는 `Store.computeIfAbsent(Object, Function, Class)`는
  JUnit 6 시그니처라 5.11.3에 없음 → `NoSuchMethodError`

`dependencyManagement.imports`에 넣은 langchain4j/langgraph4j BOM이 Spring Boot의
junit-bom을 덮어쓴 결과로 보인다.

**수정 방향.** `build.gradle`에서 JUnit 버전을 고정한다. 이미 logback·jackson에
`resolutionStrategy.eachDependency`를 쓰고 있으니 같은 방식이 자연스럽다.

```gradle
resolutionStrategy.eachDependency { details ->
    if (details.requested.group.startsWith('org.junit')) {
        // Spring Boot 4의 junit-bom 버전으로 강제
    }
}
```

또는 `dependencyManagement.imports`에 junit-bom을 **마지막에** 명시해 우선순위를 되돌린다.

**영향.** 이게 막혀 있는 동안 통합 테스트 계층 전체가 dark다 — 마이그레이션 적용 여부를
자동으로 잡아낼 방법이 없다. **#4보다 먼저 처리해야 한다.**

---

### 🟡 #4 — `CourseRepositoryImplTest` 미작성

최초 계획의 Task 6에서 요구한 테스트가 작성되지 않았다. 커버되지 않은 것:

- [ ] Day 2개에 장소를 담아 저장 후 복원하면 Day와 순서가 그대로다
- [ ] **같은 spotId를 Day1과 Day2에 담아도 저장된다** (마이그레이션 적용 여부 검증 겸용)
- [ ] 중간 Day가 비어 있는 코스를 저장/복원해도 `dayNumber`가 1..N으로 연속이다
- [ ] `update` 저장 시 기존 `course_spots`가 정리되고 새 구성으로 대체된다

세 번째 항목이 특히 중요하다. 장소가 없는 Day는 `course_spots`에 행이 없어서, 복원 시
`courses.start_date/end_date`로 기간을 계산해 빈 Day를 채워 넣는 로직
(`CourseRepositoryImpl.java:114-125`)이 있어야 `dayNumber` 연속성 검증을 통과한다.
이 로직은 구현되어 있지만 검증하는 테스트가 없다.

**#3을 먼저 고쳐야 이 테스트를 실행할 수 있다.**

---

### 🟢 #5 — `delete()` 응답의 spot 요약이 전부 null

**위치.** `backend/.../application/course/CourseApplicationService.java:107`

```java
return CourseResult.from(deleted, Map.of());
```

빈 맵을 넘겨서 삭제 응답의 각 spot에 `title`/`category`/`thumbnail` 등이 전부 null로
내려간다. `CourseResult.from`이 null 가드를 하고 있어 터지지는 않지만
(`CourseResult.java:45-58`), 다른 엔드포인트와 응답 계약이 어긋난다.

프론트가 삭제 응답 body를 쓰지 않으므로 실사용 영향은 없다. 정리할 때 같이 처리한다.

---

### 🟢 #6 — 커밋 분리

`b715977`에 서로 무관한 작업이 함께 들어갔다.

| 묶음 | 파일 |
|---|---|
| 코스 Day 기능 (본 작업) | course/spot 백엔드, 프론트 코스 화면 3종, 검색 모달, 마이그레이션, 설계 문서 |
| **AI 실험** | `application/ai/`, `infrastructure/ai/`, `interfaces/api/ai/`, `build.gradle`(langchain4j·langgraph4j) |
| **날씨** | `services/weather.ts`, `weather.test.ts` |
| 메인 화면 개편 | `main-page.tsx`, `app-nav.tsx` |

PR을 올리기 전에 최소한 AI와 날씨는 분리하는 게 좋다.

**부수 사항:** `frontend/yarn.lock`이 새로 커밋됐는데 `frontend/package-lock.json`도
함께 존재한다. 패키지 매니저를 하나로 정하고 나머지 락파일을 지워야 한다.

---

## 기타 메모

### spots 테이블이 비어 있다

`GET /api/v1/spots`의 `totalCount`가 0이다. 코스에 담을 장소가 없으므로 코스 생성을
실제로 테스트하려면 관광데이터 수집을 먼저 돌려야 한다. **순서를 지켜야 한다:**

| 순서 | 엔드포인트 | 비고 |
|---|---|---|
| 1 | `POST /api/v1/admin/spots/collect` | 스팟 본체 수집 (`contentId` 확보) |
| 2 | `POST /api/v1/admin/spots/collect-images` | 1 이후에만 |
| 3 | `POST /api/v1/admin/spots/collect-info` | 1 이후에만 |

전부 `lDongRegnCd`(시도) / `lDongSignguCd`(시군구)를 받는다. 예: `51`/`150` = 강원 강릉.
외부 TourAPI를 수천 번 호출하는 장기 작업이라 타임아웃이 30분으로 잡혀 있다.

### admin API 403 문제는 해결됐다 (재시작 필요)

`/api/v1/admin/**`은 `SecurityConfig.java:52`에서 `hasRole("ADMIN")`을 요구한다.
계정 `admin123`의 DB role은 ADMIN이 맞고, 쿠키를 실어 보내면 인가가 정상 통과하는 것을
실측했다(인가 통과 시 404, 미통과 시 403).

문제는 **Swagger UI가 요청에 인증 쿠키를 안 실어 보낸 것**이었다. `application.yaml`에
아래를 추가해 뒀으므로 **백엔드를 재시작하면 해결된다.**

```yaml
springdoc:
  swagger-ui:
    with-credentials: true
```

재시작이 여의치 않으면 Swagger 우측 상단 **Authorize** 버튼에 토큰을 직접 넣어도 된다.
`JwtAuthenticationFilter`가 쿠키 다음으로 `Authorization: Bearer` 헤더를 보기 때문이다
(`:74-80`, `:101-107`). 토큰은 로그인 응답 body에 없고 httpOnly 쿠키로만 오므로,
DevTools → Application → Cookies → `http://localhost:8080` → `access_token`에서 복사한다.

---

## 권장 작업 순서

1. **#1** `GET /spots` 500 수정 — 메인 페이지가 깨져 있으므로 최우선
2. **#2** sessionStorage 중복 생성 수정 — 데이터가 잘못 쌓이는 문제
3. **#3** JUnit 버전 충돌 해소 — 통합 테스트를 되살려야 #4가 가능
4. **#4** `CourseRepositoryImplTest` 작성
5. **#5**, **#6** 정리
6. 수동 검증 한 바퀴: 로그인 → 여행 → 직접 코스 생성 → 날짜 3일 → Day1에 2곳, Day2에 1곳
   (**Day1과 같은 장소 하나 포함**), Day3은 비움 → 저장 → 상세에서 Day별 확인 → 목록 확인
