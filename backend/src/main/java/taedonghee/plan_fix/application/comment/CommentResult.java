package taedonghee.plan_fix.application.comment;
import java.time.OffsetDateTime;
import taedonghee.plan_fix.infrastructure.board.CommentJpaEntity;
/** 댓글 처리 결과. API에 전달할 본문, 작성자, 부모 댓글 및 시각 정보를 담는다. */
public record CommentResult(Long commentId, Long userId, Long boardId, Long parentCommentId,
                            String content, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                            String authorName) {
    /** JPA 엔티티를 댓글 응답용 결과 객체로 변환한다. */
    static CommentResult from(CommentJpaEntity c, String authorName) {
        return new CommentResult(c.getCommentId(), c.getUserId(), c.getBoardId(), c.getParentCommentId(),
                c.getContent(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt(), authorName);
    }
}
