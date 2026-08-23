package taedonghee.plan_fix.infrastructure.oauth;

import org.springframework.stereotype.Component;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * OAuth state 및 PKCE 값 생성기
 */
@Component
public class PkceGenerator {

    private static final int STATE_BYTES = 32;
    private static final int CODE_VERIFIER_BYTES = 48;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * CSRF 방지용 state 생성
     */
    public String generateState() {
        return randomUrlSafe(STATE_BYTES);
    }

    /**
     * PKCE code_verifier 생성
     */
    public String generateCodeVerifier() {
        return randomUrlSafe(CODE_VERIFIER_BYTES);
    }

    /**
     * code_verifier의 S256 code_challenge 계산
     */
    public String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return encoder.encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "SHA-256을 사용할 수 없습니다.");
        }
    }

    private String randomUrlSafe(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
