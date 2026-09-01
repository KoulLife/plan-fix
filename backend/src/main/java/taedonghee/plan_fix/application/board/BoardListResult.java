package taedonghee.plan_fix.application.board;

import taedonghee.plan_fix.domain.board.BoardModel;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * [application] 공개 게시글 목록 조회 결과.
 */
public record BoardListResult(List<Item> items, int offset, int size, long totalCount) {

    /**
     * 목록에 노출할 게시글 요약.
     */
    public record Item(
            Long boardId,
            String title,
            String thumbnail,
            Long userId,
            long likeCount,
            long viewCount,
            long commentCount,
            OffsetDateTime createdAt
    ) {
        public static Item from(BoardModel board) {
            return new Item(
                    board.boardId(),
                    board.title(),
                    board.thumbnail(),
                    board.userId(),
                    board.likeCount(),
                    board.viewCount(),
                    board.commentCount(),
                    board.createdAt()
            );
        }
    }
}
