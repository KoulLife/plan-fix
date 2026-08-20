package taedonghee.plan_fix.infrastructure.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * [infrastructure] TourAPI areaBasedList2 응답 전체를 그대로 매핑하는 DTO.
 *
 * 응답 구조가 response.header / response.body.items.item 으로 3중 중첩이라 그대로 옮겨 담는다.
 * 결과가 0건일 때는 items가 배열이 아니라 빈 문자열("")로 오기 때문에, items()에서 null을
 * 방어적으로 처리한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaBasedListResponse(Response response) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(Header header, Body body) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Header(String resultCode, String resultMsg) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Body(Items items, int numOfRows, int pageNo, int totalCount) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Items(List<AreaBasedListItem> item) {}

	public boolean isSuccess() {
		return response != null
			&& response.header() != null
			&& "0000".equals(response.header().resultCode());
	}

	public int totalCount() {
		if (response == null || response.body() == null) {
			return 0;
		}
		return response.body().totalCount();
	}

	public List<AreaBasedListItem> items() {
		if (response == null || response.body() == null || response.body().items() == null
			|| response.body().items().item() == null) {
			return List.of();
		}
		return response.body().items().item();
	}
}
