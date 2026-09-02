# 직접 코스 생성(Day별 여행 일정) Implementation Plan

**Spec:** `docs/superpowers/specs/2026-09-02-manual-course-builder-design.md` — 먼저 정독할 것.
설계 근거·응답 JSON 형태·검증 규칙은 전부 스펙에 있고, 이 문서는 **작업 순서와 완료 기준**만 담는다.

**Goal:** 헤더 "여행" → "직접 코스 생성"을 눌렀을 때, 여행 날짜를 정하고 Day별로 장소를 검색해
담아 코스를 저장하고, 저장한 코스를 상세·목록에서 다시 볼 수 있게 한다.

**Tech Stack:** Spring Boot / Spring Data JPA / Spring Security / Postgres 17 / JUnit 5 + AssertJ
· React 19 + TypeScript + react-router-dom 6 + Tailwind / Testing Library + Jest(craco)

## Global Constraints

- **TDD로 간다.** 각 Task는 실패하는 테스트 → 최소 구현 → 통과 순서다.
- **커밋은 사용자가 명시적으로 요청하기 전에는 하지 않는다.**
- 기존 코드의 주석 스타일(한글, `[domain]`/`[application]` 같은 레이어 태그, "왜"를 적는 주석)을 따른다.
- 새 파일은 기존 패키지 구조를 그대로 따른다: `domain` → `infrastructure` → `application` → `interfaces`.
  `domain`은 프레임워크에 의존하지 않는다.
- 프론트는 `@/` 별칭 import, 함수형 컴포넌트 + `export default`, Tailwind 유틸리티 클래스,
  한글 UI 문구·주석이라는 기존 관례를 따른다.
- **Task 1(마이그레이션)을 먼저 실행하지 않으면 Task 3 이후 테스트가 실패한다.**
  `ddl-auto: update`는 컬럼을 추가할 뿐 제약을 삭제하지 않는다.
- 백엔드 테스트 실행: `cd backend && ./gradlew test --tests "<FQCN>" --console=plain`
  (PowerShell이면 `cd backend; .\gradlew.bat test --tests "<FQCN>" --console=plain`)
- 프론트 테스트 실행: `cd frontend && yarn test --watchAll=false <경로>` / 타입 검사 `yarn typecheck`

---

## Phase 1 — 스키마와 도메인

### Task 1: 마이그레이션 작성 및 실행

**Files:** Create `docs/migrations/2026-09-02-add-course-days.sql`

- [ ] 스펙의 "데이터 모델 > 마이그레이션" 절 SQL을 그대로 파일로 만든다.
- [ ] **로컬 DB에 실행한다.** 예: `docker exec -i docker-postgres-1 psql -U planfix -d planfix < docs/migrations/2026-09-02-add-course-days.sql`
- [ ] 실행 후 확인: `\d course_spots`에 `day_number`가 있고, `uk_course_spots_course_spot`이
      **사라졌고**, `uk_course_spots_course_day_sequence`가 생겼는지.

**완료 기준:** 같은 `course_id`에 같은 `spot_id`를 `day_number`만 다르게 두 번 INSERT 해도 성공한다.

### Task 2: `CourseDayModel` 신규

**Files:**
- Create `backend/src/main/java/taedonghee/plan_fix/domain/course/CourseDayModel.java`
- Test `backend/src/test/java/taedonghee/plan_fix/domain/course/CourseDayModelTest.java`

**계약:** `record CourseDayModel(int dayNumber, List<CourseSpotModel> spots)`

**테스트로 고정할 규칙** (스펙 "도메인 모델 > CourseDayModel"):
- [ ] `dayNumber < 1`이면 `CoreException(BAD_REQUEST)`
- [ ] `spots == null`이면 예외, **빈 리스트는 정상**
- [ ] 원소에 null이 있으면 예외
- [ ] 같은 Day 안에 같은 `spotId`가 두 번이면 예외
- [ ] 31개 이상이면 예외
- [ ] `spots`가 불변이다 (`List.copyOf`) — 반환된 리스트에 `add` 하면 `UnsupportedOperationException`

### Task 3: `CourseModel`을 Day 구조로 전환

**Files:**
- Modify `domain/course/CourseModel.java`
- Modify `backend/src/test/java/taedonghee/plan_fix/domain/course/CourseModelTest.java`

