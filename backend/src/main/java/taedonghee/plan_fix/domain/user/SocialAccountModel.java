package taedonghee.plan_fix.domain.user;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * 소셜 계정 연결 Model
 */
public class SocialAccountModel {

    private static final int PROVIDER_USER_ID_MAX_LENGTH = 255;

    private final Long socialAccountId;
    private final Long userId;
    private final SocialProvider provider;
    private final String providerUserId;
    private final String providerEmail;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private SocialAccountModel(
            Long socialAccountId,
            Long userId,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (provider == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "provider는 필수입니다.");
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "providerUserId는 필수입니다.");
        }
        if (providerUserId.length() > PROVIDER_USER_ID_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "providerUserId는 255자 이하여야 합니다.");
        }

        this.socialAccountId = socialAccountId;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 소셜 계정 연결 생성
     */
    public static SocialAccountModel create(
            Long userId, SocialProvider provider, String providerUserId, String providerEmail) {
        OffsetDateTime now = OffsetDateTime.now();
        return new SocialAccountModel(null, userId, provider, providerUserId, providerEmail, now, now);
    }

    /**
     * 저장된 소셜 계정 정보를 기반으로 Model 복원
     */
    public static SocialAccountModel reconstruct(
            Long socialAccountId,
            Long userId,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new SocialAccountModel(
                socialAccountId, userId, provider, providerUserId, providerEmail, createdAt, updatedAt);
    }

    public Long getSocialAccountId() {
        return socialAccountId;
    }

    public Long getUserId() {
        return userId;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
