package taedonghee.plan_fix.application.spot;

/**
 * [application] 공개 스팟 목록 조회 요청. category/region/sigungu는 없으면(null) 필터링하지 않는다.
 * sort는 "latest"|"popular" 문자열이며, null이면 latest로 취급한다.
 */
public record SpotListQuery(String category, String region, String sigungu, String sort, int offset, int size) {
}
