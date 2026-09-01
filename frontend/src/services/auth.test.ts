import { setApiBaseUrl } from "@/test-utils/env";

describe("signIn", () => {
  const originalApiBaseUrl = process.env.REACT_APP_API_BASE_URL;
  const originalFetch = global.fetch;

  afterEach(() => {
    setApiBaseUrl(originalApiBaseUrl);
    global.fetch = originalFetch;
    jest.resetModules();
  });

  test("REACT_APP_API_BASE_URL이 설정되지 않았으면 에러를 던진다", async () => {
    setApiBaseUrl(undefined);
    jest.resetModules();
    const { signIn } = require("./auth") as typeof import("./auth");

    await expect(signIn({ loginId: "testuser1", password: "Password1!" })).rejects.toThrow(
      "REACT_APP_API_BASE_URL이 설정되지 않았습니다.",
    );
  });

  test("loginId와 password만 body에 담아 POST /auth/login을 호출한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const userResponse = {
      user: {
        id: 1,
        username: "testuser1",
        email: "test@planfix.kr",
      },
    };

    const fetchSpy = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => userResponse,
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { signIn } = require("./auth") as typeof import("./auth");

    const result = await signIn({ loginId: "testuser1", password: "Password1!" });

    expect(result).toEqual(userResponse);
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({
        loginId: "testuser1",
        password: "Password1!",
      }),
    });
  });

  test("로그인 실패 시 서버가 반환한 message를 에러 메시지로 사용한다", async () => {
    setApiBaseUrl("http://localhost:8080/api/v1");
    const fetchSpy = jest.fn().mockResolvedValue({
      ok: false,
      json: async () => ({
        code: "INVALID_CREDENTIALS",
        message: "아이디 또는 비밀번호가 일치하지 않습니다.",
      }),
    });
    global.fetch = fetchSpy as unknown as typeof fetch;
    jest.resetModules();
    const { signIn } = require("./auth") as typeof import("./auth");

    await expect(signIn({ loginId: "testuser1", password: "WrongPassword1!" })).rejects.toThrow(
      "아이디 또는 비밀번호가 일치하지 않습니다.",
    );
  });
});
