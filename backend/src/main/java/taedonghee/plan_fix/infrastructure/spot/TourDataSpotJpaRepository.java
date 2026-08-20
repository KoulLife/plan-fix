package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface TourDataSpotJpaRepository extends JpaRepository<TourDataSpotJpaEntity, Long> {

	Optional<TourDataSpotJpaEntity> findByContentId(Long contentId);

	List<TourDataSpotJpaEntity> findBySigungu(String sigungu);

	List<TourDataSpotJpaEntity> findBySigunguAndImageCollectedAtIsNull(String sigungu);

	List<TourDataSpotJpaEntity> findBySigunguAndInfoCollectedAtIsNull(String sigungu);
}
