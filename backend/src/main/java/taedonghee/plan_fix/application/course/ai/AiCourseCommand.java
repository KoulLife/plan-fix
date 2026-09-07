package taedonghee.plan_fix.application.course.ai;

import java.time.LocalDate;
import java.util.List;

/**
 * [application] AI 코스 초안 생성 요청.
 *
 * 기간은 코스 생성 화면에서 이미 고른 값을 그대로 넘겨받는다(다시 묻지 않는다).
 */
public record AiCourseCommand(
	String region,
	String sigungu,
	LocalDate startDate,
	LocalDate endDate,
	List<CourseTheme> themes,
	CourseCompanion companion,
	/** 사용자가 "꼭 가고 싶다"고 고정한 장소. 비어 있어도 된다. */
	List<Long> anchorSpotIds
) {
	public AiCourseCommand {
		themes = themes == null ? List.of() : List.copyOf(themes);
		anchorSpotIds = anchorSpotIds == null ? List.of() : List.copyOf(anchorSpotIds);
		companion = companion == null ? CourseCompanion.COUPLE : companion;
	}
}
