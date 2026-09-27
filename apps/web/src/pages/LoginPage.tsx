import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { fetchCsrf, readError } from '../api';
import type { CsrfToken, CurrentUser } from '../types';

export function LoginPage({
  user,
  csrf,
  onLogin,
  onCsrfChange,
}: {
  user: CurrentUser | null;
  csrf: CsrfToken | null;
  onLogin: (u: CurrentUser) => void;
  onCsrfChange: (token: CsrfToken) => void;
}) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  if (user) {
    return (
      <Navigate
        to={user.passwordChangeRequired ? '/change-password' : user.role === 'ADMIN' ? '/admin/employees' : '/me'}
        replace
      />
    );
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const f = new FormData(e.currentTarget);
      const token = csrf ?? (await fetchCsrf());
      const body = new URLSearchParams({ username: String(f.get('username')), password: String(f.get('password')) });
      const r = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', [token.headerName]: token.token },
        body,
      });
      if (!r.ok) throw new Error((await readError(r, '로그인하지 못했습니다.')).message);
      const u = (await r.json()) as CurrentUser;
      onCsrfChange(await fetchCsrf());
      onLogin(u);
      navigate(u.passwordChangeRequired ? '/change-password' : u.role === 'ADMIN' ? '/admin/employees' : '/me', {
        replace: true,
      });
    } catch (x) {
      setError(x instanceof Error ? x.message : '로그인하지 못했습니다.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="centered">
      <form className="card form" onSubmit={submit}>
        <p className="eyebrow">BIT COMPUTER</p>
        <h1>직원 포털</h1>
        <label>
          아이디
          <input name="username" autoComplete="username" required />
        </label>
        <label>
          비밀번호
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        {error && <p className="error">{error}</p>}
        <button disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button>
      </form>
    </main>
  );
}
