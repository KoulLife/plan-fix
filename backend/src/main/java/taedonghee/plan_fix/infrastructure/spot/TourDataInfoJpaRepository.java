package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface TourDataInfoJpaRepository extends JpaRepository<TourDataInfoJpaEntity, Long> {

	Optional<TourDataInfoJpaEntity> findByContentId(Long contentId);
}
