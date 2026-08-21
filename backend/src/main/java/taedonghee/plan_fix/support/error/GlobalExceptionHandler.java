package taedonghee.plan_fix.support.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [support] 전역 예외 처리.
 *
 * 이 핸들러가 없으면 CoreException이 DispatcherServlet 밖으로 빠져나가고,
 * 컨테이너가 /error로 forward하면서 시큐리티 필터체인을 다시 타게 된다.
 * /error는 permitAll 대상이 아니라 결국 본문 없는 403이 나가고, 원래 에러가 통째로 가려진다.
 * 여기서 잡아 응답으로 바꿔야 ErrorType에 정의해 둔 상태 코드와 메시지가 실제로 전달된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 도메인·애플리케이션이 의도적으로 던진 예외. 메시지를 그대로 내보낸다. */
	@ExceptionHandler(CoreException.class)
	public ResponseEntity<ErrorResponse> handleCoreException(CoreException e) {
		ErrorType errorType = e.getErrorType();
		log.warn("CoreException: {} - {}", errorType, e.getMessage());
		return ResponseEntity.status(errorType.getStatus())
			.body(ErrorResponse.of(errorType, e.getMessage()));
	}

	/**
	 * 예상하지 못한 예외. 내부 사정이 드러나지 않도록 응답에는 고정 메시지만 담고,
	 * 원인 파악에 필요한 스택트레이스는 로그로만 남긴다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("처리되지 않은 예외", e);
		ErrorType errorType = ErrorType.INTERNAL_ERROR;
		return ResponseEntity.status(errorType.getStatus())
			.body(ErrorResponse.of(errorType, errorType.getMessage()));
	}
}
