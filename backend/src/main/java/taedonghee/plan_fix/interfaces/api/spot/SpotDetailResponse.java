package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotDetailResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * [interfaces] 공개 스팟 상세 조회 응답 DTO.
 * images/info는 TourAPI로 수집된 스팟일 때만 채워지고, 그 외에는 각각 빈 리스트/null이다.
 * isLiked는 요청한 사람(비로그인이면 false) 기준이다.
 */
public record SpotDetailResponse(
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
        long viewCount,
        long likeCount,
        long commentCount,
        List<String> images,
        TourInfo info,
        boolean isLiked
) {

    public static SpotDetailResponse from(SpotDetailResult result) {
        return new SpotDetailResponse(
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
                result.images(),
                result.info() == null ? null : TourInfo.from(result.info()),
                result.isLiked()
        );
    }

    /** detailIntro2 결과. 값이 없는 필드는 null로 내려간다(예: firstMenu/treatMenu/lcnsno는 음식점만). */
    public record TourInfo(
            String tel,
            String parkInfo,
            String timeInfo,
            String restInfo,
            String firstMenu,
            String treatMenu,
            String lcnsno
    ) {

        public static TourInfo from(SpotDetailResult.TourInfo info) {
            return new TourInfo(
                    info.tel(),
                    info.parkInfo(),
                    info.timeInfo(),
                    info.restInfo(),
                    info.firstMenu(),
                    info.treatMenu(),
                    info.lcnsno()
            );
        }
    }
}
