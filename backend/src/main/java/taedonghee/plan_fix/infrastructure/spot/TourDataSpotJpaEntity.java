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
 * [infrastructure] TourDataSpot의 JPA 매핑 전용 엔티티. domain.spot.TourDataSpotModel과 별개로 둔다.
 * JPA가 요구하는 기본 생성자/PK 전략 같은 영속성 관심사가 domain을 오염시키지 않게 하기 위함.
 */
@Entity
@Table(
	name = "tour_data_spots",
	indexes = {
		@Index(name = "idx_tour_data_spots_contentid", columnList = "contentid", unique = true),
		@Index(name = "idx_tour_data_spots_sigungu", columnList = "sigungu")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourDataSpotJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tour_data_spot_id")
	private Long tourDataSpotId;

	@Column(name = "contentid", nullable = false)
	private Long contentId;

	@Column(name = "spot_id", nullable = false)
	private Long spotId;

	@Column(name = "address", length = 500)
	private String address;

	@Column(name = "category", length = 20)
	private String category;

	@Column(name = "createdtime", length = 20)
	private String createdTime;

	@Column(name = "thumbnail", length = 500)
	private String thumbnail;

	@Column(name = "mapx")
	private Double mapX;

	@Column(name = "mapy")
	private Double mapY;

	@Column(name = "title", nullable = false, length = 300)
	private String title;

	@Column(name = "reg", length = 10)
	private String reg;

	@Column(name = "sigungu", length = 10)
	private String sigungu;

	@Column(name = "lcls", length = 20)
	private String lcls;

	@Column(name = "zipcode", length = 20)
	private String zipcode;

	/** detailImage2 조회를 시도한 시각. null이면 아직 시도 안 함(재실행 대상). */
	@Column(name = "image_collected_at", columnDefinition = "timestamptz")
	private OffsetDateTime imageCollectedAt;

	/** detailIntro2 조회를 시도한 시각. null이면 아직 시도 안 함(재실행 대상). */
	@Column(name = "info_collected_at", columnDefinition = "timestamptz")
	private OffsetDateTime infoCollectedAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime updatedAt;

	@Builder
	private TourDataSpotJpaEntity(Long tourDataSpotId, Long contentId, Long spotId, String address, String category,
		String createdTime, String thumbnail, Double mapX, Double mapY, String title, String reg, String sigungu,
		String lcls, String zipcode, OffsetDateTime imageCollectedAt, OffsetDateTime infoCollectedAt,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.tourDataSpotId = tourDataSpotId;
		this.contentId = contentId;
		this.spotId = spotId;
		this.address = address;
		this.category = category;
		this.createdTime = createdTime;
		this.thumbnail = thumbnail;
		this.mapX = mapX;
		this.mapY = mapY;
		this.title = title;
		this.reg = reg;
		this.sigungu = sigungu;
		this.lcls = lcls;
		this.zipcode = zipcode;
		this.imageCollectedAt = imageCollectedAt;
		this.infoCollectedAt = infoCollectedAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
}
