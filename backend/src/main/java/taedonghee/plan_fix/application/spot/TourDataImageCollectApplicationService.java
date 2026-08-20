package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import taedonghee.plan_fix.domain.spot.TourDataImageModel;
import taedonghee.plan_fix.domain.spot.TourDataImageRepository;
import taedonghee.plan_fix.domain.spot.TourDataSpotModel;
import taedonghee.plan_fix.domain.spot.TourDataSpotRepository;
import taedonghee.plan_fix.infrastructure.spot.DetailImageItem;
import taedonghee.plan_fix.infrastructure.spot.TourApiClient;
import taedonghee.plan_fix.infrastructure.spot.TourApiProperties;
import taedonghee.plan_fix.infrastructure.spot.TourApiQuotaExceededException;

import java.util.List;

/**
 * [application] detailImage2로 각 관광데이터 스팟의 이미지 목록을 수집한다.
 *
 * 이미지는 TourAPI의 contentId로 연동된다. contentId로 이미 저장된 TourDataSpotModel을 찾아
 * 그 tourDataSpotId를 이미지에 연결하므로, 이 작업 전에 스팟 수집(collect)이 먼저 끝나 있어야 한다.
 *
 * 대상은 imageCollectedAt이 null인 건(아직 시도 안 한 건)만이다. 한 건을 처리하면 이미지가 0장이어도
 * imageCollectedAt을 찍어두기 때문에, 재실행하면 자연스럽게 남은 건부터 이어서 진행된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataImageCollectApplicationService {

	private final TourApiClient tourApiClient;
	private final TourApiProperties props;
	private final TourDataSpotRepository tourDataSpotRepository;
	private final TourDataImageRepository tourDataImageRepository;

	/**
	 * 의도적으로 @Transactional을 붙이지 않는다. 1000건 넘는 외부 API 호출을 한 트랜잭션으로 묶으면
	 * 도중에 예외가 나갈 때 그때까지 찍어둔 imageCollectedAt이 전부 롤백되어 진행 상황이 사라지고,
	 * DB 트랜잭션도 수 분간 열려 있게 된다. 건별로 커밋되게 두어야 중단 지점부터 재개할 수 있다.
	 */
	public CollectImageResult collect(String lDongSignguCd) {
		List<TourDataSpotModel> spots = tourDataSpotRepository.findBySigunguAndImageNotCollected(lDongSignguCd);
		log.info("이미지 수집 대상(미수집) 스팟: {}건", spots.size());

		int savedImages = 0;
		int spotsWithImages = 0;
		int failCount = 0;
		int processed = 0;
		boolean quotaExceeded = false;

		for (TourDataSpotModel spot : spots) {
			try {
				int saved = collectOne(spot);
				if (saved > 0) {
					savedImages += saved;
					spotsWithImages++;
				}
				processed++;
			} catch (TourApiQuotaExceededException e) {
				// 키 전체가 막힌 상태라 계속 호출해도 전부 실패한다. 남은 건은 다음 실행에서 이어서 처리.
				quotaExceeded = true;
				log.warn("일일 요청 한도 초과로 중단합니다. 처리 {}건 / 남은 {}건은 다음 실행 대상입니다.",
					processed, spots.size() - processed);
				break;
			} catch (Exception e) {
				failCount++;
				log.warn("이미지 수집 실패: contentId={}, {}", spot.contentId(), e.getMessage());
			}
			sleep();
		}

		int remaining = spots.size() - processed - failCount;
		log.info("이미지 수집 완료: 이미지 {}건 / 이미지 있는 스팟 {}건 / 실패 {}건 / 남은 {}건 (한도초과중단={})",
			savedImages, spotsWithImages, failCount, remaining, quotaExceeded);
		return new CollectImageResult(lDongSignguCd, spots.size(), processed, spotsWithImages, savedImages,
			failCount, remaining, quotaExceeded);
	}

	private int collectOne(TourDataSpotModel spot) {
		List<DetailImageItem> items = tourApiClient.fetchDetailImages(spot.contentId());

		List<TourDataImageModel> images = items.stream()
			.filter(item -> blankToNull(item.originimgurl()) != null)
			.map(item -> toDomain(spot, item))
			.toList();

		if (!images.isEmpty()) {
			// 재수집 시 중복 누적을 막기 위해 기존 이미지를 지우고 새로 넣는다
			tourDataImageRepository.deleteByTourDataSpotId(spot.tourDataSpotId());
			tourDataImageRepository.saveAll(images);
		}

		// 이미지가 0장이어도 "시도했음"을 남겨서 다음 실행 때 다시 호출하지 않게 한다
		spot.markImageCollected();
		tourDataSpotRepository.save(spot);

		return images.size();
	}

	private TourDataImageModel toDomain(TourDataSpotModel spot, DetailImageItem item) {
		return TourDataImageModel.builder()
			.tourDataSpotId(spot.tourDataSpotId())
			.contentId(spot.contentId())
			.imageName(blankToNull(item.imgname()))
			.originalImage(blankToNull(item.originimgurl()))
			.smallImage(blankToNull(item.smallimageurl()))
			.build();
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	private void sleep() {
		try {
			Thread.sleep(props.callIntervalMs());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("이미지 수집 중단됨", e);
		}
	}

	public record CollectImageResult(
		String lDongSignguCd,
		int targetSpotCount,
		int processedCount,
		int spotsWithImages,
		int savedImageCount,
		int failCount,
		int remainingCount,
		boolean quotaExceeded
	) {
	}
}
