package taedonghee.plan_fix.infrastructure.board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.board.BoardLikeModel;
import taedonghee.plan_fix.domain.board.BoardLikeRepository;

/**
 * [infrastructure] domain.BoardLikeRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class BoardLikeRepositoryImpl implements BoardLikeRepository {

    private final BoardLikeJpaRepository boardLikeJpaRepository;

    @Override
    public boolean existsByUserIdAndBoardId(Long userId, Long boardId) {
        return boardLikeJpaRepository.findByUserIdAndBoardId(userId, boardId).isPresent();
    }

    @Override
    public BoardLikeModel save(BoardLikeModel like) {
        BoardLikeJpaEntity saved = boardLikeJpaRepository.save(BoardLikeJpaEntity.builder()
                .boardLikeId(like.boardLikeId())
                .userId(like.userId())
                .boardId(like.boardId())
                .createdAt(like.createdAt())
                .build());
        return BoardLikeModel.reconstruct(saved.getBoardLikeId(), saved.getUserId(), saved.getBoardId(), saved.getCreatedAt());
    }

    @Override
    public boolean deleteByUserIdAndBoardId(Long userId, Long boardId) {
        return boardLikeJpaRepository.deleteByUserIdAndBoardId(userId, boardId) > 0;
    }
}
