package taedonghee.plan_fix.domain.spot;

/**
 * [domain] 공개 스팟 목록 조회의 검색 조건.
 * 필드가 null이면 그 조건은 필터링하지 않는다(전체 허용).
 */
public record SpotSearchCondition(String category, String region, String sigungu) {
}
