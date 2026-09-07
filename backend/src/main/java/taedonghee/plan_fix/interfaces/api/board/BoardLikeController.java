package taedonghee.plan_fix.interfaces.api.board;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.board.BoardLikeApplicationService;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

/**
 * [interfaces] 게시글 좋아요/좋아요 취소 API. 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/boards/{boardId}/like")
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeApplicationService boardLikeApplicationService;

    /** 이미 좋아요한 상태면 조용히 무시하고 현재 상태를 그대로 응답한다(idempotent). */
    @PostMapping
    public ResponseEntity<BoardLikeResponse> like(
            @PathVariable Long boardId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(BoardLikeResponse.from(boardLikeApplicationService.like(principal.id(), boardId)));
    }

    /** 좋아요하지 않은 상태에서 호출해도 조용히 무시하고 현재 상태를 그대로 응답한다(idempotent). */
    @DeleteMapping
    public ResponseEntity<BoardLikeResponse> unlike(
            @PathVariable Long boardId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(BoardLikeResponse.from(boardLikeApplicationService.unlike(principal.id(), boardId)));
    }
}
