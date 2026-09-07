package taedonghee.plan_fix.infrastructure.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * [infrastructure] BoardLike의 JPA 매핑 전용 엔티티.
 */
@Entity
@Table(
        name = "board_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_board_likes_user_board", columnNames = {"user_id", "board_id"}),
        indexes = @Index(name = "idx_board_likes_board_id", columnList = "board_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLikeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_like_id")
    private Long boardLikeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Builder
    private BoardLikeJpaEntity(Long boardLikeId, Long userId, Long boardId, OffsetDateTime createdAt) {
        this.boardLikeId = boardLikeId;
        this.userId = userId;
        this.boardId = boardId;
        this.createdAt = createdAt;
    }
}
