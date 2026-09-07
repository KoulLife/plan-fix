package taedonghee.plan_fix.application.course.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotSourceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLM 출력은 믿지 않는다는 전제의 안전망이라, 여기가 뚫리면 없는 장소가 그대로
 * 사용자에게 나가거나 저장 시 DB 제약에 걸린다. 화면으로는 확인이 어려운 영역이다.
 */
class AiCoursePlanValidatorTest {

	private final AiCoursePlanValidator validator = new AiCoursePlanValidator();

	@Test
	@DisplayName("후보에 없는 id를 지어내면 그 항목만 걸러낸다")
	void drops_hallucinated_spot_ids() {
		List<SpotModel> candidates = List.of(
			spot(1L, "경포해변", "관광지", 37.80, 128.90),
			spot(2L, "안목커피거리", "카페/음료", 37.77, 128.94)
		);
		LlmCoursePlan plan = new LlmCoursePlan(List.of(
			new LlmCoursePlan.Day(1, List.of(
				new LlmCoursePlan.Entry(1L, "바다부터"),
				new LlmCoursePlan.Entry(999L, "존재하지 않는 장소"),
				new LlmCoursePlan.Entry(2L, "커피 한 잔")
			))
		));

		Optional<AiCoursePlanValidator.ValidatedPlan> result =
			validator.validate(plan, candidates, List.of(), 1);

		assertThat(result).isPresent();
		assertThat(result.get().days().get(0)).extracting(SpotModel::spotId).containsExactly(1L, 2L);
	}

	@Test
	@DisplayName("같은 장소를 여러 날에 넣으면 뒤쪽을 제거한다")
	void removes_duplicate_spots_across_days() {
		List<SpotModel> candidates = List.of(
			spot(1L, "경포해변", "관광지", 37.80, 128.90),
			spot(2L, "안목커피거리", "카페/음료", 37.77, 128.94)
		);
		LlmCoursePlan plan = new LlmCoursePlan(List.of(
			new LlmCoursePlan.Day(1, List.of(new LlmCoursePlan.Entry(1L, "첫날"), new LlmCoursePlan.Entry(2L, "카페"))),
			new LlmCoursePlan.Day(2, List.of(new LlmCoursePlan.Entry(1L, "또 경포해변")))
		));

		Optional<AiCoursePlanValidator.ValidatedPlan> result =
			validator.validate(plan, candidates, List.of(), 2);

		assertThat(result).isPresent();
		assertThat(result.get().days().get(0)).extracting(SpotModel::spotId).containsExactly(1L, 2L);
		assertThat(result.get().days().get(1)).isEmpty();
	}

	@Test
	@DisplayName("고정 장소가 응답에서 빠지면 되살린다")
	void restores_missing_anchor() {
		SpotModel anchor = spot(10L, "꼭 갈 곳", "관광지", 37.80, 128.90);
		List<SpotModel> candidates = List.of(
			spot(1L, "경포해변", "관광지", 37.81, 128.91),
			spot(2L, "안목커피거리", "카페/음료", 37.79, 128.93)
		);
		// LLM이 고정 장소를 무시한 응답
		LlmCoursePlan plan = new LlmCoursePlan(List.of(
			new LlmCoursePlan.Day(1, List.of(new LlmCoursePlan.Entry(1L, "바다"), new LlmCoursePlan.Entry(2L, "카페")))
		));

		Optional<AiCoursePlanValidator.ValidatedPlan> result =
			validator.validate(plan, candidates, List.of(anchor), 1);

		assertThat(result).isPresent();
		assertThat(result.get().days().get(0)).extracting(SpotModel::spotId).contains(10L);
	}

	@Test
	@DisplayName("하루 안에서 40km 넘게 벌어지면 폴백시킨다")
	void rejects_plan_with_too_much_intra_day_distance() {
		List<SpotModel> candidates = List.of(
			spot(1L, "강릉", "관광지", 37.80, 128.90),
			spot(2L, "춘천", "관광지", 37.87, 127.73) // 약 100km
		);
		LlmCoursePlan plan = new LlmCoursePlan(List.of(
			new LlmCoursePlan.Day(1, List.of(new LlmCoursePlan.Entry(1L, "a"), new LlmCoursePlan.Entry(2L, "b")))
		));

		assertThat(validator.validate(plan, candidates, List.of(), 1)).isEmpty();
	}

	@Test
	@DisplayName("배치가 사실상 비어 있으면 폴백시킨다")
	void rejects_empty_plan() {
		List<SpotModel> candidates = List.of(spot(1L, "경포해변", "관광지", 37.80, 128.90));
		LlmCoursePlan plan = new LlmCoursePlan(List.of(
			new LlmCoursePlan.Day(1, List.of(new LlmCoursePlan.Entry(999L, "없는 장소")))
		));

		assertThat(validator.validate(plan, candidates, List.of(), 1)).isEmpty();
	}

	@Test
	@DisplayName("LLM이 써준 이유를 장소별로 보존한다")
	void keeps_llm_reasons() {
		List<SpotModel> candidates = List.of(spot(1L, "경포해변", "관광지", 37.80, 128.90));
		LlmCoursePlan plan = new LlmCoursePlan(List.of(
			new LlmCoursePlan.Day(1, List.of(new LlmCoursePlan.Entry(1L, "첫날 도착해서 바로 가기 좋아요")))
		));

		Optional<AiCoursePlanValidator.ValidatedPlan> result =
			validator.validate(plan, candidates, List.of(), 1);

		assertThat(result).isPresent();
		assertThat(result.get().reasons().get(1L)).isEqualTo("첫날 도착해서 바로 가기 좋아요");
	}

	private static SpotModel spot(Long id, String title, String category, double lat, double lng) {
		return SpotModel.builder()
			.spotId(id)
			.sourceType(SpotSourceType.TOUR_API)
			.attributes(new SpotModel.SourceAttributes(
				title, category, "51", "150", "강원특별자치도",
				BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), "thumb.jpg", "설명"))
			.build();
	}
}
