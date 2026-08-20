package taedonghee.plan_fix.domain.spot;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Spot Model
 */
public class SpotModel {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int CATEGORY_MAX_LENGTH = 50;
    private static final int REGION_MAX_LENGTH = 50;
    private static final int SIGUNGU_MAX_LENGTH = 50;
    private static final int ADDRESS_MAX_LENGTH = 255;
    private static final int THUMBNAIL_MAX_LENGTH = 500;
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    private final Long spotId;
    private final String title;
    private final String category;
    private final String region;
    private final String sigungu;
    private final String address;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String thumbnail;
    private final String description;
    private final Long viewCount;
    private final Long likeCount;
    private final Long commentCount;
    private final SpotStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private SpotModel(
            Long spotId,
            String title,
            String category,
            String region,
            String sigungu,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String thumbnail,
            String description,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            SpotStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateRequiredText(title, "title", TITLE_MAX_LENGTH);
        validateRequiredText(category, "category", CATEGORY_MAX_LENGTH);
        validateNullableText(region, "region", REGION_MAX_LENGTH);
        validateNullableText(sigungu, "sigungu", SIGUNGU_MAX_LENGTH);
        validateNullableText(address, "address", ADDRESS_MAX_LENGTH);
        validateNullableText(thumbnail, "thumbnail", THUMBNAIL_MAX_LENGTH);
        validateCoordinate(latitude, "latitude", MIN_LATITUDE, MAX_LATITUDE);
        validateCoordinate(longitude, "longitude", MIN_LONGITUDE, MAX_LONGITUDE);
        validateCount(viewCount, "viewCount");
        validateCount(likeCount, "likeCount");
        validateCount(commentCount, "commentCount");

        this.spotId = spotId;
        this.title = title;
        this.category = category;
        this.region = region;
        this.sigungu = sigungu;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.thumbnail = thumbnail;
        this.description = description;
        this.viewCount = viewCount == null ? 0L : viewCount;
        this.likeCount = likeCount == null ? 0L : likeCount;
        this.commentCount = commentCount == null ? 0L : commentCount;
        this.status = status == null ? SpotStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 Spot 생성
     */
    public static SpotModel create(
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
        OffsetDateTime now = OffsetDateTime.now();
        return new SpotModel(
                null,
                title,
                category,
                region,
                sigungu,
                address,
                latitude,
                longitude,
                thumbnail,
                description,
                0L,
                0L,
                0L,
                SpotStatus.ACTIVE,
                now,
                now
        );
    }

    /**
     * 저장된 Spot 정보 기반 도메인 복원
     */
    public static SpotModel reconstruct(
            Long spotId,
            String title,
            String category,
            String region,
            String sigungu,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String thumbnail,
            String description,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            SpotStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new SpotModel(
                spotId,
                title,
                category,
                region,
                sigungu,
                address,
                latitude,
                longitude,
                thumbnail,
                description,
                viewCount,
                likeCount,
                commentCount,
                status,
                createdAt,
                updatedAt
        );
    }

    /**
     * Spot 기본 정보 수정
     */
    public SpotModel update(
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
        if (status == SpotStatus.DELETED) {
            throw new CoreException(ErrorType.CONFLICT, "삭제된 Spot은 수정할 수 없습니다. spotId=" + spotId);
        }
        return new SpotModel(
                spotId,
                title,
                category,
                region,
                sigungu,
                address,
                latitude,
                longitude,
                thumbnail,
                description,
                viewCount,
                likeCount,
                commentCount,
                status,
                createdAt,
                OffsetDateTime.now()
        );
    }

    /**
     * Spot 삭제 상태 변경
     */
    public SpotModel delete() {
        if (status == SpotStatus.DELETED) {
            throw new CoreException(ErrorType.CONFLICT, "이미 삭제된 Spot입니다. spotId=" + spotId);
        }
        return new SpotModel(
                spotId,
                title,
                category,
                region,
                sigungu,
                address,
                latitude,
                longitude,
                thumbnail,
                description,
                viewCount,
                likeCount,
                commentCount,
                SpotStatus.DELETED,
                createdAt,
                OffsetDateTime.now()
        );
    }

    /**
     * 필수 문자열 길이 검증
     */
    private void validateRequiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + "는 필수입니다");
        }
        if (value.length() > maxLength) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + "는 " + maxLength + "자 이하여야 합니다");
        }
    }

    /**
     * 선택 문자열 길이 검증
     */
    private void validateNullableText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + "는 공백일 수 없습니다");
        }
        if (value.length() > maxLength) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + "는 " + maxLength + "자 이하여야 합니다");
        }
    }

    /**
     * 위도 경도 범위 검증
     */
    private void validateCoordinate(BigDecimal value, String fieldName, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return;
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + " 범위가 올바르지 않습니다");
        }
    }

    /**
     * 집계 수치 음수 검증
     */
    private void validateCount(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + "는 음수일 수 없습니다");
        }
    }

    public Long getSpotId() {
        return spotId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getRegion() {
        return region;
    }

    public String getSigungu() {
        return sigungu;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getDescription() {
        return description;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
