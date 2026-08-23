package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * [domain] 사용자가 스팟을 좋아요한 이력 한 건. users와 spots를 잇는 다대다 관계 테이블(spot_likes)의
 * canonical 모델이다. (userId, spotId) 조합은 DB 유니크 제약으로 유일함이 보장된다.
 */
public class SpotLikeModel {

    private final Long spotLikeId;
    private final Long userId;
    private final Long spotId;
    private final OffsetDateTime createdAt;

    private SpotLikeModel(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (spotId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "spotId는 필수입니다.");
        }
        this.spotLikeId = spotLikeId;
        this.userId = userId;
        this.spotId = spotId;
        this.createdAt = createdAt;
    }

    /** 신규 좋아요 생성 */
    public static SpotLikeModel create(Long userId, Long spotId) {
        return new SpotLikeModel(null, userId, spotId, OffsetDateTime.now());
    }

    /** infrastructure의 영속 데이터 복원 */
    public static SpotLikeModel reconstruct(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt) {
        return new SpotLikeModel(spotLikeId, userId, spotId, createdAt);
    }

    public Long spotLikeId() { return spotLikeId; }
    public Long userId() { return userId; }
    public Long spotId() { return spotId; }
    public OffsetDateTime createdAt() { return createdAt; }
}
