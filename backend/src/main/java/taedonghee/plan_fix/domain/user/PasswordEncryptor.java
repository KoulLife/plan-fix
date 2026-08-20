package taedonghee.plan_fix.domain.user;

/**
 * 비밀번호 암호화 
 */
public interface PasswordEncryptor {

    /**
     * 평문 비밀번호 암호화
     */
    String encrypt(String rawPassword);

    /**
     * 평문 비밀번호와 암호화 비밀번호 일치 여부 검증
     */
    boolean matches(String rawPassword, String encryptedPassword);
}
