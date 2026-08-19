package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface TourDataImageJpaRepository extends JpaRepository<TourDataImageJpaEntity, Long> {

	List<TourDataImageJpaEntity> findByTourDataSpotId(Long tourDataSpotId);

	void deleteByTourDataSpotId(Long tourDataSpotId);
}
