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
 * board_images 테이블 JPA 매핑 엔티티
 * 게시글 본문에 첨부된 이미지 URL과 순서를 저장한다
 */
@Entity
@Table(
        name = "board_images",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_board_images_board_sequence", columnNames = {"board_id", "sequence"})
        },
        indexes = {
                @Index(name = "idx_board_images_board_id", columnList = "board_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardImageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_image_id")
    private Long boardImageId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Builder
    private BoardImageJpaEntity(Long boardImageId, Long boardId, String imageUrl, String altText, int sequence,
                                OffsetDateTime createdAt) {
        this.boardImageId = boardImageId;
        this.boardId = boardId;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.sequence = sequence;
        this.createdAt = createdAt;
    }
}
