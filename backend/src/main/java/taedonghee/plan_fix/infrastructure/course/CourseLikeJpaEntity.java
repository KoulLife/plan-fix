package taedonghee.plan_fix.infrastructure.course;

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
 * [infrastructure] CourseLike의 JPA 매핑 전용 엔티티.
 */
@Entity
@Table(
        name = "course_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_likes_user_course", columnNames = {"user_id", "course_id"}),
        indexes = @Index(name = "idx_course_likes_course_id", columnList = "course_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseLikeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_like_id")
    private Long courseLikeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Builder
    private CourseLikeJpaEntity(Long courseLikeId, Long userId, Long courseId, OffsetDateTime createdAt) {
        this.courseLikeId = courseLikeId;
        this.userId = userId;
        this.courseId = courseId;
        this.createdAt = createdAt;
    }
}
