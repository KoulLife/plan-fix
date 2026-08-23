package taedonghee.plan_fix.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieFactoryTest {

    private final CookieFactory cookieFactory = new CookieFactory(false, "Lax");

    @Test
    void access_token_쿠키는_HttpOnly이며_전체_경로에_걸린다() {
        ResponseCookie cookie = cookieFactory.accessToken("jwt-value", 3600);

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("jwt-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void 만료용_access_token_쿠키는_MaxAge가_0이다() {
        assertThat(cookieFactory.expiredAccessToken().getMaxAge().getSeconds()).isZero();
    }

    @Test
    void oauth_tx_쿠키는_인증_경로로_한정되고_5분간_유효하다() {
        ResponseCookie cookie = cookieFactory.oauthTx("tx-value");

        assertThat(cookie.getName()).isEqualTo("oauth_tx");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(300);
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void 운영_설정에서는_Secure가_켜진다() {
        assertThat(new CookieFactory(true, "None").accessToken("v", 60).isSecure()).isTrue();
    }
}
