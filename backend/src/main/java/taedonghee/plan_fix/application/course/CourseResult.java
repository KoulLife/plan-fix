package taedonghee.plan_fix.application.course;

import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseStatus;
import taedonghee.plan_fix.domain.course.CourseVisibility;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 코스 처리 결과
 */
public record CourseResult(
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
     * CourseModel을 응답용 결과 객체로 변환
     */
    public static CourseResult from(CourseModel course) {
        List<Spot> spots = IntStream.range(0, course.spots().size())
                .mapToObj(index -> Spot.from(course.spots().get(index), index))
                .toList();
        return new CourseResult(course.courseId(), course.userId(), course.title(), course.description(),
                course.thumbnail(), course.visibility(), course.status(), course.viewCount(), course.likeCount(),
                spots, course.createdAt(), course.updatedAt());
    }

    /**
     * 코스에 포함된 spot 응답 정보
     */
    public record Spot(Long spotId, String memo, int sequence) {

        private static Spot from(CourseSpotModel spot, int sequence) {
            return new Spot(spot.spotId(), spot.memo(), sequence);
        }
    }
}
