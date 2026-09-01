package taedonghee.plan_fix.infrastructure.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseRepository;
import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * CourseRepository JPA 구현체
 */
@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseJpaRepository courseJpaRepository;
    private final CourseSpotJpaRepository courseSpotJpaRepository;

    /**
     * 코스 저장 처리
     */
    @Override
    public CourseModel save(CourseModel course) {
        CourseJpaEntity saved = courseJpaRepository.save(toEntity(course));
        // 코스에 포함된 spot 순서를 단순하게 맞추기 위해 기존 목록을 지우고 다시 저장한다
        courseSpotJpaRepository.deleteByCourseId(saved.getCourseId());
        courseSpotJpaRepository.flush();
        courseSpotJpaRepository.saveAll(IntStream.range(0, course.spots().size())
                .mapToObj(index -> CourseSpotJpaEntity.builder()
                        .courseId(saved.getCourseId())
                        .spotId(course.spots().get(index).spotId())
                        .memo(course.spots().get(index).memo())
                        .sequence(index)
                        .createdAt(OffsetDateTime.now())
                        .build())
                .toList());
        return toDomain(saved);
    }

    /**
     * course_id 기반 코스 단건 조회 처리
     */
    @Override
    public Optional<CourseModel> findById(Long courseId) {
        return courseJpaRepository.findById(courseId).map(this::toDomain);
    }

    /**
     * user_id 기반 활성 코스 목록 조회 처리
     */
    @Override
    public List<CourseModel> findActiveByUserId(Long userId) {
        return courseJpaRepository.findByUserIdAndStatusOrderByCourseIdDesc(userId, CourseStatus.ACTIVE)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     */
    private CourseJpaEntity toEntity(CourseModel course) {
        return CourseJpaEntity.builder()
                .courseId(course.courseId())
                .userId(course.userId())
                .title(course.title())
                .description(course.description())
                .thumbnail(course.thumbnail())
                .visibility(course.visibility())
                .status(course.status())
                .viewCount(course.viewCount())
                .likeCount(course.likeCount())
                .createdAt(course.createdAt())
                .updatedAt(course.updatedAt())
                .build();
    }

    /**
     * JPA 엔티티와 course_spots 목록을 도메인 모델로 변환
     */
    private CourseModel toDomain(CourseJpaEntity entity) {
        List<CourseSpotModel> spots = courseSpotJpaRepository.findByCourseIdOrderBySequenceAsc(entity.getCourseId())
                .stream()
                .map(courseSpot -> new CourseSpotModel(courseSpot.getSpotId(), courseSpot.getMemo()))
                .toList();
        return CourseModel.reconstruct(entity.getCourseId(), entity.getUserId(), entity.getTitle(),
                entity.getDescription(), entity.getThumbnail(), entity.getVisibility(), entity.getStatus(),
                entity.getViewCount(), entity.getLikeCount(), spots, entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
