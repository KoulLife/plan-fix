package taedonghee.plan_fix.interfaces.api.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.course.CourseApplicationService;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

import java.util.List;

/**
 * 코스 API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseApplicationService courseApplicationService;

    /**
     * 코스 생성 API
     */
    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody CourseRequest.Create request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseResponse.from(courseApplicationService.create(principal.id(), request.toCommand())));
    }

    /**
     * 로그인 사용자의 코스 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<List<CourseResponse>> listMine(@AuthenticationPrincipal AuthenticatedUser principal) {
        List<CourseResponse> responses = courseApplicationService.listMine(principal.id()).stream()
                .map(CourseResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 로그인 사용자의 코스 단건 조회 API
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getMine(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(CourseResponse.from(courseApplicationService.getMine(principal.id(), courseId)));
    }

    /**
     * 로그인 사용자의 코스 수정 API
     */
    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long courseId,
            @RequestBody CourseRequest.Update request
    ) {
        return ResponseEntity.ok(CourseResponse.from(
                courseApplicationService.update(principal.id(), courseId, request.toCommand())));
    }

    /**
     * 로그인 사용자의 코스 삭제 API
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<CourseResponse> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(CourseResponse.from(courseApplicationService.delete(principal.id(), courseId)));
    }
}
