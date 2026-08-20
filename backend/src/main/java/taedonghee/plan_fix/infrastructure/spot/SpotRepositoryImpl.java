package taedonghee.plan_fix.infrastructure.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;

import java.util.Optional;

/**
 * [infrastructure] domain.SpotRepository 포트의 JPA 구현체(어댑터).
 * domain <- infrastructure: domain의 인터페이스를 구현하며, JPA 엔티티 <-> domain 모델 변환을 책임진다.
 */
@Repository
@RequiredArgsConstructor
public class SpotRepositoryImpl implements SpotRepository {

	private final SpotJpaRepository spotJpaRepository;

	@Override
	public SpotModel save(SpotModel spot) {
		SpotJpaEntity saved = spotJpaRepository.save(toEntity(spot));
		return toDomain(saved);
	}

	@Override
	public Optional<SpotModel> findById(Long spotId) {
		return spotJpaRepository.findById(spotId).map(this::toDomain);
	}

	@Override
	public long countAll() {
		return spotJpaRepository.count();
	}

	private SpotJpaEntity toEntity(SpotModel spot) {
		return SpotJpaEntity.builder()
			.spotId(spot.spotId())
			.build();
	}

	private SpotModel toDomain(SpotJpaEntity entity) {
		return SpotModel.of(entity.getSpotId());
	}
}
