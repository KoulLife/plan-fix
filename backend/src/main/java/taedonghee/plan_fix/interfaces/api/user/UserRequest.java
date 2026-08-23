package taedonghee.plan_fix.interfaces.api.user;

import taedonghee.plan_fix.application.user.UserCommand;

/**
 * 사용자 API 요청 DTO
 */
public final class UserRequest {

    private UserRequest() {
    }

    /**
     * 사용자 생성 HTTP 요청값
     */
    public record Create(
            String username,
            String name,
            String email,
            String loginId,
            String password
    ) {

        /**
         * application 입력값 변환
         */
        public UserCommand.Create toCommand() {
            return new UserCommand.Create(username, name, email, loginId, password);
        }
    }

    /**
     * 사용자 프로필 수정 HTTP 요청값
     */
    public record Update(
            String username,
            String name,
            String email
    ) {

        /**
         * application 입력값 변환
         */
        public UserCommand.Update toCommand() {
            return new UserCommand.Update(username, name, email);
        }
    }
}
