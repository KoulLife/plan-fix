package taedonghee.plan_fix.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * oauth.kakao.* 설정 바인딩
 *
 * scope는 카카오 디벨로퍼스의 동의항목 설정과 정확히 일치해야 한다.
 * 앱에 설정되지 않은 항목을 요청하면 카카오가 KOE205로 거절한다.
 */
@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
        String authorizeUri,
        String tokenUri,
        String userInfoUri,
        String redirectUri,
        String scope,
        String clientId,
        String clientSecret
) {
}
