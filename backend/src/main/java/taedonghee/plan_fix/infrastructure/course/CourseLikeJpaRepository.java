package taedonghee.plan_fix.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseLikeJpaRepository extends JpaRepository<CourseLikeJpaEntity, Long> {

    Optional<CourseLikeJpaEntity> findByUserIdAndCourseId(Long userId, Long courseId);

    @Modifying
    @Query("DELETE FROM CourseLikeJpaEntity c WHERE c.userId = :userId AND c.courseId = :courseId")
    long deleteByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
