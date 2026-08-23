package taedonghee.plan_fix.application.auth;

import taedonghee.plan_fix.domain.user.UserModel;

/**
 * 인증 결과 DTO
 */
public record AuthResult(
        String accessToken,
        long expiresIn,
        Long userId,
        String username,
        String email
) {

    /**
     * 발급 토큰과 사용자 정보 결합
     */
    public static AuthResult of(AuthToken token, UserModel user) {
        return new AuthResult(
                token.accessToken(), token.expiresIn(), user.getUserId(), user.getUsername(), user.getEmail());
    }
}
