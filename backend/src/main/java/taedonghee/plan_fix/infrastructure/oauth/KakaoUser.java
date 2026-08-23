package taedonghee.plan_fix.infrastructure.oauth;

/**
 * 카카오 사용자 정보
 */
public record KakaoUser(String id, String nickname, String email, boolean emailVerified) {
}
