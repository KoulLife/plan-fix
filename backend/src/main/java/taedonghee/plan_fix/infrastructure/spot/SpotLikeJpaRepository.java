package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface SpotLikeJpaRepository extends JpaRepository<SpotLikeJpaEntity, Long> {

    Optional<SpotLikeJpaEntity> findByUserIdAndSpotId(Long userId, Long spotId);

    List<SpotLikeJpaEntity> findAllByUserIdAndSpotIdIn(Long userId, Collection<Long> spotIds);

    @Modifying
    @Query("DELETE FROM SpotLikeJpaEntity s WHERE s.userId = :userId AND s.spotId = :spotId")
    long deleteByUserIdAndSpotId(@Param("userId") Long userId, @Param("spotId") Long spotId);
}
