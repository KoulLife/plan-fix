package taedonghee.plan_fix.application.board;

import taedonghee.plan_fix.domain.board.BoardImageModel;
import taedonghee.plan_fix.domain.board.BoardModel;
import taedonghee.plan_fix.domain.board.BoardStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 게시글 처리 결과
 */
public record BoardResult(
        Long boardId,
        Long courseId,
        Long userId,
        String title,
        String content,
        String thumbnail,
        BoardStatus status,
        long viewCount,
        long likeCount,
        long commentCount,
        List<Image> images,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * BoardModel을 응답용 결과 객체로 변환
     */
    public static BoardResult from(BoardModel board) {
        return new BoardResult(
                board.boardId(),
                board.courseId(),
                board.userId(),
                board.title(),
                board.content(),
                board.thumbnail(),
                board.status(),
                board.viewCount(),
                board.likeCount(),
                board.commentCount(),
                IntStream.range(0, board.images().size())
                        .mapToObj(index -> Image.from(board.images().get(index), index))
                        .toList(),
                board.createdAt(),
                board.updatedAt()
        );
    }

    /**
     * 게시글 이미지 응답 정보
     */
    public record Image(String imageUrl, String altText, int sequence) {

        /**
         * BoardImageModel을 순서값이 포함된 응답 정보로 변환
         */
        private static Image from(BoardImageModel image, int sequence) {
            return new Image(image.imageUrl(), image.altText(), sequence);
        }
    }
}
