import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import CourseDetailPage from "./course-detail-page";
import * as courseService from "@/services/course";

jest.mock("@/services/course");

const mockCourse: courseService.CourseResponse = {
  courseId: 10,
  userId: 1,
  title: "강릉 바다 여행",
  description: "2박 3일 힐링 코스",
  thumbnail: null,
  visibility: "PUBLIC",
  status: "ACTIVE",
  viewCount: 15,
  likeCount: 5,
  startDate: "2026-09-12",
  endDate: "2026-09-13",
  days: [
    {
      dayNumber: 1,
      spots: [
        {
          spotId: 101,
          sequence: 0,
          memo: "오전 10시 도착",
          title: "경포해변",
          category: "관광지",
          region: "51",
          sigungu: "150",
          address: "강원특별자치도 강릉시 안현동",
          thumbnail: null,
          latitude: null,
          longitude: null,
        },
      ],
    },
    {
      dayNumber: 2,
      spots: [],
    },
  ],
  createdAt: "2026-09-02T10:00:00Z",
  updatedAt: "2026-09-02T10:00:00Z",
};

describe("CourseDetailPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = (courseId = "10") => {
    return render(
      <MemoryRouter initialEntries={[`/courses/${courseId}`]}>
        <Routes>
          <Route path="/courses/:courseId" element={<CourseDetailPage />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it("코스 정보를 성공적으로 로드하여 Day별 장소를 렌더링한다", async () => {
    (courseService.fetchCourse as jest.Mock).mockResolvedValue(mockCourse);

    renderComponent();

    expect(screen.getByText(/여행 코스를 불러오는 중입니다/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("강릉 바다 여행")).toBeInTheDocument();
      expect(screen.getByText("2박 3일 힐링 코스")).toBeInTheDocument();
      expect(screen.getByText("경포해변")).toBeInTheDocument();
      expect(screen.getByText("💬 오전 10시 도착")).toBeInTheDocument();
      expect(screen.getByText("아직 계획이 없어요.")).toBeInTheDocument();
    });
  });

  it("존재하지 않는 코스(null)일 경우 안내 문구를 표시한다", async () => {
    (courseService.fetchCourse as jest.Mock).mockResolvedValue(null);

    renderComponent("999");

    await waitFor(() => {
      expect(screen.getByText("존재하지 않거나 삭제된 코스입니다.")).toBeInTheDocument();
    });
  });
});
