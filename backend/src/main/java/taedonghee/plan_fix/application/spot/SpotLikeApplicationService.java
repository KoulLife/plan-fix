package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * [application] 스팟 좋아요/좋아요 취소. 둘 다 idempotent다 — 중복 좋아요·안 한 것 취소는
 * 에러 없이 현재 상태를 그대로 응답한다.
 */
@Service
@RequiredArgsConstructor
public class SpotLikeApplicationService {

    private final SpotRepository spotRepository;
    private final SpotLikeRepository spotLikeRepository;

    @Transactional
    public SpotLikeResult like(Long userId, Long spotId) {
        SpotModel spot = getActiveSpotOrThrow(spotId);

        if (spotLikeRepository.existsByUserIdAndSpotId(userId, spotId)) {
            return new SpotLikeResult(true, spot.likeCount());
        }

        try {
            spotLikeRepository.save(SpotLikeModel.create(userId, spotId));
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 좋아요 요청끼리의 경쟁. 유니크 제약이 막았으니 이미 좋아요된 것으로 본다.
            // 카운트는 먼저 커밋된 쪽이 이미 올렸으므로 여기서는 늘리지 않는다.
            return new SpotLikeResult(true, spot.likeCount());
        }

        spotRepository.incrementLikeCount(spotId);
        return new SpotLikeResult(true, spot.likeCount() + 1);
    }

    @Transactional
    public SpotLikeResult unlike(Long userId, Long spotId) {
        SpotModel spot = getActiveSpotOrThrow(spotId);

        boolean deleted = spotLikeRepository.deleteByUserIdAndSpotId(userId, spotId);
        if (!deleted) {
            return new SpotLikeResult(false, spot.likeCount());
        }

        spotRepository.decrementLikeCount(spotId);
        return new SpotLikeResult(false, Math.max(spot.likeCount() - 1, 0));
    }

    private SpotModel getActiveSpotOrThrow(Long spotId) {
        return spotRepository.findById(spotId)
                .filter(s -> s.status() == SpotStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "spot not found. spotId=" + spotId));
    }
}
