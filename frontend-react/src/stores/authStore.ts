import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User, FeatureAccess, authApi, auth } from '../lib/api';

interface AuthState {
  user: User | null;
  features: FeatureAccess | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  lastFeaturesRefresh: number | null;
  
  // Actions
  setUser: (user: User | null) => void;
  setFeatures: (features: FeatureAccess | null) => void;
  login: (token: string) => Promise<void>;
  logout: () => void;
  fetchUser: () => Promise<void>;
  fetchFeatures: () => Promise<void>;
  refreshFeaturesIfNeeded: () => Promise<void>;
}

// Features cache duration: 1 minute (short cache to detect admin changes faster)
const FEATURES_CACHE_DURATION = 1 * 60 * 1000;

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      features: null,
      isLoading: false,
      isAuthenticated: auth.isLoggedIn(),
      lastFeaturesRefresh: null,

      setUser: (user) => set({ user, isAuthenticated: !!user }),
      
      setFeatures: (features) => set({ features, lastFeaturesRefresh: Date.now() }),

      login: async (token: string) => {
        auth.setToken(token);
        set({ isAuthenticated: true, isLoading: true });
        
        try {
          await get().fetchUser();
          await get().fetchFeatures();
        } finally {
          set({ isLoading: false });
        }
      },

      logout: () => {
        auth.logout();
        set({ user: null, features: null, isAuthenticated: false, lastFeaturesRefresh: null });
      },

      fetchUser: async () => {
        try {
          const user = await authApi.me();
          auth.setUser(user);
          set({ user, isAuthenticated: true });
        } catch (error) {
          console.error('Failed to fetch user:', error);
          set({ user: null, isAuthenticated: false });
        }
      },

      fetchFeatures: async () => {
        try {
          const response = await authApi.features();
          set({ features: response.features, lastFeaturesRefresh: Date.now() });
          // Also store in localStorage for backward compatibility
          localStorage.setItem('retyrment_features', JSON.stringify(response.features));
        } catch (error) {
          console.error('Failed to fetch features:', error);
        }
      },

      // Auto-refresh features if cache is stale
      refreshFeaturesIfNeeded: async () => {
        const { lastFeaturesRefresh, isAuthenticated } = get();
        
        if (!isAuthenticated) return;
        
        const now = Date.now();
        const isStale = !lastFeaturesRefresh || (now - lastFeaturesRefresh > FEATURES_CACHE_DURATION);
        
        if (isStale) {
          await get().fetchFeatures();
        }
      },
    }),
    {
      name: 'retyrment-auth',
      partialize: (state) => ({ 
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        features: state.features,
        lastFeaturesRefresh: state.lastFeaturesRefresh,
      }),
    }
  )
);

// Initialize features on app load if authenticated
if (auth.isLoggedIn()) {
  // Refresh features after a small delay to ensure store is ready
  setTimeout(() => {
    useAuthStore.getState().refreshFeaturesIfNeeded();
  }, 100);
}
