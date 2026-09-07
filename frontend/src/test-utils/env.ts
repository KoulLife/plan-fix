const mutableEnv = import.meta.env as unknown as Record<string, string | undefined>;

export function setApiBaseUrl(value: string | undefined) {
  if (value === undefined) {
    delete mutableEnv.VITE_API_BASE_URL;
  } else {
    mutableEnv.VITE_API_BASE_URL = value;
  }
}
