import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import type { CurrentUser, Role } from '../types';

export function Protected({
  user,
  role,
  children,
}: {
  user: CurrentUser | null;
  role?: Role;
  children: ReactNode;
}) {
  if (!user) return <Navigate to="/login" replace />;
  if (user.passwordChangeRequired) return <Navigate to="/change-password" replace />;
  if (role && user.role !== role) return <Navigate to="/me" replace />;
  return children;
}
