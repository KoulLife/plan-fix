package taedonghee.plan_fix.infrastructure.course;

/** 공동 코스 참여자의 권한. 현재 MEMBER는 조회 전용이며 수정 권한은 OWNER에게만 있다. */
public enum CourseMemberRole {
    OWNER,
    VIEWER,
    EDITOR
}
