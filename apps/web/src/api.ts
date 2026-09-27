import type { ApiError, CsrfToken } from './types';

export async function readError(response: Response, fallback: string) {
  const body = (await response.json().catch(() => ({}))) as ApiError;
  return body.message ?? fallback;
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
  if (!response.ok) throw new Error(await readError(response, '요청을 처리하지 못했습니다.'));
  return response.status === 204 ? (undefined as T) : (response.json() as Promise<T>);
}
