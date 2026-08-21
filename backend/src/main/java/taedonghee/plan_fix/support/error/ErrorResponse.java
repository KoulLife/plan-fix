package taedonghee.plan_fix.support.error;

/**
 * [support] 에러 응답 본문.
 * code는 에러 종류, message는 사용자에게 보여줄 설명이다.
 */
public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorType errorType, String message) {
		return new ErrorResponse(errorType.getCode(), message);
	}
}
