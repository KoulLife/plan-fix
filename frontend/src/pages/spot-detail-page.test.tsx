import { StrictMode } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import SpotDetailPage from "@/pages/spot-detail-page";
import { fetchSpotDetail, likeSpot, unlikeSpot, UnauthorizedError, type SpotDetail } from "@/services/spots";

jest.mock("@/services/spots");

const mockedFetchSpotDetail = fetchSpotDetail as jest.MockedFunction<typeof fetchSpotDetail>;
const mockedLikeSpot = likeSpot as jest.MockedFunction<typeof likeSpot>;
const mockedUnlikeSpot = unlikeSpot as jest.MockedFunction<typeof unlikeSpot>;

function renderAt(spotId: string, { strict = false }: { strict?: boolean } = {}) {
  const tree = (
    <MemoryRouter
      initialEntries={[`/spots/${spotId}`]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route path="/spots/:spotId" element={<SpotDetailPage />} />
        <Route path="/login" element={<div>로그인 페이지</div>} />
      </Routes>
    </MemoryRouter>
  );

  return render(strict ? <StrictMode>{tree}</StrictMode> : tree);
}

afterEach(() => {
  mockedFetchSpotDetail.mockReset();
});

test("shows a loading status while the detail is being fetched", () => {
  mockedFetchSpotDetail.mockReturnValue(new Promise(() => {}));

  renderAt("1");

  expect(screen.getByRole("status", { name: "장소 정보를 불러오는 중..." })).toBeInTheDocument();
});

test("calls fetchSpotDetail only once per spot even under StrictMode's dev-mode double effect invocation", async () => {
  // 상세 조회는 호출될 때마다 서버의 조회수를 올리는 부작용이 있다. React.StrictMode는
  // 개발 모드에서 effect를 마운트→클린업→재마운트로 일부러 두 번 실행하는데, 이때 매번
  // fetch를 새로 호출하면 화면 한 번 들어왔는데 조회수가 2씩 올라간다(실제로 겪은 버그).
  mockedFetchSpotDetail.mockResolvedValue({
    spotId: 1,
    title: "정동진",
    category: "관광지",
    region: null,
    sigungu: null,
    address: null,
    latitude: null,
    longitude: null,
    thumbnail: null,
    description: null,
    viewCount: 1,
    likeCount: 0,
    commentCount: 0,
    images: [],
    info: null,
    isLiked: false,
  });

  renderAt("1", { strict: true });

  await screen.findByRole("heading", { name: "정동진" });
  expect(mockedFetchSpotDetail).toHaveBeenCalledTimes(1);
});

test("renders the spot detail once it loads", async () => {
  mockedFetchSpotDetail.mockResolvedValue({
    spotId: 1,
    title: "정동진",
    category: "관광지",
    region: "51",
    sigungu: "150",
    address: "강원특별자치도 강릉시",
    latitude: 37.1,
    longitude: 129.0,
    thumbnail: "https://example.com/thumb.jpg",
    description: "동해안의 대표 해변",
    viewCount: 11,
    likeCount: 3,
    commentCount: 1,
    images: [],
    info: null,
    isLiked: false,
  });

  renderAt("1");

  expect(await screen.findByRole("heading", { name: "정동진" })).toBeInTheDocument();
  expect(screen.getByText("관광지")).toBeInTheDocument();
  expect(screen.getByText("강원특별자치도 강릉시")).toBeInTheDocument();
  expect(screen.getByText("동해안의 대표 해변")).toBeInTheDocument();
  expect(screen.getByText("좋아요 3")).toBeInTheDocument();
  expect(screen.getByText("조회수 11")).toBeInTheDocument();
  expect(mockedFetchSpotDetail).toHaveBeenCalledWith("1");
});

test("omits the address and description when they are missing", async () => {
  mockedFetchSpotDetail.mockResolvedValue({
    spotId: 2,
    title: "이름만 있는 장소",
    category: "관광지",
    region: null,
    sigungu: null,
    address: null,
    latitude: null,
    longitude: null,
    thumbnail: null,
    description: null,
    viewCount: 0,
    likeCount: 0,
    commentCount: 0,
    images: [],
    info: null,
    isLiked: false,
  });

  renderAt("2");

  expect(await screen.findByRole("heading", { name: "이름만 있는 장소" })).toBeInTheDocument();
  expect(screen.queryByText("null")).not.toBeInTheDocument();
  expect(screen.queryByText("이용 안내")).not.toBeInTheDocument();
  // 메인 사진 하나만 있어야 한다 (갤러리 없음)
  expect(screen.getAllByRole("img")).toHaveLength(1);
});

test("renders extra photos as a gallery when images are present", async () => {
  mockedFetchSpotDetail.mockResolvedValue({
    spotId: 1,
    title: "국립대관령자연휴양림",
    category: "관광지",
    region: "51",
    sigungu: "150",
    address: null,
    latitude: null,
    longitude: null,
    thumbnail: "https://example.com/main.jpg",
    description: null,
    viewCount: 1,
    likeCount: 0,
    commentCount: 0,
    images: ["https://example.com/1.jpg", "https://example.com/2.jpg"],
    info: null,
    isLiked: false,
  });

  renderAt("1");

  await screen.findByRole("heading", { name: "국립대관령자연휴양림" });
  // 메인 사진 1장 + 갤러리 2장
  expect(screen.getAllByRole("img")).toHaveLength(3);
  expect(screen.getByAltText("국립대관령자연휴양림 사진 1")).toHaveAttribute(
    "src",
    "https://example.com/1.jpg",
  );
  expect(screen.getByAltText("국립대관령자연휴양림 사진 2")).toHaveAttribute(
    "src",
    "https://example.com/2.jpg",
  );
});

test("renders only the usage info fields that have a value", async () => {
  mockedFetchSpotDetail.mockResolvedValue({
    spotId: 1,
    title: "국립대관령자연휴양림",
    category: "관광지",
    region: null,
    sigungu: null,
    address: null,
    latitude: null,
    longitude: null,
    thumbnail: null,
    description: null,
    viewCount: 0,
    likeCount: 0,
    commentCount: 0,
    images: [],
    info: {
      tel: "033-641-9990",
      parkInfo: "가능",
      timeInfo: "09:00~18:00",
      restInfo: "매주 화요일",
      firstMenu: null,
      treatMenu: null,
      lcnsno: null,
    },
    isLiked: false,
  });

  renderAt("1");

  await screen.findByRole("heading", { name: "국립대관령자연휴양림" });
  expect(screen.getByText("이용 안내")).toBeInTheDocument();
  expect(screen.getByText("033-641-9990")).toBeInTheDocument();
  expect(screen.getByText("가능")).toBeInTheDocument();
  expect(screen.getByText("09:00~18:00")).toBeInTheDocument();
  expect(screen.getByText("매주 화요일")).toBeInTheDocument();
  expect(screen.queryByText("대표메뉴")).not.toBeInTheDocument();
  expect(screen.queryByText("취급메뉴")).not.toBeInTheDocument();
  expect(screen.queryByText("인허가번호")).not.toBeInTheDocument();
});

test("shows restaurant menu fields when the spot has them", async () => {
  mockedFetchSpotDetail.mockResolvedValue({
    spotId: 523,
    title: "롱블랙",
    category: "음식점",
    region: null,
    sigungu: null,
    address: null,
    latitude: null,
    longitude: null,
    thumbnail: null,
    description: null,
    viewCount: 0,
    likeCount: 0,
    commentCount: 0,
    images: [],
    info: {
      tel: null,
      parkInfo: null,
      timeInfo: null,
      restInfo: null,
      firstMenu: "롱블랙",
      treatMenu: "초당옥수수라떼",
      lcnsno: "20180405110",
    },
    isLiked: false,
  });

  renderAt("523");

  await screen.findByRole("heading", { name: "롱블랙" });
  expect(screen.getByText("대표메뉴")).toBeInTheDocument();
  expect(screen.getByText("롱블랙", { selector: "dd" })).toBeInTheDocument();
  expect(screen.getByText("취급메뉴")).toBeInTheDocument();
  expect(screen.getByText("초당옥수수라떼")).toBeInTheDocument();
  expect(screen.getByText("인허가번호")).toBeInTheDocument();
  expect(screen.getByText("20180405110")).toBeInTheDocument();
});

function spotFixture(overrides: Partial<SpotDetail>): SpotDetail {
  return {
    spotId: 1,
    title: "정동진",
    category: "관광지",
    region: null,
    sigungu: null,
    address: null,
    latitude: null,
    longitude: null,
    thumbnail: null,
    description: null,
    viewCount: 0,
    likeCount: 3,
    commentCount: 0,
    images: [],
    info: null,
    isLiked: false,
    ...overrides,
  };
}

test("클릭하면 좋아요를 요청하고 하트와 카운트를 갱신한다", async () => {
  mockedFetchSpotDetail.mockResolvedValue(spotFixture({ isLiked: false, likeCount: 3 }));
  mockedLikeSpot.mockResolvedValue({ liked: true, likeCount: 4 });

  renderAt("1");
  await screen.findByRole("heading", { name: "정동진" });

  const likeButton = screen.getByRole("button", { name: "정동진 좋아요" });
  expect(likeButton).toHaveAttribute("aria-pressed", "false");
  fireEvent.click(likeButton);

  expect(await screen.findByText("좋아요 4")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "정동진 좋아요 취소" })).toHaveAttribute("aria-pressed", "true");
  expect(mockedLikeSpot).toHaveBeenCalledWith(1);
});

