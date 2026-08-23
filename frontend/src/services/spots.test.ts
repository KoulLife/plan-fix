// process.env의 타입 선언이 readonly라 캐스팅해서 값을 바꾼다.
const mutableEnv = process.env as unknown as Record<string, string | undefined>;

function setApiBaseUrl(value: string | undefined) {
  if (value === undefined) {
    delete mutableEnv.REACT_APP_API_BASE_URL;
  } else {
    mutableEnv.REACT_APP_API_BASE_URL = value;
  }
}

// REACT_APP_API_BASE_URL을 모듈 로드 시점에 한 번만 읽으므로(services/auth.ts와 같은 방식),
// 값을 바꿔 가며 테스트하려면 매번 process.env를 세팅한 뒤 모듈을 다시 불러와야 한다.
describe("fetchPopularSpots", () => {
  const originalApiBaseUrl = process.env.REACT_APP_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    jest.resetModules();
  });

  test("REACT_APP_API_BASE_URL이 없으면 빈 목록을 반환하고 fetch를 호출하지 않는다", async () => {
    setApiBaseUrl(undefined);
    const fetchSpy = jest.fn();
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchPopularSpots } = require("./spots") as typeof import("./spots");

    const result = await fetchPopularSpots();

    expect(result).toEqual([]);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  test("sort=popular와 size를 쿼리에 담아 호출하고 items를 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const items = [
      { spotId: 1, title: "정동진", category: "관광지", region: "51", sigungu: "150", thumbnail: "thumb.jpg" },
    ];
    const fetchSpy = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items, offset: 0, size: 2, totalCount: 1 }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchPopularSpots } = require("./spots") as typeof import("./spots");

    const result = await fetchPopularSpots({ region: "51", sigungu: "150" });

    expect(result).toEqual(items);
    const calledUrl = new URL(fetchSpy.mock.calls[0][0] as string);
    expect(calledUrl.origin + calledUrl.pathname).toBe("http://localhost:8080/api/v1/spots");
    expect(calledUrl.searchParams.get("sort")).toBe("popular");
    expect(calledUrl.searchParams.get("size")).toBe("2");
    expect(calledUrl.searchParams.get("region")).toBe("51");
    expect(calledUrl.searchParams.get("sigungu")).toBe("150");
  });

  test("size를 지정하지 않으면 기본값 2를 쓴다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items: [], offset: 0, size: 2, totalCount: 0 }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchPopularSpots } = require("./spots") as typeof import("./spots");

    await fetchPopularSpots();

    const calledUrl = new URL(fetchSpy.mock.calls[0][0] as string);
    expect(calledUrl.searchParams.get("size")).toBe("2");
    expect(calledUrl.searchParams.has("region")).toBe(false);
    expect(calledUrl.searchParams.has("sigungu")).toBe(false);
  });

  test("응답이 실패(ok=false)면 에러를 던진다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({ ok: false, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchPopularSpots } = require("./spots") as typeof import("./spots");

    await expect(fetchPopularSpots()).rejects.toThrow("인기 장소를 불러오지 못했습니다.");
  });
});

describe("fetchSpotDetail", () => {
  const originalApiBaseUrl = process.env.REACT_APP_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    jest.resetModules();
  });

  test("REACT_APP_API_BASE_URL이 없으면 null을 반환하고 fetch를 호출하지 않는다", async () => {
    setApiBaseUrl(undefined);
    const fetchSpy = jest.fn();
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchSpotDetail } = require("./spots") as typeof import("./spots");

    const result = await fetchSpotDetail(1);

    expect(result).toBeNull();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  test("정상 응답이면 상세 정보를 그대로 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const detail = {
      spotId: 1,
      title: "정동진",
      category: "관광지",
      region: "51",
      sigungu: "150",
      address: "강원특별자치도 강릉시",
      latitude: 37.1,
      longitude: 129.0,
      thumbnail: "thumb.jpg",
      description: "동해안의 대표 해변",
      viewCount: 11,
      likeCount: 3,
      commentCount: 1,
      images: ["https://example.com/1.jpg"],
      info: {
        tel: "033-000-0000",
        parkInfo: "가능",
        timeInfo: "09:00~18:00",
        restInfo: "연중무휴",
        firstMenu: null,
        treatMenu: null,
        lcnsno: null,
      },
      isLiked: true,
    };
    const fetchSpy = jest.fn().mockResolvedValue({ ok: true, status: 200, json: async () => detail });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchSpotDetail } = require("./spots") as typeof import("./spots");

    const result = await fetchSpotDetail(1);

    expect(result).toEqual(detail);
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/spots/1", { credentials: "include" });
  });

  test("404면 null을 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({ ok: false, status: 404, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchSpotDetail } = require("./spots") as typeof import("./spots");

    const result = await fetchSpotDetail(999);

    expect(result).toBeNull();
  });

  test("404가 아닌 실패면 에러를 던진다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({ ok: false, status: 500, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { fetchSpotDetail } = require("./spots") as typeof import("./spots");

    await expect(fetchSpotDetail(1)).rejects.toThrow("장소 정보를 불러오지 못했습니다.");
  });
});

describe.each([
  ["likeSpot", "POST"],
  ["unlikeSpot", "DELETE"],
] as const)("%s", (fnName, method) => {
  const originalApiBaseUrl = process.env.REACT_APP_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    jest.resetModules();
  });

  function load() {
    jest.resetModules();
    return require("./spots") as typeof import("./spots");
  }

  test("쿠키를 포함해 올바른 method로 호출하고 결과를 그대로 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ liked: method === "POST", likeCount: 4 }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    const spots = load();

    const result = await spots[fnName](1);

    expect(result).toEqual({ liked: method === "POST", likeCount: 4 });
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/spots/1/like", {
      method,
      credentials: "include",
    });
  });

  test.each([401, 403])("%d 응답이면 UnauthorizedError를 던진다", async (status) => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({ ok: false, status, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    const spots = load();

    await expect(spots[fnName](1)).rejects.toThrow(spots.UnauthorizedError);
  });

  test("그 외 실패면 일반 에러를 던진다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({ ok: false, status: 500, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    const spots = load();

    await expect(spots[fnName](1)).rejects.toThrow("좋아요 처리에 실패했습니다.");
  });

  test("REACT_APP_API_BASE_URL이 없으면 UnauthorizedError를 던지고 fetch를 호출하지 않는다", async () => {
    setApiBaseUrl(undefined);
    const fetchSpy = jest.fn();
    global.fetch = fetchSpy as unknown as typeof fetch;
    const spots = load();

    await expect(spots[fnName](1)).rejects.toThrow(spots.UnauthorizedError);
    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
