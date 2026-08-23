package taedonghee.plan_fix.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * oauth.kakao.* 설정 바인딩
 */
@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
        String authorizeUri,
        String tokenUri,
        String userInfoUri,
        String redirectUri,
        String clientId,
        String clientSecret
) {
}
