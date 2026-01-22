import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Card, CardContent } from '../components/ui';
import { useAuthStore } from '../stores/authStore';

export function ErrorPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuthStore();

  const searchParams = new URLSearchParams(location.search);
  const message =
    (location.state as { message?: string } | null)?.message ||
    searchParams.get('message') ||
    'Something went wrong.';

  const title =
    (location.state as { title?: string } | null)?.title ||
    searchParams.get('title') ||
    'Oops!';

  return (
    <div className="min-h-screen bg-slate-50 bg-pattern flex items-center justify-center p-6">
      <Card className="w-full max-w-lg">
        <CardContent className="p-8">
          <div className="text-center space-y-4">
            <div className="text-5xl">⚠️</div>
            <h1 className="text-2xl font-semibold text-slate-800">{title}</h1>
            <p className="text-slate-600">{message}</p>
            <div className="flex flex-wrap justify-center gap-3 pt-2">
              <Button variant="secondary" onClick={() => navigate(-1)}>
                Go Back
              </Button>
              <Button onClick={() => navigate('/')}>
                Go to Home
              </Button>
              {!isAuthenticated && (
                <Button variant="ghost" onClick={() => navigate('/login')}>
                  Go to Login
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
