package taedonghee.plan_fix.infrastructure.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 공개 목록 조회(최신순). status는 ACTIVE로 고정한다.
     * offset/limit은 JPQL의 LIMIT/OFFSET 절(Jakarta Persistence 3.1+)로 직접 처리한다.
     */
    @Query("""
            SELECT b FROM BoardJpaEntity b
            WHERE b.status = taedonghee.plan_fix.domain.board.BoardStatus.ACTIVE
            ORDER BY b.boardId DESC
            LIMIT :limit OFFSET :offset
            """)
    List<BoardJpaEntity> searchActiveByLatest(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 공개 목록 조회(인기순). 인기 점수 = like_count*0.9 + view_count*0.1, 동점이면 boardId 내림차순.
     */
    @Query("""
            SELECT b FROM BoardJpaEntity b
            WHERE b.status = taedonghee.plan_fix.domain.board.BoardStatus.ACTIVE
            ORDER BY (b.likeCount * 0.9 + b.viewCount * 0.1) DESC, b.boardId DESC
            LIMIT :limit OFFSET :offset
            """)
    List<BoardJpaEntity> searchActiveByPopular(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 활성 게시글 전체 건수.
     */
    @Query("""
            SELECT COUNT(b) FROM BoardJpaEntity b
            WHERE b.status = taedonghee.plan_fix.domain.board.BoardStatus.ACTIVE
            """)
    long countActive();
}
