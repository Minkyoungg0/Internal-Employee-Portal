import { FormEvent, ReactNode, useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';

type Role = 'EMPLOYEE' | 'ADMIN';

type CurrentUser = {
  accountId: number;
  employeeId: number;
  employeeNumber: string;
  username: string;
  role: Role;
};

type CsrfToken = {
  headerName: string;
  parameterName: string;
  token: string;
};

type ApiError = {
  code?: string;
  message?: string;
};

async function readError(response: Response, fallback: string) {
  const body = await response.json().catch(() => ({})) as ApiError;
  return body.message ?? fallback;
}

async function fetchCsrf() {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  if (!response.ok) throw new Error('보안 토큰을 발급하지 못했습니다.');
  return response.json() as Promise<CsrfToken>;
}

function LoginPage({
  currentUser,
  csrf,
  onCsrfChange,
  onLogin,
}: {
  currentUser: CurrentUser | null;
  csrf: CsrfToken | null;
  onCsrfChange: (token: CsrfToken | null) => void;
  onLogin: (user: CurrentUser) => void;
}) {
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  if (currentUser) {
    return <Navigate to={currentUser.role === 'ADMIN' ? '/admin/employees' : '/me'} replace />;
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError('');

    try {
      const form = new FormData(event.currentTarget);
      const token = csrf ?? await fetchCsrf();
      onCsrfChange(token);
      const body = new URLSearchParams();
      body.set('username', String(form.get('username') ?? ''));
      body.set('password', String(form.get('password') ?? ''));

      const response = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
          [token.headerName]: token.token,
        },
        body,
      });
      if (!response.ok) throw new Error(await readError(response, '로그인하지 못했습니다.'));

      const user = await response.json() as CurrentUser;
      onLogin(user);
      try {
        onCsrfChange(await fetchCsrf());
      } catch {
        onCsrfChange(null);
      }
      navigate(user.role === 'ADMIN' ? '/admin/employees' : '/me', { replace: true });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '로그인하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="centered">
      <form className="card login" onSubmit={submit}>
        <p className="eyebrow">BIT COMPUTER</p>
        <h1>직원 포털</h1>
        <label>아이디<input name="username" autoComplete="username" required /></label>
        <label>비밀번호<input name="password" type="password" autoComplete="current-password" required /></label>
        {error && <p className="error" role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>{submitting ? '로그인 중…' : '로그인'}</button>
        <p className="muted">권한에 따라 직원 포털 또는 관리자 화면으로 이동합니다.</p>
      </form>
    </main>
  );
}

function Shell({
  title,
  user,
  onLogout,
  children,
}: {
  title: string;
  user: CurrentUser;
  onLogout: () => Promise<void>;
  children: ReactNode;
}) {
  return (
    <div className="app-shell">
      <aside>
        <strong>Employee Portal</strong>
        <p className="account">{user.employeeNumber} · {user.username}</p>
        <nav>
          <Link to="/me">내 정보</Link>
          {user.role === 'ADMIN' && <Link to="/admin/employees">직원 관리</Link>}
        </nav>
        <button className="logout" type="button" onClick={() => void onLogout()}>로그아웃</button>
      </aside>
      <main className="content"><h1>{title}</h1>{children}</main>
    </div>
  );
}

function ProfilePage({ user, onLogout }: { user: CurrentUser; onLogout: () => Promise<void> }) {
  return (
    <Shell title="내 정보" user={user} onLogout={onLogout}>
      <section className="card">
        <p><strong>사번</strong> {user.employeeNumber}</p>
        <p className="muted">다음 단계에서 인적사항 조회·수정 기능을 연결합니다.</p>
      </section>
    </Shell>
  );
}

function EmployeesPage({ user, onLogout }: { user: CurrentUser; onLogout: () => Promise<void> }) {
  return (
    <Shell title="직원 관리" user={user} onLogout={onLogout}>
      <section className="card"><p>직원 목록·상세·퇴사 처리·배경 조회 기능이 들어갈 자리입니다.</p></section>
    </Shell>
  );
}

function ProtectedRoute({ user, role, children }: { user: CurrentUser | null; role?: Role; children: ReactNode }) {
  const location = useLocation();
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (role && user.role !== role) return <Navigate to="/me" replace />;
  return children;
}

function SystemStatus() {
  const [status, setStatus] = useState('확인 중');
  useEffect(() => {
    fetch('/api/health')
      .then((response) => response.ok ? response.json() : Promise.reject())
      .then(() => setStatus('백엔드 연결됨'))
      .catch(() => setStatus('백엔드 연결 실패'));
  }, []);
  return <span className="status">{status}</span>;
}

export default function App() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [csrf, setCsrf] = useState<CsrfToken | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function initialize() {
      try {
        const token = await fetchCsrf();
        setCsrf(token);
        const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
        if (response.ok) setCurrentUser(await response.json() as CurrentUser);
      } catch {
        setCsrf(null);
      } finally {
        setLoading(false);
      }
    }
    void initialize();
  }, []);

  async function logout() {
    try {
      const token = csrf ?? await fetchCsrf();
      const response = await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { [token.headerName]: token.token },
      });
      if (response.ok || response.status === 401 || response.status === 403) {
        setCurrentUser(null);
        setCsrf(await fetchCsrf());
      }
    } catch {
      // 네트워크 실패 시 서버 세션 상태를 알 수 없으므로 현재 화면을 유지한다.
    }
  }

  if (loading) return <main className="centered"><p>로그인 상태를 확인하고 있습니다…</p></main>;

  return (
    <>
      <SystemStatus />
      <Routes>
        <Route path="/login" element={
          <LoginPage currentUser={currentUser} csrf={csrf} onCsrfChange={setCsrf} onLogin={setCurrentUser} />
        } />
        <Route path="/me" element={
          <ProtectedRoute user={currentUser}>
            <ProfilePage user={currentUser!} onLogout={logout} />
          </ProtectedRoute>
        } />
        <Route path="/admin/employees" element={
          <ProtectedRoute user={currentUser} role="ADMIN">
            <EmployeesPage user={currentUser!} onLogout={logout} />
          </ProtectedRoute>
        } />
        <Route path="*" element={<Navigate to={currentUser ? '/me' : '/login'} replace />} />
      </Routes>
    </>
  );
}
