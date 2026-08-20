package taedonghee.plan_fix.interfaces.api.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.user.UserApplicationService;

import java.util.List;

/**
 * 사용자 API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    /**
     * 사용자 생성 API
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody UserRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userApplicationService.create(request.toCommand())));
    }

    /**
     * 사용자 단건 조회 API
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> get(@PathVariable Long userId) {
        return ResponseEntity.ok(UserResponse.from(userApplicationService.get(userId)));
    }

    /**
     * 사용자 전체 조회 API
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        List<UserResponse> responses = userApplicationService.getAll().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 사용자 프로필 수정 API
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long userId,
            @RequestBody UserRequest.Update request
    ) {
        return ResponseEntity.ok(UserResponse.from(userApplicationService.update(userId, request.toCommand())));
    }

    /**
     * 사용자 탈퇴 API
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponse> withdraw(@PathVariable Long userId) {
        return ResponseEntity.ok(UserResponse.from(userApplicationService.withdraw(userId)));
    }
}
