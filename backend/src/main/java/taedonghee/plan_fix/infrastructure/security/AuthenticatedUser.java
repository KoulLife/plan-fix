package taedonghee.plan_fix.infrastructure.security;

import taedonghee.plan_fix.domain.user.UserRole;

/**
 * SecurityContext 인증 사용자 정보
 */
public record AuthenticatedUser(
        Long id,
        String username,
        UserRole role
) {
}
