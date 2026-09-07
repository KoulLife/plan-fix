package taedonghee.plan_fix.domain.course;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * [domain] 사용자가 코스를 좋아요한 이력 한 건.
 */
public class CourseLikeModel {

    private final Long courseLikeId;
    private final Long userId;
    private final Long courseId;
    private final OffsetDateTime createdAt;

    private CourseLikeModel(Long courseLikeId, Long userId, Long courseId, OffsetDateTime createdAt) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (courseId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "courseId는 필수입니다.");
        }
        this.courseLikeId = courseLikeId;
        this.userId = userId;
        this.courseId = courseId;
        this.createdAt = createdAt;
    }

    public static CourseLikeModel create(Long userId, Long courseId) {
        return new CourseLikeModel(null, userId, courseId, OffsetDateTime.now());
    }

    public static CourseLikeModel reconstruct(Long courseLikeId, Long userId, Long courseId, OffsetDateTime createdAt) {
        return new CourseLikeModel(courseLikeId, userId, courseId, createdAt);
    }

    public Long courseLikeId() { return courseLikeId; }
    public Long userId() { return userId; }
    public Long courseId() { return courseId; }
    public OffsetDateTime createdAt() { return createdAt; }
}
