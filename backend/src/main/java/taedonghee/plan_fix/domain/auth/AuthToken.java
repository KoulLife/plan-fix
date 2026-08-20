package taedonghee.plan_fix.domain.auth;

/**
 * 인증 토큰 발급 결과
 */
public record AuthToken(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
