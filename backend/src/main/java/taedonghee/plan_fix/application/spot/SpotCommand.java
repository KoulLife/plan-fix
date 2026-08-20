package taedonghee.plan_fix.application.spot;

import java.math.BigDecimal;

/**
 * Spot Command DTO
 */
public final class SpotCommand {

    private SpotCommand() {
    }

    /**
     * Spot 생성 입력값
     */
    public record Create(
            String title,
            String category,
            String region,
            String sigungu,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String thumbnail,
            String description
    ) {
    }

    /**
     * Spot 수정 입력값
     */
    public record Update(
            String title,
            String category,
            String region,
            String sigungu,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String thumbnail,
            String description
    ) {
    }
}
