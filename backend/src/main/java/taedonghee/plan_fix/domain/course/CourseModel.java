package taedonghee.plan_fix.domain.course;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 코스 Model
 */
public class CourseModel {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 1000;
    private static final int THUMBNAIL_MAX_LENGTH = 500;

    private final Long courseId;
    private final Long userId;
    private final String title;
    private final String description;
    private final String thumbnail;
    private final CourseVisibility visibility;
    private final CourseStatus status;
    private final long viewCount;
    private final long likeCount;
    private final List<CourseSpotModel> spots;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    /**
     * 코스 생성 및 복원 시 공통으로 사용하는 생성자
     */
    private CourseModel(Long courseId, Long userId, String title, String description, String thumbnail,
                        CourseVisibility visibility, CourseStatus status, long viewCount, long likeCount,
                        List<CourseSpotModel> spots, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        validate(userId, title, description, thumbnail, viewCount, likeCount, spots);
        this.courseId = courseId;
        this.userId = userId;
        this.title = normalizeRequired(title);
        this.description = normalizeOptional(description);
        this.thumbnail = normalizeOptional(thumbnail);
        this.visibility = visibility == null ? CourseVisibility.PRIVATE : visibility;
        this.status = status == null ? CourseStatus.ACTIVE : status;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.spots = List.copyOf(spots);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 코스 생성
     */
    public static CourseModel create(Long userId, String title, String description, String thumbnail,
                                     CourseVisibility visibility, List<CourseSpotModel> spots) {
        OffsetDateTime now = OffsetDateTime.now();
        return new CourseModel(null, userId, title, description, thumbnail, visibility, CourseStatus.ACTIVE,
                0L, 0L, spots, now, now);
    }

    /**
     * 저장된 코스 정보를 기반으로 CourseModel 복원
     * DB에 이미 저장되어 있던 Course 데이터를 다시 도메인 모델로 복원할 때 사용
     */
    public static CourseModel reconstruct(Long courseId, Long userId, String title, String description,
                                          String thumbnail, CourseVisibility visibility, CourseStatus status,
                                          long viewCount, long likeCount, List<CourseSpotModel> spots,
                                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new CourseModel(courseId, userId, title, description, thumbnail, visibility, status,
                viewCount, likeCount, spots, createdAt, updatedAt);
    }

    /**
     * 코스 기본 정보 및 포함된 spot 목록 수정
     */
    public CourseModel update(String title, String description, String thumbnail, CourseVisibility visibility,
                              List<CourseSpotModel> spots) {
        ensureActive();
        return new CourseModel(courseId, userId, title, description, thumbnail, visibility, status,
                viewCount, likeCount, spots, createdAt, OffsetDateTime.now());
    }

    /**
     * 코스 삭제 상태 변경
     */
    public CourseModel delete() {
        if (status == CourseStatus.DELETED) {
            return this;
        }
        return new CourseModel(courseId, userId, title, description, thumbnail, visibility, CourseStatus.DELETED,
                viewCount, likeCount, spots, createdAt, OffsetDateTime.now());
    }

    /**
     * 요청 사용자가 코스 작성자인지 검증
     */
    public void ensureOwner(Long requestedUserId) {
        if (!userId.equals(requestedUserId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "course access denied. courseId=" + courseId);
        }
    }

    /**
     * 삭제된 코스 수정 방지
     */
    private void ensureActive() {
        if (status == CourseStatus.DELETED) {
            throw new CoreException(ErrorType.CONFLICT, "deleted course. courseId=" + courseId);
        }
    }

    /**
     * 코스 필수값, 길이, 카운트, spot 목록 검증
     */
    private void validate(Long userId, String title, String description, String thumbnail,
                          long viewCount, long likeCount, List<CourseSpotModel> spots) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is required.");
        }
        if (title == null || title.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "title is required.");
        }
        if (title.strip().length() > TITLE_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "title must be 100 characters or less.");
        }
        if (description != null && description.strip().length() > DESCRIPTION_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "description must be 1000 characters or less.");
        }
        if (thumbnail != null && thumbnail.strip().length() > THUMBNAIL_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "thumbnail must be 500 characters or less.");
        }
        if (viewCount < 0 || likeCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "counts must not be negative.");
        }
        if (spots == null || spots.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "course must contain at least one spot.");
        }
        if (spots.stream().anyMatch(Objects::isNull)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "spots must not contain null.");
        }
        long distinctSpotCount = spots.stream().map(CourseSpotModel::spotId).distinct().count();
        if (distinctSpotCount != spots.size()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "course spots must not contain duplicates.");
        }
    }

    private String normalizeRequired(String value) {
        return value.strip();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    public Long courseId() { return courseId; }
    public Long userId() { return userId; }
    public String title() { return title; }
    public String description() { return description; }
    public String thumbnail() { return thumbnail; }
    public CourseVisibility visibility() { return visibility; }
    public CourseStatus status() { return status; }
    public long viewCount() { return viewCount; }
    public long likeCount() { return likeCount; }
    public List<CourseSpotModel> spots() { return spots; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }
}
