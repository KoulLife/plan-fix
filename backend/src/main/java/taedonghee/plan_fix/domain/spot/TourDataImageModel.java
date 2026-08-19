package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * [domain] 관광 데이터 스팟(TourDataSpotModel)에 딸린 이미지 한 장.
 * 하나의 TourDataSpotModel이 여러 TourDataImageModel을 가질 수 있다 (tourDataSpotId로 참조).
 * JPA 등 프레임워크 의존 없이, 비즈니스 규칙만 표현한다.
 */
public class TourDataImageModel {

	private final Long tourDataImageId;
	private final Long tourDataSpotId;
	private final Long contentId;
	private final String imageName;
	private final String originalImage;
	private final String smallImage;
	private final OffsetDateTime createdAt;
	private final OffsetDateTime updatedAt;

	private TourDataImageModel(Builder builder) {
		if (builder.tourDataSpotId == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "tourDataSpotId는 필수입니다.");
		}
		if (builder.originalImage == null || builder.originalImage.isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "originalImage는 비어 있을 수 없습니다.");
		}

		this.tourDataImageId = builder.tourDataImageId;
		this.tourDataSpotId = builder.tourDataSpotId;
		this.contentId = builder.contentId;
		this.imageName = builder.imageName;
		this.originalImage = builder.originalImage;
		this.smallImage = builder.smallImage;
		this.createdAt = builder.createdAt;
		this.updatedAt = builder.updatedAt;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Long tourDataImageId() { return tourDataImageId; }
	public Long tourDataSpotId() { return tourDataSpotId; }
	public Long contentId() { return contentId; }
	public String imageName() { return imageName; }
	public String originalImage() { return originalImage; }
	public String smallImage() { return smallImage; }
	public OffsetDateTime createdAt() { return createdAt; }
	public OffsetDateTime updatedAt() { return updatedAt; }

	/**
	 * [domain] TourDataImageModel 빌더.
	 * 신규 생성(createdAt/updatedAt 미지정 시 현재 시각)과, infrastructure의 영속 데이터 복원(모든 필드 지정)
	 * 두 경우 모두 이 빌더 하나로 처리한다.
	 */
	public static class Builder {
		private Long tourDataImageId;
		private Long tourDataSpotId;
		private Long contentId;
		private String imageName;
		private String originalImage;
		private String smallImage;
		private OffsetDateTime createdAt = OffsetDateTime.now();
		private OffsetDateTime updatedAt = OffsetDateTime.now();

		private Builder() {
		}

		public Builder tourDataImageId(Long tourDataImageId) { this.tourDataImageId = tourDataImageId; return this; }
		public Builder tourDataSpotId(Long tourDataSpotId) { this.tourDataSpotId = tourDataSpotId; return this; }
		public Builder contentId(Long contentId) { this.contentId = contentId; return this; }
		public Builder imageName(String imageName) { this.imageName = imageName; return this; }
		public Builder originalImage(String originalImage) { this.originalImage = originalImage; return this; }
		public Builder smallImage(String smallImage) { this.smallImage = smallImage; return this; }
		public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
		public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

		public TourDataImageModel build() {
			return new TourDataImageModel(this);
		}
	}
}
