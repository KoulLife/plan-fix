package taedonghee.plan_fix.infrastructure.spot;

/**
 * [infrastructure] TourAPI 일일 요청 한도 초과(HTTP 429 / LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR).
 *
 * 개별 콘텐츠의 문제가 아니라 키 전체가 막힌 상태라, 계속 호출해봐야 전부 실패한다.
 * 수집 루프에서 이 예외를 만나면 남은 건을 포기하고 즉시 중단하기 위해 별도 타입으로 둔다.
 */
public class TourApiQuotaExceededException extends RuntimeException {

	public TourApiQuotaExceededException(String message) {
		super(message);
	}
}
