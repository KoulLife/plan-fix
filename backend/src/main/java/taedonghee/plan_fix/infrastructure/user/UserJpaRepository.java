package taedonghee.plan_fix.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
