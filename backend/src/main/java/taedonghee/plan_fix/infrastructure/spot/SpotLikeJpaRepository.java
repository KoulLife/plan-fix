package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface SpotLikeJpaRepository extends JpaRepository<SpotLikeJpaEntity, Long> {

    Optional<SpotLikeJpaEntity> findByUserIdAndSpotId(Long userId, Long spotId);

    /** deleteBy 파생 쿼리는 Spring Data가 자동으로 트랜잭션을 걸고, 지운 행 수를 반환한다. */
    long deleteByUserIdAndSpotId(Long userId, Long spotId);
}
