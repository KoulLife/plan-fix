package taedonghee.plan_fix.interfaces.api.board;

import taedonghee.plan_fix.application.board.BoardCommand;
import taedonghee.plan_fix.domain.board.BoardImageModel;

import java.util.List;

/**
 * 게시글 API 요청 DTO
 */
public final class BoardRequest {

    private BoardRequest() {
    }

    /**
     * 게시글 생성 요청
     */
    public record Create(
            Long courseId,
            String title,
            String content,
            String thumbnail,
            List<Image> images
    ) {
        /**
         * HTTP 요청 DTO를 Application Command로 변환
         */
        public BoardCommand.Create toCommand() {
            return new BoardCommand.Create(courseId, title, content, thumbnail, toImageModels(images));
        }
    }

    /**
     * 게시글 수정 요청
     */
    public record Update(
            Long courseId,
            String title,
            String content,
            String thumbnail,
            List<Image> images
    ) {
        /**
         * HTTP 요청 DTO를 Application Command로 변환
         */
        public BoardCommand.Update toCommand() {
            return new BoardCommand.Update(courseId, title, content, thumbnail, toImageModels(images));
        }
    }

    /**
     * 게시글 이미지 요청 값
     */
    public record Image(String imageUrl, String altText) {
    }

    /**
     * 요청 이미지 목록을 도메인 값 객체로 변환
     */
    private static List<BoardImageModel> toImageModels(List<Image> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .map(image -> image == null ? null : new BoardImageModel(image.imageUrl(), image.altText()))
                .toList();
    }
}
