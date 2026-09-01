const mutableEnv = process.env as unknown as Record<string, string | undefined>;

export function setApiBaseUrl(value: string | undefined) {
  if (value === undefined) {
    delete mutableEnv.REACT_APP_API_BASE_URL;
  } else {
    mutableEnv.REACT_APP_API_BASE_URL = value;
  }
}
