package taedonghee.plan_fix.domain.spot;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SpotModelTest {

	@Test
	void 소스_유래_필드를_갱신해도_카운터와_상태는_보존된다() {
		SpotModel spot = spot(SpotSourceType.TOUR_API)
			.likeCount(42L)
			.viewCount(1000L)
			.commentCount(7L)
			.status(SpotStatus.HIDDEN)
			.build();

		boolean updated = spot.updateFromSource(SpotSourceType.TOUR_API, attributes("갱신된 제목"));

		assertThat(updated).isTrue();
		assertThat(spot.title()).isEqualTo("갱신된 제목");
		assertThat(spot.likeCount()).isEqualTo(42L);
		assertThat(spot.viewCount()).isEqualTo(1000L);
		assertThat(spot.commentCount()).isEqualTo(7L);
		assertThat(spot.status()).isEqualTo(SpotStatus.HIDDEN);
	}

	@Test
	void 직접등록_스팟은_TourAPI_수집이_갱신하지_못한다() {
		SpotModel spot = spot(SpotSourceType.NATIVE).build();

		boolean updated = spot.updateFromSource(SpotSourceType.TOUR_API, attributes("TourAPI가 덮어쓴 제목"));

		assertThat(updated).isFalse();
		assertThat(spot.title()).isEqualTo("원래 제목");
	}

	@Test
	void 다른_소스가_소유한_스팟도_갱신하지_못한다() {
		SpotModel spot = spot(SpotSourceType.KAKAO).build();

		assertThat(spot.updateFromSource(SpotSourceType.TOUR_API, attributes("남의 제목"))).isFalse();
		assertThat(spot.title()).isEqualTo("원래 제목");
	}

	@Test
	void 신규_생성_시_카운터는_0이고_상태는_ACTIVE다() {
		SpotModel spot = SpotModel.builder()
			.sourceType(SpotSourceType.TOUR_API)
			.attributes(attributes("새 스팟"))
			.build();

		assertThat(spot.viewCount()).isZero();
		assertThat(spot.likeCount()).isZero();
		assertThat(spot.commentCount()).isZero();
		assertThat(spot.status()).isEqualTo(SpotStatus.ACTIVE);
	}

	private SpotModel.Builder spot(SpotSourceType sourceType) {
		return SpotModel.builder()
			.spotId(1L)
			.sourceType(sourceType)
			.attributes(attributes("원래 제목"));
	}

	private SpotModel.SourceAttributes attributes(String title) {
		return new SpotModel.SourceAttributes(
			title, "관광지", "51", "150", "강원 강릉시",
			new BigDecimal("37.7204054"), new BigDecimal("128.9983814"), "thumb.jpg", null);
	}
}
