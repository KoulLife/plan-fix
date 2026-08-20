package taedonghee.plan_fix.domain.auth;

import taedonghee.plan_fix.domain.user.UserModel;

/**
 * 인증 토큰 발급
 */
public interface AuthTokenProvider {

    /**
     * 사용자 인증 토큰 생성
     */
    AuthToken create(UserModel user);
}
