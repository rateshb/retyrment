import { ReactNode, useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { auth } from '../lib/api';

interface ProtectedRouteProps {
  children: ReactNode;
  requireFeature?: string;
  requireAdmin?: boolean;
}

export function ProtectedRoute({ children, requireFeature, requireAdmin }: ProtectedRouteProps) {
  const location = useLocation();
  const { isAuthenticated, user, features, fetchUser, fetchFeatures } = useAuthStore();

  // Fetch user data if we have a token but no user
  useEffect(() => {
    if (auth.isLoggedIn() && !user) {
      fetchUser();
      fetchFeatures();
    }
  }, [user, fetchUser, fetchFeatures]);

  // Not logged in
  if (!auth.isLoggedIn()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check admin requirement
  if (requireAdmin && user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  // Check feature requirement
  if (requireFeature && features && !(features as unknown as Record<string, boolean>)[requireFeature]) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
