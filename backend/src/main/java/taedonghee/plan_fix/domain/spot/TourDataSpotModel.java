package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * [domain] TourAPI 등 외부 관광 데이터 소스로부터 수집한 스팟 원본 정보.
 * JPA 등 프레임워크 의존 없이, 비즈니스 규칙만 표현한다. 영속성 매핑은 infrastructure가 담당한다.
 * spotId로 canonical 엔티티인 SpotModel을 참조한다.
 * 필드가 많아 생성은 {@link Builder}를 통해서만 한다.
 */
public class TourDataSpotModel {

	private final Long tourDataSpotId;
	private final Long contentId;
	private Long spotId;
	private String address;
	private String category;
	private String createdTime;
	private String thumbnail;
	private Double mapX;
	private Double mapY;
	private String title;
	private String reg;
	private String sigungu;
	private String lcls;
	private String zipcode;
	private OffsetDateTime imageCollectedAt;
	private OffsetDateTime infoCollectedAt;
	private final OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

	private TourDataSpotModel(Builder builder) {
		if (builder.contentId == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "contentId는 필수입니다.");
		}
		if (builder.title == null || builder.title.isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "title은 비어 있을 수 없습니다.");
		}

		this.tourDataSpotId = builder.tourDataSpotId;
		this.contentId = builder.contentId;
		this.spotId = builder.spotId;
		this.address = builder.address;
		this.category = builder.category;
		this.createdTime = builder.createdTime;
		this.thumbnail = builder.thumbnail;
		this.mapX = builder.mapX;
		this.mapY = builder.mapY;
		this.title = builder.title;
		this.reg = builder.reg;
		this.sigungu = builder.sigungu;
		this.lcls = builder.lcls;
		this.zipcode = builder.zipcode;
		this.imageCollectedAt = builder.imageCollectedAt;
		this.infoCollectedAt = builder.infoCollectedAt;
		this.createdAt = builder.createdAt;
		this.updatedAt = builder.updatedAt;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 파싱 시점엔 이 스팟이 속할 SpotModel이 아직 저장 전이라 spotId를 모른다.
	 * SpotModel이 저장돼 id가 생긴 뒤, 그 id를 여기에 한 번만 채운다.
	 */
	public void assignSpotId(Long spotId) {
		if (this.spotId != null) {
			throw new CoreException(ErrorType.CONFLICT, "spotId는 이미 지정되어 있습니다.");
		}
		if (spotId == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "spotId는 필수입니다.");
		}
		this.spotId = spotId;
	}

	/** TourAPI 재수집 결과로 속성을 갱신한다 (spotId, contentId, createdAt은 불변) */
	public void updateFromTourApi(String address, String category, String createdTime, String thumbnail,
		Double mapX, Double mapY, String title, String reg, String sigungu, String lcls, String zipcode) {
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
		touchUpdated();
	}

	/**
	 * detailImage2 조회를 "시도했음"을 기록한다. 이미지가 0장이어도 찍는다.
	 * 재실행 시 이 값이 null인 건만 다시 호출해서 API 할당량을 아끼기 위함이다.
	 * (실패한 건은 찍지 않으므로 다음 실행 때 자연히 재시도된다.)
	 */
	public void markImageCollected() {
		this.imageCollectedAt = OffsetDateTime.now();
	}

	/** detailIntro2 조회를 "시도했음"을 기록한다. 상세 정보가 없어도 찍는다. */
	public void markInfoCollected() {
		this.infoCollectedAt = OffsetDateTime.now();
	}

	private void touchUpdated() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Long tourDataSpotId() { return tourDataSpotId; }
	public Long contentId() { return contentId; }
	public Long spotId() { return spotId; }
	public String address() { return address; }
	public String category() { return category; }
	public String createdTime() { return createdTime; }
	public String thumbnail() { return thumbnail; }
	public Double mapX() { return mapX; }
	public Double mapY() { return mapY; }
	public String title() { return title; }
	public String reg() { return reg; }
	public String sigungu() { return sigungu; }
	public String lcls() { return lcls; }
	public String zipcode() { return zipcode; }
	public OffsetDateTime imageCollectedAt() { return imageCollectedAt; }
	public OffsetDateTime infoCollectedAt() { return infoCollectedAt; }
	public OffsetDateTime createdAt() { return createdAt; }
	public OffsetDateTime updatedAt() { return updatedAt; }

	/**
	 * [domain] TourDataSpotModel 빌더.
	 * 신규 생성(createdAt/updatedAt 미지정 시 현재 시각)과, infrastructure의 영속 데이터 복원(모든 필드 지정)
	 * 두 경우 모두 이 빌더 하나로 처리한다.
	 */
	public static class Builder {
		private Long tourDataSpotId;
		private Long contentId;
		private Long spotId;
		private String address;
		private String category;
		private String createdTime;
		private String thumbnail;
		private Double mapX;
		private Double mapY;
		private String title;
		private String reg;
		private String sigungu;
		private String lcls;
		private String zipcode;
		private OffsetDateTime imageCollectedAt;
		private OffsetDateTime infoCollectedAt;
		private OffsetDateTime createdAt = OffsetDateTime.now();
		private OffsetDateTime updatedAt = OffsetDateTime.now();

		private Builder() {
		}

		public Builder tourDataSpotId(Long tourDataSpotId) { this.tourDataSpotId = tourDataSpotId; return this; }
		public Builder contentId(Long contentId) { this.contentId = contentId; return this; }
		public Builder spotId(Long spotId) { this.spotId = spotId; return this; }
		public Builder address(String address) { this.address = address; return this; }
		public Builder category(String category) { this.category = category; return this; }
		public Builder createdTime(String createdTime) { this.createdTime = createdTime; return this; }
		public Builder thumbnail(String thumbnail) { this.thumbnail = thumbnail; return this; }
		public Builder mapX(Double mapX) { this.mapX = mapX; return this; }
		public Builder mapY(Double mapY) { this.mapY = mapY; return this; }
		public Builder title(String title) { this.title = title; return this; }
		public Builder reg(String reg) { this.reg = reg; return this; }
		public Builder sigungu(String sigungu) { this.sigungu = sigungu; return this; }
		public Builder lcls(String lcls) { this.lcls = lcls; return this; }
		public Builder zipcode(String zipcode) { this.zipcode = zipcode; return this; }
		public Builder imageCollectedAt(OffsetDateTime imageCollectedAt) { this.imageCollectedAt = imageCollectedAt; return this; }
		public Builder infoCollectedAt(OffsetDateTime infoCollectedAt) { this.infoCollectedAt = infoCollectedAt; return this; }
		public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
		public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

		public TourDataSpotModel build() {
			return new TourDataSpotModel(this);
		}
	}
}
