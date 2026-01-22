# Retyrment React Frontend

A modern React-based frontend for the Retyrment financial planning application.

## Tech Stack

- **React 19** - UI library
- **TypeScript** - Type safety
- **Vite** - Fast build tool
- **TanStack Query** - Data fetching & caching
- **React Router v7** - Client-side routing
- **Zustand** - Lightweight state management
- **Tailwind CSS v4** - Utility-first styling
- **Lucide React** - Icons
- **Recharts** - Charts & visualizations

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn
- Backend server running on port 8080

### Installation

```bash
# Navigate to the frontend-react directory
cd frontend-react

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at http://localhost:3000

### Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server on port 3000 |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm run lint` | Run ESLint |

## Project Structure

```
frontend-react/
├── public/                 # Static assets
├── src/
│   ├── components/        # Reusable components
│   │   ├── Layout/        # Layout components (Sidebar, MainLayout)
│   │   ├── ui/            # UI primitives (Button, Card, Modal, etc.)
│   │   └── ProtectedRoute.tsx
│   ├── lib/               # Utilities & API
│   │   ├── api.ts         # API client & types
│   │   └── utils.ts       # Helper functions
│   ├── pages/             # Page components
│   │   ├── Dashboard.tsx
│   │   ├── Investments.tsx
│   │   ├── Login.tsx
│   │   └── placeholders/  # Placeholder pages
│   ├── stores/            # Zustand stores
│   │   └── authStore.ts   # Authentication state
│   ├── App.tsx            # Main app with routing
│   ├── main.tsx           # Entry point
│   └── index.css          # Global styles & Tailwind
├── index.html
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

## Features

### Implemented Pages

| Page | Status | Description |
|------|--------|-------------|
| Dashboard | ✅ Full | Financial overview with stats & recommendations |
| Investments | ✅ Full | CRUD for investments with emergency fund tagging |
| Login | ✅ Full | Google OAuth login |
| All others | 🔨 Placeholder | Ready for implementation |

### Key Features

- **Protected Routes**: Automatic redirect to login for unauthenticated users
- **Feature-based Access**: Pages hidden based on user's feature access
- **Role-based Access**: Admin-only pages protected
- **Toast Notifications**: Global toast system for feedback
- **Responsive Design**: Mobile-friendly sidebar & layouts
- **Data Caching**: TanStack Query for optimistic updates

## API Integration

The API client is centralized in `src/lib/api.ts`:

```typescript
import { api } from '../lib/api';

// Fetch investments
const investments = await api.investments.getAll();

// Create investment
await api.investments.create({ name: 'My Fund', type: 'MUTUAL_FUND', ... });

// Update investment
await api.investments.update(id, updatedData);

// Delete investment
await api.investments.delete(id);
```

### Using with TanStack Query

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';

// Query
const { data, isLoading } = useQuery({
  queryKey: ['investments'],
  queryFn: api.investments.getAll,
});

// Mutation with cache invalidation
const mutation = useMutation({
  mutationFn: api.investments.create,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['investments'] });
  },
});
```

## Styling

Using Tailwind CSS v4 with custom theme variables defined in `src/index.css`:

```css
/* Custom colors */
--color-primary-500: #6366f1;
--color-success-500: #10b981;
--color-danger-500: #ef4444;

/* Component classes */
.btn-primary { ... }
.card { ... }
.nav-item { ... }
```

## Authentication

Authentication is managed via Zustand store (`src/stores/authStore.ts`):

```typescript
import { useAuthStore } from '../stores/authStore';

function MyComponent() {
  const { user, isAuthenticated, logout } = useAuthStore();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }
  
  return <div>Welcome, {user.name}</div>;
}
```

## Adding New Pages

1. Create page component in `src/pages/`:

```typescript
import { MainLayout } from '../components/Layout';

export function MyPage() {
  return (
    <MainLayout title="My Page" subtitle="Description">
      {/* Your content */}
    </MainLayout>
  );
}
```

2. Add route in `src/App.tsx`:

```typescript
<Route path="/my-page" element={
  <ProtectedRoute requireFeature="myFeature">
    <MyPage />
  </ProtectedRoute>
} />
```

3. Add to sidebar in `src/components/Layout/Sidebar.tsx`

## Environment Variables

Create `.env.local` for local overrides:

```env
VITE_API_BASE=http://localhost:8080/api
```

## Building for Production

```bash
npm run build
```

Output will be in `dist/` directory. Deploy to any static hosting:

- Nginx
- Vercel
- Netlify
- AWS S3 + CloudFront

## Migrating from Legacy Frontend

The React frontend is a complete rewrite of the vanilla JS frontend. Key differences:

| Aspect | Legacy | React |
|--------|--------|-------|
| State | Global variables | Zustand stores |
| Routing | Multi-page (HTML files) | SPA with React Router |
| Data fetching | Manual fetch | TanStack Query |
| Components | Copy-paste HTML | Reusable React components |
| Styling | Inline + CSS | Tailwind CSS |
| Types | None | TypeScript |

The legacy frontend remains in `/frontend` for reference during migration.
