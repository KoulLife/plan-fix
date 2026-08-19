package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.TourDataSpotModel;
import taedonghee.plan_fix.domain.spot.TourDataSpotRepository;
import taedonghee.plan_fix.infrastructure.spot.AreaBasedListItem;
import taedonghee.plan_fix.infrastructure.spot.TourApiClient;
import taedonghee.plan_fix.infrastructure.spot.TourApiProperties;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * [application] areaBasedList2를 lDongRegnCd(법정동 시도코드) / lDongSignguCd(법정동 시군구코드)
 * 기준으로 전체 contentType × 전체 페이지를 순회하며 파싱하고 저장한다.
 *
 * 파싱 결과 한 건은 canonical 스팟(SpotModel) 1개 + 관광데이터(TourDataSpotModel) 1개를 짝지은
 * {@link CollectedSpot}이다. 즉 신규 저장 건수만큼 SpotModel과 TourDataSpotModel이 1:1로 생성된다.
 *
 * 재수집(같은 contentId가 이미 있는 경우)에는 SpotModel을 새로 만들지 않고 기존 spotId를 그대로 유지한 채
 * 속성만 갱신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataSpotCollectApplicationService {

	/** contentTypeId: 12=관광지, 14=문화시설, 15=축제공연행사, 25=여행코스, 28=레포츠, 32=숙박, 38=쇼핑, 39=음식점 */
	private static final int[] CONTENT_TYPE_IDS = {12, 14, 15, 25, 28, 32, 38, 39};

	private final TourApiClient tourApiClient;
	private final TourApiProperties props;
	private final SpotRepository spotRepository;
	private final TourDataSpotRepository tourDataSpotRepository;

	@Transactional
	public CollectResult collect(String lDongRegnCd, String lDongSignguCd) {
		int created = 0;
		int updated = 0;

		for (int contentTypeId : CONTENT_TYPE_IDS) {
			for (CollectedSpot collected : collectByContentType(lDongRegnCd, lDongSignguCd, contentTypeId)) {
				if (persist(collected)) {
					created++;
				} else {
					updated++;
				}
			}
		}

		log.info("수집 완료: lDongRegnCd={}, lDongSignguCd={}, 신규 {}건 / 갱신 {}건",
			lDongRegnCd, lDongSignguCd, created, updated);
		return new CollectResult(lDongRegnCd, lDongSignguCd, created, updated);
	}

	/**
	 * 저장한다. 신규면 SpotModel을 먼저 저장해 채번된 id로 연결하고, 기존 건이면 기존 spotId를 유지한 채 갱신한다.
	 *
	 * @return 신규 생성이면 true, 기존 건 갱신이면 false
	 */
	private boolean persist(CollectedSpot collected) {
		TourDataSpotModel parsed = collected.tourDataSpot();
		Optional<TourDataSpotModel> existing = tourDataSpotRepository.findByContentId(parsed.contentId());

		if (existing.isPresent()) {
			TourDataSpotModel target = existing.get();
			target.updateFromTourApi(parsed.address(), parsed.category(), parsed.createdTime(), parsed.thumbnail(),
				parsed.mapX(), parsed.mapY(), parsed.title(), parsed.reg(), parsed.sigungu(), parsed.lcls(),
				parsed.zipcode());
			tourDataSpotRepository.save(target);
			return false;
		}

		SpotModel savedSpot = spotRepository.save(collected.spot());
		collected.link(savedSpot.spotId());
		tourDataSpotRepository.save(parsed);
		return true;
	}

	/** 타입 단위로 예외를 흡수한다. 한 타입에서 실패해도 나머지 타입 파싱은 계속 진행한다. */
	private List<CollectedSpot> collectByContentType(String lDongRegnCd, String lDongSignguCd, int contentTypeId) {
		List<CollectedSpot> spots = new ArrayList<>();
		try {
			int totalCount = tourApiClient.fetchTotalCount(lDongRegnCd, lDongSignguCd, contentTypeId);
			if (totalCount == 0) {
				log.info("  contentTypeId={} : 0건, 스킵", contentTypeId);
				return spots;
			}

			int totalPages = (int) Math.ceil((double) totalCount / props.pageSize());
			log.info("  contentTypeId={} : totalCount={}, pages={}", contentTypeId, totalCount, totalPages);

			for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
				List<AreaBasedListItem> items = tourApiClient.fetchPage(lDongRegnCd, lDongSignguCd, contentTypeId, pageNo);
				items.stream().map(this::toDomain).forEach(spots::add);
				sleep();
			}
		} catch (CoreException e) {
			log.warn("  contentTypeId={} : 파싱 실패, 이 타입은 여기까지만 파싱하고 다음 타입으로 진행 (parsed={}). {}",
				contentTypeId, spots.size(), e.getMessage());
		}
		return spots;
	}

	/** item 하나당 SpotModel 1개 + TourDataSpotModel 1개를 만들어 짝지어 반환한다. */
	private CollectedSpot toDomain(AreaBasedListItem item) {
		return new CollectedSpot(SpotModel.create(), toTourDataSpot(item));
	}

	private TourDataSpotModel toTourDataSpot(AreaBasedListItem item) {
		return TourDataSpotModel.builder()
			.contentId(parseLong(item.contentid()))
			.address(blankToNull(item.addr1()))
			.category(blankToNull(item.contenttypeid()))
			.createdTime(blankToNull(item.createdtime()))
			.thumbnail(blankToNull(item.firstimage()))
			.mapX(parseDouble(item.mapx()))
			.mapY(parseDouble(item.mapy()))
			.title(item.title())
			.reg(blankToNull(item.lDongRegnCd()))
			.sigungu(blankToNull(item.lDongSignguCd()))
			.lcls(blankToNull(item.lclsSystm3()))
			.zipcode(blankToNull(item.zipcode()))
			.build();
	}

	private Long parseLong(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double parseDouble(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	private void sleep() {
		try {
			Thread.sleep(props.callIntervalMs());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("수집 중단됨", e);
		}
	}

	public record CollectResult(String lDongRegnCd, String lDongSignguCd, int createdCount, int updatedCount) {
	}
}
