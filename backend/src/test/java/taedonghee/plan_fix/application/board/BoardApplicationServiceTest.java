package taedonghee.plan_fix.application.board;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.application.course.CourseApplicationService;
import taedonghee.plan_fix.domain.board.BoardImageModel;
import taedonghee.plan_fix.domain.board.BoardModel;
import taedonghee.plan_fix.domain.board.BoardRepository;
import taedonghee.plan_fix.domain.board.BoardStatus;
import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseVisibility;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardApplicationServiceTest {

    @Test
    void create_validates_linked_course_owner_and_saves_board() {
        Fixture fixture = new Fixture();
        when(fixture.courses.getActiveOwnedCourseOrThrow(10L, 1L)).thenReturn(course(10L, 1L));

        BoardResult result = fixture.service().create(10L, new BoardCommand.Create(
                1L,
                "Trip board",
                "<p><strong>Nice trip</strong></p>",
                null,
                List.of(new BoardImageModel("https://example.com/one.jpg", "cover"))
        ));

        assertThat(result.boardId()).isNotNull();
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.courseId()).isEqualTo(1L);
        assertThat(result.images()).extracting(BoardResult.Image::sequence).containsExactly(0);
        assertThat(result.thumbnail()).isEqualTo("https://example.com/one.jpg");
        verify(fixture.courses).getActiveOwnedCourseOrThrow(10L, 1L);
    }

    @Test
    void create_without_course_does_not_validate_course() {
        Fixture fixture = new Fixture();

        BoardResult result = fixture.service().create(10L, new BoardCommand.Create(
                null,
                "Trip board",
                "<p>Content</p>",
                null,
                List.of()
        ));

        assertThat(result.courseId()).isNull();
        verify(fixture.courses, never()).getActiveOwnedCourseOrThrow(10L, null);
    }

    @Test
    void update_requires_board_owner() {
        Fixture fixture = new Fixture();
        BoardResult created = fixture.service().create(10L, new BoardCommand.Create(
                null, "Before", "<p>Before</p>", null, List.of()));

        assertThatThrownBy(() -> fixture.service().update(99L, created.boardId(), new BoardCommand.Update(
                null, "After", "<p>After</p>", null, List.of())))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void delete_soft_deletes_and_removes_from_my_list() {
        Fixture fixture = new Fixture();
        BoardResult created = fixture.service().create(10L, new BoardCommand.Create(
                null, "Board", "<p>Content</p>", null, List.of()));

        BoardResult deleted = fixture.service().delete(10L, created.boardId());

        assertThat(deleted.status()).isEqualTo(BoardStatus.DELETED);
        assertThat(fixture.service().listMine(10L)).isEmpty();
    }

    private static CourseModel course(Long userId, Long courseId) {
        return CourseModel.reconstruct(
                courseId,
                userId,
                "Course",
                null,
                null,
                CourseVisibility.PRIVATE,
                taedonghee.plan_fix.domain.course.CourseStatus.ACTIVE,
                0L,
                0L,
                List.of(new CourseSpotModel(1L, null)),
                null,
                null
        );
    }

    static class Fixture {
        final InMemoryBoardRepository boards = new InMemoryBoardRepository();
        final CourseApplicationService courses = mock(CourseApplicationService.class);

        BoardApplicationService service() {
            return new BoardApplicationService(boards, courses);
        }
    }

    static class InMemoryBoardRepository implements BoardRepository {
        private final List<BoardModel> saved = new ArrayList<>();
        private long sequence = 0L;

        @Override
        public BoardModel save(BoardModel board) {
            BoardModel stored = BoardModel.reconstruct(
                    board.boardId() == null ? ++sequence : board.boardId(),
                    board.courseId(),
                    board.userId(),
                    board.title(),
                    board.content(),
                    board.thumbnail(),
                    board.status(),
                    board.viewCount(),
                    board.likeCount(),
                    board.commentCount(),
                    board.images(),
                    board.createdAt(),
                    board.updatedAt()
            );
            saved.removeIf(existing -> existing.boardId().equals(stored.boardId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<BoardModel> findById(Long boardId) {
            return saved.stream().filter(board -> board.boardId().equals(boardId)).findFirst();
        }

        @Override
        public List<BoardModel> findActiveByUserId(Long userId) {
            return saved.stream()
                    .filter(board -> board.userId().equals(userId))
                    .filter(board -> board.status() == BoardStatus.ACTIVE)
                    .toList();
        }
    }
}
