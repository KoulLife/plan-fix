package taedonghee.plan_fix.application.user;

/**
 * 사용자 Command DTO
 */
public final class UserCommand {

    private UserCommand() {
    }

    /**
     * 사용자 생성 입력값
     */
    public record Create(
            String username,
            String email,
            String loginId,
            String password
    ) {
    }

    /**
     * 사용자 프로필 수정 입력값
     */
    public record Update(
            String username,
            String email
    ) {
    }
}
