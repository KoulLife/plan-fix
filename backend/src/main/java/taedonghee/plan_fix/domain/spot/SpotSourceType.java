package taedonghee.plan_fix.domain.spot;

/**
 * [domain] 스팟이 어디서 왔는지. 소스 유래 필드를 누가 갱신할 권한이 있는지를 정한다.
 *
 * 예를 들어 source_type이 NATIVE인 스팟은 서비스에서 직접 등록한 것이므로
 * TourAPI 재수집이 제목·주소 같은 값을 덮어써서는 안 된다.
 */
public enum SpotSourceType {

	/** TourAPI 수집 */
	TOUR_API,

	/** 카카오 (예정) */
	KAKAO,

	/** 네이버 (예정) */
	NAVER,

	/** 서비스에서 직접 등록. 대응하는 소스 테이블 행이 없다. */
	NATIVE
}
