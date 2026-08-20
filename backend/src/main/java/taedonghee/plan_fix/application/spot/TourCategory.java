package taedonghee.plan_fix.application.spot;

import java.util.Arrays;

/**
 * [application] TourAPI contentTypeId를 서비스가 노출할 한글 명칭으로 옮긴다.
 *
 * spots.category는 서비스가 그대로 보여주는 값이라 코드가 아닌 명칭으로 저장한다.
 * 코드 자체는 tour_data_spots.category에 원본 그대로 남아 있다.
 */
public enum TourCategory {

	ATTRACTION("12", "관광지"),
	CULTURAL_FACILITY("14", "문화시설"),
	FESTIVAL("15", "축제공연행사"),
	TRAVEL_COURSE("25", "여행코스"),
	LEPORTS("28", "레포츠"),
	LODGING("32", "숙박"),
	SHOPPING("38", "쇼핑"),
	RESTAURANT("39", "음식점");

	private static final String UNKNOWN = "기타";

	private final String contentTypeId;
	private final String displayName;

	TourCategory(String contentTypeId, String displayName) {
		this.contentTypeId = contentTypeId;
		this.displayName = displayName;
	}

	/**
	 * spots.category는 NOT NULL이라 모르는 코드도 값을 내놔야 한다.
	 * TourAPI가 새 contentTypeId를 추가해도 수집이 멈추지 않도록 "기타"로 떨어뜨린다.
	 */
	public static String displayNameOf(String contentTypeId) {
		return Arrays.stream(values())
			.filter(category -> category.contentTypeId.equals(contentTypeId))
			.map(category -> category.displayName)
			.findFirst()
			.orElse(UNKNOWN);
	}

	public String contentTypeId() {
		return contentTypeId;
	}

	public String displayName() {
		return displayName;
	}
}
