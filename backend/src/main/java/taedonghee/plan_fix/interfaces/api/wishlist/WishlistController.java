package taedonghee.plan_fix.interfaces.api.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.wishlist.WishlistApplicationService;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;
import taedonghee.plan_fix.interfaces.api.board.BoardResponse;
import taedonghee.plan_fix.interfaces.api.course.CourseResponse;

import java.util.List;

/**
 * [interfaces] 위시리스트 조회 API. 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistApplicationService wishlistApplicationService;

    /**
     * 사용자가 좋아요 누른 스팟 목록 조회
     */
    @GetMapping("/spots")
    public ResponseEntity<List<WishlistSpotResponse>> listLikedSpots(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<WishlistSpotResponse> responses = wishlistApplicationService.listLikedSpots(principal.id())
                .stream()
                .map(WishlistSpotResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 사용자가 좋아요 누른 코스 목록 조회
     */
    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> listLikedCourses(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<CourseResponse> responses = wishlistApplicationService.listLikedCourses(principal.id())
                .stream()
                .map(CourseResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 사용자가 좋아요 누른 게시글 목록 조회
     */
    @GetMapping("/boards")
    public ResponseEntity<List<BoardResponse>> listLikedBoards(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<BoardResponse> responses = wishlistApplicationService.listLikedBoards(principal.id())
                .stream()
                .map(BoardResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
