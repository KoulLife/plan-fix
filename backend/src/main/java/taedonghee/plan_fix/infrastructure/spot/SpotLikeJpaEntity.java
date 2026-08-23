package taedonghee.plan_fix.infrastructure.spot;

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
 * [infrastructure] SpotLike의 JPA 매핑 전용 엔티티. spots 쪽 다른 엔티티들과 같은 관례로
 * user_id/spot_id를 @ManyToOne 없이 순수 컬럼으로 둔다(infrastructure.user에 대한 의존을 만들지 않기 위함).
 */
@Entity
@Table(
        name = "spot_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_spot_likes_user_spot", columnNames = {"user_id", "spot_id"}),
        indexes = @Index(name = "idx_spot_likes_spot_id", columnList = "spot_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotLikeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spot_like_id")
    private Long spotLikeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Builder
    private SpotLikeJpaEntity(Long spotLikeId, Long userId, Long spotId, OffsetDateTime createdAt) {
        this.spotLikeId = spotLikeId;
        this.userId = userId;
        this.spotId = spotId;
        this.createdAt = createdAt;
    }
}
