package taedonghee.plan_fix.support.swagger;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [support] Swagger(OpenAPI) 전역 설정.
 * 별도 @Tag/@Operation 없이도 컨트롤러가 자동으로 문서화된다.
 * UI: /swagger-ui.html, 스펙: /v3/api-docs
 *
 * JWT 관련 설정은 문서에 Authorize 버튼을 그리기 위한 표기일 뿐이다.
 * 실제 인증 처리는 infrastructure.security의 SecurityConfig와 JwtAuthenticationFilter가 담당한다.
 */
@Configuration
@SecurityScheme(
	name = SwaggerConfig.BEARER_AUTH,
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT"
)
public class SwaggerConfig {

	static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("plan-fix API")
				.description("강릉 여행 코스 추천 서비스 API 문서")
				.version("v1"))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
