package taedonghee.plan_fix.application.course;

import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseVisibility;

import java.util.List;

/**
 * 코스 요청 Command
 */
public final class CourseCommand {

    private CourseCommand() {
    }

    /**
     * 코스 생성 요청 값
     */
    public record Create(
            String title,
            String description,
            String thumbnail,
            CourseVisibility visibility,
            List<CourseSpotModel> spots
    ) {
    }

    /**
     * 코스 수정 요청 값
     */
    public record Update(
            String title,
            String description,
            String thumbnail,
            CourseVisibility visibility,
            List<CourseSpotModel> spots
    ) {
    }
}
