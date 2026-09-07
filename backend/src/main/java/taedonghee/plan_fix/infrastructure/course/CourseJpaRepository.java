package taedonghee.plan_fix.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;

import taedonghee.plan_fix.domain.course.CourseStatus;

import java.util.List;

/**
 * CourseJpaEntity Spring Data JPA Repository
 */
public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, Long> {

    /**
     * 사용자별 활성 코스 목록 조회
     */
    List<CourseJpaEntity> findByUserIdAndStatusOrderByCourseIdDesc(Long userId, CourseStatus status);

    /**
     * 사용자가 좋아요 누른 활성 코스 목록 조회 (최신 좋아요 순)
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM CourseJpaEntity c
            JOIN CourseLikeJpaEntity cl ON c.courseId = cl.courseId
            WHERE cl.userId = :userId AND c.status = taedonghee.plan_fix.domain.course.CourseStatus.ACTIVE
            ORDER BY cl.createdAt DESC
            """)
    List<CourseJpaEntity> findLikedCoursesByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE CourseJpaEntity c SET c.likeCount = c.likeCount + 1 WHERE c.courseId = :courseId")
    void incrementLikeCount(@org.springframework.data.repository.query.Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE CourseJpaEntity c
            SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END
            WHERE c.courseId = :courseId
            """)
    void decrementLikeCount(@org.springframework.data.repository.query.Param("courseId") Long courseId);
}
