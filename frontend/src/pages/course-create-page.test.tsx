import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import CourseCreatePage from "./course-create-page";
import * as courseService from "@/services/course";
import * as spotService from "@/services/spots";

jest.mock("@/services/course");
jest.mock("@/services/spots");

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

describe("CourseCreatePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    sessionStorage.clear();
    (spotService.searchSpots as jest.Mock).mockResolvedValue({
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
    (courseService.createCourse as jest.Mock).mockResolvedValue({
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

  it("저장 실패 시 에러 메시지를 표시하고 입력 상태를 유지한다", async () => {
    (courseService.createCourse as jest.Mock).mockRejectedValue(new Error("저장 실패 서버 에러"));

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
