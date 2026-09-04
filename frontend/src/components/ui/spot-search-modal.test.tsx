import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { Mock } from "vitest";
import SpotSearchModal from "./spot-search-modal";
import * as spotService from "@/services/spots";

vi.mock("@/services/spots");

const mockSpots: spotService.PopularSpot[] = [
  {
    spotId: 1,
    title: "경포해변",
    category: "관광지",
    region: "51",
    sigungu: "150",
    thumbnail: "http://example.com/beach.jpg",
  },
  {
    spotId: 2,
    title: "안목커피거리",
    category: "음식점",
    region: "51",
    sigungu: "150",
    thumbnail: null,
  },
];

describe("SpotSearchModal", () => {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
    onSelect: vi.fn(),
    excludedSpotIds: [],
    dayNumber: 2,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    (spotService.searchSpots as Mock).mockResolvedValue({
      items: mockSpots,
      offset: 0,
      size: 20,
      totalCount: 2,
    });
  });

  it("open=false 일 때는 렌더링되지 않는다", () => {
    render(<SpotSearchModal {...defaultProps} open={false} />);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("open=true 일 때 헤더에 Day 번호와 초기 인기 목록이 렌더링된다", async () => {
    render(<SpotSearchModal {...defaultProps} />);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Day 2에 추가")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
      expect(screen.getByText("안목커피거리")).toBeInTheDocument();
    });

    expect(spotService.searchSpots).toHaveBeenCalledWith({ sort: "popular", size: 20 });
  });

  it("검색어 입력 시 300ms 디바운스 후 keyword로 검색을 호출한다", async () => {
    vi.useFakeTimers();

    try {
      render(<SpotSearchModal {...defaultProps} />);

      const input = screen.getByPlaceholderText(/장소 이름, 지역으로 검색해보세요/i);

      fireEvent.change(input, { target: { value: "속초" } });

      // 300ms 전에는 keyword로 호출되지 않음
      expect(spotService.searchSpots).not.toHaveBeenCalledWith({ keyword: "속초", size: 20 });

      await act(async () => {
        await vi.advanceTimersByTimeAsync(300);
      });

      expect(spotService.searchSpots).toHaveBeenCalledWith({ keyword: "속초", size: 20 });
    } finally {
      vi.useRealTimers();
    }
  });

  it("장소 선택 버튼을 누르면 onSelect와 onClose가 호출된다", async () => {
    render(<SpotSearchModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    const selectButtons = screen.getAllByRole("button", { name: "선택" });
    fireEvent.click(selectButtons[0]);

    expect(defaultProps.onSelect).toHaveBeenCalledWith(mockSpots[0]);
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it("excludedSpotIds에 포함된 항목은 '담김'으로 표시되고 선택할 수 없다", async () => {
    render(<SpotSearchModal {...defaultProps} excludedSpotIds={[1]} />);

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    expect(screen.getByText("담김")).toBeInTheDocument();

    const selectButtons = screen.getAllByRole("button", { name: "선택" });
    expect(selectButtons).toHaveLength(1); // spotId: 2만 선택 가능
  });

  it("Escape 키나 배경 클릭 시 onClose가 호출된다", () => {
    render(<SpotSearchModal {...defaultProps} />);

    fireEvent.keyDown(document, { key: "Escape" });
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);

    const backdrop = screen.getByTestId("spot-search-backdrop");
    fireEvent.mouseDown(backdrop);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(2);
  });

  it("검색 에러 발생 시 에러 메시지를 표시한다", async () => {
    (spotService.searchSpots as Mock).mockRejectedValue(new Error("네트워크 오류"));

    render(<SpotSearchModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText("네트워크 오류")).toBeInTheDocument();
    });
  });
});
