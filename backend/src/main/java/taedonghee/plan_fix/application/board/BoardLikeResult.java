package taedonghee.plan_fix.application.board;

/**
 * [application] 게시글 좋아요/취소 결과.
 */
public record BoardLikeResult(boolean liked, long likeCount) {
}
