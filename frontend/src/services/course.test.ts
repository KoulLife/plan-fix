import { createCourse, fetchCourse, fetchMyCourses } from "./course";
import { UnauthorizedError } from "./spots";
import { setApiBaseUrl } from "@/test-utils/env";

describe("course service", () => {
  const originalEnv = process.env.REACT_APP_API_BASE_URL;

  beforeEach(() => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    jest.resetAllMocks();
  });

  afterAll(() => {
    setApiBaseUrl(originalEnv);
  });

  describe("createCourse", () => {
    it("credentials: include와 payload를 전송하여 성공 응답을 반환한다", async () => {
      const mockCourse = {
        courseId: 1,
        title: "강릉 여행",
        days: [{ dayNumber: 1, spots: [] }],
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockCourse,
      });

      const payload = {
        title: "강릉 여행",
        startDate: "2026-09-12",
        endDate: "2026-09-12",
        days: [{ dayNumber: 1, spots: [{ spotId: 10 }] }],
      };

      const result = await createCourse(payload);

      expect(global.fetch).toHaveBeenCalledWith("http://localhost:8080/api/v1/courses", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });
      expect(result).toEqual(mockCourse);
    });

    it("401/403 응답 시 UnauthorizedError를 던진다", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 401,
        ok: false,
      });

      await expect(
        createCourse({ title: "Test", days: [{ dayNumber: 1, spots: [] }] })
      ).rejects.toThrow(UnauthorizedError);
    });
  });

  describe("fetchMyCourses", () => {
    it("내 코스 목록을 가져온다", async () => {
      const mockList = [{ courseId: 1, title: "코스 1" }];
      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockList,
      });

      const result = await fetchMyCourses();
      expect(global.fetch).toHaveBeenCalledWith("http://localhost:8080/api/v1/courses", {
        credentials: "include",
      });
      expect(result).toEqual(mockList);
    });

    it("401/403 응답 시 UnauthorizedError를 던진다", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 403,
        ok: false,
      });

      await expect(fetchMyCourses()).rejects.toThrow(UnauthorizedError);
    });
  });

  describe("fetchCourse", () => {
    it("404 응답 시 null을 반환한다", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 404,
        ok: false,
      });

      const result = await fetchCourse(999);
      expect(result).toBeNull();
    });

    it("정상 응답 시 코스 정보를 반환한다", async () => {
      const mockCourse = { courseId: 1, title: "코스 1" };
      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockCourse,
      });

      const result = await fetchCourse(1);
      expect(result).toEqual(mockCourse);
    });
  });
});
