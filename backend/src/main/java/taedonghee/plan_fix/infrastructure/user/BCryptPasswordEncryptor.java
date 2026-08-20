package taedonghee.plan_fix.infrastructure.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import taedonghee.plan_fix.domain.user.PasswordEncryptor;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * BCrypt 기반 비밀번호 암호화
 */
@Component
@RequiredArgsConstructor
public class BCryptPasswordEncryptor implements PasswordEncryptor {

    private final PasswordEncoder encoder;

    /**
     * BCrypt 비밀번호 해시 생성
     */
    @Override
    public String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "password must not be blank.");
        }
        return encoder.encode(rawPassword);
    }

    /**
     * BCrypt 비밀번호 일치 여부 검증
     */
    @Override
    public boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encryptedPassword);
    }
}