**계약 변경:**
- 필드 `List<CourseSpotModel> spots` → `List<CourseDayModel> days`
- 필드 `LocalDate startDate`, `LocalDate endDate` 추가
- `create(userId, title, description, thumbnail, visibility, startDate, endDate, days)`
- `reconstruct(..., startDate, endDate, days, createdAt, updatedAt)`
- `update(title, description, thumbnail, visibility, startDate, endDate, days)`
- 게터 `days()`, `startDate()`, `endDate()` — `spots()`는 삭제

**테스트로 고정할 규칙** (스펙 "도메인 모델 > CourseModel 변경"의 1~5번):
- [ ] `days`가 null/빈 리스트면 예외, 31개 이상이면 예외
- [ ] `dayNumber`가 `index + 1`과 어긋나면 예외 (예: `[Day2, Day3]`, `[Day1, Day3]`)
- [ ] 모든 Day의 장소 합이 0개면 예외
- [ ] **일부 Day가 비어 있어도 다른 Day에 장소가 있으면 정상**
- [ ] `startDate`만 있거나 `endDate`만 있으면 예외
- [ ] `endDate < startDate`면 예외
- [ ] 기간 일수(`DAYS.between + 1`)와 `days.size()`가 다르면 예외
- [ ] 날짜가 둘 다 null이면 `days.size()`와 무관하게 정상 (기존 데이터 복원 경로)
- [ ] **서로 다른 Day에 같은 `spotId`가 있어도 정상** — 기존의 "코스 전체 중복 금지" 테스트는 삭제한다

**주의:** 이 Task를 끝내면 `CourseResult`, `CourseCommand`, `CourseRepositoryImpl`,
`CourseApplicationService`가 컴파일되지 않는다. Task 4~7에서 순서대로 따라간다.
중간 상태에서 `./gradlew test`를 돌리면 컴파일 에러가 나는 게 정상이다.

---

## Phase 2 — 저장소

### Task 4: JPA 엔티티에 컬럼·제약 반영

**Files:**
- Modify `infrastructure/course/CourseJpaEntity.java` — `LocalDate startDate`, `LocalDate endDate`
  (`@Column(name = "start_date")`, `columnDefinition` 불필요)
- Modify `infrastructure/course/CourseSpotJpaEntity.java` — `int dayNumber`,
  `@Table`의 `uniqueConstraints`를 `{"course_id","day_number","sequence"}` **하나만** 남기고
  `uk_course_spots_course_spot`은 **삭제**, 인덱스 선언은 그대로

**완료 기준:** 애플리케이션이 뜨고, `ddl-auto: update`가 Task 1에서 지운 제약을 다시 만들지 않는다.

### Task 5: `SpotRepository.findAllByIdIn` 추가

**Files:**
- Modify `domain/spot/SpotRepository.java` — `List<SpotModel> findAllByIdIn(Collection<Long> spotIds)`
- Modify `infrastructure/spot/SpotJpaRepository.java` — `List<SpotJpaEntity> findAllBySpotIdIn(Collection<Long>)`
- Modify `infrastructure/spot/SpotRepositoryImpl.java` — 위임 + `toDomain` 매핑
- Modify `backend/src/test/java/taedonghee/plan_fix/infrastructure/spot/SpotRepositoryImplTest.java`

**테스트:**
- [ ] 여러 id를 넘기면 해당 spot들이 모두 조회된다
- [ ] 존재하지 않는 id가 섞여 있으면 그것만 빠지고 나머지는 조회된다
- [ ] 빈 컬렉션을 넘기면 빈 리스트를 반환한다(쿼리를 날리지 않아도 된다)
- [ ] HIDDEN 상태 spot도 조회된다 — **상태 필터링은 호출부(application)의 책임**이다

**주의:** 기존 테스트에 `SpotRepository` 페이크 구현이 여러 개 있다
(`SpotDetailApplicationServiceTest`, `SpotLikeApplicationServiceTest` 등).
인터페이스에 메서드가 추가되면 전부 컴파일 에러가 나므로 각 페이크에도 구현을 추가한다
(쓰지 않는 곳은 `throw new UnsupportedOperationException()`이 기존 관례다).

### Task 6: `CourseRepositoryImpl` — Day별 저장/복원

**Files:**
- Modify `infrastructure/course/CourseRepositoryImpl.java`
- Modify `infrastructure/course/CourseSpotJpaRepository.java` (정렬 조회)
- Test `backend/src/test/java/taedonghee/plan_fix/infrastructure/course/CourseRepositoryImplTest.java`
  (없으면 신규 — `@SpringBootTest` + `@Transactional`로 로컬 DB에 쓰고 롤백)

