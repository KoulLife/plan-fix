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
 * course_spots 테이블 JPA 매핑 엔티티
 * 코스에 포함된 spot의 일차, 순서와 메모를 저장한다
 */
@Entity
@Table(
        name = "course_spots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_spots_course_day_sequence", columnNames = {"course_id", "day_number", "sequence"})
        },
        indexes = {
                @Index(name = "idx_course_spots_course_id", columnList = "course_id"),
                @Index(name = "idx_course_spots_spot_id", columnList = "spot_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSpotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_spot_id")
    private Long courseSpotId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Builder
    private CourseSpotJpaEntity(Long courseSpotId, Long courseId, Long spotId, int dayNumber, int sequence, String memo,
                                OffsetDateTime createdAt) {
        this.courseSpotId = courseSpotId;
        this.courseId = courseId;
        this.spotId = spotId;
        this.dayNumber = dayNumber;
        this.sequence = sequence;
        this.memo = memo;
        this.createdAt = createdAt;
    }
}
