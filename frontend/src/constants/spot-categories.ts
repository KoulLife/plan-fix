/**
 * 스팟 카테고리 목록.
 *
 * 값은 백엔드 TourCategory가 spots.category에 저장하는 한글 명칭과 정확히 일치해야 한다
 * (TourAPI contentTypeId 매핑 + 음식점 중 lclsSystm3가 FD05*면 "카페/음료"로 분리).
 * 목록 필터와 검색 모달이 같은 값을 쓰도록 여기 한 곳에서만 정의한다.
 */
export const SPOT_CATEGORY_OPTIONS = [
  "음식점",
  "카페/음료",
  "관광지",
  "숙박",
  "쇼핑",
  "레포츠",
  "문화시설",
  "축제공연행사",
] as const;

export type SpotCategory = (typeof SPOT_CATEGORY_OPTIONS)[number];

/** 쿼리 파라미터 등 외부 입력이 알려진 카테고리인지 검사한다. */
export function isKnownCategory(value: string | null | undefined): value is SpotCategory {
  return !!value && (SPOT_CATEGORY_OPTIONS as readonly string[]).includes(value);
}
