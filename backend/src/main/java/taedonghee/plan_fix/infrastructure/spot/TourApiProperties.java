package taedonghee.plan_fix.infrastructure.spot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [infrastructure] TourAPI 호출 설정.
 * lDongRegnCd/lDongSignguCd는 고정 설정값이 아니라 호출 시점에 파라미터로 받으므로 여기 두지 않는다.
 */
@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(
	String baseUrl,
	String serviceKey,
	String mobileOs,
	String mobileApp,
	int pageSize,
	long callIntervalMs,
	long connectTimeoutMs,
	long readTimeoutMs
) {
}
