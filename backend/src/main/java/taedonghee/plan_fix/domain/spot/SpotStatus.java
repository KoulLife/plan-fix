package taedonghee.plan_fix.domain.spot;

/**
 * [domain] 스팟의 노출 상태. 서비스가 소유하는 값이라 수집이 건드리지 않는다.
 */
public enum SpotStatus {

	/** 정상 노출 */
	ACTIVE,

	/** 관리자가 숨김 처리 */
	HIDDEN
}
