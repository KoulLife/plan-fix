import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import MainPage from "@/pages/main-page";
import { signOut } from "@/services/auth";
import { fetchPopularBoards } from "@/services/board";
import { fetchPopularSpots, likeSpot, unlikeSpot, UnauthorizedError } from "@/services/spots";

const mockedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

jest.mock("@/services/spots");
jest.mock("@/services/board");
jest.mock("@/services/auth");

const mockedFetchPopularSpots = fetchPopularSpots as jest.MockedFunction<typeof fetchPopularSpots>;
const mockedFetchPopularBoards = fetchPopularBoards as jest.MockedFunction<typeof fetchPopularBoards>;
const mockedLikeSpot = likeSpot as jest.MockedFunction<typeof likeSpot>;
const mockedUnlikeSpot = unlikeSpot as jest.MockedFunction<typeof unlikeSpot>;
const mockedSignOut = signOut as jest.MockedFunction<typeof signOut>;

function renderMainPage() {
  return render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <MainPage />
    </MemoryRouter>,
  );
}

describe("MainPage popular spots carousel", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedFetchPopularBoards.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });
  });

  test("fetches popular spots with size 6 on initial load", async () => {
    mockedFetchPopularSpots.mockResolvedValue({
      items: [
        {
          spotId: 1,
          title: "경포해변",
          category: "관광지",
          region: "51",
          sigungu: "150",
          thumbnail: "https://example.com/thumb1.jpg",
        },
        {
          spotId: 2,
          title: "안목해변",
          category: "관광지",
          region: "51",
          sigungu: "150",
          thumbnail: "https://example.com/thumb2.jpg",
        },
      ],
      offset: 0,
      size: 6,
      totalCount: 2,
    });

    renderMainPage();

    expect(mockedFetchPopularSpots).toHaveBeenCalledWith({
      region: undefined,
      sigungu: undefined,
      size: 6,
    });

    expect(await screen.findByText("경포해변")).toBeInTheDocument();
    expect(screen.getByText("안목해변")).toBeInTheDocument();
  });

  test("shows empty message when fetch fails or returns empty list", async () => {
    mockedFetchPopularSpots.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });

    renderMainPage();

    expect(await screen.findByText("표시할 인기 장소가 없어요.")).toBeInTheDocument();
  });

  test("clicking '인기 장소 더보기' navigates to /spots/popular", async () => {
    mockedFetchPopularSpots.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });

    renderMainPage();

    const moreButton = screen.getByRole("button", { name: "인기 장소 더보기" });
    fireEvent.click(moreButton);

    expect(mockedNavigate).toHaveBeenCalledWith("/spots/popular");
  });

  test("clicking '인기 장소 더보기' with selected region navigates to /spots/popular with region query", async () => {
    mockedFetchPopularSpots.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });

    renderMainPage();

    fireEvent.click(screen.getByRole("button", { name: "여행 지역 선택: 강원도 / 지역 선택" }));
    fireEvent.click(screen.getByRole("button", { name: "강릉" }));
    fireEvent.click(screen.getByRole("button", { name: "강릉 선택하기" }));

    const moreButton = screen.getByRole("button", { name: "인기 장소 더보기" });
    fireEvent.click(moreButton);

    expect(mockedNavigate).toHaveBeenCalledWith("/spots/popular?region=%EA%B0%95%EB%A6%89");
  });

  test("carousel arrow buttons scroll the carousel left and right based on scroll position", async () => {
    mockedFetchPopularSpots.mockResolvedValue({
      items: [
        {
          spotId: 1,
          title: "경포해변",
          category: "관광지",
          region: "51",
          sigungu: "150",
          thumbnail: null,
        },
      ],
      offset: 0,
      size: 6,
      totalCount: 1,
    });

    renderMainPage();
    await screen.findByText("경포해변");

    const carouselElement = screen.getByText("경포해변").closest(".overflow-x-auto") as HTMLElement;
    const scrollByMock = jest.fn();
    carouselElement.scrollBy = scrollByMock;

    Object.defineProperty(carouselElement, "scrollWidth", { value: 1000, configurable: true });
    Object.defineProperty(carouselElement, "clientWidth", { value: 300, configurable: true });
    Object.defineProperty(carouselElement, "scrollLeft", { value: 0, configurable: true, writable: true });

    fireEvent.scroll(carouselElement);

    expect(screen.queryByRole("button", { name: "이전 인기 장소 보기" })).not.toBeInTheDocument();
    const rightButton = await screen.findByRole("button", { name: "다음 인기 장소 보기" });
    expect(rightButton).toBeInTheDocument();

    fireEvent.click(rightButton);
    expect(scrollByMock).toHaveBeenCalledWith(
      expect.objectContaining({ left: expect.any(Number), behavior: "smooth" }),
    );

    carouselElement.scrollLeft = 200;
    fireEvent.scroll(carouselElement);

    const leftButton = await screen.findByRole("button", { name: "이전 인기 장소 보기" });
    expect(leftButton).toBeInTheDocument();

    fireEvent.click(leftButton);
    expect(scrollByMock).toHaveBeenCalledWith(
      expect.objectContaining({ left: expect.any(Number), behavior: "smooth" }),
    );
    expect(scrollByMock.mock.calls[1][0].left).toBeLessThan(0);
  });

  test("toggles like on a spot card and handles like/unlike correctly", async () => {
    mockedFetchPopularSpots.mockResolvedValue({
      items: [
        {
          spotId: 10,
          title: "속초해수욕장",
          category: "관광지",
          region: "51",
          sigungu: "160",
          thumbnail: null,
        },
      ],
      offset: 0,
      size: 6,
      totalCount: 1,
    });
    mockedLikeSpot.mockResolvedValue({ liked: true, likeCount: 5 });
    mockedUnlikeSpot.mockResolvedValue({ liked: false, likeCount: 4 });

    renderMainPage();
    await screen.findByText("속초해수욕장");

    const likeButton = screen.getByRole("button", { name: "속초해수욕장 좋아요" });
    expect(likeButton).toHaveAttribute("aria-pressed", "false");

    // Click to like
    fireEvent.click(likeButton);
    expect(mockedLikeSpot).toHaveBeenCalledWith(10);

    const likedButton = await screen.findByRole("button", { name: "속초해수욕장 좋아요 취소" });
    expect(likedButton).toHaveAttribute("aria-pressed", "true");

    // Click to unlike
    fireEvent.click(likedButton);
    expect(mockedUnlikeSpot).toHaveBeenCalledWith(10);

    const unlikedButton = await screen.findByRole("button", { name: "속초해수욕장 좋아요" });
    expect(unlikedButton).toHaveAttribute("aria-pressed", "false");
  });

  test("ignores UnauthorizedError silently when like is pressed without authentication", async () => {
    mockedFetchPopularSpots.mockResolvedValue({
      items: [
        {
          spotId: 20,
          title: "오죽헌",
          category: "문화시설",
          region: "51",
          sigungu: "150",
          thumbnail: null,
        },
      ],
      offset: 0,
      size: 6,
      totalCount: 1,
    });
    mockedLikeSpot.mockRejectedValue(new UnauthorizedError());

    renderMainPage();
    await screen.findByText("오죽헌");

    const likeButton = screen.getByRole("button", { name: "오죽헌 좋아요" });
    fireEvent.click(likeButton);

    await waitFor(() => {
      expect(mockedLikeSpot).toHaveBeenCalledWith(20);
    });

    // Should remain unliked without throwing error or crashing
    expect(screen.getByRole("button", { name: "오죽헌 좋아요" })).toHaveAttribute("aria-pressed", "false");
  });
});

