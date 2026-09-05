import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import type { Mock } from "vitest";
import { MemoryRouter } from "react-router-dom";
import CourseCreatePage from "./course-create-page";
import * as courseService from "@/services/course";
import * as spotService from "@/services/spots";

vi.mock("@/services/course");
vi.mock("@/services/spots");

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe("CourseCreatePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    (spotService.searchSpots as Mock).mockResolvedValue({
      items: [
        {
          spotId: 101,
          title: "경포해변",
          category: "관광지",
          region: "51",
          sigungu: "150",
          thumbnail: null,
        },
      ],
      offset: 0,
      size: 20,
      totalCount: 1,
    });
  });

  const renderPage = () => {
    return render(
      <MemoryRouter>
        <CourseCreatePage />
      </MemoryRouter>
    );
  };

  it("초기 렌더링 시 기본 날짜 범위에 맞춰 Day 카드가 렌더링된다", () => {
    renderPage();
    expect(screen.getByText("나만의 여행 코스 만들기")).toBeInTheDocument();
    expect(screen.getByTestId("day-card-1")).toBeInTheDocument();
    expect(screen.getByTestId("day-card-2")).toBeInTheDocument();
    expect(screen.getByTestId("day-card-3")).toBeInTheDocument();
  });

  it("제목이 없거나 담긴 장소가 0개이면 저장 버튼이 비활성화된다", () => {
    renderPage();
    const saveButton = screen.getByRole("button", { name: "코스 저장하기" });
    expect(saveButton).toBeDisabled();
    expect(screen.getByText(/코스 제목을 입력해주세요/i)).toBeInTheDocument();
  });

  it("장소 추가를 누르면 모달이 열리고 장소를 선택하면 해당 Day에 추가된다", async () => {
    renderPage();

    const titleInput = screen.getByPlaceholderText(/2박 3일 강릉 힐링/i);
    fireEvent.change(titleInput, { target: { value: "강릉 바다 여행" } });

    // Day 1의 장소 추가 버튼 클릭
    const addButtons = screen.getAllByRole("button", { name: /장소 추가/i });
    fireEvent.click(addButtons[0]);

    // 모달 오픈 확인 및 장소 선택
    await waitFor(() => {
      expect(screen.getByText("Day 1에 추가")).toBeInTheDocument();
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    const selectButton = screen.getByRole("button", { name: "선택" });
    fireEvent.click(selectButton);

    // Day 1에 장소가 추가되었는지 확인
    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    // 이제 저장이 활성화됨
    const saveButton = screen.getByRole("button", { name: "코스 저장하기" });
    expect(saveButton).not.toBeDisabled();
  });

  it("저장 버튼 클릭 시 createCourse를 호출하고 상세 화면으로 이동한다", async () => {
    (courseService.createCourse as Mock).mockResolvedValue({
      courseId: 123,
      title: "강릉 바다 여행",
      days: [],
    });

    renderPage();

    const titleInput = screen.getByPlaceholderText(/2박 3일 강릉 힐링/i);
    fireEvent.change(titleInput, { target: { value: "강릉 바다 여행" } });

    const addButtons = screen.getAllByRole("button", { name: /장소 추가/i });
    fireEvent.click(addButtons[0]);

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "선택" }));

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    const saveButton = screen.getByRole("button", { name: "코스 저장하기" });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(courseService.createCourse).toHaveBeenCalledWith(
        expect.objectContaining({
          title: "강릉 바다 여행",
          days: expect.arrayContaining([
            expect.objectContaining({
              dayNumber: 1,
              spots: [expect.objectContaining({ spotId: 101 })],
            }),
          ]),
        })
      );
      expect(mockNavigate).toHaveBeenCalledWith("/courses/123", { replace: true });
    });
  });

  it("여행 기간 버튼을 누르면 캘린더가 열리고 시작일/종료일을 선택해 적용하면 Day 카드 수가 바뀐다", async () => {
    renderPage();

    const rangeButton = screen.getByRole("button", { name: /~/ });
    fireEvent.click(rangeButton);

    expect(screen.getByText("여행 기간 선택")).toBeInTheDocument();

    // 현재 보여지는 달의 10일/11일을 골라 1박 2일로 만든다 (오늘 날짜에 의존하지 않게 고정 날짜 사용)
    const now = new Date();
    const label = (day: number) => `${now.getFullYear()}년 ${now.getMonth() + 1}월 ${day}일`;

    fireEvent.click(screen.getByRole("button", { name: label(10) }));

    // 시작일만 고르면 아직 적용 버튼이 비활성화 상태
    expect(screen.getByRole("button", { name: "적용" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: label(11) }));

    const applyButton = screen.getByRole("button", { name: "적용" });
    expect(applyButton).not.toBeDisabled();
    fireEvent.click(applyButton);

    // 모달이 닫히고 2일 일정(1박 2일)으로 Day 카드가 바뀐다
    await waitFor(() => {
      expect(screen.queryByText("여행 기간 선택")).not.toBeInTheDocument();
    });
    expect(screen.getByTestId("day-card-1")).toBeInTheDocument();
    expect(screen.getByTestId("day-card-2")).toBeInTheDocument();
    expect(screen.queryByTestId("day-card-3")).not.toBeInTheDocument();
  });

  it("취소 버튼을 누르면 기간 변경 없이 캘린더가 닫힌다", async () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /~/ }));
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByText("여행 기간 선택")).toBeInTheDocument();

    fireEvent.click(within(dialog).getByRole("button", { name: "취소" }));

    await waitFor(() => {
      expect(screen.queryByText("여행 기간 선택")).not.toBeInTheDocument();
    });
    expect(screen.getByTestId("day-card-3")).toBeInTheDocument();
  });

  it("저장 실패 시 에러 메시지를 표시하고 입력 상태를 유지한다", async () => {
    (courseService.createCourse as Mock).mockRejectedValue(new Error("저장 실패 서버 에러"));

    renderPage();

    const titleInput = screen.getByPlaceholderText(/2박 3일 강릉 힐링/i);
    fireEvent.change(titleInput, { target: { value: "강릉 바다 여행" } });

    const addButtons = screen.getAllByRole("button", { name: /장소 추가/i });
    fireEvent.click(addButtons[0]);

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "선택" }));

    await waitFor(() => {
      expect(screen.getByText("경포해변")).toBeInTheDocument();
    });

    const saveButton = screen.getByRole("button", { name: "코스 저장하기" });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText("저장 실패 서버 에러")).toBeInTheDocument();
      expect(screen.getByDisplayValue("강릉 바다 여행")).toBeInTheDocument();
    });
  });
});
