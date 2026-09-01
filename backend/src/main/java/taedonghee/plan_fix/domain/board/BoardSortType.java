package taedonghee.plan_fix.domain.board;

/**
 * [domain] 공개 게시글 목록 조회의 정렬 기준.
 */
public enum BoardSortType {

    /** 최근 등록된 순 (boardId 내림차순) */
    LATEST,

    /** 인기순: like_count*0.9 + view_count*0.1 내림차순, 동점이면 boardId 내림차순 */
    POPULAR
}
