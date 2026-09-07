package taedonghee.plan_fix.domain.course;

/**
 * [domain] 코스 좋아요 저장소 포트.
 */
public interface CourseLikeRepository {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    CourseLikeModel save(CourseLikeModel like);

    boolean deleteByUserIdAndCourseId(Long userId, Long courseId);
}
