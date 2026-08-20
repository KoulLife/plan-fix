package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * spots 테이블 Spring Data JPA Repository
 */
public interface SpotJpaRepository extends JpaRepository<SpotJpaEntity, Long> {
}
