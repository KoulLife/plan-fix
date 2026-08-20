package taedonghee.plan_fix.interfaces.api.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;
import taedonghee.plan_fix.application.spot.TourDataImageCollectApplicationService;
import taedonghee.plan_fix.application.spot.TourDataInfoCollectApplicationService;
import taedonghee.plan_fix.application.spot.TourDataSpotCollectApplicationService;

/**
 * [interfaces] 관광데이터 수집용 관리자 API.
 * 컨트롤러는 HTTP 변환만 담당하고, 처리는 application에 위임한다.
 */
@RestController
@RequestMapping("/api/v1/admin/spots")
@RequiredArgsConstructor
public class SpotAdminController {

	private static final long COLLECT_TIMEOUT_MS = 1_800_000L;

	private final TourDataSpotCollectApplicationService tourDataSpotCollectApplicationService;
	private final TourDataImageCollectApplicationService tourDataImageCollectApplicationService;
	private final TourDataInfoCollectApplicationService tourDataInfoCollectApplicationService;

	/** 예: POST /api/v1/admin/spots/collect?lDongRegnCd=51&lDongSignguCd=150 (강원 강릉) */
	@PostMapping("/collect")
	public WebAsyncTask<TourDataSpotCollectApplicationService.CollectResult> collect(
		@RequestParam String lDongRegnCd,
		@RequestParam String lDongSignguCd
	) {
		return new WebAsyncTask<>(
			COLLECT_TIMEOUT_MS,
			() -> tourDataSpotCollectApplicationService.collect(lDongRegnCd, lDongSignguCd)
		);
	}

	/**
	 * 수집된 스팟들의 이미지를 detailImage2로 채운다. contentId로 연동되므로 collect 이후에 호출해야 한다.
	 * 예: POST /api/v1/admin/spots/collect-images?lDongSignguCd=150
	 */
	@PostMapping("/collect-images")
	public WebAsyncTask<TourDataImageCollectApplicationService.CollectImageResult> collectImages(
		@RequestParam String lDongSignguCd
	) {
		return new WebAsyncTask<>(
			COLLECT_TIMEOUT_MS,
			() -> tourDataImageCollectApplicationService.collect(lDongSignguCd)
		);
	}

	/**
	 * 수집된 스팟들의 상세 정보를 detailIntro2로 채운다.
	 * contentId와 category를 저장된 스팟에서 가져오므로 collect 이후에 호출해야 한다.
	 * 예: POST /api/v1/admin/spots/collect-info?lDongSignguCd=150
	 */
	@PostMapping("/collect-info")
	public WebAsyncTask<TourDataInfoCollectApplicationService.CollectInfoResult> collectInfo(
		@RequestParam String lDongSignguCd
	) {
		return new WebAsyncTask<>(
			COLLECT_TIMEOUT_MS,
			() -> tourDataInfoCollectApplicationService.collect(lDongSignguCd)
		);
	}
}
