package taedonghee.plan_fix.interfaces.api.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.course.ai.AiCourseDraftApplicationService;
import taedonghee.plan_fix.application.course.ai.AiCourseDraftResult;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

/**
 * [interfaces] AI 코스 추천 API.
 *
 * 초안만 돌려주고 저장하지 않는다. 사용자가 코스 생성 화면에서 확인·수정한 뒤
 * 기존 코스 생성 API로 저장한다.
 */
@RestController
@RequestMapping("/api/v1/courses/ai")
@RequiredArgsConstructor
public class AiCourseController {

	private final AiCourseDraftApplicationService aiCourseDraftApplicationService;

	/**
	 * 예: POST /api/v1/courses/ai/draft
	 *
	 * 로그인이 필요하다(SecurityConfig의 anyRequest().authenticated()에 걸린다).
	 * 좋아요 이력으로 개인화하는 것이 이 기능의 핵심이고, 뒤에 LLM 호출을 붙일 때
	 * 무인증으로 열려 있으면 API 쿼터를 그대로 소진시킬 수 있어서다.
	 */
	@PostMapping("/draft")
	public ResponseEntity<AiCourseDraftResult> draft(
		@AuthenticationPrincipal AuthenticatedUser principal,
		@RequestBody AiCourseRequest request
	) {
		Long userId = principal == null ? null : principal.id();
		return ResponseEntity.ok(aiCourseDraftApplicationService.createDraft(userId, request.toCommand()));
	}
}
