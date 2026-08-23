package taedonghee.plan_fix.infrastructure.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceGeneratorTest {

    private final PkceGenerator generator = new PkceGenerator();

    @Test
    void state는_호출마다_달라진다() {
        assertThat(generator.generateState()).isNotEqualTo(generator.generateState());
    }

    @Test
    void code_challenge는_RFC7636_예시와_일치한다() {
        // RFC 7636 Appendix B
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        assertThat(generator.codeChallenge(verifier)).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void code_verifier는_URL_안전_문자만_포함한다() {
        assertThat(generator.generateCodeVerifier()).matches("^[A-Za-z0-9_-]+$");
    }
}
