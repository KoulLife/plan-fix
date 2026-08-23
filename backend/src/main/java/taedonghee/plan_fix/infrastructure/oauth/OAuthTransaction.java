package taedonghee.plan_fix.infrastructure.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * state와 code_verifier의 쿠키 직렬화 형식
 */
public record OAuthTransaction(String state, String codeVerifier) {

    private static final String SEPARATOR = ".";

    /**
     * 쿠키에 담을 문자열로 인코딩
     */
    public String encode() {
        return encodePart(state) + SEPARATOR + encodePart(codeVerifier);
    }

    /**
     * 쿠키 문자열 디코딩. 형식이 깨졌으면 빈 결과를 반환한다.
     */
    public static Optional<OAuthTransaction> decode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.split("\\" + SEPARATOR);
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(new OAuthTransaction(decodePart(parts[0]), decodePart(parts[1])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String encodePart(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
