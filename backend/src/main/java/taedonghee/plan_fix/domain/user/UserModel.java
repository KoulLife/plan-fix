package taedonghee.plan_fix.domain.user;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * 사용자 Model
 */
public class UserModel {

    private static final int USERNAME_MIN_LENGTH = 2;
    private static final int USERNAME_MAX_LENGTH = 30;
    private static final int EMAIL_MAX_LENGTH = 255;

    // 이름 형식 검증: 한글만 허용
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[가-힣]+$");

    // 이메일 형식 검증: 아이디@도메인.확장자 형식, 확장자는 영문 2자 이상
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Long userId;
    private final String username;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private UserModel(
            Long userId,
            String username,
            String email,
            UserRole role,
            UserStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateUsername(username);
        validateEmail(email);

        this.userId = userId;
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
     * 저장된 사용자 정보를 기반으로 UserModel 복원 
     * DB에 이미 저장되어 있던 User 데이터를 다시 UserModel 객체로 복원할 때 사용
     */
    public static UserModel reconstruct(
            Long userId,
            String username,
            String email,
            UserRole role,
            UserStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new UserModel(userId, username, email, role, status, createdAt, updatedAt);
    }

    /**
     * 사용자 프로필 수정
     */
    public UserModel updateProfile(String username, String email) {
        if (status == UserStatus.WITHDRAWN) {
            throw new CoreException(ErrorType.CONFLICT, "탈퇴한 사용자는 수정할 수 없습니다. userId=" + userId);
        }
        return new UserModel(userId, username, email, role, status, createdAt, OffsetDateTime.now());
    }

    /**
     * 사용자 탈퇴 상태 변경
     */
    public UserModel withdraw() {
        if (status == UserStatus.WITHDRAWN) {
            throw new CoreException(ErrorType.CONFLICT, "이미 탈퇴한 사용자입니다. userId=" + userId);
        }
        return new UserModel(userId, username, email, role, UserStatus.WITHDRAWN, createdAt, OffsetDateTime.now());
    }

    /**
     * username 필수값, 길이, 한글 형식 검증
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 필수입니다.");
        }
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 2자 이상 30자 이하여야 합니다.");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "username은 한글만 사용할 수 있습니다.");
        }
    }

    /**
     * email 형식 및 길이 검증
     */
    private void validateEmail(String email) {
        if (email == null) {
            return;
        }
        if (email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "email은 공백일 수 없습니다.");
        }
        if (email.length() > EMAIL_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "email은 255자 이하여야 합니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "email 형식이 올바르지 않습니다.");
        }
    }

    public Long getUserId() {
        return userId;
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
