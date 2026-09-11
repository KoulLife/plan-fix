package taedonghee.plan_fix.interfaces.api.comment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
import taedonghee.plan_fix.application.comment.*; import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser; import java.util.*;
/** 게시글 댓글·대댓글 HTTP API. 인증 사용자 정보를 서비스에 전달한다. */
@RestController @RequestMapping("/api/v1/boards/{boardId}/comments") @RequiredArgsConstructor
public class CommentController {
 private final CommentApplicationService service;
 /** 댓글 입력값. parentCommentId가 없으면 일반 댓글, 있으면 해당 댓글의 대댓글이다. */
 public record Request(String content, Long parentCommentId){}
 /** 게시글의 활성 댓글 목록 조회 API. */
 @GetMapping public List<CommentResult> list(@PathVariable Long boardId){return service.list(boardId);}
 /** 로그인한 사용자의 댓글·대댓글 등록 API. 생성 성공 시 201을 반환한다. */
 @PostMapping public ResponseEntity<CommentResult> create(@PathVariable Long boardId,@AuthenticationPrincipal AuthenticatedUser u,@RequestBody Request r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(u.id(),boardId,r.parentCommentId(),r.content()));}
 /** 댓글 작성자의 본문 수정 API. */
 @PatchMapping("/{commentId}") public CommentResult update(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long commentId,@RequestBody Request r){return service.update(u.id(),commentId,r.content());}
 /** 작성자 또는 관리자의 댓글 삭제 API. 상태 변경 성공 시 204를 반환한다. */
 @DeleteMapping("/{commentId}") public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long commentId){service.delete(u.id(),u.role()==taedonghee.plan_fix.domain.user.UserRole.ADMIN,commentId);return ResponseEntity.noContent().build();}
}
