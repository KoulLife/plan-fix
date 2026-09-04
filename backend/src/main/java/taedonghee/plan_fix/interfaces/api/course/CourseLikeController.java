package taedonghee.plan_fix.interfaces.api.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.course.CourseLikeApplicationService;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/like")
@RequiredArgsConstructor
public class CourseLikeController {

    private final CourseLikeApplicationService courseLikeApplicationService;

    @PostMapping
    public ResponseEntity<CourseLikeResponse> like(
            @PathVariable Long courseId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(CourseLikeResponse.from(courseLikeApplicationService.like(principal.id(), courseId)));
    }

    @DeleteMapping
    public ResponseEntity<CourseLikeResponse> unlike(
            @PathVariable Long courseId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(CourseLikeResponse.from(courseLikeApplicationService.unlike(principal.id(), courseId)));
    }
}
