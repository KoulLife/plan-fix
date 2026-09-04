package taedonghee.plan_fix.infrastructure.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.course.CourseLikeModel;
import taedonghee.plan_fix.domain.course.CourseLikeRepository;

@Repository
@RequiredArgsConstructor
public class CourseLikeRepositoryImpl implements CourseLikeRepository {

    private final CourseLikeJpaRepository courseLikeJpaRepository;

    @Override
    public boolean existsByUserIdAndCourseId(Long userId, Long courseId) {
        return courseLikeJpaRepository.findByUserIdAndCourseId(userId, courseId).isPresent();
    }

    @Override
    public CourseLikeModel save(CourseLikeModel like) {
        CourseLikeJpaEntity saved = courseLikeJpaRepository.save(CourseLikeJpaEntity.builder()
                .courseLikeId(like.courseLikeId())
                .userId(like.userId())
                .courseId(like.courseId())
                .createdAt(like.createdAt())
                .build());
        return CourseLikeModel.reconstruct(saved.getCourseLikeId(), saved.getUserId(), saved.getCourseId(), saved.getCreatedAt());
    }

    @Override
    public boolean deleteByUserIdAndCourseId(Long userId, Long courseId) {
        return courseLikeJpaRepository.deleteByUserIdAndCourseId(userId, courseId) > 0;
    }
}
