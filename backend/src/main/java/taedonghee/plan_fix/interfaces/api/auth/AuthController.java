package taedonghee.plan_fix.interfaces.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.auth.AuthApplicationService;
import taedonghee.plan_fix.application.auth.AuthResult;
import taedonghee.plan_fix.infrastructure.security.CookieFactory;

/**
 * 인증 API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final CookieFactory cookieFactory;

    /**
     * 자체 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest.Login request) {
        AuthResult result = authApplicationService.login(request.toCommand());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.accessToken(result.accessToken(), result.expiresIn()).toString())
                .body(AuthResponse.from(result));
    }

    /**
     * 로그아웃 API
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredAccessToken().toString())
                .build();
    }
}
