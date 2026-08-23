package taedonghee.plan_fix.domain.spot;

/**
 * [domain] 공개 스팟 목록 조회의 정렬 기준.
 */
public enum SpotSortType {

	/** 최근 등록/수집된 순 (spotId 내림차순) */
	LATEST,

	/** 인기순: like_count*0.9 + view_count*0.1 내림차순, 동점이면 spotId 내림차순 */
	POPULAR
}
