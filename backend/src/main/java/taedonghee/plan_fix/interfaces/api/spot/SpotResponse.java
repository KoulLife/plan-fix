package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotResult;
import taedonghee.plan_fix.domain.spot.SpotStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Spot API 응답 DTO
 */
public record SpotResponse(
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
     * application 결과값 변환
     */
    public static SpotResponse from(SpotResult result) {
        return new SpotResponse(
                result.spotId(),
                result.title(),
                result.category(),
                result.region(),
                result.sigungu(),
                result.address(),
                result.latitude(),
                result.longitude(),
                result.thumbnail(),
                result.description(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
