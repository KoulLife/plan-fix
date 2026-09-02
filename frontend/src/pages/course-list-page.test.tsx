import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import CourseListPage from "./course-list-page";
import * as courseService from "@/services/course";

jest.mock("@/services/course");

const mockCourses: courseService.CourseResponse[] = [
  {
    courseId: 1,
    userId: 1,
    title: "속초 1박 2일 맛집 코스",
    description: "속초 중앙시장과 아바이마을",
    thumbnail: null,
    visibility: "PUBLIC",
    status: "ACTIVE",
    viewCount: 20,
    likeCount: 7,
    startDate: "2026-09-15",
    endDate: "2026-09-16",
    days: [
      { dayNumber: 1, spots: [] },
      { dayNumber: 2, spots: [] },
    ],
    createdAt: "2026-09-02T10:00:00Z",
    updatedAt: "2026-09-02T10:00:00Z",
  },
];

describe("CourseListPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter>
        <CourseListPage />
      </MemoryRouter>
    );
  };

  it("코스 목록이 비어 있으면 첫 여행 코스 만들기 CTA를 렌더링한다", async () => {
    (courseService.fetchMyCourses as jest.Mock).mockResolvedValue([]);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("생성한 여행 코스가 없습니다.")).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /첫 여행 코스 만들기/i })).toBeInTheDocument();
    });
  });

  it("코스 목록이 있으면 코스 카드를 렌더링한다", async () => {
    (courseService.fetchMyCourses as jest.Mock).mockResolvedValue(mockCourses);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("속초 1박 2일 맛집 코스")).toBeInTheDocument();
      expect(screen.getByText("속초 중앙시장과 아바이마을")).toBeInTheDocument();
      expect(screen.getByText("2일 일정")).toBeInTheDocument();
    });
  });
});
