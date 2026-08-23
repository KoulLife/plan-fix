package taedonghee.plan_fix.infrastructure.spot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotSearchCondition;
import taedonghee.plan_fix.domain.spot.SpotSortType;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.spot.SpotStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공개 목록 조회가 실제 DB에서 필터·정렬·offset/limit대로 동작하는지 본다.
 * 조건 조합이 SQL로 정확히 내려가야 하는 부분이라 실제 저장소를 쓴다.
 *
 * 이 DB에는 TourAPI로 이미 수집된 실 데이터(관광지/음식점 등, 1000여 건)가 함께 들어 있다.
 * 그래서 각 테스트는 실제 카테고리 값과 절대 겹치지 않는 유일한 태그를 category로 써서
 * 자기 데이터만 걸러 읽는다. category 필터 자체의 동작은 별도 테스트에서 검증한다.
 *
 * @Transactional로 각 테스트가 끝나면 롤백한다 — 순수 조회 검증이라 커밋할 이유가 없고,
 * 이전에 이 클래스가 만든 더미 행이 로컬 DB에 남아 인기순/상세 조회 결과를 오염시킨 적이 있어서다.
 */
@SpringBootTest
@Transactional
class SpotRepositoryImplTest {

    @Autowired
    private SpotRepository spotRepository;

    @Test
    void status가_ACTIVE인_스팟만_반환한다() {
        String tag = tag();
        save(tag, "51", "150", SpotStatus.ACTIVE, 0, 0);
        save(tag, "51", "150", SpotStatus.HIDDEN, 0, 0);

        List<SpotModel> result = spotRepository.searchActive(condition(tag, null, null), SpotSortType.LATEST, 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(SpotStatus.ACTIVE);
    }

    @Test
    void category가_다르면_제외한다() {
        String tag = tag();
        save(tag, "51", "150", SpotStatus.ACTIVE, 0, 0);
        save(tag + "-other", "51", "150", SpotStatus.ACTIVE, 0, 0);

        List<SpotModel> result = spotRepository.searchActive(condition(tag, null, null), SpotSortType.LATEST, 0, 20);

        assertThat(result).extracting(SpotModel::category).containsExactly(tag);
    }

    @Test
    void region_sigungu로_필터링한다() {
        String tag = tag();
        SpotModel match = save(tag, "51", "150", SpotStatus.ACTIVE, 0, 0);
        save(tag, "11", "150", SpotStatus.ACTIVE, 0, 0);
        save(tag, "51", "200", SpotStatus.ACTIVE, 0, 0);

        List<SpotModel> result = spotRepository.searchActive(condition(tag, "51", "150"), SpotSortType.LATEST, 0, 20);

        assertThat(result).extracting(SpotModel::spotId).containsExactly(match.spotId());
    }

    @Test
    void 필터가_없으면_spotId_내림차순으로_반환한다() {
        String tag = tag();
        SpotModel first = save(tag, null, null, SpotStatus.ACTIVE, 0, 0);
        SpotModel second = save(tag, null, null, SpotStatus.ACTIVE, 0, 0);

        List<SpotModel> result = spotRepository.searchActive(condition(tag, null, null), SpotSortType.LATEST, 0, 20);

        assertThat(result).extracting(SpotModel::spotId).containsExactly(second.spotId(), first.spotId());
    }

    @Test
    void offset과_limit으로_구간을_잘라낸다() {
        String tag = tag();
        save(tag, null, null, SpotStatus.ACTIVE, 0, 0);
        save(tag, null, null, SpotStatus.ACTIVE, 0, 0);
        save(tag, null, null, SpotStatus.ACTIVE, 0, 0);

        List<SpotModel> firstPage = spotRepository.searchActive(condition(tag, null, null), SpotSortType.LATEST, 0, 2);
        List<SpotModel> secondPage = spotRepository.searchActive(condition(tag, null, null), SpotSortType.LATEST, 2, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(1);
    }

    @Test
    void POPULAR_정렬은_좋아요_0_9_조회수_0_1_가중합_내림차순이다() {
        String tag = tag();
        // 점수: low=1*0.9+0*0.1=0.9, mid=0*0.9+50*0.1=5.0, high=10*0.9+0*0.1=9.0
        SpotModel low = save(tag, null, null, SpotStatus.ACTIVE, 1, 0);
        SpotModel mid = save(tag, null, null, SpotStatus.ACTIVE, 0, 50);
        SpotModel high = save(tag, null, null, SpotStatus.ACTIVE, 10, 0);

        List<SpotModel> result = spotRepository.searchActive(condition(tag, null, null), SpotSortType.POPULAR, 0, 20);

        assertThat(result).extracting(SpotModel::spotId)
                .containsExactly(high.spotId(), mid.spotId(), low.spotId());
    }

    @Test
    void POPULAR_정렬에서_점수가_같으면_spotId_내림차순이다() {
        String tag = tag();
        SpotModel first = save(tag, null, null, SpotStatus.ACTIVE, 10, 0);
        SpotModel second = save(tag, null, null, SpotStatus.ACTIVE, 10, 0);

        List<SpotModel> result = spotRepository.searchActive(condition(tag, null, null), SpotSortType.POPULAR, 0, 20);

        assertThat(result).extracting(SpotModel::spotId).containsExactly(second.spotId(), first.spotId());
    }

    @Test
    void countActive는_ACTIVE만_조건대로_센다() {
        String tag = tag();
        save(tag, "51", "150", SpotStatus.ACTIVE, 0, 0);
        save(tag, "51", "150", SpotStatus.ACTIVE, 0, 0);
        save(tag, "51", "150", SpotStatus.HIDDEN, 0, 0);
        save(tag, "11", "150", SpotStatus.ACTIVE, 0, 0);

        long count = spotRepository.countActive(condition(tag, "51", "150"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void 조회수를_원자적으로_1_증가시킨다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 0, 5);

        spotRepository.incrementViewCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.viewCount()).isEqualTo(6);
    }

    @Test
    void 좋아요수를_원자적으로_1_증가시킨다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 5, 0);

        spotRepository.incrementLikeCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.likeCount()).isEqualTo(6);
    }

    @Test
    void 좋아요수를_원자적으로_1_감소시킨다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 5, 0);

        spotRepository.decrementLikeCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.likeCount()).isEqualTo(4);
    }

    @Test
    void 좋아요수는_0_밑으로_내려가지_않는다() {
        SpotModel saved = save(tag(), null, null, SpotStatus.ACTIVE, 0, 0);

        spotRepository.decrementLikeCount(saved.spotId());

        SpotModel reloaded = spotRepository.findById(saved.spotId()).orElseThrow();
        assertThat(reloaded.likeCount()).isEqualTo(0);
    }

    private SpotSearchCondition condition(String category, String region, String sigungu) {
        return new SpotSearchCondition(category, region, sigungu);
    }

    private String tag() {
        return "tag-" + System.nanoTime();
    }

    private SpotModel save(String category, String region, String sigungu, SpotStatus status,
            long likeCount, long viewCount) {
        SpotModel spot = SpotModel.builder()
                .sourceType(SpotSourceType.TOUR_API)
                .title("테스트 스팟")
                .category(category)
                .region(region)
                .sigungu(sigungu)
                .status(status)
                .likeCount(likeCount)
                .viewCount(viewCount)
                .build();
        return spotRepository.save(spot);
    }
}
