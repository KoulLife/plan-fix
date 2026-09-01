package taedonghee.plan_fix.domain.board;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardModelTest {

    @Test
    void create_defaults_counts_status_and_uses_first_image_as_thumbnail() {
        BoardModel board = BoardModel.create(
                10L,
                1L,
                "  Trip note  ",
                "<p><strong>Hello</strong></p>",
                null,
                List.of(new BoardImageModel("https://example.com/one.jpg", "cover"))
        );

        assertThat(board.boardId()).isNull();
        assertThat(board.userId()).isEqualTo(10L);
        assertThat(board.courseId()).isEqualTo(1L);
        assertThat(board.title()).isEqualTo("Trip note");
        assertThat(board.thumbnail()).isEqualTo("https://example.com/one.jpg");
        assertThat(board.status()).isEqualTo(BoardStatus.ACTIVE);
        assertThat(board.viewCount()).isZero();
        assertThat(board.likeCount()).isZero();
        assertThat(board.commentCount()).isZero();
    }

    @Test
    void create_rejects_unsafe_html_content() {
        assertThatThrownBy(() -> BoardModel.create(
                10L,
                null,
                "Unsafe",
                "<p onclick=\"alert(1)\">hello</p>",
                null,
                List.of()
        )).isInstanceOf(CoreException.class);

        assertThatThrownBy(() -> BoardModel.create(
                10L,
                null,
                "Unsafe",
                "<script>alert(1)</script>",
                null,
                List.of()
        )).isInstanceOf(CoreException.class);
    }

    @Test
    void image_requires_http_or_https_url() {
        assertThatThrownBy(() -> new BoardImageModel("javascript:alert(1)", null))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void create_rejects_duplicate_images() {
        assertThatThrownBy(() -> BoardModel.create(
                10L,
                null,
                "Images",
                "<p>Images</p>",
                null,
                List.of(new BoardImageModel("https://example.com/one.jpg", null),
                        new BoardImageModel("https://example.com/one.jpg", "same"))
        )).isInstanceOf(CoreException.class);
    }

    @Test
    void update_keeps_counts_and_changes_content() {
        BoardModel board = BoardModel.reconstruct(
                1L,
                null,
                10L,
                "Before",
                "<p>Before</p>",
                null,
                BoardStatus.ACTIVE,
                3L,
                2L,
                1L,
                List.of(),
                null,
                null
        );

        BoardModel updated = board.update(2L, "After", "<h2>After</h2>", null,
                List.of(new BoardImageModel("https://example.com/after.jpg", null)));

        assertThat(updated.courseId()).isEqualTo(2L);
        assertThat(updated.title()).isEqualTo("After");
        assertThat(updated.content()).isEqualTo("<h2>After</h2>");
        assertThat(updated.viewCount()).isEqualTo(3L);
        assertThat(updated.likeCount()).isEqualTo(2L);
        assertThat(updated.commentCount()).isEqualTo(1L);
    }

    @Test
    void delete_changes_status_to_deleted() {
        BoardModel board = BoardModel.create(10L, null, "Title", "<p>Content</p>", null, List.of());

        BoardModel deleted = board.delete();

        assertThat(deleted.status()).isEqualTo(BoardStatus.DELETED);
    }
}
