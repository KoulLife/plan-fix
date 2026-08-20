package taedonghee.plan_fix.infrastructure.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * [infrastructure] TourAPI areaBasedList2의 item 하나에 대한 원본 응답 DTO.
 *
 * 필드명은 API 응답 그대로(소문자, 예: contentid, mapx)를 써서 Jackson이 별도 어노테이션 없이
 * 매핑하게 한다. 지금 TourDataSpotModel 매핑에 필요한 필드만 선언했고, 나머지(addr2, areacode,
 * cat1~3, tel, mlevel, modifiedtime 등)는 @JsonIgnoreProperties로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaBasedListItem(
	String contentid,
	String contenttypeid,
	String title,
	String addr1,
	String mapx,
	String mapy,
	String firstimage,
	String createdtime,
	String zipcode,
	String lDongRegnCd,
	String lDongSignguCd,
	String lclsSystm3
) {
}
