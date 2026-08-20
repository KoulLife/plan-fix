package taedonghee.plan_fix.infrastructure.security;

/**
 * JWT 검증 후 추출한 사용자 정보
 */
public record JwtClaims(
        Long userId,
        String username,
        String role
) {
}
