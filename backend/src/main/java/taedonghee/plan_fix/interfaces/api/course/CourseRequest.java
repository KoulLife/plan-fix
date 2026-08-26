package taedonghee.plan_fix.interfaces.api.course;

import taedonghee.plan_fix.application.course.CourseCommand;
import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseVisibility;

import java.util.List;

/**
 * 코스 API 요청 DTO
 */
public final class CourseRequest {

    private CourseRequest() {
    }

    /**
     * 코스 생성 요청
     */
    public record Create(
            String title,
            String description,
            String thumbnail,
            CourseVisibility visibility,
            List<Spot> spots
    ) {
        /**
         * HTTP 요청 DTO를 Application Command로 변환
         */
        public CourseCommand.Create toCommand() {
            return new CourseCommand.Create(title, description, thumbnail, visibility, toSpotModels(spots));
        }
    }

    /**
     * 코스 수정 요청
     */
    public record Update(
            String title,
            String description,
            String thumbnail,
            CourseVisibility visibility,
            List<Spot> spots
    ) {
        /**
         * HTTP 요청 DTO를 Application Command로 변환
         */
        public CourseCommand.Update toCommand() {
            return new CourseCommand.Update(title, description, thumbnail, visibility, toSpotModels(spots));
        }
    }

    /**
     * 코스에 포함할 spot 요청 값
     */
    public record Spot(Long spotId, String memo) {
    }

    /**
     * 요청 spot 목록을 도메인 값 객체로 변환
     */
    private static List<CourseSpotModel> toSpotModels(List<Spot> spots) {
        if (spots == null) {
            return null;
        }
        return spots.stream()
                .map(spot -> spot == null ? null : new CourseSpotModel(spot.spotId(), spot.memo()))
                .toList();
    }
}
