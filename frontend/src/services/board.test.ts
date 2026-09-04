import { setApiBaseUrl } from "@/test-utils/env";

describe("fetchPopularBoards", () => {
  const originalApiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    vi.resetModules();
  });

  test("VITE_API_BASE_URL이 없으면 빈 목록을 반환하고 fetch를 호출하지 않는다", async () => {
    setApiBaseUrl(undefined);
    const fetchSpy = vi.fn();
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchPopularBoards } = (await import("./board")) as typeof import("./board");

    const result = await fetchPopularBoards();

    expect(result).toEqual({ items: [], offset: 0, size: 6, totalCount: 0 });
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  test("sort=popular와 size를 쿼리에 담아 호출하고 결과를 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const items = [
      {
        boardId: 1,
        title: "강릉 당일치기 여행 코스 공유",
        thumbnail: "https://example.com/board1.jpg",
        userId: 10,
        likeCount: 15,
        viewCount: 120,
        commentCount: 4,
        createdAt: "2026-09-01T12:00:00Z",
      },
    ];
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items, offset: 0, size: 6, totalCount: 1 }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchPopularBoards } = (await import("./board")) as typeof import("./board");

    const result = await fetchPopularBoards({ size: 6 });

    expect(result).toEqual({ items, offset: 0, size: 6, totalCount: 1 });
    const calledUrl = new URL(fetchSpy.mock.calls[0][0] as string);
    expect(calledUrl.origin + calledUrl.pathname).toBe("http://localhost:8080/api/v1/boards");
    expect(calledUrl.searchParams.get("sort")).toBe("popular");
    expect(calledUrl.searchParams.get("size")).toBe("6");
  });

  test("size를 지정하지 않으면 기본값 6을 쓴다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items: [], offset: 0, size: 6, totalCount: 0 }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchPopularBoards } = (await import("./board")) as typeof import("./board");

    await fetchPopularBoards();

    const calledUrl = new URL(fetchSpy.mock.calls[0][0] as string);
    expect(calledUrl.searchParams.get("sort")).toBe("popular");
    expect(calledUrl.searchParams.get("size")).toBe("6");
  });

  test("offset 파라미터가 주어지면 쿼리에 offset을 포함한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items: [], offset: 12, size: 6, totalCount: 0 }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchPopularBoards } = (await import("./board")) as typeof import("./board");

    await fetchPopularBoards({ offset: 12, size: 6 });

    const calledUrl = new URL(fetchSpy.mock.calls[0][0] as string);
    expect(calledUrl.searchParams.get("offset")).toBe("12");
    expect(calledUrl.searchParams.get("size")).toBe("6");
  });

  test("응답이 실패(ok=false)면 에러를 던진다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = vi.fn().mockResolvedValue({ ok: false, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchPopularBoards } = (await import("./board")) as typeof import("./board");

    await expect(fetchPopularBoards()).rejects.toThrow("게시글을 불러오지 못했습니다.");
  });
});

describe("fetchBoardDetail", () => {
  const originalApiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    vi.resetModules();
  });

  test("VITE_API_BASE_URL이 없으면 null을 반환하고 fetch를 호출하지 않는다", async () => {
    setApiBaseUrl(undefined);
    const fetchSpy = vi.fn();
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchBoardDetail } = (await import("./board")) as typeof import("./board");

    const result = await fetchBoardDetail(1);

    expect(result).toBeNull();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  test("정상 응답이면 상세 정보를 그대로 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const detail = {
      boardId: 1,
      courseId: null,
      userId: 10,
      title: "강릉 카페 투어 완벽 가이드",
      content: "<p>강릉의 최고 카페들을 소개합니다.</p>",
      thumbnail: "https://example.com/thumb.jpg",
      status: "PUBLISHED",
      viewCount: 150,
      likeCount: 25,
      commentCount: 8,
      images: [
        { imageUrl: "https://example.com/cafe1.jpg", altText: "카페 1", sequence: 1 },
      ],
      createdAt: "2026-09-01T10:00:00Z",
      updatedAt: "2026-09-01T10:00:00Z",
    };
    const fetchSpy = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => detail });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchBoardDetail } = (await import("./board")) as typeof import("./board");

    const result = await fetchBoardDetail(1);

    expect(result).toEqual(detail);
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/boards/1");
  });

  test("404면 null을 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = vi.fn().mockResolvedValue({ ok: false, status: 404, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchBoardDetail } = (await import("./board")) as typeof import("./board");

    const result = await fetchBoardDetail(999);

    expect(result).toBeNull();
  });

  test("404가 아닌 실패면 에러를 던진다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = vi.fn().mockResolvedValue({ ok: false, status: 500, json: async () => ({}) });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { fetchBoardDetail } = (await import("./board")) as typeof import("./board");

    await expect(fetchBoardDetail(1)).rejects.toThrow("게시글을 불러오지 못했습니다.");
  });
});
