package taedonghee.plan_fix.domain.spot;

import java.util.Optional;

/**
 * Spot Repository
 */
public interface SpotRepository {

    /**
     * Spot 저장
     */
    SpotModel save(SpotModel spot);

    /**
     * spot_id 기반 Spot 단건 조회
     */
    Optional<SpotModel> findBySpotId(Long spotId);
}
