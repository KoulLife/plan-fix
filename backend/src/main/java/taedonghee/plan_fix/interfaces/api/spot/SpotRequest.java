package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotCommand;

import java.math.BigDecimal;

/**
 * Spot API 요청 DTO
 */
public final class SpotRequest {

    private SpotRequest() {
    }

    /**
     * Spot 생성 HTTP 요청값
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

        /**
         * application 입력값 변환
         */
        public SpotCommand.Create toCommand() {
            return new SpotCommand.Create(
                    title,
                    category,
                    region,
                    sigungu,
                    address,
                    latitude,
                    longitude,
                    thumbnail,
                    description
            );
        }
    }

    /**
     * Spot 수정 HTTP 요청값
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

        /**
         * application 입력값 변환
         */
        public SpotCommand.Update toCommand() {
            return new SpotCommand.Update(
                    title,
                    category,
                    region,
                    sigungu,
                    address,
                    latitude,
                    longitude,
                    thumbnail,
                    description
            );
        }
    }
}
