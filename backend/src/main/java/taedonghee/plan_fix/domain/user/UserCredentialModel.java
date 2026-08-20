package taedonghee.plan_fix.domain.user;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * 자체 로그인 인증정보 Model
 */
public class UserCredentialModel {

    private static final int LOGIN_ID_MIN_LENGTH = 6;
    private static final int LOGIN_ID_MAX_LENGTH = 20;
    private static final int RAW_PASSWORD_MIN_LENGTH = 8;
    private static final int RAW_PASSWORD_MAX_LENGTH = 20;
    private static final int ENCRYPTED_PASSWORD_MAX_LENGTH = 100;

    // 로그인 아이디 형식 검증: 영문 소문자와 숫자만 허용
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9]+$");

    // 비밀번호 형식 검증: 영문/숫자 포함 및 공백 사용 여부 확인
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_NUMBER_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern PASSWORD_WHITESPACE_PATTERN = Pattern.compile(".*\\s.*");

    private final Long userCredentialId;
    private final Long userId;
    private final String loginId;
    private final String password;
    private final OffsetDateTime lastLoginAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private UserCredentialModel(
            Long userCredentialId,
            Long userId, // 사용자 자체의 식별자
            String loginId, // 사용자가 로그인할 때 입력하는 아이디
            String password,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateUserId(userId);
        validateLoginId(loginId);
        validateEncryptedPassword(password);

        this.userCredentialId = userCredentialId;
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
    public static UserCredentialModel create(Long userId, String loginId, String encryptedPassword) {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserCredentialModel(null, userId, loginId, encryptedPassword, null, now, now);
    }

    /**
     * 저장된 사용자 인증정보를 기반으로 UserCredentialModel 객체를 복원
     * DB에 이미 저장되어 있는 인증정보를 도메인 모델로 변환할 때 사용
     */
    public static UserCredentialModel reconstruct(
            Long userCredentialId,
            Long userId,
            String loginId,
            String password,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new UserCredentialModel(userCredentialId, userId, loginId, password, lastLoginAt, createdAt, updatedAt);
    }

    /**
     * 마지막 로그인 시각 갱신
     */
    public UserCredentialModel markLoggedIn() {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserCredentialModel(userCredentialId, userId, loginId, password, now, createdAt, now);
    }

    /**
     * 회원가입 평문 비밀번호 정책 검증
     */
    public static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password는 필수입니다.");
        }
        if (rawPassword.length() < RAW_PASSWORD_MIN_LENGTH || rawPassword.length() > RAW_PASSWORD_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password는 8자 이상 20자 이하여야 합니다.");
        }
        if (PASSWORD_WHITESPACE_PATTERN.matcher(rawPassword).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password에는 공백을 사용할 수 없습니다.");
        }
        if (!PASSWORD_LETTER_PATTERN.matcher(rawPassword).matches()
                || !PASSWORD_NUMBER_PATTERN.matcher(rawPassword).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password는 영문과 숫자를 모두 포함해야 합니다.");
        }
    }

    /**
     * user_id 필수값 검증
     */
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
    }

    /**
     * login_id 필수값, 길이, 영문 소문자 및 숫자 형식 검증
     */
    public static void validateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "loginId는 필수입니다.");
        }
        if (loginId.length() < LOGIN_ID_MIN_LENGTH || loginId.length() > LOGIN_ID_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "loginId는 6자 이상 20자 이하여야 합니다.");
        }
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "loginId는 영문 소문자와 숫자만 사용할 수 있습니다.");
        }
    }

    /**
     * 암호화 비밀번호 필수값 및 길이 검증
     */
    private void validateEncryptedPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password는 필수입니다.");
        }
        if (password.length() > ENCRYPTED_PASSWORD_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password는 100자 이하여야 합니다.");
        }
    }

    public Long getUserCredentialId() {
        return userCredentialId;
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
