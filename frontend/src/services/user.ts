export type SignUpRequest = {
  loginId: string;
  password: string;
  name?: string | null;
  email?: string | null;
  username?: string | null;
};

export type SignUpResponse = {
  userId: number;
  username: string;
  name: string | null;
  email: string | null;
  role: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");

export function isUserApiConfigured() {
  return Boolean(apiBaseUrl);
}

export async function signUp(payload: SignUpRequest): Promise<SignUpResponse> {
  if (!apiBaseUrl) {
    throw new Error("VITE_API_BASE_URL이 설정되지 않았습니다.");
  }

  const response = await fetch(`${apiBaseUrl}/users`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null;
    throw new Error(body?.message ?? "회원가입할 수 없습니다. 입력 정보를 확인해 주세요.");
  }

  return (await response.json()) as SignUpResponse;
}
