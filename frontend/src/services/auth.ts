export type LoginRequest = {
  loginId: string;
  password: string;
};

export type LoginResponse = {
  user: {
    id: number;
    username: string;
    email: string | null;
  };
};

const apiBaseUrl = process.env.REACT_APP_API_BASE_URL?.replace(/\/$/, "");

export function isAuthApiConfigured() {
  return Boolean(apiBaseUrl);
}

export function startKakaoSignIn() {
  if (!apiBaseUrl) {
    throw new Error("REACT_APP_API_BASE_URL이 설정되지 않았습니다.");
  }

  window.location.assign(`${apiBaseUrl}/auth/kakao`);
}

export async function signIn(payload: LoginRequest): Promise<LoginResponse> {
  if (!apiBaseUrl) {
    throw new Error("REACT_APP_API_BASE_URL이 설정되지 않았습니다.");
  }

  const response = await fetch(`${apiBaseUrl}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      loginId: payload.loginId,
      password: payload.password,
    }),
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null;
    throw new Error(body?.message ?? "로그인할 수 없습니다. 아이디와 비밀번호를 확인해 주세요.");
  }

  return (await response.json()) as LoginResponse;
}

export async function signOut(): Promise<void> {
  if (!apiBaseUrl) {
    return;
  }

  await fetch(`${apiBaseUrl}/auth/logout`, { method: "POST", credentials: "include" });
}

