# 직접 코스 생성(Day별 여행 일정) 설계

작성일: 2026-09-02

## 배경

헤더의 "여행" 버튼을 누르면 `CourseSelectModal`이 열리고 "직접 코스 생성"을 고를 수 있다.
그런데 이 버튼은 `/courses/create`로 navigate하는데 `App.tsx`에 그 라우트가 없어서,
`<Route path="*">`에 걸려 `/login`으로 튕긴다. 즉 **현재 이 기능은 아예 동작하지 않는다.**

백엔드는 코스 CRUD가 이미 다 있다(`CourseController`의 POST/GET/PATCH/DELETE).
하지만 `course_spots`가 `(course_id, spot_id, sequence, memo)`뿐이라 트리플처럼
"2박 3일 → Day 1/Day 2/Day 3에 장소를 나눠 담는" 여행 일정을 표현할 수 없다.
지금 구조로 만들 수 있는 건 순서만 있는 "장소 묶음"이지 일정이 아니다.

## 목표

- 여행자가 여행 날짜를 정하고, Day별로 장소를 검색해 담고, 순서를 바꿔 코스를 완성한다.
- 저장한 코스를 상세 화면에서 Day별로 다시 볼 수 있고, 내 코스 목록에서 찾을 수 있다.
- 같은 장소(예: 2박 내내 같은 숙소)를 여러 Day에 담을 수 있다.

## 비목표

- **AI 코스 생성은 이번 범위 밖이다.** 모달의 "AI 코스 생성" 버튼은 "준비 중"으로 비활성화한다
  (지금은 라우트가 없는 `/courses/create?mode=ai`로 보내고 있어 로그인 페이지로 튕긴다).
- **지도는 넣지 않는다.** `spots`에 `latitude/longitude`가 이미 있으므로 나중에 얹어도
  스키마·API가 바뀌지 않는다. 이번엔 Day별 카드 리스트만 만든다.
- **자동저장(DRAFT)은 하지 않는다.** 편집은 브라우저 상태에서만 일어나고, "저장"을 누를 때
  `POST /api/v1/courses` 한 번으로 전체를 보낸다. `CourseStatus`는 ACTIVE/DELETED 그대로 둔다.
- 코스 좋아요(`course_likes` 테이블은 이미 있음), 코스 공개 목록/공유, 코스 수정 화면,
  이동 시간·경로 계산, 장소별 방문 시각은 이번 범위 밖이다.
- 드래그 앤 드롭 정렬은 하지 않는다(프론트에 dnd 라이브러리가 없다). 위/아래 버튼으로 정렬한다.

## 데이터 모델

### 스키마 변경

`courses`에 여행 기간을, `course_spots`에 일차를 추가한다. **Day 전용 테이블(`course_days`)은
만들지 않는다** — Day에 붙는 속성이 `day_number`뿐이라 테이블 하나가 순수 오버헤드다.
Day에 숙소·이동수단·예산 같은 고유 속성이 생기면 그때 분리한다.

| 테이블 | 컬럼 | 타입 | NULL | 비고 |
|---|---|---|---|---|
| `courses` | `start_date` | date | NULL 허용 | 여행 시작일 |
| `courses` | `end_date` | date | NULL 허용 | 여행 종료일 |
| `course_spots` | `day_number` | int | NOT NULL | 1부터 시작하는 일차 |

`start_date`/`end_date`를 NULL 허용으로 두는 이유는 **이미 저장된 코스가 있을 수 있어서**다.
신규 생성 요청은 프론트에서 항상 날짜를 보내지만, 도메인은 "둘 다 NULL이거나 둘 다 존재"를
허용해 기존 데이터 조회가 깨지지 않게 한다.

### 유니크 제약 교체

기존 제약 두 개 중 하나는 바꾸고, 하나는 **반드시 제거**한다.

| 기존 | 처리 | 이유 |
|---|---|---|
| `uk_course_spots_course_sequence (course_id, sequence)` | → `(course_id, day_number, sequence)` | sequence가 Day 안에서만 유일해야 한다 |
| `uk_course_spots_course_spot (course_id, spot_id)` | **삭제** | 같은 숙소를 Day1·Day2에 넣는 흔한 케이스를 DB가 막는다 |