test("이미 좋아요한 상태에서 클릭하면 취소를 요청한다", async () => {
  mockedFetchSpotDetail.mockResolvedValue(spotFixture({ isLiked: true, likeCount: 4 }));
  mockedUnlikeSpot.mockResolvedValue({ liked: false, likeCount: 3 });

  renderAt("1");
  await screen.findByRole("heading", { name: "정동진" });

  fireEvent.click(screen.getByRole("button", { name: "정동진 좋아요 취소" }));

  expect(await screen.findByText("좋아요 3")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "정동진 좋아요" })).toHaveAttribute("aria-pressed", "false");
  expect(mockedUnlikeSpot).toHaveBeenCalledWith(1);
});

test("로그인이 필요하면(401/403) 로그인 페이지로 이동한다", async () => {
  mockedFetchSpotDetail.mockResolvedValue(spotFixture({ isLiked: false }));
  mockedLikeSpot.mockRejectedValue(new UnauthorizedError());

  renderAt("1");
  await screen.findByRole("heading", { name: "정동진" });

  fireEvent.click(screen.getByRole("button", { name: "정동진 좋아요" }));

  expect(await screen.findByText("로그인 페이지")).toBeInTheDocument();
});

test("shows a not-found message when the spot doesn't exist", async () => {
  mockedFetchSpotDetail.mockResolvedValue(null);

  renderAt("999");

  expect(await screen.findByText("존재하지 않는 장소예요.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "돌아가기" })).toBeInTheDocument();
});

test("shows the same not-found message when the fetch fails", async () => {
  mockedFetchSpotDetail.mockRejectedValue(new Error("network error"));

  renderAt("1");

  await waitFor(() => {
    expect(screen.getByText("존재하지 않는 장소예요.")).toBeInTheDocument();
  });
});
