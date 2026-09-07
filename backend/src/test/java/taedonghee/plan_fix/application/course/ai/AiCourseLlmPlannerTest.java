package taedonghee.plan_fix.application.course.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLM 응답 파싱. 모델이 형식을 조금씩 어기는 게 일상이라(코드펜스, 머리말, 번호 접두사)
 * 그런 응답에서도 읽어낼 수 있는지 본다.
 */
class AiCourseLlmPlannerTest {

	private final AiCourseLlmPlanner planner = new AiCourseLlmPlanner(null);

	@Test
	@DisplayName("형식대로 온 응답을 Day별로 읽는다")
	void parses_well_formed_response() {
		String response = """
			DAY 1
			101 | 첫날 바다부터 보기 좋아요
			205 | 해변에서 걸어갈 수 있는 카페예요
			DAY 2
			310 | 아침 산책하기 좋은 호수예요
			""";

		LlmCoursePlan plan = planner.parse(response, 2);

		assertThat(plan.days()).hasSize(2);
		assertThat(plan.days().get(0).entries()).extracting(LlmCoursePlan.Entry::spotId).containsExactly(101L, 205L);
		assertThat(plan.days().get(0).entries().get(0).reason()).isEqualTo("첫날 바다부터 보기 좋아요");
		assertThat(plan.days().get(1).entries()).extracting(LlmCoursePlan.Entry::spotId).containsExactly(310L);
	}

	@Test
	@DisplayName("코드펜스와 머리말이 섞여 있어도 읽어낸다")
	void ignores_code_fences_and_preamble() {
		String response = """
			네, 요청하신 코스입니다!

			```
			DAY 1
			- 101 | 바다부터
			2. 205 | 카페
			```

			즐거운 여행 되세요.
			""";

		LlmCoursePlan plan = planner.parse(response, 1);

		assertThat(plan.days()).hasSize(1);
		assertThat(plan.days().get(0).entries()).extracting(LlmCoursePlan.Entry::spotId).containsExactly(101L, 205L);
	}

	@Test
	@DisplayName("요청한 일수를 넘겨 만들어낸 Day는 버린다")
	void drops_days_beyond_requested_count() {
		String response = """
			DAY 1
			101 | 첫날
			DAY 2
			202 | 둘째날
			DAY 3
			303 | 요청하지 않은 셋째날
			""";

		LlmCoursePlan plan = planner.parse(response, 2);

		assertThat(plan.days()).extracting(LlmCoursePlan.Day::dayNumber).containsExactly(1, 2);
	}

	@Test
	@DisplayName("읽을 수 있는 항목이 하나도 없으면 비어 있다고 판단한다")
	void detects_unusable_response() {
		LlmCoursePlan plan = planner.parse("죄송합니다. 코스를 만들 수 없습니다.", 2);

		assertThat(plan.isEmpty()).isTrue();
	}
}
