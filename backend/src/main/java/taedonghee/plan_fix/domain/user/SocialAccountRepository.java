package taedonghee.plan_fix.domain.user;

import java.util.Optional;

/**
 * 소셜 계정 연결 Repository
 */
public interface SocialAccountRepository {

    /**
     * provider와 provider 식별자 기반 소셜 계정 단건 조회
     */
    Optional<SocialAccountModel> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    /**
     * 소셜 계정 연결 저장
     */
    SocialAccountModel save(SocialAccountModel account);
}
