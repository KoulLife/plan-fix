package taedonghee.plan_fix.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * CourseSpotJpaEntity Spring Data JPA Repository
 */
public interface CourseSpotJpaRepository extends JpaRepository<CourseSpotJpaEntity, Long> {

    /**
     * 코스에 포함된 spot 목록을 순서대로 조회
     */
    List<CourseSpotJpaEntity> findByCourseIdOrderBySequenceAsc(Long courseId);

    /**
     * 코스에 포함된 기존 spot 목록 삭제
     */
    void deleteByCourseId(Long courseId);
}
