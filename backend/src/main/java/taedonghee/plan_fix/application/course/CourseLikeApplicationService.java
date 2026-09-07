package taedonghee.plan_fix.application.course;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.course.CourseLikeModel;
import taedonghee.plan_fix.domain.course.CourseLikeRepository;
import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseRepository;
import taedonghee.plan_fix.domain.course.CourseStatus;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

@Service
@RequiredArgsConstructor
public class CourseLikeApplicationService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;

    @Transactional
    public CourseLikeResult like(Long userId, Long courseId) {
        CourseModel course = getActiveCourseOrThrow(courseId);

        if (courseLikeRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return new CourseLikeResult(true, course.likeCount());
        }

        try {
            courseLikeRepository.save(CourseLikeModel.create(userId, courseId));
        } catch (DataIntegrityViolationException e) {
            return new CourseLikeResult(true, course.likeCount());
        }

        courseRepository.incrementLikeCount(courseId);
        return new CourseLikeResult(true, course.likeCount() + 1);
    }

    @Transactional
    public CourseLikeResult unlike(Long userId, Long courseId) {
        CourseModel course = getActiveCourseOrThrow(courseId);

        boolean deleted = courseLikeRepository.deleteByUserIdAndCourseId(userId, courseId);
        if (!deleted) {
            return new CourseLikeResult(false, course.likeCount());
        }

        courseRepository.decrementLikeCount(courseId);
        return new CourseLikeResult(false, Math.max(course.likeCount() - 1, 0));
    }

    private CourseModel getActiveCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .filter(c -> c.status() == CourseStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "course not found. courseId=" + courseId));
    }
}
