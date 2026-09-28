import { PasswordInput } from '../components/PasswordInput';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { Shell } from '../components/Shell';
import type { CsrfToken, CurrentUser } from '../types';

export function ChangePasswordPage({
  user,
  csrf,
  onChanged,
  onLogout,
}: {
  user: CurrentUser;
  csrf: CsrfToken | null;
  onChanged: () => void;
  onLogout: () => Promise<void>;
}) {
  const [error, setError] = useState('');
  const navigate = useNavigate();

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    const f = new FormData(e.currentTarget);
    const next = String(f.get('newPassword'));
    if (next !== String(f.get('confirmPassword'))) {
      setError('새 비밀번호 확인이 일치하지 않습니다.');
      return;
    }
    try {
      await api(
        '/api/me/password',
        { method: 'PUT', body: JSON.stringify({ currentPassword: f.get('currentPassword'), newPassword: next }) },
        csrf,
      );
      onChanged();
      navigate(user.role === 'ADMIN' ? '/admin/employees' : '/me', { replace: true });
    } catch (x) {
      setError(x instanceof Error ? x.message : '비밀번호를 변경하지 못했습니다.');
    }
  }

  return (
    <Shell user={user} onLogout={onLogout}>
      <div className="password-page">
        <div className="password-card-wrap">
          <h1>{user.passwordChangeRequired ? '초기 비밀번호 변경' : '비밀번호 변경'}</h1>
          <form className="card form" onSubmit={submit}>
          <p className="eyebrow">PASSWORD</p>
          {user.passwordChangeRequired && (
            <p className="muted password-notice">첫 로그인입니다. 보안을 위해 초기 비밀번호를 변경해 주세요.</p>
          )}
          <label>
            현재 비밀번호
            <PasswordInput name="currentPassword"  required />
          </label>
          <label>
            새 비밀번호
            <PasswordInput name="newPassword"  minLength={8} required />
          </label>
          <label>
            새 비밀번호 확인
            <PasswordInput name="confirmPassword"  minLength={8} required />
          </label>
          {error && <p className="error">{error}</p>}
            <button>변경하기</button>
          </form>
        </div>
      </div>
    </Shell>
  );
}
