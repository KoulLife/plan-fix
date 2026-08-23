package taedonghee.plan_fix.interfaces.api.spot;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.spot.SpotLikeApplicationService;
import taedonghee.plan_fix.application.spot.SpotLikeResult;
import taedonghee.plan_fix.domain.user.UserRole;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpotLikeControllerTest {

    private final SpotLikeApplicationService spotLikeApplicationService = mock(SpotLikeApplicationService.class);
    private final SpotLikeController controller = new SpotLikeController(spotLikeApplicationService);
    private final AuthenticatedUser principal = new AuthenticatedUser(10L, "길동", UserRole.USER);

    @Test
    void 좋아요_요청은_principal_id로_서비스를_호출하고_결과를_그대로_응답한다() {
        when(spotLikeApplicationService.like(10L, 1L)).thenReturn(new SpotLikeResult(true, 4));

        ResponseEntity<SpotLikeResponse> response = controller.like(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().liked()).isTrue();
        assertThat(response.getBody().likeCount()).isEqualTo(4);
    }

    @Test
    void 좋아요_취소_요청은_principal_id로_서비스를_호출하고_결과를_그대로_응답한다() {
        when(spotLikeApplicationService.unlike(10L, 1L)).thenReturn(new SpotLikeResult(false, 3));

        ResponseEntity<SpotLikeResponse> response = controller.unlike(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().liked()).isFalse();
        assertThat(response.getBody().likeCount()).isEqualTo(3);
    }
}
