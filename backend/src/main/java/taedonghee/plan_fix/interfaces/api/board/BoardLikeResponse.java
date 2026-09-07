package taedonghee.plan_fix.interfaces.api.board;

import taedonghee.plan_fix.application.board.BoardLikeResult;

/**
 * [interfaces] 게시글 좋아요 응답 DTO.
 */
public record BoardLikeResponse(boolean liked, long likeCount) {

    public static BoardLikeResponse from(BoardLikeResult result) {
        return new BoardLikeResponse(result.liked(), result.likeCount());
    }
}
