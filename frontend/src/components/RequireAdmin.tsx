import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';
import { isAdminRole } from '../utils/roles';

export default function RequireAdmin({ children }: { children: ReactNode }) {
  const user = useAppSelector((state) => state.auth.user);

  if (!isAdminRole(user?.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
