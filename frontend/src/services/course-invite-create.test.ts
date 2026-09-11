import { createCourseInvite } from "./course";
import { UnauthorizedError } from "./spots";
import { setApiBaseUrl } from "@/test-utils/env";

const fetchMock = vi.fn();
beforeEach(() => {
  setApiBaseUrl("http://localhost:8080/api/v1");
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});
afterEach(() => {
  setApiBaseUrl(undefined);
  vi.unstubAllGlobals();
});

test("권한과 인증 쿠키를 보내 생성된 초대 링크를 반환한다", async () => {
  const invite = { token: "test-token", inviteUrl: "http://localhost:3000/course-invites/test-token", memberRole: "EDITOR", expiresAt: "2026-09-18T10:00:00Z" };
  fetchMock.mockResolvedValue(new Response(JSON.stringify(invite), { status: 200 }));
  await expect(createCourseInvite(12, "EDITOR")).resolves.toEqual(invite);
  expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/courses/12/invites", {
    method: "POST", credentials: "include", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ memberRole: "EDITOR" }),
  });
});

test("401만 로그인 필요 오류로 처리한다", async () => {
  fetchMock.mockResolvedValue(new Response(null, { status: 401 }));
  await expect(createCourseInvite(12, "VIEWER")).rejects.toBeInstanceOf(UnauthorizedError);
});

test("403은 다시 로그인시키지 않고 소유자 권한을 안내한다", async () => {
  fetchMock.mockResolvedValue(new Response(null, { status: 403 }));
  const error = await createCourseInvite(12, "VIEWER").catch((value) => value);
  expect(error).not.toBeInstanceOf(UnauthorizedError);
  expect(error.message).toBe("코스 소유자만 친구를 초대할 수 있습니다.");
});

test("404는 초대를 찾을 수 없다는 안내로 처리한다", async () => {
  fetchMock.mockResolvedValue(new Response(null, { status: 404 }));
  await expect(createCourseInvite(12, "VIEWER")).rejects.toThrow("코스 또는 초대 기능을 찾을 수 없습니다.");
});

test("400은 백엔드의 입력 오류 메시지를 보존한다", async () => {
  fetchMock.mockResolvedValue(new Response(JSON.stringify({ message: "초대 권한은 VIEWER 또는 EDITOR만 가능합니다." }), { status: 400 }));
  await expect(createCourseInvite(12, "VIEWER")).rejects.toThrow("초대 권한은 VIEWER 또는 EDITOR만 가능합니다.");
});

test("서버 오류의 HTML이나 내부 상세를 사용자에게 노출하지 않는다", async () => {
  fetchMock.mockResolvedValue(new Response("<html>internal SQL details</html>", { status: 500 }));
  await expect(createCourseInvite(12, "VIEWER")).rejects.toThrow("초대 링크 생성 중 오류가 발생했습니다.");
});

test("연결 실패를 초대 생성 실패 안내로 처리한다", async () => {
  fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));
  await expect(createCourseInvite(12, "VIEWER")).rejects.toThrow("서버에 연결하지 못했습니다.");
});
