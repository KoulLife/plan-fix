package taedonghee.plan_fix.application.course;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseRepository;
import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseStatus;
import taedonghee.plan_fix.domain.course.CourseVisibility;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotSearchCondition;
import taedonghee.plan_fix.domain.spot.SpotSortType;
import taedonghee.plan_fix.domain.spot.SpotSourceType;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.support.error.CoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseApplicationServiceTest {

    @Test
    void create_saves_course_for_authenticated_user_with_ordered_spots() {
        Fixture fixture = new Fixture();
        SpotModel firstSpot = fixture.spots.save(spot(SpotStatus.ACTIVE));
        SpotModel secondSpot = fixture.spots.save(spot(SpotStatus.ACTIVE));

        CourseResult result = fixture.service().create(10L, new CourseCommand.Create(
                "Gangneung course", "one day", "thumb.jpg", CourseVisibility.PUBLIC,
                List.of(new CourseSpotModel(firstSpot.spotId(), "start"),
                        new CourseSpotModel(secondSpot.spotId(), "finish"))));

        assertThat(result.courseId()).isNotNull();
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.visibility()).isEqualTo(CourseVisibility.PUBLIC);
        assertThat(result.spots()).extracting(CourseResult.Spot::spotId)
                .containsExactly(firstSpot.spotId(), secondSpot.spotId());
        assertThat(result.spots()).extracting(CourseResult.Spot::sequence).containsExactly(0, 1);
    }

    @Test
    void create_rejects_missing_or_hidden_spot() {
        Fixture fixture = new Fixture();
        SpotModel hiddenSpot = fixture.spots.save(spot(SpotStatus.HIDDEN));

        assertThatThrownBy(() -> fixture.service().create(10L, new CourseCommand.Create(
                "Hidden spot course", null, null, null, List.of(new CourseSpotModel(hiddenSpot.spotId(), null)))))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> fixture.service().create(10L, new CourseCommand.Create(
                "Missing spot course", null, null, null, List.of(new CourseSpotModel(999L, null)))))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void update_requires_course_owner() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spot(SpotStatus.ACTIVE));
        CourseResult created = fixture.service().create(10L, new CourseCommand.Create(
                "Before", null, null, null, List.of(new CourseSpotModel(spot.spotId(), null))));

        assertThatThrownBy(() -> fixture.service().update(99L, created.courseId(), new CourseCommand.Update(
                "After", null, null, null, List.of(new CourseSpotModel(spot.spotId(), null)))))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void delete_soft_deletes_and_removes_from_my_list() {
        Fixture fixture = new Fixture();
        SpotModel spot = fixture.spots.save(spot(SpotStatus.ACTIVE));
        CourseResult created = fixture.service().create(10L, new CourseCommand.Create(
                "Course", null, null, null, List.of(new CourseSpotModel(spot.spotId(), null))));

        CourseResult deleted = fixture.service().delete(10L, created.courseId());

        assertThat(deleted.status()).isEqualTo(CourseStatus.DELETED);
        assertThat(fixture.service().listMine(10L)).isEmpty();
    }

    private static SpotModel spot(SpotStatus status) {
        return SpotModel.builder()
                .sourceType(SpotSourceType.NATIVE)
                .title("Spot")
                .category("Place")
                .status(status)
                .build();
    }

    static class Fixture {
        final InMemoryCourseRepository courses = new InMemoryCourseRepository();
        final InMemorySpotRepository spots = new InMemorySpotRepository();

        CourseApplicationService service() {
            return new CourseApplicationService(courses, spots);
        }
    }

    static class InMemoryCourseRepository implements CourseRepository {
        private final List<CourseModel> saved = new ArrayList<>();
        private long sequence = 0L;

        @Override
        public CourseModel save(CourseModel course) {
            CourseModel stored = CourseModel.reconstruct(
                    course.courseId() == null ? ++sequence : course.courseId(),
                    course.userId(),
                    course.title(),
                    course.description(),
                    course.thumbnail(),
                    course.visibility(),
                    course.status(),
                    course.viewCount(),
                    course.likeCount(),
                    course.spots(),
                    course.createdAt(),
                    course.updatedAt()
            );
            saved.removeIf(existing -> existing.courseId().equals(stored.courseId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<CourseModel> findById(Long courseId) {
            return saved.stream().filter(course -> course.courseId().equals(courseId)).findFirst();
        }

        @Override
        public List<CourseModel> findActiveByUserId(Long userId) {
            return saved.stream()
                    .filter(course -> course.userId().equals(userId))
                    .filter(course -> course.status() == CourseStatus.ACTIVE)
                    .toList();
        }
    }

    static class InMemorySpotRepository implements SpotRepository {
        private final List<SpotModel> saved = new ArrayList<>();
        private long sequence = 0L;

        @Override
        public SpotModel save(SpotModel spot) {
            SpotModel stored = SpotModel.builder()
                    .spotId(spot.spotId() == null ? ++sequence : spot.spotId())
                    .sourceType(spot.sourceType())
                    .attributes(new SpotModel.SourceAttributes(spot.title(), spot.category(), spot.region(),
                            spot.sigungu(), spot.address(), spot.latitude(), spot.longitude(), spot.thumbnail(),
                            spot.description()))
                    .viewCount(spot.viewCount())
                    .likeCount(spot.likeCount())
                    .commentCount(spot.commentCount())
                    .status(spot.status())
                    .createdAt(spot.createdAt())
                    .updatedAt(spot.updatedAt())
                    .build();
            saved.removeIf(existing -> existing.spotId().equals(stored.spotId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<SpotModel> findById(Long spotId) {
            return saved.stream().filter(spot -> spot.spotId().equals(spotId)).findFirst();
        }

        @Override
        public long countAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpotModel> searchActive(SpotSearchCondition condition, SpotSortType sort, int offset, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countActive(SpotSearchCondition condition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void incrementViewCount(Long spotId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void incrementLikeCount(Long spotId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void decrementLikeCount(Long spotId) {
            throw new UnsupportedOperationException();
        }
    }
}
