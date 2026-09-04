package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotListResult;

import java.util.List;

/**
 * [interfaces] 공개 스팟 목록 조회 응답 DTO.
 */
public record SpotResponse(List<Item> items, int offset, int size, long totalCount) {

    public static SpotResponse from(SpotListResult result) {
        List<Item> items = result.items().stream().map(Item::from).toList();
        return new SpotResponse(items, result.offset(), result.size(), result.totalCount());
    }

    public record Item(Long spotId, String title, String category, String region, String sigungu, String thumbnail, boolean isLiked) {

        public static Item from(SpotListResult.Item item) {
            return new Item(item.spotId(), item.title(), item.category(), item.region(), item.sigungu(),
                    item.thumbnail(), item.isLiked());
        }
    }
}
