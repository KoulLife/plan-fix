package taedonghee.plan_fix.infrastructure.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.spot.SpotLikeModel;
import taedonghee.plan_fix.domain.spot.SpotLikeRepository;

/**
 * [infrastructure] domain.SpotLikeRepository 포트의 JPA 구현체(어댑터).
 */
@Repository
@RequiredArgsConstructor
public class SpotLikeRepositoryImpl implements SpotLikeRepository {

    private final SpotLikeJpaRepository spotLikeJpaRepository;

    @Override
    public boolean existsByUserIdAndSpotId(Long userId, Long spotId) {
        return spotLikeJpaRepository.findByUserIdAndSpotId(userId, spotId).isPresent();
    }

    @Override
    public java.util.Set<Long> findLikedSpotIds(Long userId, java.util.Collection<Long> spotIds) {
        if (userId == null || spotIds == null || spotIds.isEmpty()) {
            return java.util.Set.of();
        }
        return spotLikeJpaRepository.findAllByUserIdAndSpotIdIn(userId, spotIds).stream()
                .map(SpotLikeJpaEntity::getSpotId)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public SpotLikeModel save(SpotLikeModel like) {
        SpotLikeJpaEntity saved = spotLikeJpaRepository.save(SpotLikeJpaEntity.builder()
                .spotLikeId(like.spotLikeId())
                .userId(like.userId())
                .spotId(like.spotId())
                .createdAt(like.createdAt())
                .build());
        return SpotLikeModel.reconstruct(saved.getSpotLikeId(), saved.getUserId(), saved.getSpotId(), saved.getCreatedAt());
    }

    @Override
    public boolean deleteByUserIdAndSpotId(Long userId, Long spotId) {
        return spotLikeJpaRepository.deleteByUserIdAndSpotId(userId, spotId) > 0;
    }
}
