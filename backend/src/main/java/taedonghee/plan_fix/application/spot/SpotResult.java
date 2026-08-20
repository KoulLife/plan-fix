package taedonghee.plan_fix.application.spot;

import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Spot 결과 DTO
 */
public record SpotResult(
        Long spotId,
        String title,
        String category,
        String region,
        String sigungu,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnail,
        String description,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        SpotStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Spot 도메인 모델 변환
     */
    public static SpotResult from(SpotModel spot) {
        return new SpotResult(
                spot.getSpotId(),
                spot.getTitle(),
                spot.getCategory(),
                spot.getRegion(),
                spot.getSigungu(),
                spot.getAddress(),
                spot.getLatitude(),
                spot.getLongitude(),
                spot.getThumbnail(),
                spot.getDescription(),
                spot.getViewCount(),
                spot.getLikeCount(),
                spot.getCommentCount(),
                spot.getStatus(),
                spot.getCreatedAt(),
                spot.getUpdatedAt()
        );
    }
}
