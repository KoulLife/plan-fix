package taedonghee.plan_fix.infrastructure.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.user.SocialAccountModel;
import taedonghee.plan_fix.domain.user.SocialAccountRepository;
import taedonghee.plan_fix.domain.user.SocialProvider;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.Optional;

/**
 * SocialAccountRepository JPA 구현체
 */
@Repository
@RequiredArgsConstructor
public class SocialAccountRepositoryImpl implements SocialAccountRepository {

    private final UserSocialAccountJpaRepository userSocialAccountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    /**
     * provider와 provider 식별자 기반 소셜 계정 단건 조회 처리
     */
    @Override
    public Optional<SocialAccountModel> findByProviderAndProviderUserId(
            SocialProvider provider, String providerUserId) {
        return userSocialAccountJpaRepository
                .findByProviderAndProviderUserId(provider.name(), providerUserId)
                .map(this::toDomain);
    }

    /**
     * 소셜 계정 연결 저장 처리
     */
    @Override
    public SocialAccountModel save(SocialAccountModel account) {
        UserJpaEntity user = userJpaRepository.findById(account.getUserId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND, "User not found. userId=" + account.getUserId()));

        UserSocialAccountJpaEntity entity = UserSocialAccountJpaEntity.builder()
                .id(account.getSocialAccountId())
                .user(user)
                .provider(account.getProvider().name())
                .providerUserId(account.getProviderUserId())
                .providerEmail(account.getProviderEmail())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();

        return toDomain(userSocialAccountJpaRepository.save(entity));
    }

    /**
     * JPA 엔티티를 도메인 모델로 변환
     */
    private SocialAccountModel toDomain(UserSocialAccountJpaEntity entity) {
        return SocialAccountModel.reconstruct(
                entity.getId(),
                entity.getUser().getId(),
                SocialProvider.valueOf(entity.getProvider()),
                entity.getProviderUserId(),
                entity.getProviderEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
