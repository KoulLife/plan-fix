package taedonghee.plan_fix.infrastructure.board;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/** comments 테이블 JPA 매핑. parentCommentId로 댓글과 대댓글 관계를 표현한다. */
@Entity
@Table(name="comments", indexes={@Index(name="idx_comments_board_status", columnList="board_id,status"), @Index(name="idx_comments_parent_comment_id", columnList="parent_comment_id")})
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class CommentJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="comment_id") private Long commentId;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="board_id", nullable=false) private Long boardId;
    /** 일반 댓글은 null, 대댓글은 부모 댓글 ID를 저장한다. */
    @Column(name="parent_comment_id") private Long parentCommentId;
    @Column(nullable=false, length=1000) private String content;
    @Column(nullable=false, length=20) private String status;
    @Column(nullable=false, columnDefinition="timestamptz") private OffsetDateTime createdAt;
    @Column(nullable=false, columnDefinition="timestamptz") private OffsetDateTime updatedAt;
    @Builder public CommentJpaEntity(Long commentId, Long userId, Long boardId, Long parentCommentId, String content, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) { this.commentId=commentId; this.userId=userId; this.boardId=boardId; this.parentCommentId=parentCommentId; this.content=content; this.status=status; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    /** 본문 수정과 함께 마지막 수정 시각을 갱신한다. */
    public void update(String content) { this.content=content; this.updatedAt=OffsetDateTime.now(); }
    /** 댓글 행과 부모 참조를 보존하는 논리 삭제. 활성 댓글 목록에서는 제외된다. */
    public void delete() { this.status="DELETED"; this.updatedAt=OffsetDateTime.now(); }
}
