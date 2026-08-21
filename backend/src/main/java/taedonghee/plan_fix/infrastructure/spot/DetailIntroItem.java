package taedonghee.plan_fix.infrastructure.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * [infrastructure] TourAPI detailIntro2의 item 하나에 대한 원본 응답 DTO.
 *
 * detailIntro2는 contentTypeId별로 필드명이 전부 다르다(예: 관광지=usetime, 음식점=opentimefood).
 * 타입별 record를 나누는 대신 나올 수 있는 필드를 전부 여기 선언하고, Jackson이 응답에 없는 필드는
 * 알아서 null로 채우게 한다. 타입별로 어느 필드를 쓸지 고르는 건 호출부(application)의 책임이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DetailIntroItem(
	String contentid,
	String contenttypeid,

	// 12 관광지
	String infocenter,
	String parking,
	String usetime,
	String restdate,

	// 14 문화시설
	String infocenterculture,
	String parkingculture,
	String usetimeculture,
	String restdateculture,

	// 15 축제공연행사
	String sponsor1tel,
	String usetimefestival,
	String playtime,

	// 25 여행코스
	String infocentertourcourse,
	String taketime,

	// 28 레포츠
	String infocenterleports,
	String parkingleports,
	String usetimeleports,
	String restdateleports,

	// 32 숙박
	String infocenterlodging,
	String parkinglodging,
	String checkintime,
	String checkouttime,

	// 38 쇼핑
	String infocentershopping,
	String parkingshopping,
	String opentime,
	String restdateshopping,

	// 39 음식점
	String infocenterfood,
	String parkingfood,
	String opentimefood,
	String restdatefood,
	String firstmenu,
	String treatmenu,
	String lcnsno
) {
}
