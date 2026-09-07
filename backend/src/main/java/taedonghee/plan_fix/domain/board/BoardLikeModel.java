package taedonghee.plan_fix.domain.board;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.OffsetDateTime;

/**
 * [domain] 사용자가 게시글을 좋아요한 이력 한 건.
 * users와 boards를 잇는 다대다 관계 테이블(board_likes)의 canonical 모델이다.
 * (userId, boardId) 조합은 DB 유니크 제약으로 유일함이 보장된다.
 */
public class BoardLikeModel {

    private final Long boardLikeId;
    private final Long userId;
    private final Long boardId;
    private final OffsetDateTime createdAt;

    private BoardLikeModel(Long boardLikeId, Long userId, Long boardId, OffsetDateTime createdAt) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (boardId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "boardId는 필수입니다.");
        }
        this.boardLikeId = boardLikeId;
        this.userId = userId;
        this.boardId = boardId;
        this.createdAt = createdAt;
    }

    /** 신규 좋아요 생성 */
    public static BoardLikeModel create(Long userId, Long boardId) {
        return new BoardLikeModel(null, userId, boardId, OffsetDateTime.now());
    }

    /** infrastructure의 영속 데이터 복원 */
    public static BoardLikeModel reconstruct(Long boardLikeId, Long userId, Long boardId, OffsetDateTime createdAt) {
        return new BoardLikeModel(boardLikeId, userId, boardId, createdAt);
    }

    public Long boardLikeId() { return boardLikeId; }
    public Long userId() { return userId; }
    public Long boardId() { return boardId; }
    public OffsetDateTime createdAt() { return createdAt; }
}