describe("MainPage popular boards carousel", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedFetchPopularSpots.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });
  });

  test("fetches popular boards with size 6 on initial load and renders cards", async () => {
    mockedFetchPopularBoards.mockResolvedValue({
      items: [
        {
          boardId: 101,
          title: "강릉 카페 투어 추천",
          thumbnail: "https://example.com/cafe.jpg",
          userId: 1,
          likeCount: 12,
          viewCount: 150,
          commentCount: 5,
          createdAt: "2026-09-01T10:00:00Z",
        },
        {
          boardId: 102,
          title: "속초 1박 2일 코스",
          thumbnail: null,
          userId: 2,
          likeCount: 8,
          viewCount: 90,
          commentCount: 2,
          createdAt: "2026-09-01T11:00:00Z",
        },
      ],
      offset: 0,
      size: 6,
      totalCount: 2,
    });

    renderMainPage();

    expect(mockedFetchPopularBoards).toHaveBeenCalledWith({ size: 6 });

    expect(await screen.findByText("강릉 카페 투어 추천")).toBeInTheDocument();
    expect(screen.getByText("속초 1박 2일 코스")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByText("8")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();

    const link1 = screen.getByText("강릉 카페 투어 추천").closest("a");
    const link2 = screen.getByText("속초 1박 2일 코스").closest("a");
    expect(link1).toHaveAttribute("href", "/boards/101");
    expect(link2).toHaveAttribute("href", "/boards/102");
  });

  test("shows empty message when fetch boards fails or returns empty list", async () => {
    mockedFetchPopularBoards.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });

    renderMainPage();

    expect(await screen.findByText("표시할 게시글이 없어요.")).toBeInTheDocument();
  });

  test("board carousel arrow buttons scroll the board carousel left and right based on scroll position", async () => {
    mockedFetchPopularBoards.mockResolvedValue({
      items: [
        {
          boardId: 101,
          title: "강릉 카페 투어 추천",
          thumbnail: null,
          userId: 1,
          likeCount: 0,
          viewCount: 0,
          commentCount: 0,
          createdAt: "2026-09-01T10:00:00Z",
        },
      ],
      offset: 0,
      size: 6,
      totalCount: 1,
    });

    renderMainPage();
    await screen.findByText("강릉 카페 투어 추천");

    const carouselElement = screen.getByText("강릉 카페 투어 추천").closest(".overflow-x-auto") as HTMLElement;
    const scrollByMock = jest.fn();
    carouselElement.scrollBy = scrollByMock;

    Object.defineProperty(carouselElement, "scrollWidth", { value: 1000, configurable: true });
    Object.defineProperty(carouselElement, "clientWidth", { value: 300, configurable: true });
    Object.defineProperty(carouselElement, "scrollLeft", { value: 0, configurable: true, writable: true });

    fireEvent.scroll(carouselElement);

    expect(screen.queryByRole("button", { name: "이전 게시글 보기" })).not.toBeInTheDocument();
    const rightButton = await screen.findByRole("button", { name: "다음 게시글 보기" });
    expect(rightButton).toBeInTheDocument();

    fireEvent.click(rightButton);
    expect(scrollByMock).toHaveBeenCalledWith(
      expect.objectContaining({ left: expect.any(Number), behavior: "smooth" }),
    );

    carouselElement.scrollLeft = 200;
    fireEvent.scroll(carouselElement);

    const leftButton = await screen.findByRole("button", { name: "이전 게시글 보기" });
    expect(leftButton).toBeInTheDocument();

    fireEvent.click(leftButton);
    expect(scrollByMock).toHaveBeenCalledWith(
      expect.objectContaining({ left: expect.any(Number), behavior: "smooth" }),
    );
    expect(scrollByMock.mock.calls[1][0].left).toBeLessThan(0);
  });
});

