package taedonghee.plan_fix.application.spot;

import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.TourDataSpotModel;

/**
 * [application] 파싱 결과 한 건 = canonical 스팟(SpotModel) 1개 + 그 스팟의 관광데이터(TourDataSpotModel) 1개.
 *
 * 두 모델을 한 쌍으로 묶어 두는 이유는, 파싱 시점에는 SpotModel이 아직 저장 전이라 spotId(DB 채번)가
 * 없어서 TourDataSpotModel.spotId로 연결할 수 없기 때문이다. 저장 단계에서 SpotModel을 먼저 저장해
 * id를 얻은 뒤 {@link TourDataSpotModel#assignSpotId(Long)}로 연결한다.
 */
public record CollectedSpot(SpotModel spot, TourDataSpotModel tourDataSpot) {

	/** SpotModel 저장 후 발급된 id를 짝인 TourDataSpotModel에 연결한다. */
	public void link(Long savedSpotId) {
		tourDataSpot.assignSpotId(savedSpotId);
	}
}
