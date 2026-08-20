package taedonghee.plan_fix.domain.spot;

import java.util.Optional;

/**
 * [domain] 저장소 포트. domain은 이 인터페이스만 알고, 구현(JPA 등)은 infrastructure가 담당한다.
 */
public interface SpotRepository {

	/** 저장 후 id가 채번된 SpotModel을 반환한다. */
	SpotModel save(SpotModel spot);

	Optional<SpotModel> findById(Long spotId);

	long countAll();
}
