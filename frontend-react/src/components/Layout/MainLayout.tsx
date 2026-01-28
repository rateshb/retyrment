import { ReactNode, useEffect, useState } from 'react';
import { Sidebar } from './Sidebar';
import { useAuthStore } from '../../stores/authStore';
import { RefreshCw } from 'lucide-react';

interface MainLayoutProps {
  children: ReactNode;
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}

export function MainLayout({ children, title, subtitle, actions }: MainLayoutProps) {
  const { refreshFeaturesIfNeeded, fetchFeatures, isAuthenticated } = useAuthStore();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    try {
      return localStorage.getItem('sidebarCollapsed') === 'true';
    } catch {
      return false;
    }
  });

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.title = title ? `${title} | Retyrment` : 'Retyrment - Financial Planning';
    }
  }, [title]);

  // Auto-refresh features when layout mounts (on page navigation)
  useEffect(() => {
    if (isAuthenticated) {
      refreshFeaturesIfNeeded();
    }
  }, [isAuthenticated, refreshFeaturesIfNeeded]);

  const handleRefreshFeatures = async () => {
    try {
      await fetchFeatures();
    } catch (error) {
    }
  };

  const toggleSidebar = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem('sidebarCollapsed', String(next));
      } catch {
        // ignore storage failures
      }
      return next;
    });
  };

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 bg-pattern">
      <Sidebar collapsed={sidebarCollapsed} onToggleCollapse={toggleSidebar} />
      
      <main className="flex-1 overflow-y-auto scrollbar-thin">
        <header className="sticky top-0 z-10 header-blur border-b border-slate-200 px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-semibold text-slate-800">{title}</h1>
              {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
            </div>
            <div className="flex items-center gap-3">
              {actions}
              <button
                onClick={handleRefreshFeatures}
                className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                title="Refresh permissions"
              >
                <RefreshCw size={16} />
              </button>
            </div>
          </div>
        </header>

        <div className="p-8">
          {children}
        </div>
      </main>
    </div>
  );
}
