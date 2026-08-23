package taedonghee.plan_fix.interfaces.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.auth.AuthResult;
import taedonghee.plan_fix.application.auth.SocialLoginApplicationService;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.OAuthTransaction;
import taedonghee.plan_fix.infrastructure.oauth.PkceGenerator;
import taedonghee.plan_fix.infrastructure.security.CookieFactory;
import taedonghee.plan_fix.support.error.CoreException;

import java.net.URI;
import java.util.Optional;

/**
 * 카카오 로그인 API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
public class KakaoAuthController {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthController.class);

    private final KakaoOAuthClient kakaoOAuthClient;
    private final PkceGenerator pkceGenerator;
    private final SocialLoginApplicationService socialLoginApplicationService;
    private final CookieFactory cookieFactory;
    private final String frontendBaseUrl;

    public KakaoAuthController(
            KakaoOAuthClient kakaoOAuthClient,
            PkceGenerator pkceGenerator,
            SocialLoginApplicationService socialLoginApplicationService,
            CookieFactory cookieFactory,
            @Value("${app.frontend-base-url}") String frontendBaseUrl
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.pkceGenerator = pkceGenerator;
        this.socialLoginApplicationService = socialLoginApplicationService;
        this.cookieFactory = cookieFactory;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * 카카오 인가 화면으로 리다이렉트
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> start() {
        String state = pkceGenerator.generateState();
        String codeVerifier = pkceGenerator.generateCodeVerifier();
        String authorizeUrl = kakaoOAuthClient.buildAuthorizeUrl(state, pkceGenerator.codeChallenge(codeVerifier));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.oauthTx(new OAuthTransaction(state, codeVerifier).encode()).toString())
                .build();
    }

    /**
     * 카카오 콜백 처리 후 프론트로 리다이렉트
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = CookieFactory.OAUTH_TX_COOKIE, required = false) String oauthTx
    ) {
        if (error != null) {
            log.warn("카카오 인가 거부 또는 실패. error={}", error);
            return failure("denied");
        }

        Optional<OAuthTransaction> transaction = OAuthTransaction.decode(oauthTx);
        if (transaction.isEmpty() || state == null || !transaction.get().state().equals(state)) {
            return failure("invalid_state");
        }
        if (code == null || code.isBlank()) {
            return failure("invalid_state");
        }

        try {
            AuthResult result = socialLoginApplicationService
                    .loginWithKakao(code, transaction.get().codeVerifier());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBaseUrl + "/main"))
                    .header(HttpHeaders.SET_COOKIE,
                            cookieFactory.accessToken(result.accessToken(), result.expiresIn()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredOauthTx().toString())
                    .build();
        } catch (CoreException e) {
            log.warn("카카오 로그인 실패. type={} message={}", e.getErrorType(), e.getMessage());
            return failure(switch (e.getErrorType()) {
                case UNAUTHORIZED -> "kakao_config";
                case INTERNAL_ERROR -> "kakao_unavailable";
                default -> "unknown";
            });
        } catch (RuntimeException e) {
            log.error("카카오 로그인 처리 중 예상치 못한 오류", e);
            return failure("unknown");
        }
    }

    /**
     * 실패 사유를 쿼리로 붙여 로그인 화면으로 리다이렉트하고 oauth_tx를 제거
     */
    private ResponseEntity<Void> failure(String errorCode) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendBaseUrl + "/login?error=" + errorCode))
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredOauthTx().toString())
                .build();
    }
}
