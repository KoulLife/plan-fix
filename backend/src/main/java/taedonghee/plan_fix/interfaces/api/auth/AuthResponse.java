package taedonghee.plan_fix.interfaces.api.auth;

import taedonghee.plan_fix.application.auth.AuthResult;

/**
 * 인증 API 응답 DTO
 */
public record AuthResponse(User user) {

    /**
     * 응답에 담기는 사용자 정보
     */
    public record User(Long id, String username, String email) {
    }

    /**
     * application 결과값 변환
     */
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(new User(result.userId(), result.username(), result.email()));
    }
}
