package taedonghee.plan_fix.domain.board;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.net.URI;

/**
 * 게시글 이미지 값 객체
 */
public record BoardImageModel(String imageUrl, String altText) {

    private static final int IMAGE_URL_MAX_LENGTH = 500;
    private static final int ALT_TEXT_MAX_LENGTH = 255;

    public BoardImageModel {
        // board_images 테이블에는 실제 이미지 URL이 반드시 필요하다
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "imageUrl is required.");
        }

        imageUrl = imageUrl.strip();
        if (imageUrl.length() > IMAGE_URL_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "imageUrl must be 500 characters or less.");
        }
        validateImageUrl(imageUrl);

        if (altText != null) {
            altText = altText.strip();
            if (altText.isEmpty()) {
                altText = null;
            } else if (altText.length() > ALT_TEXT_MAX_LENGTH) {
                throw new CoreException(ErrorType.BAD_REQUEST, "altText must be 255 characters or less.");
            }
        }
    }

    /**
     * 이미지 URL 형식 검증
     */
    private static void validateImageUrl(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "imageUrl must start with http or https.");
            }
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "imageUrl is invalid.");
        }
    }
}
