package taedonghee.plan_fix.interfaces.api.board;

import taedonghee.plan_fix.application.board.BoardListResult;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * [interfaces] 공개 게시글 목록 조회 응답 DTO.
 */
public record BoardListResponse(List<Item> items, int offset, int size, long totalCount) {

    public static BoardListResponse from(BoardListResult result) {
        List<Item> items = result.items().stream().map(Item::from).toList();
        return new BoardListResponse(items, result.offset(), result.size(), result.totalCount());
    }

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
        public static Item from(BoardListResult.Item item) {
            return new Item(
                    item.boardId(),
                    item.title(),
                    item.thumbnail(),
                    item.userId(),
                    item.likeCount(),
                    item.viewCount(),
                    item.commentCount(),
                    item.createdAt()
            );
        }
    }
}
