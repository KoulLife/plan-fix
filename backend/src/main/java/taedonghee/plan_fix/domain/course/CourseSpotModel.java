package taedonghee.plan_fix.domain.course;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * 코스에 포함되는 spot 정보 값 객체
 */
public record CourseSpotModel(Long spotId, String memo) {

    private static final int MEMO_MAX_LENGTH = 500;

    public CourseSpotModel {
        // course_spots 테이블에는 실제 spot 참조값이 반드시 필요하다
        if (spotId == null || spotId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "spotId must be positive.");
        }
        memo = normalizeMemo(memo);
    }

    private static String normalizeMemo(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        // 빈 메모는 저장하지 않고 null로 정리한다
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MEMO_MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "memo must be 500 characters or less.");
        }
        return normalized;
    }
}