⚠️ **`ddl-auto`는 로컬·운영 모두 `update`다. Hibernate의 `update`는 컬럼을 추가할 뿐
제약을 절대 삭제하지 않는다.** 따라서 아래 마이그레이션 SQL을 로컬과 운영 양쪽에서
직접 실행해야 한다. 실행하지 않으면 "같은 장소를 두 Day에 담기"가 런타임에
`DataIntegrityViolationException`으로 터진다.

`docs/migrations/2026-09-02-add-course-days.sql`:

```sql
-- 코스에 여행 기간(일자)과 일차(day_number) 개념을 추가한다.
-- ddl-auto가 update라 컬럼은 자동 추가될 수 있지만, 제약 삭제는 Hibernate가 절대 하지 않는다.
-- 로컬과 운영 양쪽에서 이 파일을 반드시 실행해야 한다.

-- 1) 여행 기간. 기존 코스에는 날짜가 없으므로 NULL을 허용한다.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS end_date DATE;

-- 2) 일차. 기존 행은 전부 1일차로 백필한 뒤 DEFAULT를 떼어
--    애플리케이션이 항상 명시적으로 값을 넣도록 강제한다.
ALTER TABLE course_spots ADD COLUMN IF NOT EXISTS day_number INT NOT NULL DEFAULT 1;
ALTER TABLE course_spots ALTER COLUMN day_number DROP DEFAULT;

-- 3) 같은 코스에 같은 spot을 못 넣게 막던 제약을 제거한다.
--    2박 내내 같은 숙소처럼, 한 장소가 여러 Day에 등장하는 건 정상이다.
--    최초 마이그레이션은 CREATE UNIQUE INDEX로, JPA @UniqueConstraint는 제약으로 만들 수 있어 둘 다 지운다.
ALTER TABLE course_spots DROP CONSTRAINT IF EXISTS uk_course_spots_course_spot;
DROP INDEX IF EXISTS uk_course_spots_course_spot;

-- 4) sequence는 코스 전체가 아니라 Day 안에서만 유일하다.
ALTER TABLE course_spots DROP CONSTRAINT IF EXISTS uk_course_spots_course_sequence;
DROP INDEX IF EXISTS uk_course_spots_course_sequence;
CREATE UNIQUE INDEX IF NOT EXISTS uk_course_spots_course_day_sequence
    ON course_spots(course_id, day_number, sequence);
```

`CourseSpotJpaEntity`의 `@Table(uniqueConstraints = ...)` 선언도 위와 똑같이 바꿔야 한다.
그대로 두면 로컬에서 `ddl-auto: update`가 방금 지운 제약을 다시 만들어 버린다.

## 도메인 모델

### 새 값 객체: `CourseDayModel`

```java
public record CourseDayModel(int dayNumber, List<CourseSpotModel> spots)
```

- `dayNumber >= 1`
- `spots`는 null일 수 없지만 **빈 리스트는 허용한다** — "Day 3은 아직 안 정했다"가 정상 상태다
- 원소에 null 금지
- **같은 Day 안에서 `spotId` 중복 금지**. 서로 다른 Day 사이의 중복은 허용한다
- `spots`는 `List.copyOf`로 불변화한다
- Day 하나에 담을 수 있는 장소는 최대 30개

`CourseSpotModel(Long spotId, String memo)`은 지금 그대로 둔다. `sequence`는 리스트 안의
위치가 곧 순서라서 값 객체가 들고 있지 않는다 — 지금 `CourseRepositoryImpl:39`가 쓰는
`.sequence(index)` 규칙을 Day 안으로 그대로 옮기는 것이다.

### `CourseModel` 변경

`List<CourseSpotModel> spots` 필드를 `List<CourseDayModel> days`로 바꾸고,
`LocalDate startDate` / `LocalDate endDate`를 추가한다.
`create`/`reconstruct`/`update` 세 팩토리·메서드의 시그니처가 함께 바뀐다.

