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
