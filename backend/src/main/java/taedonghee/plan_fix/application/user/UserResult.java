package taedonghee.plan_fix.application.user;

import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRole;
import taedonghee.plan_fix.domain.user.UserStatus;

import java.time.OffsetDateTime;

/**
 * 사용자 결과 DTO
 */
public record UserResult(
        Long id,
        String username,
        String email,
        UserRole role,
        UserStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 사용자 도메인 모델 변환
     */
    public static UserResult from(UserModel user) {
        return new UserResult(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
