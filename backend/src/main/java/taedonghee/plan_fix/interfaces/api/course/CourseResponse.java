package taedonghee.plan_fix.interfaces.api.course;

import taedonghee.plan_fix.application.course.CourseResult;
import taedonghee.plan_fix.domain.course.CourseStatus;
import taedonghee.plan_fix.domain.course.CourseVisibility;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 코스 API 응답 DTO
 */
public record CourseResponse(
        Long courseId,
        Long userId,
        String title,
        String description,
        String thumbnail,
        CourseVisibility visibility,
        CourseStatus status,
        long viewCount,
        long likeCount,
        List<Spot> spots,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * Application Result를 HTTP 응답 DTO로 변환
     */
    public static CourseResponse from(CourseResult result) {
        return new CourseResponse(result.courseId(), result.userId(), result.title(), result.description(),
                result.thumbnail(), result.visibility(), result.status(), result.viewCount(), result.likeCount(),
                result.spots().stream().map(Spot::from).toList(), result.createdAt(), result.updatedAt());
    }

    /**
     * 코스에 포함된 spot 응답 값
     */
    public record Spot(Long spotId, String memo, int sequence) {

        /**
         * Application Result의 spot 값을 응답 DTO로 변환
         */
        private static Spot from(CourseResult.Spot spot) {
            return new Spot(spot.spotId(), spot.memo(), spot.sequence());
        }
    }
}
