import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Protected } from './components/Protected';
import { LoginPage } from './pages/LoginPage';
import { ChangePasswordPage } from './pages/ChangePasswordPage';
import { ProfilePage } from './pages/ProfilePage';
import { EmployeesPage } from './pages/EmployeesPage';
import { api, fetchCsrf } from './api';
import type { CsrfToken, CurrentUser } from './types';

export default function App() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [csrf, setCsrf] = useState<CsrfToken | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCsrf()
      .then(t => {
        setCsrf(t);
        return fetch('/api/auth/me', { credentials: 'same-origin' });
      })
      .then(async r => {
        if (r.ok) setUser(await r.json());
      })
      .finally(() => setLoading(false));
  }, []);

  async function logout() {
    try {
      await api('/api/auth/logout', { method: 'POST' }, csrf);
    } finally {
      setUser(null);
      setCsrf(await fetchCsrf());
    }
  }

  if (loading) return <main className="centered">로그인 상태를 확인하고 있습니다…</main>;

  return (
    <Routes>
      <Route path="/login" element={<LoginPage user={user} csrf={csrf} onLogin={setUser} onCsrfChange={setCsrf} />} />
      <Route
        path="/change-password"
        element={
          user ? (
            <ChangePasswordPage user={user} csrf={csrf} onChanged={() => setUser({ ...user, passwordChangeRequired: false })} />
          ) : (
            <Navigate to="/login" />
          )
        }
      />
      <Route
        path="/me"
        element={
          <Protected user={user}>
            <ProfilePage user={user!} onLogout={logout} />
          </Protected>
        }
      />
      <Route
        path="/admin/employees"
        element={
          <Protected user={user} role="ADMIN">
            <EmployeesPage user={user!} csrf={csrf} onLogout={logout} />
          </Protected>
        }
      />
      <Route
        path="*"
        element={
          <Navigate
            to={user ? (user.passwordChangeRequired ? '/change-password' : user.role === 'ADMIN' ? '/admin/employees' : '/me') : '/login'}
            replace
          />
        }
      />
    </Routes>
  );
}
