package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * [domain] 스팟(spot)의 canonical 식별자 모델. JPA 등 프레임워크 의존 없이, 비즈니스 규칙만 표현한다.
 * 여러 데이터 소스(TourAPI 등)로부터 얻은 정보는 TourDataSpotModel처럼 출처별 모델이
 * spotId로 이 스팟을 참조하는 구조라, 지금은 식별자만 갖는다.
 */
public class SpotModel {

	private final Long spotId;

	private SpotModel(Long spotId) {
		this.spotId = spotId;
	}

	/** 신규 생성: 아직 저장 전이라 spotId가 없다 (DB가 채번) */
	public static SpotModel create() {
		return new SpotModel(null);
	}

	/** 이미 존재하는 스팟을 참조/복원할 때 */
	public static SpotModel of(Long spotId) {
		if (spotId == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "spotId는 필수입니다.");
		}
		return new SpotModel(spotId);
	}

	public Long spotId() {
		return spotId;
	}
}
