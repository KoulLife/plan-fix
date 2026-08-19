export type LoginRequest = {
  email: string;
  password: string;
  rememberMe: boolean;
};

export type LoginResponse = {
  user: {
    id: string;
    email: string;
    name?: string;
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
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null;
    throw new Error(body?.message ?? "로그인할 수 없습니다. 이메일과 비밀번호를 확인해 주세요.");
  }

  return (await response.json()) as LoginResponse;
}
