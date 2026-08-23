package taedonghee.plan_fix.infrastructure.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthTransactionTest {

    @Test
    void 인코딩한_값을_다시_디코딩하면_원래대로_돌아온다() {
        OAuthTransaction original = new OAuthTransaction("state-value", "verifier-value");

        OAuthTransaction decoded = OAuthTransaction.decode(original.encode()).orElseThrow();

        assertThat(decoded.state()).isEqualTo("state-value");
        assertThat(decoded.codeVerifier()).isEqualTo("verifier-value");
    }

    @Test
    void 형식이_깨진_값은_비어_있는_결과를_준다() {
        assertThat(OAuthTransaction.decode("깨진값")).isEmpty();
        assertThat(OAuthTransaction.decode(null)).isEmpty();
        assertThat(OAuthTransaction.decode("")).isEmpty();
    }
}
