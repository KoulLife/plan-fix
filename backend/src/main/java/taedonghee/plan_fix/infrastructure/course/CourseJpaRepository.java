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
}
