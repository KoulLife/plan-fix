package taedonghee.plan_fix.application.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.application.board.BoardApplicationService;
import taedonghee.plan_fix.application.board.BoardResult;
import taedonghee.plan_fix.application.course.CourseApplicationService;
import taedonghee.plan_fix.application.course.CourseResult;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;

import java.util.List;

/**
 * 위시리스트 Application Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistApplicationService {

    private final SpotRepository spotRepository;
    private final CourseApplicationService courseApplicationService;
    private final BoardApplicationService boardApplicationService;

    /**
     * 사용자가 좋아요 누른 스팟 목록 조회
     */
    public List<SpotModel> listLikedSpots(Long userId) {
        return spotRepository.findLikedByUserId(userId);
    }

    /**
     * 사용자가 좋아요 누른 코스 목록 조회
     */
    public List<CourseResult> listLikedCourses(Long userId) {
        return courseApplicationService.listLiked(userId);
    }

    /**
     * 사용자가 좋아요 누른 게시글 목록 조회
     */
    public List<BoardResult> listLikedBoards(Long userId) {
        return boardApplicationService.listLiked(userId);
    }
}
