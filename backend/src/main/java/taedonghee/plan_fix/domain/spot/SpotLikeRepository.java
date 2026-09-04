package taedonghee.plan_fix.domain.spot;

/**
 * [domain] 저장소 포트. domain은 이 인터페이스만 알고, 구현(JPA 등)은 infrastructure가 담당한다.
 */
public interface SpotLikeRepository {

    boolean existsByUserIdAndSpotId(Long userId, Long spotId);

    java.util.Set<Long> findLikedSpotIds(Long userId, java.util.Collection<Long> spotIds);

    /** (user_id, spot_id) 유니크 제약 위반 시 DataIntegrityViolationException을 그대로 던진다. */
    SpotLikeModel save(SpotLikeModel like);

    /** 실제로 지운 행이 있으면 true, 원래 좋아요하지 않았으면(지울 행이 없으면) false. */
    boolean deleteByUserIdAndSpotId(Long userId, Long spotId);
}
