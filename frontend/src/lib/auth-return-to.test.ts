import {
  authPathWithReturnTo,
  getInviteReturnTo,
  readPendingAuthReturnTo,
  savePendingAuthReturnTo,
} from "./auth-return-to";

beforeEach(() => {
  sessionStorage.clear();
  vi.useFakeTimers();
  vi.setSystemTime(new Date("2026-09-11T00:00:00Z"));
});

afterEach(() => {
  sessionStorage.clear();
  vi.useRealTimers();
  vi.restoreAllMocks();
});

test("인증 복귀 주소는 초대 토큰 경로만 허용한다", () => {
  expect(getInviteReturnTo("/course-invites/Abc_123-xyz")).toBe("/course-invites/Abc_123-xyz");
  expect(authPathWithReturnTo("/login", "/course-invites/Abc_123-xyz"))
    .toBe("/login?returnTo=%2Fcourse-invites%2FAbc_123-xyz");
});

test.each([
  "https://evil.example/course-invites/token",
  "//evil.example/course-invites/token",
  "/courses/123",
  "/course-invites/",
  "/course-invites/token/accept",
  "/course-invites/token?next=https://evil.example",
  "/course-invites/token#section",
  "/course-invites/../main",
  "/course-invites/%2E%2E",
  "/course-invites/token\n",
  "\\course-invites\\token",
])("허용하지 않는 복귀 주소를 제거한다: %s", (path) => {
  expect(getInviteReturnTo(path)).toBeNull();
  expect(authPathWithReturnTo("/signup", path)).toBe("/signup");
  savePendingAuthReturnTo(path);
  expect(readPendingAuthReturnTo()).toBeNull();
});

test("카카오 복귀 경로는 15분 후 만료되어 삭제된다", () => {
  savePendingAuthReturnTo("/course-invites/invite-token");
  expect(readPendingAuthReturnTo()).toBe("/course-invites/invite-token");
  vi.advanceTimersByTime(15 * 60 * 1000);
  expect(readPendingAuthReturnTo()).toBeNull();
  expect(sessionStorage.length).toBe(0);
});

test("과거 로그인 시도는 초대 경로 없는 새 로그인 시도에서 제거된다", () => {
  savePendingAuthReturnTo("/course-invites/old-token");
  savePendingAuthReturnTo(null);
  expect(readPendingAuthReturnTo()).toBeNull();
  expect(sessionStorage.length).toBe(0);
});

test("손상되거나 미래 시각의 세션 정보는 복귀에 사용하지 않는다", () => {
  savePendingAuthReturnTo("/course-invites/invite-token");
  const key = sessionStorage.key(0)!;
  sessionStorage.setItem(key, "invalid json");
  expect(readPendingAuthReturnTo()).toBeNull();
  expect(sessionStorage.length).toBe(0);

  savePendingAuthReturnTo("/course-invites/invite-token");
  vi.setSystemTime(new Date("2026-09-10T23:59:59Z"));
  expect(readPendingAuthReturnTo()).toBeNull();
  expect(sessionStorage.length).toBe(0);
});

test("브라우저에서 세션 저장소를 차단해도 로그인 도우미가 예외를 던지지 않는다", () => {
  vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => { throw new Error("blocked"); });
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => { throw new Error("blocked"); });
  vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => { throw new Error("blocked"); });
  expect(() => savePendingAuthReturnTo("/course-invites/invite-token")).not.toThrow();
  expect(readPendingAuthReturnTo()).toBeNull();
});
