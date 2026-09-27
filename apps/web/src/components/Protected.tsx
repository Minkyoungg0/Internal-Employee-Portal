import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import type { CurrentUser, Role } from '../types';

export function Protected({
  user,
  role,
  redirectTo = '/me',
  children,
}: {
  user: CurrentUser | null;
  role?: Role;
  redirectTo?: string;
  children: ReactNode;
}) {
  if (!user) return <Navigate to="/login" replace />;
  if (user.passwordChangeRequired) return <Navigate to="/change-password" replace />;
  if (role && user.role !== role) return <Navigate to={redirectTo} replace />;
  return children;
}
