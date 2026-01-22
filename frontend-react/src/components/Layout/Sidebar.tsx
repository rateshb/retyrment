import { Link, useLocation } from 'react-router-dom';
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

export function Sidebar() {
  const location = useLocation();
  const { user, features, logout } = useAuthStore();

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
        className={`nav-item ${isActive(item.path) ? 'active' : ''}`}
      >
        {item.icon}
        <span>{item.label}</span>
        {item.requiresPro && (
          <span className="pro-badge ml-auto">PRO</span>
        )}
      </Link>
    );
  };

  const renderSection = (title: string, items: NavItem[]) => {
    const visibleItems = items.filter(canAccess);
    if (visibleItems.length === 0) return null;

    return (
      <>
        <div className="px-4 mt-6 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          {title}
        </div>
        {visibleItems.map(renderNavItem)}
      </>
    );
  };

  return (
    <aside className="w-64 bg-white border-r border-slate-200 flex flex-col shadow-sm">
      {/* Logo */}
      <div className="p-5 border-b border-slate-200">
        <Link to="/" className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg logo-gradient flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-primary-500/30">
            ₹
          </div>
          <span className="text-xl font-bold bg-gradient-to-r from-primary-500 to-primary-700 bg-clip-text text-transparent">
            Retyrment
          </span>
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 overflow-y-auto scrollbar-thin">
        <div className="px-4 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          Overview
        </div>
        {overviewItems.map(renderNavItem)}

        {renderSection('Data Entry', dataEntryItems)}
        {renderSection('Analysis', analysisItems)}

        {/* Admin Panel - Always show for ADMIN users */}
        {user?.role === 'ADMIN' && (
          <>
            <div className="px-4 mt-6 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Administration
            </div>
            <Link
              to="/admin"
              className={`nav-item ${isActive('/admin') ? 'active' : ''}`}
            >
              <Crown size={18} />
              <span>Admin Panel</span>
            </Link>
          </>
        )}
      </nav>

      {/* User Section */}
      <div className="p-4 border-t border-slate-200">
        {/* User Profile */}
        {user && (
          <div className="flex items-center gap-3 px-3 py-2 mb-2">
            <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
              {user.profilePicture ? (
                <img src={user.profilePicture} alt="" className="w-8 h-8 rounded-full" />
              ) : (
                <User size={16} className="text-primary-600" />
              )}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-slate-700 truncate">{user.name || user.email}</p>
              <p className="text-xs text-slate-500">{user.role}</p>
            </div>
          </div>
        )}

        {/* Settings Links */}
        {settingsItems.map(renderNavItem)}

        {/* Account Link */}
        <Link
          to="/account"
          className={`nav-item ${isActive('/account') ? 'active' : ''}`}
        >
          <User size={18} />
          <span>My Account</span>
        </Link>

        {/* Logout Button */}
        <button
          onClick={logout}
          className="nav-item w-full text-left text-slate-500 hover:text-danger-500"
        >
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}
