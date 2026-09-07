package taedonghee.plan_fix.application.course.ai;

import java.util.List;

/**
 * [application] LLM이 배치한 결과를 파싱한 것. 아직 검증 전이라 그대로 믿으면 안 된다.
 * 없는 spotId가 섞여 있거나, 고정 장소가 빠졌거나, 같은 장소가 중복될 수 있다.
 */
public record LlmCoursePlan(List<Day> days) {

	public record Day(int dayNumber, List<Entry> entries) {
	}

	public record Entry(Long spotId, String reason) {
	}

	public boolean isEmpty() {
		return days == null || days.stream().allMatch(day -> day.entries().isEmpty());
	}
}
