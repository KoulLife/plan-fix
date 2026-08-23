-- users에 실명 컬럼 추가. 기존 username에 들어 있던 한글 실명을 name으로 복사한다.
-- 로컬은 ddl-auto:update가 컬럼만 만들고 백필은 하지 않으므로 이 UPDATE를 직접 실행해야 한다.
-- 운영은 ddl-auto:validate이므로 ALTER까지 직접 실행한다.
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(30);
UPDATE users SET name = username WHERE name IS NULL;