**저장:** Day를 순회하며 각 Day의 `spots` 리스트 index를 `sequence`(0부터)로 부여한다.
지금 `CourseRepositoryImpl:34`의 `IntStream.range(0, course.spots().size())` 패턴을
Day 안쪽 루프로 옮기는 것이다.

**복원:** `course_spots`를 `(day_number ASC, sequence ASC)`로 읽어 `day_number`로 그룹핑해
`CourseDayModel` 리스트를 만든다. **장소가 하나도 없는 Day는 course_spots에 행이 없으므로
복원 결과에서 빠진다** — `courses.start_date/end_date`로 기간을 알 수 있으면 그 일수만큼
빈 Day를 채워 넣고, 날짜가 없으면 실제 존재하는 최대 `day_number`까지만 채운다.
이 처리를 빠뜨리면 "Day 3이 비어 있는 코스"를 저장했다가 불러올 때 `dayNumber` 연속성
검증에 걸려 터진다.

**테스트:**
- [ ] Day 2개에 장소를 담아 저장 후 복원하면 Day와 순서가 그대로다
- [ ] **같은 spotId를 Day1과 Day2에 담아도 저장된다** (Task 1 마이그레이션 검증도 겸한다)
- [ ] 중간 Day가 비어 있는 코스를 저장/복원해도 `dayNumber`가 1..N으로 연속이다
- [ ] `update` 저장 시 기존 `course_spots`가 정리되고 새 구성으로 대체된다

---

## Phase 3 — Application / API

### Task 7: `CourseCommand` / `CourseResult` 구조 변경

**Files:**
- Modify `application/course/CourseCommand.java` — `Create`/`Update`에 `LocalDate startDate`,
  `LocalDate endDate`, `List<CourseDayModel> days` (`spots` 제거)
- Modify `application/course/CourseResult.java` — 스펙의 응답 JSON과 1:1로 맞춘다:
  - 최상위에 `startDate`, `endDate`, `List<Day> days`
  - `record Day(int dayNumber, List<Spot> spots)`
  - `record Spot(Long spotId, int sequence, String memo, String title, String category,
    String region, String sigungu, String address, String thumbnail,
    BigDecimal latitude, BigDecimal longitude)`
  - `from(CourseModel course, Map<Long, SpotModel> spotsById)`로 시그니처 변경

### Task 8: `CourseApplicationService` — N+1 제거 + 요약 조립

**Files:**
- Modify `application/course/CourseApplicationService.java`
- Modify `backend/src/test/java/taedonghee/plan_fix/application/course/CourseApplicationServiceTest.java`

**구현 방향** (스펙 "API > N+1 제거"):
1. `days`에서 `spotId`를 모아 중복 제거
2. `spotRepository.findAllByIdIn(ids)` 한 번 호출
3. 조회되지 않았거나 `status != ACTIVE`인 id가 있으면 `CoreException(NOT_FOUND, "spot not found. spotId=...")`
4. `Map<Long, SpotModel>`을 `CourseResult.from`에 넘겨 요약을 채운다
5. 기존 `validateSpots`의 spot별 `findById` 루프는 삭제한다
6. `listMine`/`getMine`도 같은 방식으로 조회 후 변환한다 (`listMine`은 **모든 코스의 spotId를
   한 번에 모아** 한 번만 조회한다 — 코스마다 조회하면 N+1이 그대로다)

**테스트:**
- [ ] 존재하지 않는 spot이 섞이면 404
- [ ] HIDDEN spot이 섞이면 404
- [ ] 정상 생성 시 응답의 각 spot에 `title`/`category`/`thumbnail`이 채워진다
- [ ] 다른 사용자의 코스를 조회/수정/삭제하면 403
- [ ] `listMine`이 코스 여러 개를 돌려줄 때 `findAllByIdIn` 호출이 1회다(페이크에서 호출 횟수 카운트)

### Task 9: `CourseRequest` / `CourseResponse` DTO

**Files:**
- Modify `interfaces/api/course/CourseRequest.java` — `record Day(int dayNumber, List<Spot> spots)`
  추가, `toCommand()`가 `CourseDayModel` 리스트로 변환
- Modify `interfaces/api/course/CourseResponse.java` — 스펙의 응답 JSON 그대로
- Modify `backend/src/test/java/taedonghee/plan_fix/interfaces/api/course/CourseControllerTest.java`

