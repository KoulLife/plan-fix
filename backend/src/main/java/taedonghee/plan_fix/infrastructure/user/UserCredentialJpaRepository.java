package taedonghee.plan_fix.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * user_credentials 테이블 Spring Data JPA Repository
 */
public interface UserCredentialJpaRepository extends JpaRepository<UserCredentialJpaEntity, Long> {

    /**
     * login_id 기반 인증정보 조회
     */
    Optional<UserCredentialJpaEntity> findByLoginId(String loginId);

    /**
     * login_id 존재 여부 조회
     */
    boolean existsByLoginId(String loginId);
}