describe("MainPage navigation and logout", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedFetchPopularSpots.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });
    mockedFetchPopularBoards.mockResolvedValue({ items: [], offset: 0, size: 6, totalCount: 0 });
  });

  test("clicking profile nav button toggles the profile menu with logout option", async () => {
    renderMainPage();

    const profileButton = screen.getByRole("button", { name: "프로필" });
    expect(screen.queryByRole("menu", { name: "프로필 메뉴" })).not.toBeInTheDocument();
    expect(screen.queryByRole("menuitem", { name: "로그아웃" })).not.toBeInTheDocument();

    // Open profile menu
    fireEvent.click(profileButton);
    expect(screen.getByRole("menu", { name: "프로필 메뉴" })).toBeInTheDocument();
    expect(screen.getByRole("menuitem", { name: "로그아웃" })).toBeInTheDocument();

    // Toggle close profile menu
    fireEvent.click(profileButton);
    expect(screen.queryByRole("menu", { name: "프로필 메뉴" })).not.toBeInTheDocument();
  });

  test("clicking outside or backdrop closes the profile menu", async () => {
    renderMainPage();

    const profileButton = screen.getByRole("button", { name: "프로필" });
    fireEvent.click(profileButton);
    expect(screen.getByRole("menu", { name: "프로필 메뉴" })).toBeInTheDocument();

    const backdrop = screen.getByTestId("profile-menu-backdrop");
    fireEvent.click(backdrop);
    expect(screen.queryByRole("menu", { name: "프로필 메뉴" })).not.toBeInTheDocument();
  });

  test("pressing Escape key closes the profile menu", async () => {
    renderMainPage();

    const profileButton = screen.getByRole("button", { name: "프로필" });
    fireEvent.click(profileButton);
    expect(screen.getByRole("menu", { name: "프로필 메뉴" })).toBeInTheDocument();

    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("menu", { name: "프로필 메뉴" })).not.toBeInTheDocument();
  });

  test("clicking logout button calls signOut and navigates to /login with replace", async () => {
    mockedSignOut.mockResolvedValue(undefined);
    renderMainPage();

    const profileButton = screen.getByRole("button", { name: "프로필" });
    fireEvent.click(profileButton);

    const logoutButton = screen.getByRole("menuitem", { name: "로그아웃" });
    fireEvent.click(logoutButton);

    expect(mockedSignOut).toHaveBeenCalledTimes(1);
    await waitFor(() => {
      expect(mockedNavigate).toHaveBeenCalledWith("/login", { replace: true });
    });
  });

  test("navigates to /login with replace even if signOut throws an error", async () => {
    mockedSignOut.mockRejectedValue(new Error("Network failed"));
    renderMainPage();

    const profileButton = screen.getByRole("button", { name: "프로필" });
    fireEvent.click(profileButton);

    const logoutButton = screen.getByRole("menuitem", { name: "로그아웃" });
    fireEvent.click(logoutButton);

    expect(mockedSignOut).toHaveBeenCalledTimes(1);
    await waitFor(() => {
      expect(mockedNavigate).toHaveBeenCalledWith("/login", { replace: true });
    });
  });

  test("disables logout button while signOut is processing", async () => {
    let resolveSignOut!: () => void;
    mockedSignOut.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveSignOut = resolve;
        }),
    );

    renderMainPage();

    const profileButton = screen.getByRole("button", { name: "프로필" });
    fireEvent.click(profileButton);

    const logoutButton = screen.getByRole("menuitem", { name: "로그아웃" });
    fireEvent.click(logoutButton);

    expect(logoutButton).toBeDisabled();
    expect(screen.getByText("로그아웃 중...")).toBeInTheDocument();

    resolveSignOut();

    await waitFor(() => {
      expect(mockedNavigate).toHaveBeenCalledWith("/login", { replace: true });
    });
  });
});
