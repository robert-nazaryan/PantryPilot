interface ApiClientHandlers {
  getAccessToken: () => string | null;
  refreshTokens: () => Promise<string | null>;
  onAuthFailure: () => void;
}

const noHandlers: ApiClientHandlers = {
  getAccessToken: () => null,
  refreshTokens: async () => null,
  onAuthFailure: () => {},
};

let handlers: ApiClientHandlers = noHandlers;

export function configureApiClient(next: ApiClientHandlers): void {
  handlers = next;
}

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  authenticated?: boolean;
  signal?: AbortSignal;
}

export async function apiFetch<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, authenticated = true, signal } = opts;

  const performRequest = async (accessToken: string | null): Promise<Response> => {
    const headers: Record<string, string> = {};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (authenticated && accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
    return fetch(`${API_BASE_URL}${path}`, {
      method,
      credentials: "include",
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  };

  let response = await performRequest(authenticated ? handlers.getAccessToken() : null);

  if (response.status === 401 && authenticated) {
    const refreshed = await handlers.refreshTokens();
    if (refreshed) {
      response = await performRequest(refreshed);
    }
    if (response.status === 401) {
      handlers.onAuthFailure();
      throw await toApiError(response);
    }
  }

  if (!response.ok) {
    throw await toApiError(response);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as { error?: string; message?: string };
    return new ApiError(
      response.status,
      body.error ?? "unknown_error",
      body.message ?? response.statusText,
    );
  } catch {
    return new ApiError(response.status, "unknown_error", response.statusText);
  }
}
