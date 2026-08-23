package taedonghee.plan_fix.interfaces.api.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.spot.SpotLikeApplicationService;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

/**
 * [interfaces] 스팟 좋아요/좋아요 취소 API. 로그인이 필요하다 — SecurityConfig가 스팟 GET만
 * permitAll이라 이 경로(POST/DELETE)는 기본 규칙(anyRequest().authenticated())에 걸려
 * 별도 설정 없이 인증을 요구한다.
 */
@RestController
@RequestMapping("/api/v1/spots/{spotId}/like")
@RequiredArgsConstructor
public class SpotLikeController {

    private final SpotLikeApplicationService spotLikeApplicationService;

    /** 이미 좋아요한 상태면 조용히 무시하고 현재 상태를 그대로 응답한다(idempotent). */
    @PostMapping
    public ResponseEntity<SpotLikeResponse> like(
            @PathVariable Long spotId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SpotLikeResponse.from(spotLikeApplicationService.like(principal.id(), spotId)));
    }

    /** 좋아요하지 않은 상태에서 호출해도 조용히 무시하고 현재 상태를 그대로 응답한다(idempotent). */
    @DeleteMapping
    public ResponseEntity<SpotLikeResponse> unlike(
            @PathVariable Long spotId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SpotLikeResponse.from(spotLikeApplicationService.unlike(principal.id(), spotId)));
    }
}
