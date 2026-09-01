package taedonghee.plan_fix.application.board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.application.course.CourseApplicationService;
import taedonghee.plan_fix.domain.board.BoardModel;
import taedonghee.plan_fix.domain.board.BoardRepository;
import taedonghee.plan_fix.domain.board.BoardStatus;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.List;

/**
 * 게시글 Application Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardApplicationService {

    private final BoardRepository boardRepository;
    private final CourseApplicationService courseApplicationService;

    /**
     * 게시글 생성 처리
     */
    @Transactional
    public BoardResult create(Long userId, BoardCommand.Create command) {
        validateLinkedCourse(userId, command.courseId()); // 게시글에 연결할 코스가 로그인 사용자의 코스인지 검증
        BoardModel board = BoardModel.create(userId, command.courseId(), command.title(), command.content(),
                command.thumbnail(), command.images());
        return BoardResult.from(boardRepository.save(board));
    }

    /**
     * 로그인 사용자의 게시글 목록 조회 처리
     */
    public List<BoardResult> listMine(Long userId) {
        return boardRepository.findActiveByUserId(userId).stream()
                .map(BoardResult::from)
                .toList();
    }

    /**
     * 게시글 단건 조회 처리
     */
    public BoardResult get(Long boardId) {
        return BoardResult.from(getActiveBoardOrThrow(boardId));
    }

    /**
     * 로그인 사용자의 게시글 수정 처리
     */
    @Transactional
    public BoardResult update(Long userId, Long boardId, BoardCommand.Update command) {
        BoardModel board = getActiveBoardOrThrow(boardId);
        board.ensureOwner(userId); // 작성자만 수정 가능
        validateLinkedCourse(userId, command.courseId()); // 새로 연결할 코스 소유권 검증
        BoardModel updated = board.update(command.courseId(), command.title(), command.content(),
                command.thumbnail(), command.images());
        return BoardResult.from(boardRepository.save(updated));
    }

    /**
     * 로그인 사용자의 게시글 삭제 상태 변경 처리
     */
    @Transactional
    public BoardResult delete(Long userId, Long boardId) {
        BoardModel board = getActiveBoardOrThrow(boardId);
        board.ensureOwner(userId); // 작성자만 삭제 가능
        return BoardResult.from(boardRepository.save(board.delete()));
    }

    /**
     * 활성 게시글 조회 실패 예외 처리
     */
    private BoardModel getActiveBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .filter(board -> board.status() == BoardStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "board not found. boardId=" + boardId));
    }

    /**
     * 게시글에 연결된 코스가 로그인 사용자의 활성 코스인지 검증
     */
    private void validateLinkedCourse(Long userId, Long courseId) {
        if (courseId != null) {
            courseApplicationService.getActiveOwnedCourseOrThrow(userId, courseId);
        }
    }
}
