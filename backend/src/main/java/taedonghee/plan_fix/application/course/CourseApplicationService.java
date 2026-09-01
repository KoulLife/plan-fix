package taedonghee.plan_fix.application.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.course.CourseModel;
import taedonghee.plan_fix.domain.course.CourseRepository;
import taedonghee.plan_fix.domain.course.CourseSpotModel;
import taedonghee.plan_fix.domain.course.CourseStatus;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.domain.spot.SpotStatus;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.List;

/**
 * 코스 Application Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseApplicationService {

    private final CourseRepository courseRepository;
    private final SpotRepository spotRepository;

    /**
     * 코스 생성 처리
     */
    @Transactional
    public CourseResult create(Long userId, CourseCommand.Create command) {
        CourseModel course = CourseModel.create(userId, command.title(), command.description(), command.thumbnail(),
                command.visibility(), command.spots());
        validateSpots(course.spots()); // 코스에 담긴 spot이 실제 사용 가능한 장소인지 검증
        return CourseResult.from(courseRepository.save(course)); // 코스 및 코스-스팟 순서 저장
    }

    /**
     * 로그인 사용자의 코스 목록 조회 처리
     */
    public List<CourseResult> listMine(Long userId) {
        return courseRepository.findActiveByUserId(userId).stream().map(CourseResult::from).toList();
    }

    /**
     * 로그인 사용자의 코스 단건 조회 처리
     */
    public CourseResult getMine(Long userId, Long courseId) {
        CourseModel course = getActiveCourseOrThrow(courseId);
        course.ensureOwner(userId); // 다른 사용자의 코스 접근 방지
        return CourseResult.from(course);
    }

    /**
     * 로그인 사용자의 코스 수정 처리
     */
    @Transactional
    public CourseResult update(Long userId, Long courseId, CourseCommand.Update command) {
        CourseModel course = getActiveCourseOrThrow(courseId);
        course.ensureOwner(userId); // 작성자만 수정 가능
        CourseModel updated = course.update(command.title(), command.description(), command.thumbnail(),
                command.visibility(), command.spots());
        validateSpots(updated.spots()); // 수정된 spot 목록 검증
        return CourseResult.from(courseRepository.save(updated));
    }

    /**
     * 로그인 사용자의 코스 삭제 상태 변경 처리
     */
    @Transactional
    public CourseResult delete(Long userId, Long courseId) {
        CourseModel course = getActiveCourseOrThrow(courseId);
        course.ensureOwner(userId); // 작성자만 삭제 가능
        return CourseResult.from(courseRepository.save(course.delete()));
    }

    /**
     * 게시글 작성 등 다른 기능에서 사용하는 코스 소유권 검증용 조회
     */
    public CourseModel getActiveOwnedCourseOrThrow(Long userId, Long courseId) {
        CourseModel course = getActiveCourseOrThrow(courseId);
        course.ensureOwner(userId);
        return course;
    }

    /**
     * 활성 코스 조회 실패 예외 처리
     */
    private CourseModel getActiveCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .filter(course -> course.status() == CourseStatus.ACTIVE)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "course not found. courseId=" + courseId));
    }

    /**
     * 코스에 포함된 spot 존재 여부 및 활성 상태 검증
     */
    private void validateSpots(List<CourseSpotModel> spots) {
        for (CourseSpotModel courseSpot : spots) {
            boolean exists = spotRepository.findById(courseSpot.spotId())
                    .filter(spot -> spot.status() == SpotStatus.ACTIVE)
                    .isPresent();
            if (!exists) {
                throw new CoreException(ErrorType.NOT_FOUND, "spot not found. spotId=" + courseSpot.spotId());
            }
        }
    }
}
