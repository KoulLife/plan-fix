package taedonghee.plan_fix.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * user_social_accounts 테이블 Spring Data JPA Repository
 */
public interface UserSocialAccountJpaRepository extends JpaRepository<UserSocialAccountJpaEntity, Long> {

    /**
     * provider와 provider 식별자 기반 소셜 계정 단건 조회
     */
    Optional<UserSocialAccountJpaEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
