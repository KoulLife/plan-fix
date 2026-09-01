package taedonghee.plan_fix.domain.course;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseModelTest {

    @Test
    void create_defaults_to_private_active_and_zero_counts() {
        CourseModel course = CourseModel.create(1L, "  Gangneung day trip  ", null, null, null,
                List.of(new CourseSpotModel(10L, "  beach  ")));

        assertThat(course.title()).isEqualTo("Gangneung day trip");
        assertThat(course.visibility()).isEqualTo(CourseVisibility.PRIVATE);
        assertThat(course.status()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(course.viewCount()).isZero();
        assertThat(course.likeCount()).isZero();
        assertThat(course.spots()).containsExactly(new CourseSpotModel(10L, "beach"));
    }

    @Test
    void course_must_have_at_least_one_spot() {
        assertThatThrownBy(() -> CourseModel.create(1L, "Empty course", null, null, null, List.of()))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void course_spots_must_not_have_duplicate_spots() {
        assertThatThrownBy(() -> CourseModel.create(1L, "Duplicate course", null, null, null,
                List.of(new CourseSpotModel(10L, null), new CourseSpotModel(10L, null))))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void update_preserves_owner_and_counts() {
        CourseModel course = CourseModel.reconstruct(1L, 7L, "Before", null, null,
                CourseVisibility.PRIVATE, CourseStatus.ACTIVE, 11L, 3L,
                List.of(new CourseSpotModel(10L, null)), null, null);

        CourseModel updated = course.update("After", "memo", "thumb.jpg", CourseVisibility.PUBLIC,
                List.of(new CourseSpotModel(20L, "second")));

        assertThat(updated.userId()).isEqualTo(7L);
        assertThat(updated.title()).isEqualTo("After");
        assertThat(updated.description()).isEqualTo("memo");
        assertThat(updated.thumbnail()).isEqualTo("thumb.jpg");
        assertThat(updated.visibility()).isEqualTo(CourseVisibility.PUBLIC);
        assertThat(updated.viewCount()).isEqualTo(11L);
        assertThat(updated.likeCount()).isEqualTo(3L);
        assertThat(updated.spots()).containsExactly(new CourseSpotModel(20L, "second"));
    }
}
