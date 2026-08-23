package taedonghee.plan_fix.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 인증 관련 쿠키 속성을 한곳에서 만드는 팩토리
 */
@Component
public class CookieFactory {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String OAUTH_TX_COOKIE = "oauth_tx";

    private static final String ROOT_PATH = "/";
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final long OAUTH_TX_MAX_AGE_SECONDS = 300;

    private final boolean secure;
    private final String sameSite;

    public CookieFactory(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /**
     * 세션 access token 쿠키 생성
     */
    public ResponseCookie accessToken(String token, long maxAgeSeconds) {
        return base(ACCESS_TOKEN_COOKIE, token, ROOT_PATH, maxAgeSeconds).build();
    }

    /**
     * 로그아웃용 만료된 access token 쿠키 생성
     */
    public ResponseCookie expiredAccessToken() {
        return base(ACCESS_TOKEN_COOKIE, "", ROOT_PATH, 0).build();
    }

    /**
     * OAuth state/code_verifier 보관 쿠키 생성
     */
    public ResponseCookie oauthTx(String value) {
        return base(OAUTH_TX_COOKIE, value, AUTH_PATH, OAUTH_TX_MAX_AGE_SECONDS).build();
    }

    /**
     * 콜백 처리 후 제거용 만료된 oauth_tx 쿠키 생성
     */
    public ResponseCookie expiredOauthTx() {
        return base(OAUTH_TX_COOKIE, "", AUTH_PATH, 0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
    }
}
