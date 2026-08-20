package taedonghee.plan_fix.support.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [support] Swagger(OpenAPI) 전역 설정.
 * 별도 @Tag/@Operation 없이도 컨트롤러가 자동으로 문서화된다.
 * UI: /swagger-ui.html, 스펙: /v3/api-docs
 */
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("plan-fix API")
				.description("강릉 여행 코스 추천 서비스 API 문서")
				.version("v1"));
	}
}
