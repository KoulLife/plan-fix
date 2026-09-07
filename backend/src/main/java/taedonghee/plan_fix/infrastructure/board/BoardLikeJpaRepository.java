package taedonghee.plan_fix.infrastructure.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface BoardLikeJpaRepository extends JpaRepository<BoardLikeJpaEntity, Long> {

    Optional<BoardLikeJpaEntity> findByUserIdAndBoardId(Long userId, Long boardId);

    @Modifying
    @Query("DELETE FROM BoardLikeJpaEntity b WHERE b.userId = :userId AND b.boardId = :boardId")
    long deleteByUserIdAndBoardId(@Param("userId") Long userId, @Param("boardId") Long boardId);
}
