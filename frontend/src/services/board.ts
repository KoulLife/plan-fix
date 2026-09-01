export type BoardItem = {
  boardId: number;
  title: string;
  thumbnail: string | null;
  userId: number;
  likeCount: number;
  viewCount: number;
  commentCount: number;
  createdAt: string;
};

export type PopularBoard = BoardItem;

export type BoardListResult = {
  items: BoardItem[];
  offset: number;
  size: number;
  totalCount: number;
};

export type BoardListResponse = BoardListResult;

export type BoardImage = {
  imageUrl: string;
  altText: string | null;
  sequence: number;
};

export type BoardDetail = {
  boardId: number;
  courseId: number | null;
  userId: number;
  title: string;
  content: string;
  thumbnail: string | null;
  status: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  images: BoardImage[];
  createdAt: string;
  updatedAt: string;
};

export type PopularBoardsParams = {
  size?: number;
  offset?: number;
};

const apiBaseUrl = process.env.REACT_APP_API_BASE_URL?.replace(/\/$/, "");

/** 공개 API라 인증 쿠키가 필요 없다. 백엔드 미설정 환경(예: 테스트)에서는 빈 목록으로 조용히 넘어간다. */
export async function fetchPopularBoards(params: PopularBoardsParams = {}): Promise<BoardListResult> {
  if (!apiBaseUrl) {
    return {
      items: [],
      offset: params.offset ?? 0,
      size: params.size ?? 6,
      totalCount: 0,
    };
  }

  const query = new URLSearchParams({ sort: "popular", size: String(params.size ?? 6) });
  if (params.offset !== undefined) {
    query.set("offset", String(params.offset));
  }

  const response = await fetch(`${apiBaseUrl}/boards?${query.toString()}`);
  if (!response.ok) {
    throw new Error("게시글을 불러오지 못했습니다.");
  }

  const body = (await response.json()) as BoardListResult;
  return body;
}

/**
 * 존재하지 않거나(404) 백엔드 미설정 환경에서는 null을 반환한다 — 페이지 쪽에서
 * "없음"과 "네트워크 에러"를 구분해 보여줄 수 있도록, 그 외 실패는 에러로 던진다.
 */
export async function fetchBoardDetail(boardId: number | string): Promise<BoardDetail | null> {
  if (!apiBaseUrl) {
    return null;
  }

  const response = await fetch(`${apiBaseUrl}/boards/${boardId}`);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error("게시글을 불러오지 못했습니다.");
  }

  return (await response.json()) as BoardDetail;
}
