package taedonghee.plan_fix.domain.user;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * 사용자 도메인 Model
 */
public class UserModel {

    private final Long id;
    private final String username;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private UserModel(
            Long id,
            String username,
            String email,
            UserRole role,
            UserStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateUsername(username);
        validateEmail(email);
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role == null ? UserRole.USER : role;
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 사용자 생성
     */
    public static UserModel create(String username, String email) {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserModel(null, username, email, UserRole.USER, UserStatus.ACTIVE, now, now);
    }

    /**
     * 저장된 사용자 복원
     */
    public static UserModel reconstruct(
            Long id,
            String username,
            String email,
            UserRole role,
            UserStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new UserModel(id, username, email, role, status, createdAt, updatedAt);
    }

    /**
     * 사용자 프로필 수정
     */
    public UserModel updateProfile(String username, String email) {
        if (status == UserStatus.WITHDRAWN) {
            throw new CoreException(ErrorType.CONFLICT, "Withdrawn users cannot be updated. id=" + id);
        }
        return new UserModel(id, username, email, role, status, createdAt, OffsetDateTime.now());
    }

    /**
     * 사용자 탈퇴 상태 변경
     */
    public UserModel withdraw() {
        if (status == UserStatus.WITHDRAWN) {
            throw new CoreException(ErrorType.CONFLICT, "User is already withdrawn. id=" + id);
        }
        return new UserModel(id, username, email, role, UserStatus.WITHDRAWN, createdAt, OffsetDateTime.now());
    }

    /**
     * username 필수값 및 길이 검증
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username must not be blank.");
        }
        if (username.length() > 30) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username must be 30 characters or less.");
        }
    }

    /**
     * email 길이 검증
     */
    private void validateEmail(String email) {
        if (email != null && email.length() > 255) {
            throw new CoreException(ErrorType.BAD_REQUEST, "email must be 255 characters or less.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