**테스트:**
- [ ] 스펙의 요청 JSON을 그대로 POST하면 201과 `days` 구조 응답이 온다
- [ ] `days`가 없거나 비면 400
- [ ] 인증 없이 호출하면 401

### Task 10: 스팟 keyword 검색

**Files:**
- Modify `domain/spot/SpotSearchCondition.java` — `record SpotSearchCondition(String keyword, String category, String region, String sigungu)`
- Modify `infrastructure/spot/SpotJpaRepository.java` — `searchActiveByLatest`,
  `searchActiveByPopular`, `countActive` **세 쿼리 모두**에
  `AND (:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))` 추가
- Modify `infrastructure/spot/SpotRepositoryImpl.java` — `condition.keyword()` 전달
- Modify `application/spot/SpotListQuery.java` — `keyword` 추가 + **공백/빈 문자열은 null로 정규화**
- Modify `application/spot/SpotListApplicationService.java`
- Modify `interfaces/api/spot/SpotController.java` — `@RequestParam(required = false) String keyword`
- Modify 관련 테스트 (`SpotRepositoryImplTest`, `SpotListApplicationServiceTest`, `SpotControllerTest`)

**주의:** `SpotSearchCondition`은 record라 생성자 인자 순서가 바뀐다. 이 타입을 만드는 모든
호출부(테스트 포함)를 찾아 고친다.

**테스트:**
- [ ] `keyword=속초`로 제목에 "속초"가 든 spot만 조회된다
- [ ] 대소문자를 무시한다
- [ ] `keyword`가 null이면 전체 조회다
- [ ] `keyword=""` / `keyword="   "`는 null과 동일하게 동작한다
- [ ] `keyword` + `category` 조합이 AND로 걸린다
- [ ] `countActive`도 같은 조건으로 세어 `totalCount`가 맞다

**Phase 3 완료 시 전체 백엔드 테스트가 통과해야 한다:** `cd backend && ./gradlew test --console=plain`

---

## Phase 4 — 프론트엔드

### Task 11: 서비스 레이어

**Files:**
- Create `frontend/src/services/course.ts`
- Modify `frontend/src/services/spots.ts` — `searchSpots(params)` 추가
- Test `frontend/src/services/course.test.ts`

**계약:** 스펙 "프론트엔드 > 서비스 레이어" 표 참조.

- [ ] 코스 API 요청에 **전부** `credentials: "include"`가 붙는다
- [ ] 401/403이면 `spots.ts`가 export하는 `UnauthorizedError`를 던진다
- [ ] `fetchCourse`는 404에 `null`을 반환한다
- [ ] `REACT_APP_API_BASE_URL`이 없으면 기존 `spots.ts`와 같은 방식으로 조용히 처리한다
      (`fetchMyCourses`는 빈 배열, `fetchCourse`는 null, `createCourse`는 `UnauthorizedError`)
- [ ] `searchSpots`는 `keyword`가 비면 쿼리에 넣지 않는다

### Task 12: 장소 검색 모달

**Files:**
- Create `frontend/src/components/ui/spot-search-modal.tsx`
- Test `frontend/src/components/ui/spot-search-modal.test.tsx`

**Props 계약:**

```ts
interface SpotSearchModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (spot: PopularSpot) => void;
  /** 이미 이 Day에 담긴 spotId. 중복 선택을 막는다 */
  excludedSpotIds: number[];
  /** 헤더에 "Day 2에 추가"로 표시 */
  dayNumber: number;
}
```

- [ ] 키워드 입력 후 300ms debounce가 지나야 검색이 호출된다 (타이머는 fake timer로 검증)
- [ ] 키워드가 비어 있으면 인기순 상위를 보여준다
- [ ] 결과를 누르면 `onSelect`가 호출되고 모달이 닫힌다
- [ ] `excludedSpotIds`에 있는 항목은 "담김"으로 표시되고 눌러도 `onSelect`가 호출되지 않는다
- [ ] Escape·배경 클릭으로 닫힌다, 열려 있는 동안 `body` 스크롤이 잠긴다
      (`course-select-modal.tsx`의 구현을 그대로 따른다)
- [ ] 검색 실패 시 에러 메시지를 보여주고 모달이 죽지 않는다

### Task 13: 코스 생성 화면

**Files:**
- Create `frontend/src/pages/course-create-page.tsx`
- Test `frontend/src/pages/course-create-page.test.tsx`

