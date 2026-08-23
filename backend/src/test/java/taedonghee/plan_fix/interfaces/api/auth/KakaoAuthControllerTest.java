package taedonghee.plan_fix.interfaces.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.auth.AuthResult;
import taedonghee.plan_fix.application.auth.SocialLoginApplicationService;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.OAuthTransaction;
import taedonghee.plan_fix.infrastructure.oauth.PkceGenerator;
import taedonghee.plan_fix.infrastructure.security.CookieFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KakaoAuthControllerTest {

    private final KakaoOAuthClient kakaoOAuthClient = mock(KakaoOAuthClient.class);
    private final SocialLoginApplicationService socialLoginApplicationService =
            mock(SocialLoginApplicationService.class);
    private final CookieFactory cookieFactory = new CookieFactory(false, "Lax");
    private final KakaoAuthController controller = new KakaoAuthController(
            kakaoOAuthClient, new PkceGenerator(), socialLoginApplicationService,
            cookieFactory, "http://localhost:3000");

    @Test
    void 로그인_시작은_카카오로_302하고_oauth_tx_쿠키를_심는다() {
        when(kakaoOAuthClient.buildAuthorizeUrl(anyString(), anyString()))
                .thenReturn("https://kauth.kakao.com/oauth/authorize?x=1");

        ResponseEntity<Void> response = controller.start();

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("https://kauth.kakao.com/oauth/authorize?x=1");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).startsWith("oauth_tx=");
    }

    @Test
    void 사용자가_동의를_거부하면_denied로_리다이렉트한다() {
        ResponseEntity<Void> response = controller.callback(null, null, "access_denied", null);

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/login?error=denied");
    }

    @Test
    void oauth_tx_쿠키가_없으면_invalid_state로_리다이렉트한다() {
        ResponseEntity<Void> response = controller.callback("code", "state", null, null);

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/login?error=invalid_state");
    }

    @Test
    void state가_다르면_invalid_state로_리다이렉트한다() {
        String cookie = new OAuthTransaction("real-state", "verifier").encode();

        ResponseEntity<Void> response = controller.callback("code", "attacker-state", null, cookie);

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/login?error=invalid_state");
    }

    @Test
    void 성공하면_access_token_쿠키를_심고_main으로_리다이렉트한다() {
        String cookie = new OAuthTransaction("state", "verifier").encode();
        when(socialLoginApplicationService.loginWithKakao("code", "verifier"))
                .thenReturn(new AuthResult("jwt-value", 3600, 1L, "길동", "a@b.com"));

        ResponseEntity<Void> response = controller.callback("code", "state", null, cookie);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/main");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anyMatch(value -> value.startsWith("access_token=jwt-value"));
    }
}
