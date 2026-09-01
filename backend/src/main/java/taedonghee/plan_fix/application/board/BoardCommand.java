package taedonghee.plan_fix.application.board;

import taedonghee.plan_fix.domain.board.BoardImageModel;

import java.util.List;

/**
 * 게시글 요청 Command
 */
public final class BoardCommand {

    private BoardCommand() {
    }

    /**
     * 게시글 생성 요청 값
     */
    public record Create(
            Long courseId,
            String title,
            String content,
            String thumbnail,
            List<BoardImageModel> images
    ) {
    }

    /**
     * 게시글 수정 요청 값
     */
    public record Update(
            Long courseId,
            String title,
            String content,
            String thumbnail,
            List<BoardImageModel> images
    ) {
    }
}
