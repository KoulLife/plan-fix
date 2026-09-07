package taedonghee.plan_fix.application.course.ai;

/**
 * [application] 동행 유형. 하루에 몇 곳을 도는 게 적당한지가 달라진다.
 * 아이를 동반하면 이동이 느려서 적게, 친구끼리면 더 많이 도는 편이다.
 */
public enum CourseCompanion {

	SOLO(4),
	COUPLE(4),
	FRIENDS(5),
	FAMILY(3);

	private final int spotsPerDay;

	CourseCompanion(int spotsPerDay) {
		this.spotsPerDay = spotsPerDay;
	}

	public int spotsPerDay() {
		return spotsPerDay;
	}
}
