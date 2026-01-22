import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ToastContainer } from './components/ui';
import {
  // Public pages
  Landing,
  About,
  Privacy,
  Terms,
  Pricing,
  Features,
  Disclaimer,
  ErrorPage,
  // Home - Smart route
  Home,
  // Main pages
  Dashboard,
  Investments,
  Login,
  Income,
  Loans,
  Insurance,
  Expenses,
  Goals,
  Family,
  Calendar,
  Retirement,
  InsuranceRecommendations,
  Simulation,
  Reports,
  Admin,
  Account,
  Settings,
  Preferences,
} from './pages';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public/Static Routes */}
          <Route path="/landing" element={<Landing />} />
          <Route path="/about" element={<About />} />
          <Route path="/privacy" element={<Privacy />} />
          <Route path="/terms" element={<Terms />} />
          <Route path="/pricing" element={<Pricing />} />
          <Route path="/features" element={<Features />} />
          <Route path="/disclaimer" element={<Disclaimer />} />
          <Route path="/login" element={<Login />} />
          <Route path="/error" element={<ErrorPage />} />

          {/* Home Route - Shows Landing or Dashboard based on auth */}
          <Route path="/" element={<Home />} />

          {/* Protected Routes */}
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/income" element={
            <ProtectedRoute requireFeature="incomePage">
              <Income />
            </ProtectedRoute>
          } />
          
          <Route path="/investments" element={
            <ProtectedRoute requireFeature="investmentPage">
              <Investments />
            </ProtectedRoute>
          } />
          
          <Route path="/loans" element={
            <ProtectedRoute requireFeature="loanPage">
              <Loans />
            </ProtectedRoute>
          } />
          
          <Route path="/insurance" element={
            <ProtectedRoute requireFeature="insurancePage">
              <Insurance />
            </ProtectedRoute>
          } />
          
          <Route path="/expenses" element={
            <ProtectedRoute requireFeature="expensePage">
              <Expenses />
            </ProtectedRoute>
          } />
          
          <Route path="/goals" element={
            <ProtectedRoute requireFeature="goalsPage">
              <Goals />
            </ProtectedRoute>
          } />
          
          <Route path="/family" element={
            <ProtectedRoute requireFeature="familyPage">
              <Family />
            </ProtectedRoute>
          } />
          
          <Route path="/calendar" element={
            <ProtectedRoute requireFeature="calendarPage">
              <Calendar />
            </ProtectedRoute>
          } />
          
          <Route path="/retirement" element={
            <ProtectedRoute requireFeature="retirementPage">
              <Retirement />
            </ProtectedRoute>
          } />
          
          <Route path="/insurance-recommendations" element={
            <ProtectedRoute requireFeature="insuranceRecommendationsPage">
              <InsuranceRecommendations />
            </ProtectedRoute>
          } />
          
          <Route path="/simulation" element={
            <ProtectedRoute requireFeature="simulationPage">
              <Simulation />
            </ProtectedRoute>
          } />
          
          <Route path="/reports" element={
            <ProtectedRoute requireFeature="reportsPage">
              <Reports />
            </ProtectedRoute>
          } />
          
          <Route path="/admin" element={
            <ProtectedRoute requireFeature="adminPanel" requireAdmin>
              <Admin />
            </ProtectedRoute>
          } />
          
          <Route path="/account" element={
            <ProtectedRoute requireFeature="accountPage">
              <Account />
            </ProtectedRoute>
          } />
          
          <Route path="/settings" element={
            <ProtectedRoute requireFeature="settingsPage">
              <Settings />
            </ProtectedRoute>
          } />
          
          <Route path="/preferences" element={
            <ProtectedRoute requireFeature="preferencesPage">
              <Preferences />
            </ProtectedRoute>
          } />

          {/* Fallback */}
          <Route path="*" element={<ErrorPage />} />
        </Routes>
        
        <ToastContainer />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
