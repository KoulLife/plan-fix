export type PopularSpot = {
  spotId: number;
  title: string;
  category: string;
  region: string | null;
  sigungu: string | null;
  thumbnail: string | null;
};

type SpotListResponse = {
  items: PopularSpot[];
  offset: number;
  size: number;
  totalCount: number;
};

export type PopularSpotsParams = {
  region?: string;
  sigungu?: string;
  size?: number;
};

/** detailIntro2 결과. 음식점이 아니면 firstMenu/treatMenu/lcnsno는 null이다. */
export type SpotTourInfo = {
  tel: string | null;
  parkInfo: string | null;
  timeInfo: string | null;
  restInfo: string | null;
  firstMenu: string | null;
  treatMenu: string | null;
  lcnsno: string | null;
};

export type SpotDetail = {
  spotId: number;
  title: string;
  category: string;
  region: string | null;
  sigungu: string | null;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  thumbnail: string | null;
  description: string | null;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  images: string[];
  info: SpotTourInfo | null;
  isLiked: boolean;
};

export type SpotLikeState = {
  liked: boolean;
  likeCount: number;
};

/** 좋아요/취소 API가 인증되지 않은 상태(401/403)로 실패했을 때 던진다. */
export class UnauthorizedError extends Error {
  constructor() {
    super("로그인이 필요합니다.");
    this.name = "UnauthorizedError";
  }
}

const apiBaseUrl = process.env.REACT_APP_API_BASE_URL?.replace(/\/$/, "");

/** 공개 API라 인증 쿠키가 필요 없다. 백엔드 미설정 환경(예: 테스트)에서는 빈 목록으로 조용히 넘어간다. */
export async function fetchPopularSpots(params: PopularSpotsParams = {}): Promise<PopularSpot[]> {
  if (!apiBaseUrl) {
    return [];
  }

  const query = new URLSearchParams({ sort: "popular", size: String(params.size ?? 2) });
  if (params.region) {
    query.set("region", params.region);
  }
  if (params.sigungu) {
    query.set("sigungu", params.sigungu);
  }

  const response = await fetch(`${apiBaseUrl}/spots?${query.toString()}`);
  if (!response.ok) {
    throw new Error("인기 장소를 불러오지 못했습니다.");
  }

  const body = (await response.json()) as SpotListResponse;
  return body.items;
}

/**
 * 존재하지 않거나(404) 백엔드 미설정 환경에서는 null을 반환한다 — 페이지 쪽에서
 * "없음"과 "네트워크 에러"를 구분해 보여줄 수 있도록, 그 외 실패는 에러로 던진다.
 *
 * 이 API는 호출할 때마다 서버의 조회수를 늘리는 부작용이 있다. 호출을 중복 없이
 * 정확히 필요한 만큼만 하는 책임은 호출부(스팟 상세 페이지)가 진다.
 *
 * credentials: "include"가 필요한 이유: 프론트(3000)와 백엔드(8080)가 다른 origin이라
 * 기본값(same-origin)으로는 access_token 쿠키가 안 실려서, 로그인한 사용자도 항상
 * isLiked: false로 내려온다.
 */
export async function fetchSpotDetail(spotId: number | string): Promise<SpotDetail | null> {
  if (!apiBaseUrl) {
    return null;
  }

  const response = await fetch(`${apiBaseUrl}/spots/${spotId}`, { credentials: "include" });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error("장소 정보를 불러오지 못했습니다.");
  }

  return (await response.json()) as SpotDetail;
}

/** 이미 좋아요한 상태에서 또 호출해도 에러 없이 현재 상태를 그대로 돌려준다(idempotent). */
export async function likeSpot(spotId: number | string): Promise<SpotLikeState> {
  return callLikeApi(spotId, "POST");
}

/** 좋아요하지 않은 상태에서 호출해도 에러 없이 현재 상태를 그대로 돌려준다(idempotent). */
export async function unlikeSpot(spotId: number | string): Promise<SpotLikeState> {
  return callLikeApi(spotId, "DELETE");
}

async function callLikeApi(spotId: number | string, method: "POST" | "DELETE"): Promise<SpotLikeState> {
  if (!apiBaseUrl) {
    throw new UnauthorizedError();
  }

  const response = await fetch(`${apiBaseUrl}/spots/${spotId}/like`, {
    method,
    credentials: "include",
  });

  if (response.status === 401 || response.status === 403) {
    throw new UnauthorizedError();
  }
  if (!response.ok) {
    throw new Error("좋아요 처리에 실패했습니다.");
  }

  return (await response.json()) as SpotLikeState;
}
