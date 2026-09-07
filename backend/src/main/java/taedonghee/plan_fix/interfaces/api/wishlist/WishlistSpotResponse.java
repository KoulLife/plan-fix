package taedonghee.plan_fix.interfaces.api.wishlist;

import taedonghee.plan_fix.domain.spot.SpotModel;

/**
 * [interfaces] 위시리스트 스팟 응답 DTO
 */
public record WishlistSpotResponse(
        Long spotId,
        String title,
        String category,
        String region,
        String sigungu,
        String address,
        String thumbnail,
        long likeCount,
        boolean isLiked
) {
    public static WishlistSpotResponse from(SpotModel spot) {
        return new WishlistSpotResponse(
                spot.spotId(),
                spot.title(),
                spot.category(),
                spot.region(),
                spot.sigungu(),
                spot.address(),
                spot.thumbnail(),
                spot.likeCount(),
                true
        );
    }
}
