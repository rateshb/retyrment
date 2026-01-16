# WealthVision Frontend

A modern, responsive personal finance planner built with HTML5, Tailwind CSS, Chart.js, and jsPDF.

## Features

- 📊 **Dashboard** - Overview of net worth, projections, and recommendations
- 💰 **Income Tracking** - Manage multiple income sources with annual increments
- 📈 **Investment Portfolio** - Track MFs, stocks, FDs, PPF, EPF, real estate, etc.
- 🏦 **Loan Management** - EMI tracking with amortization schedules
- 🛡️ **Insurance Policies** - Term, health, ULIP, endowment, money-back, annuity/pension tracking
- 🛒 **Expense Tracking** - Fixed and variable expense management
- 🎯 **Goal Planning** - Set and track financial goals
- 📅 **Investment Calendar** - Year-round view of financial commitments
- 🏖️ **Retirement Planner** - Project corpus growth with GAP analysis & interactive charts
- 📑 **Reports & Export** - PDF, Excel, and JSON export options
- ⚙️ **User Preferences** - Currency, country, theme, and notification settings
- 🔐 **Google Login** - Secure OAuth2 authentication
- ⭐ **User Roles** - FREE, PRO, and ADMIN tiers

## Quick Start

```bash
# 1. Ensure backend is running
cd ../backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 2. Start frontend server
cd ../frontend
npx http-server -p 5000 -c-1

# 3. Open browser
# New users: http://localhost:5000/landing.html
# Login: http://localhost:5000/login.html
```

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **HTML5** | - | Page structure |
| **Tailwind CSS** | 3.x (CDN) | Styling |
| **Chart.js** | 4.x (CDN) | Charts and visualizations |
| **jsPDF** | 2.5.x (CDN) | PDF report generation |
| **html2canvas** | 1.4.x (CDN) | Screenshot capture for PDFs |
| **Vanilla JavaScript** | ES6+ | Application logic |
| **localStorage** | - | JWT token & preferences storage |

## File Structure

```
frontend/
├── landing.html        # Marketing page (unregistered users)
├── login.html          # Google OAuth login
├── index.html          # Dashboard (authenticated)
├── income.html         # Income management
├── investments.html    # Investment portfolio
├── loans.html          # Loans & EMIs
├── insurance.html      # Insurance policies
├── expenses.html       # Monthly expenses
├── goals.html          # Financial goals
├── calendar.html       # Investment calendar
├── retirement.html     # Retirement planner + GAP analysis + Charts
├── reports.html        # Reports & PDF/Excel export
├── preferences.html    # User preferences (currency, theme, etc.)
├── settings.html       # App settings
├── admin.html          # Admin panel (ADMIN only)
├── css/
│   └── styles.css      # Custom styles
├── js/
│   ├── api.js          # API service layer + auth
│   ├── charts.js       # Chart.js configurations
│   ├── pdf-export.js   # PDF generation with jsPDF
│   ├── common.js       # Shared utilities + formatters
│   ├── dashboard.js    # Dashboard page logic
│   ├── income.js       # Income page logic
│   ├── investments.js  # Investments page logic
│   ├── loans.js        # Loans page logic
│   ├── insurance.js    # Insurance page logic
│   ├── expenses.js     # Expenses page logic
│   ├── goals.js        # Goals page logic
│   ├── calendar.js     # Calendar page logic
│   ├── retirement.js   # Retirement page logic
│   └── preferences.js  # Preferences page logic
└── README.md
```

## Chart.js Visualizations

The application uses Chart.js for interactive data visualizations:

### Dashboard Charts
- **Net Worth Doughnut** - Asset allocation breakdown
- **Projection Line Chart** - 5/10/20 year wealth projections
- **Income vs Expense Bar** - Monthly comparison

### Retirement Page Charts
- **Corpus Growth Chart** - Multi-line chart showing:
  - PPF balance over time
  - EPF balance over time
  - Mutual Fund growth
  - Total corpus projection
  - Annual SIP investments
  - Goal outflows (bar overlay)
- **Post-retirement projection** - Corpus depletion visualization

### Calendar Charts
- **Monthly Outflow Bar Chart** - Visualize spending patterns

## PDF Export Features

Located in `js/pdf-export.js`, provides three types of PDF reports:

### 1. Financial Summary PDF
- Net worth overview with assets/liabilities
- Income sources breakdown
- Investment portfolio table
- Active loans summary
- Goal progress with visual bars

### 2. Retirement Report PDF
- Summary cards (age, corpus, monthly income)
- GAP analysis with color-coded status
- Year-by-year projection table
- Income strategy comparison

### 3. Calendar PDF (Landscape)
- 12-month outflow grid
- All SIPs, insurance premiums, EMIs
- Monthly and yearly totals

## Authentication Flow

