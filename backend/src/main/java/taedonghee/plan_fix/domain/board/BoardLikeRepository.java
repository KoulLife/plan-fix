package taedonghee.plan_fix.domain.board;

/**
 * [domain] 게시글 좋아요 저장소 포트.
 */
public interface BoardLikeRepository {

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    /** (user_id, board_id) 유니크 제약 위반 시 DataIntegrityViolationException을 그대로 던진다. */
    BoardLikeModel save(BoardLikeModel like);

    /** 실제로 지운 행이 있으면 true, 원래 좋아요하지 않았으면(지울 행이 없으면) false. */
    boolean deleteByUserIdAndBoardId(Long userId, Long boardId);
}
