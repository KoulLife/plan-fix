package taedonghee.plan_fix.application.auth;

/**
 * 인증 Command DTO
 */
public final class AuthCommand {

    private AuthCommand() {
    }

    /**
     * 로그인 입력값
     */
    public record Login(
            String loginId,
            String password
    ) {
    }
}
