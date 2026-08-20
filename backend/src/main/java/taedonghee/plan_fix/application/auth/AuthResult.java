package taedonghee.plan_fix.application.auth;

/**
 * 인증 결과 DTO
 */
public record AuthResult(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    /**
     * 인증 토큰 결과 변환
     */
    public static AuthResult from(AuthToken token) {
        return new AuthResult(token.accessToken(), token.tokenType(), token.expiresIn());
    }
}
