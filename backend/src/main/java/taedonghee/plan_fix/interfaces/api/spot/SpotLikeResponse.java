package taedonghee.plan_fix.interfaces.api.spot;

import taedonghee.plan_fix.application.spot.SpotLikeResult;

/** [interfaces] 좋아요/취소 응답 DTO. */
public record SpotLikeResponse(boolean liked, long likeCount) {

    public static SpotLikeResponse from(SpotLikeResult result) {
        return new SpotLikeResponse(result.liked(), result.likeCount());
    }
}
