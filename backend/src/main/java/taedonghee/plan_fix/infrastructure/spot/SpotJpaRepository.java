package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface SpotJpaRepository extends JpaRepository<SpotJpaEntity, Long> {

	/**
	 * 공개 목록 조회(최신순). status는 ACTIVE로 고정하고, 나머지 조건은 null이면 걸지 않는다.
	 * offset/limit은 JPQL의 LIMIT/OFFSET 절(Jakarta Persistence 3.1+)로 직접 처리한다 —
	 * Spring Data의 Pageable은 page*size로만 offset을 계산해 임의의 offset을 표현할 수 없어서다.
	 */
	@Query("""
			SELECT s FROM SpotJpaEntity s
			WHERE s.status = taedonghee.plan_fix.domain.spot.SpotStatus.ACTIVE
			  AND (:category IS NULL OR s.category = :category)
			  AND (:region IS NULL OR s.region = :region)
			  AND (:sigungu IS NULL OR s.sigungu = :sigungu)
			ORDER BY s.spotId DESC
			LIMIT :limit OFFSET :offset
			""")
	List<SpotJpaEntity> searchActiveByLatest(
			@Param("category") String category,
			@Param("region") String region,
			@Param("sigungu") String sigungu,
			@Param("limit") int limit,
			@Param("offset") int offset
	);

	/**
	 * 공개 목록 조회(인기순). 인기 점수 = like_count*0.9 + view_count*0.1, 동점이면 spotId 내림차순.
	 * 가중치가 정책값이라 상수 대신 리터럴로 박아 둔다 — 바뀌면 이 쿼리도 함께 바뀌어야 한다.
	 */
	@Query("""
			SELECT s FROM SpotJpaEntity s
			WHERE s.status = taedonghee.plan_fix.domain.spot.SpotStatus.ACTIVE
			  AND (:category IS NULL OR s.category = :category)
			  AND (:region IS NULL OR s.region = :region)
			  AND (:sigungu IS NULL OR s.sigungu = :sigungu)
			ORDER BY (s.likeCount * 0.9 + s.viewCount * 0.1) DESC, s.spotId DESC
			LIMIT :limit OFFSET :offset
			""")
	List<SpotJpaEntity> searchActiveByPopular(
			@Param("category") String category,
			@Param("region") String region,
			@Param("sigungu") String sigungu,
			@Param("limit") int limit,
			@Param("offset") int offset
	);

	/** searchActive와 같은 조건의 전체 건수. */
	@Query("""
			SELECT COUNT(s) FROM SpotJpaEntity s
			WHERE s.status = taedonghee.plan_fix.domain.spot.SpotStatus.ACTIVE
			  AND (:category IS NULL OR s.category = :category)
			  AND (:region IS NULL OR s.region = :region)
			  AND (:sigungu IS NULL OR s.sigungu = :sigungu)
			""")
	long countActive(
			@Param("category") String category,
			@Param("region") String region,
			@Param("sigungu") String sigungu
	);

	/**
	 * view_count를 DB에서 직접 +1 한다. clearAutomatically로 영속성 컨텍스트를 비워서,
	 * 같은 트랜잭션 안에서 이후 조회가 캐시된(증가 전) 엔티티가 아니라 DB의 최신 값을 읽게 한다.
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE SpotJpaEntity s SET s.viewCount = s.viewCount + 1 WHERE s.spotId = :spotId")
	void incrementViewCount(@Param("spotId") Long spotId);

	/** like_count를 DB에서 직접 +1 한다. */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE SpotJpaEntity s SET s.likeCount = s.likeCount + 1 WHERE s.spotId = :spotId")
	void incrementLikeCount(@Param("spotId") Long spotId);

	/** like_count를 DB에서 직접 -1 한다. 0 밑으로 내려가지 않게 CASE로 가드한다. */
	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE SpotJpaEntity s
			SET s.likeCount = CASE WHEN s.likeCount > 0 THEN s.likeCount - 1 ELSE 0 END
			WHERE s.spotId = :spotId
			""")
	void decrementLikeCount(@Param("spotId") Long spotId);
}