검증 규칙(`validate`):

1. `days`는 null이거나 비어 있을 수 없고, 최대 30개다(한 달 상한).
2. `days`의 `dayNumber`는 **1부터 1씩 증가하는 연속값**이어야 한다 — 리스트의 `index + 1`과
   `dayNumber`가 일치해야 한다. "Day 1 없이 Day 2만 있는" 상태를 애초에 못 만든다.
3. 모든 Day의 장소를 합쳐 **최소 1개**는 있어야 한다(기존의 "spot 최소 1개" 규칙을 코스 전체로 옮긴 것).
4. `startDate`/`endDate`는 **둘 다 null이거나 둘 다 존재**해야 한다.
5. 둘 다 존재하면 `endDate >= startDate`이고,
   `ChronoUnit.DAYS.between(startDate, endDate) + 1 == days.size()`여야 한다.
   날짜를 정했으면 Day 수가 기간과 어긋날 수 없다.

기존의 "코스 전체에서 spotId 중복 금지" 규칙은 **삭제한다**(Day 안 중복 금지로 대체).
`title`/`description`/`thumbnail` 길이 제한과 카운터 검증은 그대로 둔다.

## API

### `POST /api/v1/courses` (요청 형태 변경)

인증 필요. 요청 본문:

```json
{
  "title": "속초 2박 3일",
  "description": "바다 보고 회 먹는 코스",
  "thumbnail": null,
  "visibility": "PRIVATE",
  "startDate": "2026-09-12",
  "endDate": "2026-09-14",
  "days": [
    { "dayNumber": 1, "spots": [{ "spotId": 12, "memo": "점심" }, { "spotId": 30, "memo": null }] },
    { "dayNumber": 2, "spots": [{ "spotId": 45, "memo": null }] },
    { "dayNumber": 3, "spots": [] }
  ]
}
```

**배열 안의 위치가 곧 `sequence`다.** 클라이언트는 sequence 숫자를 계산해 보내지 않는다.
`PATCH /api/v1/courses/{courseId}`도 같은 형태를 받는다(수정 화면은 이번 범위 밖이지만,
DTO는 생성과 같은 모양을 유지한다).

### 응답 형태 변경 — spot 요약 포함

지금 `CourseResponse.Spot`은 `spotId/memo/sequence`만 내려준다(`CourseResponse.java:39`).
이대로면 상세·목록 화면이 장소 이름을 얻으려고 `GET /spots/{id}`를 N번 호출해야 하는데,
**그 API는 호출할 때마다 조회수를 올리는 부작용이 있다**(`SpotController.java:47`).
코스를 한 번 여는 것만으로 담긴 장소들의 조회수가 전부 오르는 건 명백히 잘못이다.

그래서 코스 응답에 장소 요약을 함께 내린다:

