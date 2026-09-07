import { setApiBaseUrl } from "@/test-utils/env";

describe("signUp", () => {
  const originalApiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    vi.resetModules();
  });

  test("VITE_API_BASE_URL이 설정되지 않았으면 에러를 던진다", async () => {
    setApiBaseUrl(undefined);
    vi.resetModules();
    const { signUp } = (await import("./user")) as typeof import("./user");

    await expect(
      signUp({
        loginId: "testuser1",
        password: "Password1!",
        name: "홍길동",
        email: "hong@planfix.kr",
      }),
    ).rejects.toThrow("VITE_API_BASE_URL이 설정되지 않았습니다.");
  });

  test("POST /users를 호출하고 회원가입 결과를 반환한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const signUpResponse = {
      userId: 1,
      username: "testuser1",
      name: "홍길동",
      email: "hong@planfix.kr",
      role: "USER",
      status: "ACTIVE",
      createdAt: "2026-09-01T00:00:00Z",
      updatedAt: "2026-09-01T00:00:00Z",
    };

    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => signUpResponse,
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { signUp } = (await import("./user")) as typeof import("./user");

    const payload = {
      loginId: "testuser1",
      password: "Password1!",
      name: "홍길동",
      email: "hong@planfix.kr",
    };
    const result = await signUp(payload);

    expect(result).toEqual(signUpResponse);
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/users", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify(payload),
    });
  });

  test("회원가입 실패 시 서버가 반환한 message를 에러 메시지로 사용한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({
        code: "CONFLICT",
        message: "loginId already exists. loginId=testuser1",
      }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    vi.resetModules();
    const { signUp } = (await import("./user")) as typeof import("./user");

    await expect(
      signUp({
        loginId: "testuser1",
        password: "Password1!",
      }),
    ).rejects.toThrow("loginId already exists. loginId=testuser1");
  });
});
