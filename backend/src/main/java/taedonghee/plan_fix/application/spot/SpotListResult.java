package taedonghee.plan_fix.application.spot;

import taedonghee.plan_fix.domain.spot.SpotModel;

import java.util.List;

/**
 * [application] 공개 스팟 목록 조회 결과.
 */
public record SpotListResult(List<Item> items, int offset, int size, long totalCount) {

    /**
     * 목록에 노출할 스팟 요약. 공개 목록이라 좋아요 수 등 서비스 소유 필드는 담지 않는다.
     */
    public record Item(Long spotId, String title, String category, String region, String sigungu, String thumbnail, boolean isLiked) {

        public static Item from(SpotModel spot) {
            return from(spot, false);
        }

        public static Item from(SpotModel spot, boolean isLiked) {
            return new Item(spot.spotId(), spot.title(), spot.category(), spot.region(), spot.sigungu(),
                    spot.thumbnail(), isLiked);
        }
    }
}