```json
{
  "courseId": 7,
  "userId": 3,
  "title": "속초 2박 3일",
  "description": "바다 보고 회 먹는 코스",
  "thumbnail": null,
  "visibility": "PRIVATE",
  "status": "ACTIVE",
  "viewCount": 0,
  "likeCount": 0,
  "startDate": "2026-09-12",
  "endDate": "2026-09-14",
  "days": [
    {
      "dayNumber": 1,
      "spots": [
        {
          "spotId": 12,
          "sequence": 0,
          "memo": "점심",
          "title": "속초관광수산시장",
          "category": "음식점",
          "region": "51",
          "sigungu": "210",
          "address": "강원특별자치도 속초시 ...",
          "thumbnail": "https://.../thumb.jpg",
          "latitude": 38.2070,
          "longitude": 128.5918
        }
      ]
    }
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

`latitude`/`longitude`는 지금 화면에서 쓰지 않지만, 나중에 지도를 얹을 때 응답 형태를
다시 바꾸지 않으려고 처음부터 넣는다.

### N+1 제거 — `SpotRepository.findAllByIdIn`

`CourseApplicationService.validateSpots`는 지금 spot마다 `findById`를 호출한다
(`CourseApplicationService.java:101`). 장소 요약까지 필요해지면 이 N+1이 더 아프다.

`SpotRepository`에 `List<SpotModel> findAllByIdIn(Collection<Long> spotIds)`를 추가하고,
**한 번의 조회로 검증과 요약 채우기를 동시에 처리**한다:

1. 모든 Day의 `spotId`를 모아 중복 제거
2. `findAllByIdIn`으로 한 번에 조회
3. `status == ACTIVE`가 아니거나 조회되지 않은 id가 있으면 404
4. 살아남은 `Map<Long, SpotModel>`을 `CourseResult.from(course, spotsById)`에 넘겨 요약을 채운다

`CourseResult.from(CourseModel)`의 시그니처가 `from(CourseModel, Map<Long, SpotModel>)`로 바뀐다.
조회 경로(`listMine`, `getMine`)도 같은 방식으로 spot을 모아 조회한 뒤 변환한다.

### `GET /api/v1/spots` — keyword 검색 추가

직접 코스 생성의 핵심 인터랙션은 "가고 싶은 곳 이름을 쳐서 담기"다. 지금
`SpotSearchCondition`은 `category/region/sigungu`뿐이라 이게 안 된다.

- `SpotSearchCondition`에 `String keyword`를 **첫 번째 필드로** 추가한다
  → `record SpotSearchCondition(String keyword, String category, String region, String sigungu)`
- `SpotListQuery`에 `keyword`를 추가하고, **빈 문자열·공백만인 값은 null로 정규화**한다
  (`""`가 들어와 `LIKE '%%'`로 전건 스캔되는 걸 막는다)
- `SpotController.list`에 `@RequestParam(required = false) String keyword`를 추가한다
- `SpotJpaRepository`의 **세 쿼리 모두**(`searchActiveByLatest`, `searchActiveByPopular`,
  `countActive`)에 아래 조건을 추가한다:

```
AND (:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
```

Postgres의 `ILIKE` 대신 `LOWER(...) LIKE LOWER(...)`를 쓰는 이유는 JPQL 표준 문법만으로
표현하기 위해서다(나머지 쿼리들과 같은 방식을 유지한다).

부분일치라 인덱스를 타지 못하고 순차 스캔이 된다. 대상이 강원도 한정 수천 건 규모라
지금은 문제되지 않는다. 데이터가 커지면 `pg_trgm` GIN 인덱스를 검토한다.

## 프론트엔드

### 라우팅 (`App.tsx`)

```
/courses            → CourseListPage    (내 코스 목록)
/courses/create     → CourseCreatePage  (직접 코스 생성)
/courses/:courseId  → CourseDetailPage  (코스 상세)
```

`/courses/create`가 `/courses/:courseId`보다 먼저 매칭돼야 한다. react-router v6는 정적
세그먼트에 더 높은 점수를 주므로 선언 순서와 무관하게 동작하지만, 읽는 사람을 위해
`create`를 위에 둔다.

### 서비스 레이어

`frontend/src/services/course.ts`를 새로 만든다. 코스 API는 **전부 인증이 필요**하므로
모든 요청에 `credentials: "include"`를 붙인다(프론트 3000 / 백엔드 8080이 서로 다른 origin이라
기본값 `same-origin`으로는 `access_token` 쿠키가 실리지 않는다 — `spots.ts:109`의 주석과 같은 이유).

401/403은 `spots.ts`가 이미 export하는 `UnauthorizedError`를 재사용해 던진다.

| 함수 | 설명 |
|---|---|
| `createCourse(payload)` | `POST /courses`. 성공 시 생성된 `Course` 반환 |
| `fetchMyCourses()` | `GET /courses`. 내 코스 목록 |
| `fetchCourse(courseId)` | `GET /courses/{id}`. 404면 `null` |

`searchSpots(params)`는 코스가 아니라 스팟 API이므로 **`services/spots.ts`에 추가**한다.
기존 `fetchPopularSpots`와 같은 공개 API(인증 불필요)이고, `keyword`를 넘길 수 있는 점만 다르다.

### 코스 생성 화면 (`CourseCreatePage`)

편집 상태는 전부 React state다. 서버는 "저장"을 누르기 전까지 이 코스의 존재를 모른다.

```ts
type DraftSpot = {
  spotId: number;
  memo: string | null;
  // 화면에 바로 그리기 위해 검색 결과의 요약을 그대로 들고 있는다
  title: string;
  category: string;
  thumbnail: string | null;
};

type DraftState = {
  title: string;
  description: string;
  startDate: string;   // "YYYY-MM-DD"
  endDate: string;
  visibility: "PRIVATE" | "PUBLIC";
  days: DraftSpot[][];  // 배열 index가 곧 dayNumber - 1
};
```

`days`를 `DraftSpot[][]`로 두면 `dayNumber`를 상태에 중복 저장하지 않아도 되고,
"1부터 연속" 규칙이 자료구조로 보장된다. 서버로 보낼 때만
`days.map((spots, i) => ({ dayNumber: i + 1, spots: ... }))`로 변환한다.

동작:

- **날짜 선택 → Day 수 자동 조정.** 기간이 늘면 빈 Day를 뒤에 추가하고, 줄면 뒤쪽 Day를 제거한다.
  제거될 Day에 담긴 장소가 있으면 확인을 받고 지운다(말없이 날리지 않는다).
- **장소 추가.** Day마다 "장소 추가" 버튼 → `SpotSearchModal`이 열린다. 어느 Day에서 열었는지를
  모달에 넘겨, 고른 장소가 그 Day의 맨 뒤에 붙는다.
- **순서 변경.** 각 장소 카드의 ▲▼ 버튼으로 같은 Day 안에서 한 칸씩 이동한다.
  맨 위/맨 아래에서는 해당 버튼이 비활성화된다.
- **Day 이동.** 카드의 셀렉트로 다른 Day로 옮긴다. 옮긴 Day의 맨 뒤에 붙는다.
- **삭제.** 카드의 X 버튼.
- **메모.** 카드마다 최대 500자 입력(백엔드 `CourseSpotModel`의 제한과 같다).
- **저장.** 제목이 비었거나 담긴 장소가 0개면 버튼이 비활성화되고 이유를 안내한다.
  성공하면 `navigate('/courses/' + courseId, { replace: true })` — 뒤로가기로 편집 화면에
  다시 돌아오지 않게 `replace`를 쓴다.

**임시 백업.** `DraftState`가 바뀔 때마다 `sessionStorage`의 `planfix:course-draft`에 저장하고,
화면 진입 시 값이 있으면 복원한다. 저장에 성공하면 삭제한다. 자동저장이 아니라 실수로 새로고침·
뒤로가기했을 때의 안전망이다 — `sessionStorage` 접근은 try/catch로 감싸 실패해도 화면이 죽지 않게 한다.

### 장소 검색 모달 (`SpotSearchModal`)

- 키워드 입력 → **300ms debounce** 후 `searchSpots({ keyword, size: 20 })` 호출
- 키워드가 비어 있으면 인기순 상위를 보여준다(`sort=popular`) — 빈 화면을 주지 않는다
- 카테고리 필터 칩(관광지/음식점/숙박 등)은 있으면 좋지만 필수는 아니다
- 결과 항목을 누르면 `onSelect(spot)`을 호출하고 모달을 닫는다
- 이미 그 Day에 담긴 장소는 "담김"으로 표시하고 선택을 막는다(백엔드가 같은 Day 중복을 거부한다)
- 접근성·닫기 동작은 `CourseSelectModal`을 그대로 따른다
  (`role="dialog"`, `aria-modal`, Escape 닫기, 배경 클릭 닫기, `body` 스크롤 잠금)

### 코스 상세 (`CourseDetailPage`) / 목록 (`CourseListPage`)

- 상세: 제목·기간·설명 헤더 + Day별 섹션. 각 장소 카드는 썸네일·제목·카테고리·주소·메모를 보여주고,
  누르면 `/spots/{spotId}`로 이동한다. 장소가 없는 Day는 "아직 계획이 없어요"로 표시한다.
- 목록: 내 코스 카드 그리드(제목·기간·장소 수·썸네일). 비어 있으면 "첫 코스를 만들어보세요" +
  코스 만들기 CTA.
- 두 화면 모두 로딩·에러·비로그인(`UnauthorizedError` → `/login`) 상태를 처리한다.
- 두 화면 모두 `AppNav`를 포함한다(`app-nav.tsx`의 `isTripActive`가 이미 `/courses`를 활성으로 친다).

### `CourseSelectModal` 수정

"AI 코스 생성" 버튼은 이번 범위 밖이다. 지금은 라우트 없는 `/courses/create?mode=ai`로 보내
로그인 페이지로 튕기므로, **버튼을 `disabled`로 바꾸고 "준비 중" 배지를 단다**.
"직접 코스 생성"은 `/courses/create`로 그대로 보낸다(이제 라우트가 생겨 정상 동작한다).

## 레이어별 변경

| 파일 | 변경 |
|---|---|
| `docs/migrations/2026-09-02-add-course-days.sql` | 신규. 컬럼 추가 + 유니크 제약 교체 |
| `domain/course/CourseDayModel.java` | 신규. `record CourseDayModel(int dayNumber, List<CourseSpotModel> spots)` |
| `domain/course/CourseModel.java` | `spots` → `days`, `startDate`/`endDate` 추가, 검증 규칙 교체 |
| `domain/spot/SpotRepository.java` | `findAllByIdIn(Collection<Long>)` 추가 |
| `domain/spot/SpotSearchCondition.java` | `keyword` 필드 추가(첫 번째) |
| `infrastructure/course/CourseJpaEntity.java` | `startDate`/`endDate` 컬럼 추가 |
| `infrastructure/course/CourseSpotJpaEntity.java` | `dayNumber` 컬럼 추가, `@UniqueConstraint` 교체(spot 중복 제약 제거) |
| `infrastructure/course/CourseSpotJpaRepository.java` | 조회를 `(day_number, sequence)` 정렬로 |
| `infrastructure/course/CourseRepositoryImpl.java` | Day별로 `sequence`를 0부터 부여해 저장, 조회 시 Day로 재조립 |
| `infrastructure/spot/SpotJpaRepository.java` | 세 쿼리에 keyword 조건 추가, `findAllBySpotIdIn` 추가 |
| `infrastructure/spot/SpotRepositoryImpl.java` | `findAllByIdIn` 위임, keyword 전달 |
| `application/course/CourseCommand.java` | `Create`/`Update`에 `startDate`/`endDate`, `spots` → `days` |
| `application/course/CourseResult.java` | `days` 중첩 구조 + spot 요약 필드, `from(course, spotsById)`로 시그니처 변경 |
| `application/course/CourseApplicationService.java` | `validateSpots` N+1 제거, 요약 조회 후 결과 조립 |
| `application/spot/SpotListQuery.java` | `keyword` 추가 + 빈 문자열 → null 정규화 |
| `application/spot/SpotListApplicationService.java` | `keyword`를 `SpotSearchCondition`에 전달 |
| `interfaces/api/course/CourseRequest.java` | `Day` 중첩 record 추가, `toCommand` 변환 |
| `interfaces/api/course/CourseResponse.java` | `days` 중첩 + spot 요약 필드 |
| `interfaces/api/spot/SpotController.java` | `list`에 `keyword` 파라미터 추가 |
| `frontend/src/App.tsx` | `/courses`, `/courses/create`, `/courses/:courseId` 라우트 추가 |
| `frontend/src/services/course.ts` | 신규. `createCourse`/`fetchMyCourses`/`fetchCourse` |
| `frontend/src/services/spots.ts` | `searchSpots(params)` 추가 |
| `frontend/src/pages/course-create-page.tsx` | 신규 |
| `frontend/src/pages/course-detail-page.tsx` | 신규 |
| `frontend/src/pages/course-list-page.tsx` | 신규 |
| `frontend/src/components/ui/spot-search-modal.tsx` | 신규 |
| `frontend/src/components/ui/course-select-modal.tsx` | AI 버튼 비활성화 + "준비 중" 배지 |

## 테스트

**백엔드**

- `CourseModelTest`: dayNumber 연속성 위반 거부, 빈 Day 허용, 전체 장소 0개 거부,
  같은 Day 내 spot 중복 거부, **다른 Day 간 중복 허용**, 날짜 한쪽만 있으면 거부,
  `endDate < startDate` 거부, 기간과 `days.size()` 불일치 거부
- `CourseRepositoryImplTest`(`@SpringBootTest` + `@Transactional`): Day별 저장 후 복원 시
  `(day_number, sequence)` 순서 보존, **같은 spot을 서로 다른 Day에 저장 성공**
  (마이그레이션을 실행하지 않았다면 이 테스트가 실패해서 알려준다)
- `CourseApplicationServiceTest`: 존재하지 않는 spot 404, HIDDEN spot 404,
  응답에 spot 요약이 채워짐, 소유자가 아니면 403
- `CourseControllerTest`: `days` 요청/응답 매핑
- `SpotRepositoryImplTest`: keyword 부분일치, 대소문자 무시, keyword가 null이면 전체 조회,
  keyword와 다른 필터 조합
- `SpotControllerTest`: `keyword` 파라미터가 조건으로 전달됨

**프론트엔드**

- `course-create-page.test.tsx`: 날짜 변경 시 Day 수 조정, 검색 모달로 장소 담기,
  ▲▼ 순서 변경, Day 이동, 저장 시 전송되는 payload 형태(`days[i].dayNumber === i+1`),
  제목/장소 없으면 저장 비활성, sessionStorage 복원
- `spot-search-modal.test.tsx`: 키워드 입력 debounce 후 검색 호출, 결과 선택,
  이미 담긴 장소 선택 불가, Escape·배경 클릭으로 닫힘
- `course-detail-page.test.tsx`: Day별 렌더링, 빈 Day 안내, 장소 클릭 시 이동
- `course-list-page.test.tsx`: 목록 렌더링, 빈 상태 CTA
- `course-select-modal.test.tsx`(기존 수정): AI 버튼 비활성 확인
- `app-nav.test.tsx`(기존): "여행" 클릭 → 모달 → 직접 생성 → `/courses/create` 이동 확인

## 미해결/향후 과제

- **AI 코스 생성**이 이 구조 위에 올라온다. `AiTestApplicationService`의 LangGraph 워크플로가
  `CourseCommand.Create`(days 중첩 구조)를 그대로 만들어내면, 생성 화면에 프리필해서
  사용자가 손보고 저장하는 흐름이 자연스럽다. 이번 설계는 그 형태를 염두에 두고 만들었다.
- **지도.** `latitude`/`longitude`를 코스 응답에 이미 넣어 뒀으므로, 카카오맵 SDK를 붙이고
  Day별 마커·동선을 그리는 건 프론트만의 작업이 된다.
- **코스 수정 화면.** `PATCH`는 생성과 같은 DTO를 받으므로, 생성 화면을 초기 상태만 다르게
  재사용하는 방향이 자연스럽다.
- **장소별 방문 시각·이동 시간**이 필요해지면 `course_spots`에 `start_time`을 추가하거나,
  Day 고유 속성이 생기는 시점에 `course_days` 테이블을 분리한다.
- **검색 성능.** `LIKE '%keyword%'`는 인덱스를 타지 못한다. 데이터가 커지면 `pg_trgm` GIN
  인덱스를 검토한다.
- **코스 공개.** `visibility`는 이미 있지만 공개 코스를 남에게 보여주는 API는 없다.
  게시글(`boards`)이 코스를 참조하는 경로가 이미 있으므로(`BoardApplicationService:127`),
  공개 정책은 그쪽과 함께 정한다.