```
1. User clicks "Sign in with Google" on login.html
2. Redirects to backend: /api/oauth2/authorization/google
3. Google OAuth consent screen
4. Backend creates user, generates JWT
5. Redirects to frontend with token: /?token=xxx
6. Frontend stores token in localStorage
7. All API calls include: Authorization: Bearer <token>
```

## User Roles

| Role | Access |
|------|--------|
| **FREE** | Basic tracking, 3 goals, view-only retirement |
| **PRO** | Custom assumptions, PDF exports, recommendations |
| **ADMIN** | User management, upgrade/downgrade users |

## Pages Overview

| Page | URL | Auth | Description |
|------|-----|------|-------------|
| Landing | `landing.html` | ❌ | Marketing page |
| Login | `login.html` | ❌ | Google OAuth |
| Dashboard | `index.html` | ✅ | Financial overview |
| Income | `income.html` | ✅ | Income sources |
| Investments | `investments.html` | ✅ | Portfolio tracker |
| Loans | `loans.html` | ✅ | Loan management |
| Insurance | `insurance.html` | ✅ | Policy tracker |
| Expenses | `expenses.html` | ✅ | Expense tracking |
| Goals | `goals.html` | ✅ | Goal planning |
| Calendar | `calendar.html` | ✅ | Outflow calendar |
| Retirement | `retirement.html` | ✅ | Retirement + GAP + Charts |
| Reports | `reports.html` | ✅ | PDF/Excel exports (PRO) |
| Preferences | `preferences.html` | ✅ | User settings |
| Settings | `settings.html` | ✅ | Configuration |
| Admin | `admin.html` | ✅ ADMIN | User management |

## Retirement Planner Features

| Feature | Description |
|---------|-------------|
| **Tabbed Interface** | Income / GAP / Expenses tabs |
| **Income Strategies** | Simple Depletion, 4% Rule, Sustainable |
| **Interactive Chart** | Collapsible corpus growth visualization |
| **Settings Panel** | Adjust assumptions (PRO only) |
| **Rate Reduction** | PPF/EPF rates decrease over time option |
| **Effective Year** | Choose when settings take effect |
| **GAP Analysis** | Required vs Projected corpus |

## User Preferences

| Category | Options |
|----------|---------|
| **Regional** | Country, Currency, Date Format, Number Format |
| **Display** | Theme (Light/Dark/System), Show Amount in Words |
| **Financial** | Default Inflation, Equity/Debt Returns, Retirement Age |
| **Notifications** | Email alerts, Payment reminders |

## API Configuration

The frontend expects the backend API at `http://localhost:8080/api`.

To change this, edit `js/api.js`:

```javascript
const API_BASE = 'http://localhost:8080/api';
```

## Development

No build step required! Edit files and refresh browser.

```bash
# Using Node.js http-server (recommended)
npx http-server -p 5000 -c-1

# Using Python
python -m http.server 5000

# Using npx serve
npx serve -l 5000 -s
```

## Insurance Features

### Policy Types

| Type | Features |
|------|----------|
| **Term Life** | Coverage end age, continues after retirement |
| **Health** | Group/Personal/Family Floater subtypes |
| **ULIP** | Fund value tracking, market-linked |
| **Endowment** | Guaranteed returns + bonus |
| **Money-Back** | Periodic payouts at specified years |
| **Annuity** | Pay for N years, receive monthly from N+1 |

### Premium Scheduling

| Frequency | Schedule Options |
|-----------|-----------------|
| Yearly | Renewal month (Jan-Dec) |
| Half-Yearly | Payment months (e.g., Jan & Jul) |
| Quarterly | First month (e.g., Jan for Jan/Apr/Jul/Oct) |
| Monthly | Payment day (1-28) |

## Investment Features

### SIP/RD Day Tracking
- **SIP Day** - Day of month (1-28) when SIP is debited
- **RD Day** - Day of month for recurring deposit
- **Evaluation Date** - When current value was last checked

These dates are used in the Calendar to show exact debit days.

## External Libraries (CDN)

```html
<!-- Tailwind CSS -->
<script src="https://cdn.tailwindcss.com"></script>

<!-- Chart.js -->
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<!-- PDF Export -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js"></script>
```

## Browser Support

| Browser | Supported |
|---------|-----------|
| Chrome | ✅ Recommended |
| Firefox | ✅ |
| Edge | ✅ |
| Safari | ⚠️ Limited testing |
| Mobile | ⚠️ Not optimized |

## Notes

- All data stored in MongoDB via backend
- JWT tokens expire after 24 hours
- Charts auto-update on data changes
- Modal forms for add/edit operations
- Numbers formatted in Indian style (lakhs/crores)
- Insurance premiums included in retirement expense projection
- PDF exports require PRO subscription
- Preferences are stored per-user in the database