**상태 구조는 스펙의 `DraftState`를 그대로 쓴다** (`days: DraftSpot[][]` — index가 곧 dayNumber-1).

- [ ] 시작일/종료일을 정하면 Day 카드 수가 기간 일수와 같아진다
- [ ] 기간을 줄여 장소가 담긴 Day가 사라질 때는 확인을 받는다
- [ ] "장소 추가"를 누르면 그 Day 번호로 `SpotSearchModal`이 열리고, 고른 장소가 그 Day 맨 뒤에 붙는다
- [ ] ▲▼로 같은 Day 안에서 순서가 바뀌고, 맨 위/아래에서는 해당 버튼이 비활성이다
- [ ] 셀렉트로 다른 Day로 옮기면 그 Day 맨 뒤에 붙는다
- [ ] X로 삭제된다
- [ ] 제목이 비었거나 담긴 장소가 0개면 저장 버튼이 비활성이고 이유가 보인다
- [ ] 저장 시 전송되는 payload가 스펙의 요청 JSON 형태다 —
      `days[i].dayNumber === i + 1`, 빈 Day도 `spots: []`로 포함
- [ ] 저장 성공 시 `/courses/{courseId}`로 `replace` 이동한다
- [ ] 저장 실패 시 에러를 보여주고 편집 상태가 유지된다 (입력한 내용이 날아가지 않는다)
- [ ] 상태가 바뀔 때마다 `sessionStorage`의 `planfix:course-draft`에 저장되고, 재진입 시 복원된다
- [ ] 저장 성공 후에는 `planfix:course-draft`가 지워진다
- [ ] `sessionStorage` 접근이 실패해도(try/catch) 화면이 정상 동작한다

### Task 14: 코스 상세 / 목록 화면

**Files:**
- Create `frontend/src/pages/course-detail-page.tsx` + `course-detail-page.test.tsx`
- Create `frontend/src/pages/course-list-page.tsx` + `course-list-page.test.tsx`

- [ ] 상세: Day별 섹션에 장소가 순서대로 렌더링되고, 장소를 누르면 `/spots/{spotId}`로 이동한다
- [ ] 상세: 장소가 없는 Day는 "아직 계획이 없어요"로 표시된다
- [ ] 상세: 존재하지 않는 코스(null)면 안내 문구를 보여준다
- [ ] 목록: 코스 카드에 제목·기간·장소 수가 보인다
- [ ] 목록: 비어 있으면 "코스 만들기" CTA가 보이고 `/courses/create`로 간다
- [ ] 두 화면 모두 로딩 상태와 `UnauthorizedError` → `/login` 이동을 처리한다
- [ ] 두 화면 모두 `AppNav`를 렌더링한다

### Task 15: 라우팅 연결 및 모달 수정

**Files:**
- Modify `frontend/src/App.tsx` — `/courses`, `/courses/create`, `/courses/:courseId` 추가
  (`create`를 `:courseId`보다 위에)
- Modify `frontend/src/components/ui/course-select-modal.tsx` — AI 버튼 `disabled` + "준비 중" 배지
- Modify `frontend/src/components/ui/course-select-modal.test.tsx`
- Modify `frontend/src/components/ui/app-nav.tsx` — `handleSelectAiCourse` 제거 또는 no-op 처리
- Modify `frontend/src/components/ui/app-nav.test.tsx`

- [ ] "여행" → "직접 코스 생성"을 누르면 `/courses/create`가 렌더링된다 (**로그인 페이지로 튕기지 않는다**)
- [ ] AI 버튼은 비활성이고 눌러도 아무 데도 가지 않는다

---

## 최종 검증

- [ ] `cd backend && ./gradlew test --console=plain` 전체 통과
- [ ] `cd frontend && yarn typecheck` 통과
- [ ] `cd frontend && yarn test --watchAll=false` 전체 통과
- [ ] 앱을 띄워 **수동으로 한 바퀴**: 로그인 → 여행 → 직접 코스 생성 → 날짜 3일 지정 →
      Day1에 검색해서 2곳, Day2에 1곳(Day1과 **같은 장소 하나 포함**), Day3은 비움 →
      저장 → 상세 화면에서 Day별로 그대로 보이는지 → 목록에 뜨는지
- [ ] 위 수동 검증에서 "같은 장소를 두 Day에" 넣는 단계가 실패하면 **Task 1 마이그레이션이
      운영/로컬 DB에 실행되지 않은 것**이다
