package taedonghee.plan_fix.domain.course;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseModelTest {

    @Test
    @DisplayName("신규 생성 시 기본값(비공개, ACTIVE, 0 카운트)이 설정된다")
    void create_defaults_to_private_active_and_zero_counts() {
        LocalDate start = LocalDate.of(2026, 9, 12);
        LocalDate end = LocalDate.of(2026, 9, 12);
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(10L, "  beach  ")))
        );

        CourseModel course = CourseModel.create(1L, "  Gangneung day trip  ", null, null, null,
                start, end, days);

        assertThat(course.title()).isEqualTo("Gangneung day trip");
        assertThat(course.visibility()).isEqualTo(CourseVisibility.PRIVATE);
        assertThat(course.status()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(course.viewCount()).isZero();
        assertThat(course.likeCount()).isZero();
        assertThat(course.startDate()).isEqualTo(start);
        assertThat(course.endDate()).isEqualTo(end);
        assertThat(course.days()).hasSize(1);
        assertThat(course.days().get(0).spots()).containsExactly(new CourseSpotModel(10L, "beach"));
    }

    @Test
    @DisplayName("days가 null이거나 비어 있거나 30개를 초과하면 예외가 발생한다")
    void throwsWhenDaysNullOrEmptyOrExceedsLimit() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 1);

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, start, end, null))
                .isInstanceOf(CoreException.class);

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, start, end, List.of()))
                .isInstanceOf(CoreException.class);

        List<CourseDayModel> over30Days = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            over30Days.add(new CourseDayModel(i, i == 1 ? List.of(new CourseSpotModel(1L, null)) : List.of()));
        }
        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, null, null, over30Days))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("dayNumber가 1부터 순차적으로 증가하지 않으면 예외가 발생한다")
    void throwsWhenDayNumbersNotSequentialFromOne() {
        List<CourseDayModel> invalidDays1 = List.of(
                new CourseDayModel(2, List.of(new CourseSpotModel(1L, null))),
                new CourseDayModel(3, List.of())
        );
        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, null, null, invalidDays1))
                .isInstanceOf(CoreException.class);

        List<CourseDayModel> invalidDays2 = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(1L, null))),
                new CourseDayModel(3, List.of())
        );
        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, null, null, invalidDays2))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("모든 Day의 장소 개수 합이 0개면 예외가 발생한다")
    void throwsWhenTotalSpotsCountIsZero() {
        List<CourseDayModel> emptyDays = List.of(
                new CourseDayModel(1, List.of()),
                new CourseDayModel(2, List.of())
        );

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, null, null, emptyDays))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("course must contain at least one spot");
    }

    @Test
    @DisplayName("일부 Day가 비어 있어도 다른 Day에 장소가 최소 1개 있으면 정상 생성된다")
    void allowsSomeDaysToBeEmpty() {
        LocalDate start = LocalDate.of(2026, 9, 12);
        LocalDate end = LocalDate.of(2026, 9, 14);
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(10L, null))),
                new CourseDayModel(2, List.of()),
                new CourseDayModel(3, List.of(new CourseSpotModel(20L, null)))
        );

        CourseModel course = CourseModel.create(1L, "Title", null, null, null, start, end, days);
        assertThat(course.days()).hasSize(3);
        assertThat(course.days().get(1).spots()).isEmpty();
    }

    @Test
    @DisplayName("startDate와 endDate 중 하나만 존재하면 예외가 발생한다")
    void throwsWhenOnlyOneDateExists() {
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(10L, null)))
        );

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null,
                LocalDate.of(2026, 9, 12), null, days))
                .isInstanceOf(CoreException.class);

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null,
                null, LocalDate.of(2026, 9, 12), days))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("endDate가 startDate보다 앞서면 예외가 발생한다")
    void throwsWhenEndDateBeforeStartDate() {
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(10L, null)))
        );

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null,
                LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 11), days))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("기간 일수(endDate - startDate + 1)와 days.size()가 일치하지 않으면 예외가 발생한다")
    void throwsWhenDurationDoesNotMatchDaysSize() {
        LocalDate start = LocalDate.of(2026, 9, 12);
        LocalDate end = LocalDate.of(2026, 9, 14); // 3일 기간
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(10L, null))),
                new CourseDayModel(2, List.of()) // 2개 Day만 제공
        );

        assertThatThrownBy(() -> CourseModel.create(1L, "Title", null, null, null, start, end, days))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("must match date duration");
    }

    @Test
    @DisplayName("날짜가 둘 다 null인 경우 days.size()와 무관하게 정상 생성된다")
    void allowsNullDatesWithoutDurationCheck() {
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(10L, null))),
                new CourseDayModel(2, List.of(new CourseSpotModel(20L, null)))
        );

        CourseModel course = CourseModel.create(1L, "Title", null, null, null, null, null, days);
        assertThat(course.startDate()).isNull();
        assertThat(course.endDate()).isNull();
        assertThat(course.days()).hasSize(2);
    }

    @Test
    @DisplayName("서로 다른 Day에 같은 spotId가 존재해도 정상 생성된다")
    void allowsSameSpotIdAcrossDifferentDays() {
        LocalDate start = LocalDate.of(2026, 9, 12);
        LocalDate end = LocalDate.of(2026, 9, 13);
        List<CourseDayModel> days = List.of(
                new CourseDayModel(1, List.of(new CourseSpotModel(100L, "Day1 숙소"))),
                new CourseDayModel(2, List.of(new CourseSpotModel(100L, "Day2 숙소")))
        );

        CourseModel course = CourseModel.create(1L, "2박 여행", null, null, null, start, end, days);
        assertThat(course.days().get(0).spots().get(0).spotId()).isEqualTo(100L);
        assertThat(course.days().get(1).spots().get(0).spotId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("update 시 소유자 및 조회수/좋아요 수가 유지된다")
    void update_preserves_owner_and_counts() {
        CourseModel course = CourseModel.reconstruct(1L, 7L, "Before", null, null,
                CourseVisibility.PRIVATE, CourseStatus.ACTIVE, 11L, 3L,
                null, null,
                List.of(new CourseDayModel(1, List.of(new CourseSpotModel(10L, null)))), null, null);

        LocalDate start = LocalDate.of(2026, 9, 12);
        LocalDate end = LocalDate.of(2026, 9, 12);
        CourseModel updated = course.update("After", "memo", "thumb.jpg", CourseVisibility.PUBLIC,
                start, end, List.of(new CourseDayModel(1, List.of(new CourseSpotModel(20L, "second")))));

        assertThat(updated.userId()).isEqualTo(7L);
        assertThat(updated.title()).isEqualTo("After");
        assertThat(updated.description()).isEqualTo("memo");
        assertThat(updated.thumbnail()).isEqualTo("thumb.jpg");
        assertThat(updated.visibility()).isEqualTo(CourseVisibility.PUBLIC);
        assertThat(updated.viewCount()).isEqualTo(11L);
        assertThat(updated.likeCount()).isEqualTo(3L);
        assertThat(updated.startDate()).isEqualTo(start);
        assertThat(updated.endDate()).isEqualTo(end);
        assertThat(updated.days().get(0).spots()).containsExactly(new CourseSpotModel(20L, "second"));
    }
}
