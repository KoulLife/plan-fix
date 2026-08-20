package taedonghee.plan_fix.interfaces.api.auth;

import taedonghee.plan_fix.application.auth.AuthResult;

/**
 * 인증 API 응답 DTO
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    /**
     * application 결과값 변환
     */
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.accessToken(), result.tokenType(), result.expiresIn());
    }
}
