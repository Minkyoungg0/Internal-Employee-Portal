import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { CurrentUser } from '../types';

export function Shell({
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
        <p className="account">
          {user.employeeNumber} · {user.username}
        </p>
        <nav>
          <Link to="/me">내 정보</Link>
          <Link to="/change-password">비밀번호 변경</Link>
          {user.role === 'ADMIN' && <Link to="/admin/employees">직원 관리</Link>}
        </nav>
        <button className="logout" onClick={() => void onLogout()}>
          로그아웃
        </button>
      </aside>
      <main className="content">
        <h1>{title}</h1>
        {children}
      </main>
    </div>
  );
}
