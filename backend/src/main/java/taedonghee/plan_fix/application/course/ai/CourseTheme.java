package taedonghee.plan_fix.application.course.ai;

import java.util.Map;

/**
 * [application] 사용자가 고르는 여행 테마. 테마마다 카테고리 선호 가중치를 갖는다.
 *
 * 좋아요·조회수가 거의 쌓이지 않은 상태라 인기순 정렬이 사실상 무작위다.
 * 그래서 "무엇을 좋아하는지"는 이 테마 선택에서 받아 후보 점수에 반영한다.
 */
public enum CourseTheme {

	HEALING(Map.of(
		"관광지", 1.0,
		"카페/음료", 0.6,
		"문화시설", 0.5,
		"음식점", 0.4,
		"레포츠", 0.2
	)),
	FOOD(Map.of(
		"음식점", 1.0,
		"카페/음료", 0.6,
		"관광지", 0.4,
		"쇼핑", 0.3,
		"문화시설", 0.2
	)),
	CAFE(Map.of(
		"카페/음료", 1.0,
		"관광지", 0.6,
		"음식점", 0.5,
		"쇼핑", 0.3,
		"문화시설", 0.2
	)),
	ACTIVITY(Map.of(
		"레포츠", 1.0,
		"관광지", 0.7,
		"음식점", 0.4,
		"카페/음료", 0.3,
		"축제공연행사", 0.3
	)),
	CULTURE(Map.of(
		"문화시설", 1.0,
		"관광지", 0.7,
		"축제공연행사", 0.6,
		"음식점", 0.4,
		"카페/음료", 0.3
	));

	private final Map<String, Double> categoryWeights;

	CourseTheme(Map<String, Double> categoryWeights) {
		this.categoryWeights = categoryWeights;
	}

	/** 이 테마에서 해당 카테고리의 선호도. 정의되지 않은 카테고리는 약하게 취급한다. */
	public double weightOf(String category) {
		return categoryWeights.getOrDefault(category, 0.15);
	}
}
