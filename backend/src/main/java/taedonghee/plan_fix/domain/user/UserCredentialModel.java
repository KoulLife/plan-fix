package taedonghee.plan_fix.domain.user;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * 자체 로그인 인증정보 Model
 */
public class UserCredentialModel {

    private final Long id;
    private final Long userId;
    private final String loginId;
    private final String password;
    private final OffsetDateTime lastLoginAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private UserCredentialModel(
            Long id,
            Long userId,
            String loginId,
            String password,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateLoginId(loginId);
        this.id = id;
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 인증정보 생성
     */
    public static UserCredentialModel create(Long userId, String loginId, String password) {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserCredentialModel(null, userId, loginId, password, null, now, now);
    }

    /**
     * 저장된 인증정보 복원
     */
    public static UserCredentialModel reconstruct(
            Long id,
            Long userId,
            String loginId,
            String password,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new UserCredentialModel(id, userId, loginId, password, lastLoginAt, createdAt, updatedAt);
    }

    /**
     * 마지막 로그인 시각 갱신
     */
    public UserCredentialModel markLoggedIn() {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserCredentialModel(id, userId, loginId, password, now, createdAt, now);
    }

    /**
     * login_id 길이 검증
     */
    private void validateLoginId(String loginId) {
        if (loginId != null && loginId.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "loginId must be 50 characters or less.");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
