package taedonghee.plan_fix.domain.course;

import java.util.List;
import java.util.Optional;

/**
 * Course 저장소 추상화
 */
public interface CourseRepository {

    /**
     * 코스 저장 처리
     */
    CourseModel save(CourseModel course);

    /**
     * course_id 기반 코스 단건 조회 처리
     */
    Optional<CourseModel> findById(Long courseId);

    /**
     * user_id 기반 활성 코스 목록 조회 처리
     */
    List<CourseModel> findActiveByUserId(Long userId);
}
