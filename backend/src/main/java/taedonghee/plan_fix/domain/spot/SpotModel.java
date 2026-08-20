package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * [domain] 스팟(spot)의 canonical 모델. JPA 등 프레임워크 의존 없이, 비즈니스 규칙만 표현한다.
 *
 * 여러 데이터 소스(TourAPI, 향후 카카오·네이버)와 서비스 직접등록을 아우르는 상위 개념이다.
 * 소스별 원본은 TourDataSpotModel처럼 각자의 모델로 두고, 그쪽이 spotId로 이 스팟을 참조한다.
 * 직접등록 스팟은 대응하는 소스 모델이 아예 없으므로, 이 모델은 소스 없이도 완결돼야 한다.
 *
 * 필드는 소유권에 따라 셋으로 나뉜다.
 * - 소스 유래: title, category, region, sigungu, address, latitude, longitude, thumbnail, description
 *   → 그 스팟을 소유한 소스만 {@link #updateFromSource}로 갱신한다.
 * - 서비스 소유: viewCount, likeCount, commentCount, status
 *   → 수집이 건드리면 안 되므로 갱신 메서드로 접근할 수 없다.
 * - 메타: createdAt, updatedAt
 */
public class SpotModel {

	private final Long spotId;
	private final SpotSourceType sourceType;

	// 소스 유래
	private String title;
	private String category;
	private String region;
	private String sigungu;
	private String address;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private String thumbnail;
	private String description;

	// 서비스 소유
	private final long viewCount;
	private final long likeCount;
	private final long commentCount;
	private final SpotStatus status;

	private final OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

	private SpotModel(Builder builder) {
		if (builder.sourceType == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "sourceType은 필수입니다.");
		}
		if (builder.title == null || builder.title.isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "title은 비어 있을 수 없습니다.");
		}
		if (builder.category == null || builder.category.isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "category는 비어 있을 수 없습니다.");
		}

		this.spotId = builder.spotId;
		this.sourceType = builder.sourceType;
		this.title = builder.title;
		this.category = builder.category;
		this.region = builder.region;
		this.sigungu = builder.sigungu;
		this.address = builder.address;
		this.latitude = builder.latitude;
		this.longitude = builder.longitude;
		this.thumbnail = builder.thumbnail;
		this.description = builder.description;
		this.viewCount = builder.viewCount;
		this.likeCount = builder.likeCount;
		this.commentCount = builder.commentCount;
		this.status = builder.status;
		this.createdAt = builder.createdAt;
		this.updatedAt = builder.updatedAt;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 소스 유래 필드를 갱신한다. 재수집 경로에서 쓴다.
	 *
	 * 요청한 소스가 이 스팟의 소유 소스가 아니면 아무것도 하지 않고 false를 반환한다.
	 * 예외를 던지지 않는 이유는, 1000건 단위 수집 배치가 남의 소스 스팟 하나 때문에 멈추면 안 되기 때문이다.
	 * 카운터와 status는 서비스 소유라 이 메서드로는 건드릴 수 없다.
	 *
	 * @return 갱신했으면 true, 소유 소스가 아니라 건너뛰었으면 false
	 */
	public boolean updateFromSource(SpotSourceType requestedBy, SourceAttributes attributes) {
		if (requestedBy == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "requestedBy는 필수입니다.");
		}
		if (this.sourceType != requestedBy) {
			return false;
		}
		if (attributes.title() == null || attributes.title().isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "title은 비어 있을 수 없습니다.");
		}
		if (attributes.category() == null || attributes.category().isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "category는 비어 있을 수 없습니다.");
		}

		this.title = attributes.title();
		this.category = attributes.category();
		this.region = attributes.region();
		this.sigungu = attributes.sigungu();
		this.address = attributes.address();
		this.latitude = attributes.latitude();
		this.longitude = attributes.longitude();
		this.thumbnail = attributes.thumbnail();
		this.description = attributes.description();
		this.updatedAt = OffsetDateTime.now();
		return true;
	}

	public Long spotId() { return spotId; }
	public SpotSourceType sourceType() { return sourceType; }
	public String title() { return title; }
	public String category() { return category; }
	public String region() { return region; }
	public String sigungu() { return sigungu; }
	public String address() { return address; }
	public BigDecimal latitude() { return latitude; }
	public BigDecimal longitude() { return longitude; }
	public String thumbnail() { return thumbnail; }
	public String description() { return description; }
	public long viewCount() { return viewCount; }
	public long likeCount() { return likeCount; }
	public long commentCount() { return commentCount; }
	public SpotStatus status() { return status; }
	public OffsetDateTime createdAt() { return createdAt; }
	public OffsetDateTime updatedAt() { return updatedAt; }

	/**
	 * [domain] 소스가 제공하는 필드 묶음. {@link #updateFromSource}와 신규 생성이 같은 형태를 쓰도록 모아 둔다.
	 * 카운터·status가 여기 없는 것이 곧 "수집은 그 값을 정할 수 없다"는 규칙이다.
	 */
	public record SourceAttributes(
		String title,
		String category,
		String region,
		String sigungu,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String thumbnail,
		String description
	) {
	}

	/**
	 * [domain] SpotModel 빌더.
	 * 신규 생성(카운터 0, status ACTIVE)과 infrastructure의 영속 데이터 복원(모든 필드 지정)을 함께 처리한다.
	 */
	public static class Builder {
		private Long spotId;
		private SpotSourceType sourceType;
		private String title;
		private String category;
		private String region;
		private String sigungu;
		private String address;
		private BigDecimal latitude;
		private BigDecimal longitude;
		private String thumbnail;
		private String description;
		private long viewCount = 0L;
		private long likeCount = 0L;
		private long commentCount = 0L;
		private SpotStatus status = SpotStatus.ACTIVE;
		private OffsetDateTime createdAt = OffsetDateTime.now();
		private OffsetDateTime updatedAt = OffsetDateTime.now();

		private Builder() {
		}

		public Builder spotId(Long spotId) { this.spotId = spotId; return this; }
		public Builder sourceType(SpotSourceType sourceType) { this.sourceType = sourceType; return this; }
		public Builder title(String title) { this.title = title; return this; }
		public Builder category(String category) { this.category = category; return this; }
		public Builder region(String region) { this.region = region; return this; }
		public Builder sigungu(String sigungu) { this.sigungu = sigungu; return this; }
		public Builder address(String address) { this.address = address; return this; }
		public Builder latitude(BigDecimal latitude) { this.latitude = latitude; return this; }
		public Builder longitude(BigDecimal longitude) { this.longitude = longitude; return this; }
		public Builder thumbnail(String thumbnail) { this.thumbnail = thumbnail; return this; }
		public Builder description(String description) { this.description = description; return this; }
		public Builder viewCount(long viewCount) { this.viewCount = viewCount; return this; }
		public Builder likeCount(long likeCount) { this.likeCount = likeCount; return this; }
		public Builder commentCount(long commentCount) { this.commentCount = commentCount; return this; }
		public Builder status(SpotStatus status) { this.status = status; return this; }
		public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
		public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

		/** 소스가 제공하는 필드를 한 번에 채운다. */
		public Builder attributes(SourceAttributes attributes) {
			return title(attributes.title())
				.category(attributes.category())
				.region(attributes.region())
				.sigungu(attributes.sigungu())
				.address(attributes.address())
				.latitude(attributes.latitude())
				.longitude(attributes.longitude())
				.thumbnail(attributes.thumbnail())
				.description(attributes.description());
		}

		public SpotModel build() {
			return new SpotModel(this);
		}
	}
}
