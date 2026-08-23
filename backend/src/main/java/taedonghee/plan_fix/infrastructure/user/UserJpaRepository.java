package taedonghee.plan_fix.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * users 테이블 Spring Data JPA Repository
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * username 존재 여부 조회
     */
    boolean existsByUsername(String username);

    /**
     * email 존재 여부 조회
     */
    boolean existsByEmail(String email);

    /**
     * email 기반 사용자 단건 조회
     */
    Optional<UserJpaEntity> findByEmail(String email);
}
