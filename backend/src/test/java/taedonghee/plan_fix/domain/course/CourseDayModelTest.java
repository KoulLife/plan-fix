package taedonghee.plan_fix.domain.course;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseDayModelTest {

    @Test
    @DisplayName("dayNumber가 1 미만이면 예외가 발생한다")
    void throwsWhenDayNumberLessThanOne() {
        assertThatThrownBy(() -> new CourseDayModel(0, List.of()))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("spots가 null이면 예외가 발생하지만 빈 리스트는 정상 생성된다")
    void allowsEmptySpots() {
        assertThatThrownBy(() -> new CourseDayModel(1, null))
                .isInstanceOf(CoreException.class);

        CourseDayModel day = new CourseDayModel(1, List.of());
        assertThat(day.dayNumber()).isEqualTo(1);
        assertThat(day.spots()).isEmpty();
    }

    @Test
    @DisplayName("spots 원소에 null이 있으면 예외가 발생한다")
    void throwsWhenSpotElementIsNull() {
        List<CourseSpotModel> spots = new ArrayList<>();
        spots.add(null);

        assertThatThrownBy(() -> new CourseDayModel(1, spots))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("같은 Day 안에 같은 spotId가 중복되면 예외가 발생한다")
    void throwsWhenDuplicateSpotIdInSameDay() {
        List<CourseSpotModel> spots = List.of(
                new CourseSpotModel(10L, "메모1"),
                new CourseSpotModel(10L, "메모2")
        );

        assertThatThrownBy(() -> new CourseDayModel(1, spots))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("duplicate spotId in same day");
    }

    @Test
    @DisplayName("한 Day의 장소가 31개 이상이면 예외가 발생한다")
    void throwsWhenSpotsExceedMaxLimit() {
        List<CourseSpotModel> spots = new ArrayList<>();
        for (long i = 1; i <= 31; i++) {
            spots.add(new CourseSpotModel(i, null));
        }

        assertThatThrownBy(() -> new CourseDayModel(1, spots))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("spots는 불변 리스트로 저장된다")
    void spotsIsImmutable() {
        List<CourseSpotModel> original = new ArrayList<>();
        original.add(new CourseSpotModel(1L, "메모"));

        CourseDayModel day = new CourseDayModel(1, original);
        assertThatThrownBy(() -> day.spots().add(new CourseSpotModel(2L, "추가")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
