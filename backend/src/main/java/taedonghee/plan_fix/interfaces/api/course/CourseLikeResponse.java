package taedonghee.plan_fix.interfaces.api.course;

import taedonghee.plan_fix.application.course.CourseLikeResult;

public record CourseLikeResponse(boolean liked, long likeCount) {

    public static CourseLikeResponse from(CourseLikeResult result) {
        return new CourseLikeResponse(result.liked(), result.likeCount());
    }
}
