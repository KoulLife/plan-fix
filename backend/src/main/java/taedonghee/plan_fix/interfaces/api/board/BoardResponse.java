package taedonghee.plan_fix.interfaces.api.board;

import taedonghee.plan_fix.application.board.BoardResult;
import taedonghee.plan_fix.domain.board.BoardStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 게시글 API 응답 DTO
 */
public record BoardResponse(
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
        boolean isLiked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public BoardResponse(
            Long boardId, Long courseId, Long userId, String title, String content, String thumbnail,
            BoardStatus status, long viewCount, long likeCount, long commentCount, List<Image> images,
            OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        this(boardId, courseId, userId, title, content, thumbnail, status, viewCount, likeCount, commentCount, images, false, createdAt, updatedAt);
    }

    /**
     * Application Result를 HTTP 응답 DTO로 변환
     */
    public static BoardResponse from(BoardResult result) {
        return new BoardResponse(
                result.boardId(),
                result.courseId(),
                result.userId(),
                result.title(),
                result.content(),
                result.thumbnail(),
                result.status(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.images().stream().map(Image::from).toList(),
                result.isLiked(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    /**
     * 게시글 이미지 응답 값
     */
    public record Image(String imageUrl, String altText, int sequence) {

        /**
         * Application Result의 이미지 값을 응답 DTO로 변환
         */
        private static Image from(BoardResult.Image image) {
            return new Image(image.imageUrl(), image.altText(), image.sequence());
        }
    }
}
