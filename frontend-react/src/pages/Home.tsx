import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { Landing } from './Landing';
import { Dashboard } from './Dashboard';

export function Home() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated, login, isLoading } = useAuthStore();

  // Handle OAuth callback token
  useEffect(() => {
    const token = searchParams.get('token');
    if (token) {
      // Clear the token from URL
      setSearchParams({});
      // Process login
      login(token).then(() => {
        // Token processed, will re-render as authenticated
      }).catch((error) => {
        navigate('/login');
      });
    }
  }, [searchParams, setSearchParams, login, navigate]);

  // Show loading while processing token
  if (isLoading || searchParams.get('token')) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center text-white font-bold text-3xl animate-pulse">
            ₹
          </div>
          <p className="text-slate-600">Signing you in...</p>
        </div>
      </div>
    );
  }

  // Show Dashboard if authenticated, Landing if not
  if (isAuthenticated) {
    return <Dashboard />;
  }

  return <Landing />;
}

export default Home;
