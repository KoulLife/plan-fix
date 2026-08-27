package taedonghee.plan_fix.infrastructure.board;

import org.springframework.data.jpa.repository.JpaRepository;
import taedonghee.plan_fix.domain.board.BoardStatus;

import java.util.List;

/**
 * BoardJpaEntity Spring Data JPA Repository
 */
public interface BoardJpaRepository extends JpaRepository<BoardJpaEntity, Long> {

    /**
     * 사용자별 활성 게시글 목록 조회
     */
    List<BoardJpaEntity> findByUserIdAndStatusOrderByBoardIdDesc(Long userId, BoardStatus status);
}
