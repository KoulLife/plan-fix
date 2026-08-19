package taedonghee.plan_fix.infrastructure.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * [infrastructure] TourAPI detailImage2의 item 하나에 대한 원본 응답 DTO.
 *
 * 필드명은 API 응답 그대로(소문자, 예: originimgurl)를 써서 Jackson이 별도 어노테이션 없이 매핑하게 한다.
 * 사용하지 않는 필드(cpyrhtDivCd, serialnum)는 @JsonIgnoreProperties로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DetailImageItem(
	String contentid,
	String imgname,
	String originimgurl,
	String smallimageurl
) {
}
