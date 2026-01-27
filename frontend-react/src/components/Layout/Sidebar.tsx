import { Link, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/authStore';
import {
  LayoutDashboard,
  Wallet,
  TrendingUp,
  Building2,
  Shield,
  ShoppingCart,
  Target,
  Users,
  Calendar,
  Umbrella,
  Stethoscope,
  Dice5,
  FileText,
  Settings,
  Sliders,
  User,
  Crown,
  LogOut,
  ChevronDown,
  ChevronRight,
  PanelLeftClose,
  PanelLeftOpen,
} from 'lucide-react';

interface NavItem {
  path: string;
  label: string;
  icon: React.ReactNode;
  feature?: string;
  requiresPro?: boolean;
  adminOnly?: boolean;
}

const overviewItems: NavItem[] = [
  { path: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard size={18} /> },
];

const dataEntryItems: NavItem[] = [
  { path: '/income', label: 'Income', icon: <Wallet size={18} />, feature: 'incomePage' },
  { path: '/investments', label: 'Investments', icon: <TrendingUp size={18} />, feature: 'investmentPage' },
  { path: '/loans', label: 'Loans', icon: <Building2 size={18} />, feature: 'loanPage' },
  { path: '/insurance', label: 'Insurance', icon: <Shield size={18} />, feature: 'insurancePage' },
  { path: '/expenses', label: 'Expenses', icon: <ShoppingCart size={18} />, feature: 'expensePage' },
  { path: '/goals', label: 'Goals', icon: <Target size={18} />, feature: 'goalsPage' },
  { path: '/family', label: 'Family', icon: <Users size={18} />, feature: 'familyPage' },
];

const analysisItems: NavItem[] = [
  { path: '/calendar', label: 'Calendar', icon: <Calendar size={18} />, feature: 'calendarPage' },
  { path: '/retirement', label: 'Retirement', icon: <Umbrella size={18} />, feature: 'retirementPage' },
  { path: '/insurance-recommendations', label: 'Insurance Advisor', icon: <Stethoscope size={18} />, feature: 'insuranceRecommendationsPage' },
  { path: '/simulation', label: 'Simulation', icon: <Dice5 size={18} />, feature: 'simulationPage', requiresPro: true },
  { path: '/reports', label: 'Reports', icon: <FileText size={18} />, feature: 'reportsPage' },
];

const settingsItems: NavItem[] = [
  { path: '/preferences', label: 'Preferences', icon: <Sliders size={18} />, feature: 'preferencesPage' },
  { path: '/settings', label: 'Settings', icon: <Settings size={18} />, feature: 'settingsPage' },
];

interface SidebarProps {
  collapsed?: boolean;
  onToggleCollapse?: () => void;
}

export function Sidebar({ collapsed = false, onToggleCollapse }: SidebarProps) {
  const location = useLocation();
  const { user, features, logout } = useAuthStore();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [avatarError, setAvatarError] = useState(false);

  const avatarUrl = user?.picture || user?.profilePicture;

  useEffect(() => {
    setAvatarError(false);
  }, [avatarUrl]);

  const isActive = (path: string) => location.pathname === path;

  const canAccess = (item: NavItem) => {
    if (item.adminOnly && user?.role !== 'ADMIN') return false;
    if (item.feature && features && !(features as unknown as Record<string, boolean>)[item.feature]) return false;
    return true;
  };

  const renderNavItem = (item: NavItem) => {
    if (!canAccess(item)) return null;

    return (
      <Link
        key={item.path}
        to={item.path}
        className={`nav-item ${isActive(item.path) ? 'active' : ''} ${collapsed ? 'justify-center px-3' : ''}`}
        title={collapsed ? item.label : undefined}
        aria-label={collapsed ? item.label : undefined}
      >
        {item.icon}
        {!collapsed && <span>{item.label}</span>}
        {item.requiresPro && (
          !collapsed && <span className="pro-badge ml-auto">PRO</span>
        )}
      </Link>
    );
  };

  const renderSection = (title: string, items: NavItem[]) => {
    const visibleItems = items.filter(canAccess);
    if (visibleItems.length === 0) return null;

    return (
      <>
        {!collapsed && (
          <div className="px-4 mt-6 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
            {title}
          </div>
        )}
        {visibleItems.map(renderNavItem)}
      </>
    );
  };

  const visibleSettingsItems = settingsItems.filter(canAccess);

  return (
    <aside className={`bg-white border-r border-slate-200 flex flex-col shadow-sm transition-all duration-200 ${collapsed ? 'w-20' : 'w-64'}`}>
      {/* Logo */}
      <div className={`p-5 border-b border-slate-200 ${collapsed ? '' : 'flex items-center justify-between'}`}>
        <Link to="/" className={`flex items-center gap-3 ${collapsed ? 'justify-center' : ''}`}>
          <div className="w-10 h-10 rounded-lg logo-gradient flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-primary-500/30">
            ₹
          </div>
          {!collapsed && (
            <span className="text-xl font-bold bg-gradient-to-r from-primary-500 to-primary-700 bg-clip-text text-transparent">
              Retyrment
            </span>
          )}
        </Link>
        {onToggleCollapse && (
          <button
            onClick={onToggleCollapse}
            className={`p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors ${collapsed ? 'mt-3 mx-auto' : ''}`}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <PanelLeftOpen size={16} /> : <PanelLeftClose size={16} />}
          </button>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 overflow-y-auto scrollbar-thin">
        {!collapsed && (
          <div className="px-4 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Overview
          </div>
        )}
        {overviewItems.map(renderNavItem)}

        {renderSection('Data Entry', dataEntryItems)}
        {renderSection('Analysis', analysisItems)}

        {/* Admin Panel - Always show for ADMIN users */}
        {user?.role === 'ADMIN' && (
          <>
            {!collapsed && (
              <div className="px-4 mt-6 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Administration
              </div>
            )}
            <Link
              to="/admin"
              className={`nav-item ${isActive('/admin') ? 'active' : ''} ${collapsed ? 'justify-center px-3' : ''}`}
              title={collapsed ? 'Admin Panel' : undefined}
              aria-label={collapsed ? 'Admin Panel' : undefined}
            >
              <Crown size={18} />
              {!collapsed && <span>Admin Panel</span>}
            </Link>
          </>
        )}
      </nav>

      {/* User Section */}
      <div className="p-4 border-t border-slate-200">
        {/* Settings Toggle */}
        {visibleSettingsItems.length > 0 && (
          <button
            type="button"
            onClick={() => setSettingsOpen((prev) => !prev)}
            className={`nav-item w-full ${collapsed ? 'justify-center px-3' : ''}`}
            title={collapsed ? (user?.name || user?.email || 'Account') : undefined}
            aria-label={collapsed ? (user?.name || user?.email || 'Account') : undefined}
          >
            {avatarUrl && !avatarError ? (
              <img
                src={avatarUrl}
                alt={user?.name || 'User'}
                className="w-7 h-7 rounded-full object-cover"
                referrerPolicy="no-referrer"
                onError={() => setAvatarError(true)}
              />
            ) : (
              <span className="w-7 h-7 rounded-full bg-slate-100 text-slate-500 flex items-center justify-center">
                <User size={16} />
              </span>
            )}
            {!collapsed && (
              <div className="flex flex-col items-start">
                <span>{user?.name || user?.email || 'Account'}</span>
                {user?.role && <span className="text-xs text-slate-500">{user.role}</span>}
              </div>
            )}
            {!collapsed && (
              <span className="ml-auto text-slate-400">
                {settingsOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
              </span>
            )}
          </button>
        )}

        {/* Settings Links */}
        {settingsOpen && visibleSettingsItems.map(renderNavItem)}

        {/* Account Link */}
        {settingsOpen && (
          <Link
            to="/account"
            className={`nav-item ${isActive('/account') ? 'active' : ''} ${collapsed ? 'justify-center px-3' : ''}`}
            title={collapsed ? 'My Account' : undefined}
            aria-label={collapsed ? 'My Account' : undefined}
          >
            <User size={18} />
            {!collapsed && <span>My Account</span>}
          </Link>
        )}

        {/* Logout Button */}
        {settingsOpen && (
          <button
            onClick={logout}
            className={`nav-item w-full text-left text-slate-500 hover:text-danger-500 ${collapsed ? 'justify-center px-3' : ''}`}
            title={collapsed ? 'Logout' : undefined}
            aria-label={collapsed ? 'Logout' : undefined}
          >
            <LogOut size={18} />
            {!collapsed && <span>Logout</span>}
          </button>
        )}
      </div>
    </aside>
  );
}
