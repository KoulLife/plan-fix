import { UnauthorizedError } from "./spots";

export type CourseSpotSummary = {
  spotId: number;
  sequence: number;
  memo: string | null;
  title: string;
  category: string;
  region: string | null;
  sigungu: string | null;
  address: string | null;
  thumbnail: string | null;
  latitude: number | null;
  longitude: number | null;
};

export type CourseDay = {
  dayNumber: number;
  spots: CourseSpotSummary[];
};

export type CourseResponse = {
  courseId: number;
  userId: number;
  title: string;
  description: string | null;
  thumbnail: string | null;
  visibility: "PUBLIC" | "PRIVATE";
  status: "ACTIVE" | "DELETED";
  viewCount: number;
  likeCount: number;
  startDate: string | null;
  endDate: string | null;
  days: CourseDay[];
  createdAt: string;
  updatedAt: string;
};

export type CreateCourseDayInput = {
  dayNumber: number;
  spots: { spotId: number; memo?: string | null }[];
};

export type CreateCoursePayload = {
  title: string;
  description?: string | null;
  thumbnail?: string | null;
  visibility?: "PUBLIC" | "PRIVATE";
  startDate?: string | null;
  endDate?: string | null;
  days: CreateCourseDayInput[];
};

function getApiBaseUrl(): string | undefined {
  return import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");
}

/** 코스 생성 API 호출 */
export async function createCourse(payload: CreateCoursePayload): Promise<CourseResponse> {
  const apiBaseUrl = getApiBaseUrl();
  if (!apiBaseUrl) {
    throw new UnauthorizedError();
  }

  const response = await fetch(`${apiBaseUrl}/courses`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(payload),
  });

  if (response.status === 401 || response.status === 403) {
    throw new UnauthorizedError();
  }
  if (!response.ok) {
    const errorBody = await response.text().catch(() => "");
    throw new Error(errorBody || "코스 생성에 실패했습니다.");
  }

  return (await response.json()) as CourseResponse;
}

/** 내 코스 목록 조회 API 호출 */
export async function fetchMyCourses(): Promise<CourseResponse[]> {
  const apiBaseUrl = getApiBaseUrl();
  if (!apiBaseUrl) {
    return [];
  }

  const response = await fetch(`${apiBaseUrl}/courses`, {
    credentials: "include",
  });

  if (response.status === 401 || response.status === 403) {
    throw new UnauthorizedError();
  }
  if (!response.ok) {
    throw new Error("코스 목록을 불러오지 못했습니다.");
  }

  return (await response.json()) as CourseResponse[];
}

/** 코스 단건 조회 API 호출 */
export async function fetchCourse(courseId: number | string): Promise<CourseResponse | null> {
  const apiBaseUrl = getApiBaseUrl();
  if (!apiBaseUrl) {
    return null;
  }

  const response = await fetch(`${apiBaseUrl}/courses/${courseId}`, {
    credentials: "include",
  });

  if (response.status === 404) {
    return null;
  }
  if (response.status === 401 || response.status === 403) {
    throw new UnauthorizedError();
  }
  if (!response.ok) {
    throw new Error("코스 정보를 불러오지 못했습니다.");
  }

  return (await response.json()) as CourseResponse;
}
