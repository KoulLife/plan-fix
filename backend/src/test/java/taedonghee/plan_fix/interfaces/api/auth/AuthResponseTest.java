package taedonghee.plan_fix.interfaces.api.auth;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.application.auth.AuthResult;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void 응답에는_토큰이_없고_사용자_정보만_담긴다() {
        AuthResponse response = AuthResponse.from(
                new AuthResult("jwt-value", 3600, 1L, "gildong", "a@b.com"));

        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().username()).isEqualTo("gildong");
        assertThat(response.user().email()).isEqualTo("a@b.com");
    }
}
