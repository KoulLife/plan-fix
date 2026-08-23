// FIXME: 백엔드 POST /api/v1/auth/login은 { loginId, password }를 받는다.
// 로그인·회원가입 폼이 email 기반이라 실제 API로는 자체 로그인이 성립하지 않는다.
// 이메일 로그인으로 갈지 아이디 로그인으로 갈지 정한 뒤 폼과 함께 맞춰야 한다.
export type LoginRequest = {
  email: string;
  password: string;
  rememberMe: boolean;
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
    body: JSON.stringify(payload),
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
