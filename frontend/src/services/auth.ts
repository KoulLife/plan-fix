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

export function startGoogleSignIn() {
  if (!apiBaseUrl) {
    throw new Error("REACT_APP_API_BASE_URL is not configured.");
  }

  window.location.assign(`${apiBaseUrl}/auth/google`);
}

export async function signIn(payload: LoginRequest): Promise<LoginResponse> {
  if (!apiBaseUrl) {
    throw new Error("REACT_APP_API_BASE_URL is not configured.");
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
    throw new Error(body?.message ?? "Unable to sign in. Please check your credentials.");
  }

  return (await response.json()) as LoginResponse;
}
