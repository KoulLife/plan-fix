package taedonghee.plan_fix.application.spot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 순수 함수라 DB/Spring 없이 검증한다(로컬 DB에 데이터를 남기지 않기 위함).
 */
class TourCategoryTest {

	@Test
	void 음식점이면서_lcls가_FD05로_시작하면_카페_음료로_분류한다() {
		assertThat(TourCategory.displayNameOf("39", "FD050100")).isEqualTo("카페/음료");
		assertThat(TourCategory.displayNameOf("39", "FD050200")).isEqualTo("카페/음료");
		assertThat(TourCategory.displayNameOf("39", "FD050300")).isEqualTo("카페/음료");
	}

	@Test
	void 음식점이면서_lcls가_FD05가_아니면_음식점_그대로_분류한다() {
		assertThat(TourCategory.displayNameOf("39", "FD010100")).isEqualTo("음식점");
		assertThat(TourCategory.displayNameOf("39", "FD020300")).isEqualTo("음식점");
	}

	@Test
	void 음식점이면서_lcls가_없으면_음식점_그대로_분류한다() {
		assertThat(TourCategory.displayNameOf("39", null)).isEqualTo("음식점");
	}

	@Test
	void 음식점이_아니면_lcls와_무관하게_기존_카테고리를_유지한다() {
		assertThat(TourCategory.displayNameOf("12", "FD050100")).isEqualTo("관광지");
		assertThat(TourCategory.displayNameOf("32", null)).isEqualTo("숙박");
	}

	@Test
	void 모르는_contentTypeId는_기타로_떨어진다() {
		assertThat(TourCategory.displayNameOf("99", null)).isEqualTo("기타");
		assertThat(TourCategory.displayNameOf("99", "FD050100")).isEqualTo("기타");
	}
}
