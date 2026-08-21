package taedonghee.plan_fix.infrastructure.spot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.spot.SpotStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * [infrastructure] Spot의 JPA 매핑 전용 엔티티. domain.spot.SpotModel과 별개로 둔다.
 * JPA가 요구하는 기본 생성자/PK 전략 같은 영속성 관심사가 domain을 오염시키지 않게 하기 위함.
 */
@Entity
@Table(
	name = "spots",
	indexes = {
		@Index(name = "idx_spots_region_sigungu", columnList = "region, sigungu"),
		@Index(name = "idx_spots_status", columnList = "status")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "spot_id")
	private Long spotId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 20)
	private SpotSourceType sourceType;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "category", nullable = false, length = 50)
	private String category;

	@Column(name = "region", length = 50)
	private String region;

	@Column(name = "sigungu", length = 50)
	private String sigungu;

	@Column(name = "address", length = 255)
	private String address;

	@Column(name = "latitude", precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(name = "thumbnail", length = 500)
	private String thumbnail;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "like_count", nullable = false)
	private long likeCount;

	@Column(name = "comment_count", nullable = false)
	private long commentCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SpotStatus status;

	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime updatedAt;

	@Builder
	private SpotJpaEntity(Long spotId, SpotSourceType sourceType, String title, String category, String region,
		String sigungu, String address, BigDecimal latitude, BigDecimal longitude, String thumbnail,
		String description, long viewCount, long likeCount, long commentCount, SpotStatus status,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.spotId = spotId;
		this.sourceType = sourceType;
		this.title = title;
		this.category = category;
		this.region = region;
		this.sigungu = sigungu;
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
		this.thumbnail = thumbnail;
		this.description = description;
		this.viewCount = viewCount;
		this.likeCount = likeCount;
		this.commentCount = commentCount;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
}
