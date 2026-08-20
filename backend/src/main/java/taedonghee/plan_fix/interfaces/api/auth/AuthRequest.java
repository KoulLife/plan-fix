package taedonghee.plan_fix.interfaces.api.auth;

import taedonghee.plan_fix.application.auth.AuthCommand;

/**
 * 인증 API 요청 DTO
 */
public final class AuthRequest {

    private AuthRequest() {
    }

    /**
     * 로그인 HTTP 요청값
     */
    public record Login(
            String loginId,
            String password
    ) {

        /**
         * application 입력값 변환
         */
        public AuthCommand.Login toCommand() {
            return new AuthCommand.Login(loginId, password);
        }
    }
}
