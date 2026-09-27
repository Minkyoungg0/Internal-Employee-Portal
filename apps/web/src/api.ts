import type { ApiError, CsrfToken } from './types';

export async function readError(response: Response, fallback: string) {
  const body = (await response.json().catch(() => ({}))) as ApiError;
  return { code: body.code, message: body.message ?? fallback };
}

let sessionExpiryHandled = false;

export class RequestError extends Error {
  constructor(public code: string | undefined, message: string, public status: number) {
    super(message);
  }
}

export async function fetchCsrf(): Promise<CsrfToken> {
  const r = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  if (!r.ok) throw new Error('보안 토큰을 발급하지 못했습니다.');
  return r.json();
}

export async function api<T>(url: string, options: RequestInit = {}, csrf?: CsrfToken | null): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type', 'application/json');
  if (options.method && options.method !== 'GET') {
    const token = csrf ?? (await fetchCsrf());
    headers.set(token.headerName, token.token);
  }
  const response = await fetch(url, { ...options, headers, credentials: 'same-origin' });
  if (!response.ok) {
    const error = await readError(response, '요청을 처리하지 못했습니다.');
    if (response.status === 401 && url !== '/api/auth/login' && !sessionExpiryHandled) {
      sessionExpiryHandled = true;
      window.alert(error.code === 'SESSION_EXPIRED' ? '세션이 만료되었습니다. 다시 로그인해 주세요.' : '로그인이 필요합니다.');
      window.location.assign('/login');
    }
    throw new RequestError(error.code, error.message, response.status);
  }
  return response.status === 204 ? (undefined as T) : (response.json() as Promise<T>);
}
