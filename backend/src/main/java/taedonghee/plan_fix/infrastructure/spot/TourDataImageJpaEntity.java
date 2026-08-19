package taedonghee.plan_fix.infrastructure.spot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * [infrastructure] TourDataImage의 JPA 매핑 전용 엔티티. domain.spot.TourDataImageModel과 별개로 둔다.
 * JPA가 요구하는 기본 생성자/PK 전략 같은 영속성 관심사가 domain을 오염시키지 않게 하기 위함.
 */
@Entity
@Table(
	name = "tour_data_images",
	indexes = @Index(name = "idx_tour_data_images_spot", columnList = "tour_data_spot_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourDataImageJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tour_data_image_id")
	private Long tourDataImageId;

	@Column(name = "tour_data_spot_id", nullable = false)
	private Long tourDataSpotId;

	@Column(name = "contentid")
	private Long contentId;

	@Column(name = "image_name", length = 300)
	private String imageName;

	@Column(name = "original_image", length = 500)
	private String originalImage;

	@Column(name = "small_image", length = 500)
	private String smallImage;

	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime updatedAt;

	@Builder
	private TourDataImageJpaEntity(Long tourDataImageId, Long tourDataSpotId, Long contentId, String imageName,
		String originalImage, String smallImage, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.tourDataImageId = tourDataImageId;
		this.tourDataSpotId = tourDataSpotId;
		this.contentId = contentId;
		this.imageName = imageName;
		this.originalImage = originalImage;
		this.smallImage = smallImage;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
}
