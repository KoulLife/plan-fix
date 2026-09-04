package taedonghee.plan_fix.application.board;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.board.BoardLikeModel;
import taedonghee.plan_fix.domain.board.BoardLikeRepository;
import taedonghee.plan_fix.domain.board.BoardModel;
import taedonghee.plan_fix.domain.board.BoardRepository;
import taedonghee.plan_fix.domain.board.BoardStatus;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * [application] 게시글 좋아요/좋아요 취소. 둘 다 idempotent다 — 중복 좋아요·안 한 것 취소는
 * 에러 없이 현재 상태를 그대로 응답한다.
 */
@Service
@RequiredArgsConstructor
public class BoardLikeApplicationService {

    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;

    @Transactional
    public BoardLikeResult like(Long userId, Long boardId) {
        BoardModel board = getActiveBoardOrThrow(boardId);

        if (boardLikeRepository.existsByUserIdAndBoardId(userId, boardId)) {
            return new BoardLikeResult(true, board.likeCount());
        }

        try {
            boardLikeRepository.save(BoardLikeModel.create(userId, boardId));
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 좋아요 요청끼리의 경쟁. 유니크 제약이 막았으니 이미 좋아요된 것으로 본다.
            return new BoardLikeResult(true, board.likeCount());
        }

        boardRepository.incrementLikeCount(boardId);
        return new BoardLikeResult(true, board.likeCount() + 1);
    }

    @Transactional
    public BoardLikeResult unlike(Long userId, Long boardId) {
        BoardModel board = getActiveBoardOrThrow(boardId);

        boolean deleted = boardLikeRepository.deleteByUserIdAndBoardId(userId, boardId);
        if (!deleted) {
            return new BoardLikeResult(false, board.likeCount());
        }

        boardRepository.decrementLikeCount(boardId);
        return new BoardLikeResult(false, Math.max(board.likeCount() - 1, 0));
    }

    private BoardModel getActiveBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .filter(b -> b.status() == BoardStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "board not found. boardId=" + boardId));
    }
}
