package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.TourDataImageModel;
import taedonghee.plan_fix.domain.spot.TourDataImageRepository;
import taedonghee.plan_fix.domain.spot.TourDataSpotModel;
import taedonghee.plan_fix.domain.spot.TourDataSpotRepository;

import java.util.List;

/**
 * [application] 한 스팟의 이미지 저장을 하나의 트랜잭션으로 묶는다.
 *
 * 수집 서비스({@link TourDataImageCollectApplicationService})는 외부 API 호출 때문에
 * 의도적으로 트랜잭션을 열지 않는다. 하지만 저장 단계는 트랜잭션이 반드시 필요하다.
 * deleteByTourDataSpotId는 Spring Data의 derived delete라 "SELECT 후 건별 em.remove()"로 동작하는데,
 * 이 계열 메서드에는 Spring Data가 @Transactional을 붙여주지 않아
 * 트랜잭션 없이 호출하면 "No EntityManager with actual transaction available" 로 실패한다.
 * (save/saveAll은 SimpleJpaRepository가 자체 트랜잭션을 갖고 있어 단독 호출도 동작한다.)
 *
 * 별도 빈으로 분리한 이유는 같은 빈 내부 호출이 프록시를 타지 않아 @Transactional이 무시되기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class TourDataImagePersister {

	private final TourDataImageRepository tourDataImageRepository;
	private final TourDataSpotRepository tourDataSpotRepository;

	/**
	 * 기존 이미지를 지우고 새로 저장한 뒤 "시도했음" 도장까지 한 트랜잭션에서 처리한다.
	 * 셋이 한 단위로 커밋되므로, 중간에 실패하면 도장도 남지 않아 다음 실행에서 그대로 재시도된다.
	 */
	@Transactional
	public void replaceImages(TourDataSpotModel spot, List<TourDataImageModel> images) {
		if (!images.isEmpty()) {
			// 재수집 시 중복 누적을 막기 위해 기존 이미지를 지우고 새로 넣는다
			tourDataImageRepository.deleteByTourDataSpotId(spot.tourDataSpotId());
			tourDataImageRepository.saveAll(images);
		}

		// 이미지가 0장이어도 "시도했음"을 남겨서 다음 실행 때 다시 호출하지 않게 한다
		spot.markImageCollected();
		tourDataSpotRepository.save(spot);
	}
}
