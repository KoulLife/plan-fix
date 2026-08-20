package taedonghee.plan_fix.infrastructure.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;

import java.util.Optional;

/**
 * SpotRepository JPA 구현체
 */
@Repository
@RequiredArgsConstructor
public class SpotRepositoryImpl implements SpotRepository {

    private final SpotJpaRepository spotJpaRepository;

    /**
     * Spot 저장 처리
     */
    @Override
    public SpotModel save(SpotModel spot) {
        return toDomain(spotJpaRepository.save(toEntity(spot)));
    }

    /**
     * spot_id 기반 Spot 단건 조회 처리
     */
    @Override
    public Optional<SpotModel> findBySpotId(Long spotId) {
        return spotJpaRepository.findById(spotId).map(this::toDomain);
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     */
    private SpotJpaEntity toEntity(SpotModel spot) {
        return SpotJpaEntity.builder()
                .id(spot.getSpotId())
                .title(spot.getTitle())
                .category(spot.getCategory())
                .region(spot.getRegion())
                .sigungu(spot.getSigungu())
                .address(spot.getAddress())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .thumbnail(spot.getThumbnail())
                .description(spot.getDescription())
                .viewCount(spot.getViewCount())
                .likeCount(spot.getLikeCount())
                .commentCount(spot.getCommentCount())
                .status(spot.getStatus())
                .createdAt(spot.getCreatedAt())
                .updatedAt(spot.getUpdatedAt())
                .build();
    }

    /**
     * JPA 엔티티를 도메인 모델로 변환
     */
    private SpotModel toDomain(SpotJpaEntity entity) {
        return SpotModel.reconstruct(
                entity.getId(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getRegion(),
                entity.getSigungu(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getThumbnail(),
                entity.getDescription(),
                entity.getViewCount(),
                entity.getLikeCount(),
                entity.getCommentCount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
