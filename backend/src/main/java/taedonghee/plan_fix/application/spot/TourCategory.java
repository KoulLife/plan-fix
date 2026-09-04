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

	/**
	 * lclsSystm3(음식 소분류) 접두사. FD05xxxx는 카페/찻집/기타음료점이다 — TourAPI 분류체계 문서 참고.
	 * 음식점(39) 안에서 카페/음료를 별도 카테고리로 빼내는 데만 쓴다.
	 */
	private static final String CAFE_LCLS_PREFIX = "FD05";
	private static final String CAFE_DISPLAY_NAME = "카페/음료";

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

	/**
	 * 음식점(39)은 lclsSystm3 소분류로 카페/음료를 구분해 별도 카테고리로 내린다.
	 * lcls가 없거나(구버전 데이터) 카페 계열이 아니면 기존 displayNameOf(contentTypeId)와 동일하다.
	 */
	public static String displayNameOf(String contentTypeId, String lcls) {
		if (RESTAURANT.contentTypeId.equals(contentTypeId) && lcls != null && lcls.startsWith(CAFE_LCLS_PREFIX)) {
			return CAFE_DISPLAY_NAME;
		}
		return displayNameOf(contentTypeId);
	}

	public String contentTypeId() {
		return contentTypeId;
	}

	public String displayName() {
		return displayName;
	}
}
