-- 테스트용 참여 멤버 1건. 실제 users/courses가 있을 때만 삽입되며 중복 실행해도 안전합니다.
INSERT INTO course_members (course_id, user_id, role, created_at)
SELECT c.course_id,
       u.user_id,
       'VIEWER',
       CURRENT_TIMESTAMP
FROM courses c
JOIN users u ON u.user_id <> c.user_id
WHERE c.status = 'ACTIVE'
  AND u.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM course_members cm
      WHERE cm.course_id = c.course_id AND cm.user_id = u.user_id
  )
ORDER BY c.course_id, u.user_id
LIMIT 1;
