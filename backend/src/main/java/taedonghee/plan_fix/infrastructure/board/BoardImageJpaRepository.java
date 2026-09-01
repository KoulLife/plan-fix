package taedonghee.plan_fix.infrastructure.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * BoardImageJpaEntity Spring Data JPA Repository
 */
public interface BoardImageJpaRepository extends JpaRepository<BoardImageJpaEntity, Long> {

    /**
     * 게시글 이미지 목록을 순서대로 조회
     */
    List<BoardImageJpaEntity> findByBoardIdOrderBySequenceAsc(Long boardId);

    /**
     * 게시글에 포함된 기존 이미지 목록 삭제
     */
    void deleteByBoardId(Long boardId);
}
