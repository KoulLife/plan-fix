package taedonghee.plan_fix.interfaces.api.spot;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.spot.SpotDetailApplicationService;
import taedonghee.plan_fix.application.spot.SpotDetailResult;
import taedonghee.plan_fix.application.spot.SpotListApplicationService;
import taedonghee.plan_fix.application.spot.SpotListQuery;
import taedonghee.plan_fix.application.spot.SpotListResult;
import taedonghee.plan_fix.domain.user.UserRole;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpotControllerTest {

    private final SpotListApplicationService spotListApplicationService = mock(SpotListApplicationService.class);
    private final SpotDetailApplicationService spotDetailApplicationService = mock(SpotDetailApplicationService.class);
    private final SpotController controller =
            new SpotController(spotListApplicationService, spotDetailApplicationService);

    @Test
    void 조회_결과를_응답_DTO로_변환한다() {
        SpotListResult result = new SpotListResult(
                List.of(new SpotListResult.Item(1L, "정동진", "관광지", "51", "150", "thumb.jpg")),
                0, 20, 1);
        when(spotListApplicationService.list(new SpotListQuery("정동", "관광지", "51", "150", "popular", 0, 20)))
                .thenReturn(result);

        ResponseEntity<SpotResponse> response = controller.list("정동", "관광지", "51", "150", "popular", 0, 20);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SpotResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offset()).isEqualTo(0);
        assertThat(body.size()).isEqualTo(20);
        assertThat(body.totalCount()).isEqualTo(1);
        assertThat(body.items()).hasSize(1);
        SpotResponse.Item item = body.items().getFirst();
        assertThat(item.spotId()).isEqualTo(1L);
        assertThat(item.title()).isEqualTo("정동진");
        assertThat(item.category()).isEqualTo("관광지");
        assertThat(item.region()).isEqualTo("51");
        assertThat(item.sigungu()).isEqualTo("150");
        assertThat(item.thumbnail()).isEqualTo("thumb.jpg");
    }

    @Test
    void 필터_파라미터가_없으면_null_query로_넘긴다() {
        when(spotListApplicationService.list(new SpotListQuery(null, null, null, null, null, 0, 20)))
                .thenReturn(new SpotListResult(List.of(), 0, 20, 0));

        controller.list(null, null, null, null, null, 0, 20);

        verify(spotListApplicationService).list(eq(new SpotListQuery(null, null, null, null, null, 0, 20)));
    }

    @Test
    void 상세_조회_결과를_응답_DTO로_변환한다() {
        SpotDetailResult.TourInfo tourInfo = new SpotDetailResult.TourInfo(
                "033-000-0000", "가능", "09:00~18:00", "연중무휴", null, null, null);
        SpotDetailResult result = new SpotDetailResult(1L, "정동진", "관광지", "51", "150",
                "강원특별자치도 강릉시", new BigDecimal("37.1"), new BigDecimal("129.0"), "thumb.jpg",
                "동해안의 대표 해변", 11, 3, 1,
                List.of("http://example.com/1.jpg"), tourInfo, true);
        when(spotDetailApplicationService.get(1L, null)).thenReturn(result);

        ResponseEntity<SpotDetailResponse> response = controller.get(1L, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SpotDetailResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.spotId()).isEqualTo(1L);
        assertThat(body.title()).isEqualTo("정동진");
        assertThat(body.category()).isEqualTo("관광지");
        assertThat(body.region()).isEqualTo("51");
        assertThat(body.sigungu()).isEqualTo("150");
        assertThat(body.address()).isEqualTo("강원특별자치도 강릉시");
        assertThat(body.latitude()).isEqualByComparingTo("37.1");
        assertThat(body.longitude()).isEqualByComparingTo("129.0");
        assertThat(body.thumbnail()).isEqualTo("thumb.jpg");
        assertThat(body.description()).isEqualTo("동해안의 대표 해변");
        assertThat(body.viewCount()).isEqualTo(11);
        assertThat(body.likeCount()).isEqualTo(3);
        assertThat(body.commentCount()).isEqualTo(1);
        assertThat(body.images()).containsExactly("http://example.com/1.jpg");
        assertThat(body.info()).isNotNull();
        assertThat(body.info().tel()).isEqualTo("033-000-0000");
        assertThat(body.isLiked()).isTrue();
    }

    @Test
    void TourAPI_부가정보가_없으면_images는_빈리스트_info는_null이다() {
        SpotDetailResult result = new SpotDetailResult(2L, "직접등록 스팟", "관광지", null, null,
                null, null, null, null, null, 0, 0, 0, List.of(), null, false);
        when(spotDetailApplicationService.get(2L, null)).thenReturn(result);

        ResponseEntity<SpotDetailResponse> response = controller.get(2L, null);

        SpotDetailResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.images()).isEmpty();
        assertThat(body.info()).isNull();
        assertThat(body.isLiked()).isFalse();
    }

    @Test
    void 로그인한_사용자면_principal_id를_viewerUserId로_넘긴다() {
        AuthenticatedUser principal = new AuthenticatedUser(10L, "길동", UserRole.USER);
        SpotDetailResult result = new SpotDetailResult(1L, "정동진", "관광지", null, null,
                null, null, null, null, null, 0, 0, 0, List.of(), null, true);
        when(spotDetailApplicationService.get(1L, 10L)).thenReturn(result);

        ResponseEntity<SpotDetailResponse> response = controller.get(1L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLiked()).isTrue();
        verify(spotDetailApplicationService).get(1L, 10L);
    }

    @Test
    void 존재하지_않는_스팟이면_예외가_그대로_전파된다() {
        when(spotDetailApplicationService.get(999L, null))
                .thenThrow(new CoreException(ErrorType.NOT_FOUND, "spot not found. spotId=999"));

        assertThatThrownBy(() -> controller.get(999L, null)).isInstanceOf(CoreException.class);
    }
}
