# Retyrment - Complete Features Summary

**Last Updated:** January 15, 2026  
**Version:** 1.0.0

---

## 🎯 Core Features Overview

Retyrment is a comprehensive personal finance planning application designed for Indian investors. It provides complete tracking, analysis, and planning tools for managing personal finances, investments, and retirement planning.

---

## 📊 Dashboard Features

### High Priority Alerts (Replaces 10-Year Projection)
- **Corpus Gap Analysis**: Shows retirement shortfall with specific amount needed
- **Reinvestment Opportunities**: Lists maturing investments (FD, RD, PPF, Insurance) before retirement
- **Post-Loan Investment**: Tracks when EMIs end and funds become available for investment
- **Savings Breakdown**: Shows how monthly savings can be allocated to:
  - Emergency Fund requirements
  - Corpus contribution needs
  - Goal funding recommendations
- **Illiquid Asset Analysis**: Analyzes if selling Gold/Real Estate would help meet corpus requirements
- **At-Risk Goals**: Highlights goals that are underfunded

### Net Worth Summary
- Total assets, liabilities, and net worth
- Asset allocation breakdown
- Growth trends and projections

---

## 💰 Data Entry Features

### 1. Income Tracking
- **Access**: Always visible to all users
- Multiple income sources (salary, freelance, rental, etc.)
- Monthly amount tracking
- Annual increment percentage
- Active/inactive status
- Total monthly income calculation

### 2. Investment Tracking
- **Access**: Visible by default (admin can restrict)
- **Investment Types**:
  - **Default Allowed**: Mutual Funds, PPF, EPF, FD, RD, Real Estate
  - **Admin-Controlled**: Stocks, NPS, Gold, Crypto, Cash
- SIP tracking with step-up
- Maturity date tracking (FD, RD, PPF)
- Current value and expected returns
- Corpus contribution calculation
- All investments (except Gold, Real Estate, Crypto) contribute to retirement corpus

### 3. Loan Management
- **Access**: Always visible to all users
- Multiple loan types (home, personal, car, etc.)
- EMI calculation and tracking
- Interest rate and tenure
- Amortization schedule
- Post-loan investment opportunities (when EMI ends)

### 4. Insurance Policies
- **Access**: Visible by default (admin can restrict)
- **Insurance Types**:
  - **Available**: Health, Term Life, ULIP, Endowment, Money-Back
  - **Blocked by Default**: Vehicle, Pension, Life Savings (admin can unblock)
- Premium frequency (monthly, quarterly, yearly)
- Maturity date tracking (ULIP, Endowment, Money-Back)
- Sum assured and fund value
- Maturity values included in retirement inflows

### 5. Expense Tracking
- **Access**: Always visible to all users
- Fixed expenses (rent, EMI, insurance premiums)
- Variable expenses (groceries, utilities, entertainment)
- Category-based organization
- Monthly expense breakdown

### 6. Goal Planning
- **Access**: Visible by default (admin can restrict)
- Financial goals (education, marriage, house, etc.)
- Target amount and target year
- Priority levels (High/Medium/Low)
- Recurrence (One-time/Recurring)
- Funding status and GAP analysis
- Goal outflows shown in retirement matrix

---

## 📈 Analysis & Planning Features

### 1. Calendar Page
- **Access**: Restricted by default (admin must enable)
- Year-round view of financial commitments
- Monthly and yearly outflow visualization
- Track SIPs, insurance premiums, EMIs, goals
- Manual calendar entries for one-time payments
- Category breakdown
- Top categories by amount

### 2. Retirement Planning
- **Access**: Visible by default (admin can restrict)

#### Retirement Matrix Tab (Always Visible)
- Year-by-year corpus projection
- **PPF + EPF Combined Column**: Shows combined balance with individual ROIs displayed
- **Mutual Funds Column**: With SIP contributions
- **Other Liquid Column**: FD, RD, Stocks, Cash with hover breakdown showing what's included
- **Inflows**: Maturing investments and insurance policies (shown in green)
- **Goal Outflows**: Withdrawals for goals (shown in red)
- **Net Corpus**: Final projected balance
- GAP analysis with color-coded indicators
- Income strategy selection (Simple Depletion, 4% Rule, Sustainable)

#### Strategy Planner Tab (Restricted by default, admin-enabled)
- Analyze reinvestment opportunities
- Illiquid asset sale scenarios (Gold/Real Estate)
- Post-loan investment planning
- Save "What-If" strategies
- View strategy impact on corpus
- Compare baseline vs. strategy-applied projections
- Deployment year tracking

### 3. Monte Carlo Simulation
- **Access**: Restricted by default (admin must enable)
- Probabilistic financial projections
- Configurable parameters:
  - Number of simulations (100-10,000)
  - Projection years (1-50)
- Percentile-based outcomes:
  - 10th percentile (worst case)
  - 25th percentile
  - 50th percentile (median)
  - 75th percentile
  - 90th percentile (best case)
- Visual distribution charts
- Uses SecureRandom for cryptographic randomness

---

## 📑 Reports & Export Features

### Access Control
All report types require admin-enabled access:
- PDF Export
- Excel Export
- JSON Export
- Data Import

### Available Reports

#### 1. Financial Summary PDF
- Net worth overview (assets, liabilities, net worth)
- Income sources breakdown with totals
- Investment portfolio with:
  - Total invested, current value, total gain, return percentage
  - Individual investments with gains and returns
- Active loans summary with:
  - Total outstanding, total EMI
  - Individual loans with rates and tenure
- Insurance policies with:
  - Total annual premium
  - Individual policies with sum assured and premium
- Monthly expenses breakdown:
  - Total monthly expenses
  - Category-wise breakdown with percentages
- Goal progress with visual indicators

#### 2. Retirement Report PDF
- Current asset breakdown (starting balances for all asset types)
- Maturing investments & inflows:
  - Total maturing amount
  - Maturing investments table
  - Maturing insurance policies table
- Year-by-year projection matrix:
  - PPF+EPF combined column
  - Mutual Funds column
  - Other Liquid column
  - Inflows and outflows
  - Net corpus
- GAP analysis
- Income strategy details

#### 3. Calendar Report PDF (Landscape)
- 12-month outflow grid
- Category breakdown:
  - Top 10 categories by total amount
  - Percentage of yearly total
  - Sorted by amount (descending)
- Monthly and yearly totals
- All SIPs, insurance premiums, EMIs

#### 4. Excel Export
- Complete retirement matrix in spreadsheet format
- All financial data
- Formatted for analysis

#### 5. JSON Backup/Restore
- Full data export for backup
- Import functionality to restore data
- Complete data portability

---

## 🔐 Authentication & User Management

### Authentication
- **Google OAuth2** - Secure authentication
- **JWT Tokens** - Stateless session management
- **Automatic Session** - Persists across browser restarts

### User Roles

#### FREE Users
- Basic tracking and data entry
- View-only retirement projections
- Limited features
- Unlimited duration

#### PRO Users
- All FREE features
- Custom retirement assumptions
- PDF/Excel/JSON exports (if admin-enabled)
- Monte Carlo simulation (if admin-enabled)
- Strategy Planner (if admin-enabled)
- Calendar access (if admin-enabled)
- **Time-Limited Subscriptions**: PRO access expires based on subscription end date

#### ADMIN Users
- All PRO features
- User management
- Feature access control
- Role assignment
- Trial extensions
- Permanent access

### Trial System
- **7-Day PRO Trial**: New users automatically receive PRO access for 7 days
- **Trial Extension**: Admins can extend trial periods
- **Automatic Reversion**: System reverts to FREE after trial expiry

### Time-Limited Roles
- Admins can grant temporary PRO/ADMIN access
- Set expiry dates for role changes
- Automatic reversion to original role after expiry
- Track role change reason and admin who made the change

---

## 🎛️ Feature Access Control System

### Per-User Access Control
Admins can control feature access on a per-user basis, overriding default role-based access.

#### Page Visibility
| Page | Default | Admin Control |
|------|---------|---------------|
| Income | ✅ All | Always visible |
| Investment | ✅ All | Can restrict |
| Loan | ✅ All | Always visible |
| Insurance | ✅ All | Can restrict |
| Expense | ✅ All | Always visible |
| Goals | ✅ All | Can restrict |
| Calendar | ❌ Restricted | Admin-enabled |
| Retirement | ✅ All | Can restrict |
| Reports | ❌ Restricted | Admin-enabled |
| Simulation | ❌ Restricted | Admin-enabled |
| Preferences | ❌ Restricted | Admin-enabled |
| Settings | ✅ All | Always visible |
| Account | ✅ All | Always visible |
| Admin Panel | ❌ Admin only | Admin only |

#### Investment Type Control
- **Default Allowed**: MF, PPF, EPF, FD, RD, Real Estate
- **Admin-Controlled**: Stocks, NPS, Gold, Crypto, Cash
- Restricted types don't appear in dropdowns

#### Insurance Type Control
- **Default Blocked**: Vehicle, Pension, Life Savings
- Admin can customize blocked types per user
- Blocked types don't appear in dropdowns

#### Retirement Tab Control
- **Matrix Tab**: Always visible
- **Strategy Planner Tab**: Restricted by default, admin-enabled

#### Report Access Control
- PDF Export: Admin-controlled
- Excel Export: Admin-controlled
- JSON Export: Admin-controlled
- Data Import: Admin-controlled

---

## 👑 Admin Features

### User Management
- View all users with statistics
- Search users by email
- Change user roles (FREE/PRO/ADMIN)
- Set time-limited role changes
- Extend trial periods
- Remove role expiry (make permanent)
- Delete users
- Force check and revert expired roles

### Feature Access Management
- Control page visibility per user
- Set allowed investment types per user
- Set blocked insurance types per user
- Enable/disable retirement tabs per user
- Control report access (PDF/Excel/JSON/Import) per user

### Statistics Dashboard
- Total users count
- Users by role (FREE/PRO/ADMIN)
- Active trials count
- Temporary roles count
- Expiring roles (next 7 days)

---

## 👤 Account Management

### My Account Page
- **Access**: Always visible to all users
- View profile information
- Check subscription status:
  - Trial period (if active)
  - PRO subscription (if active)
  - Days remaining
  - Expiry dates
- View feature access permissions
- See allowed investment types
- See blocked insurance types
- View role expiry information

### Settings Page
- **Access**: Always visible to all users
- Default assumptions (inflation rate, returns)
- Personal info (name, age, retirement age)
- Update calculation parameters

### Preferences Page
- **Access**: Restricted by default (admin must enable)
- Currency settings
- Country/region settings
- Theme preferences
- Notification preferences

---

## 🔧 Technical Features

### Backend
- **Spring Boot 3.2** - Modern Java framework
- **MongoDB** - NoSQL database
- **Spring Security** - OAuth2 + JWT authentication
- **Scheduled Tasks** - Automatic role expiry checks
- **RESTful APIs** - Clean API design
- **Code Quality Tools**:
  - JaCoCo (Code Coverage - 78%)
  - SpotBugs (Bug detection)
  - PMD (Code quality)
  - Checkstyle (Code style)

### Frontend
- **HTML5 + Tailwind CSS** - Modern, responsive UI
- **Chart.js** - Interactive charts
- **jsPDF** - Client-side PDF generation
- **Vanilla JavaScript** - No framework dependencies
- **localStorage** - Client-side session management

### Security
- Google OAuth2 authentication
- JWT token-based sessions
- CORS configuration
- Input validation
- SecureRandom for simulations

---

## 📱 Pages & Navigation

### Public Pages
- **Landing Page** (`landing.html`) - Marketing page
- **Login Page** (`login.html`) - Google OAuth login
- **Product Page** (`product.html`) - Product details
- **Features Page** (`features.html`) - Feature showcase
- **Pricing Page** (`pricing.html`) - Pricing information
- **About Page** (`about.html`) - About Retyrment

### Authenticated Pages
- **Dashboard** (`index.html`) - Financial overview
- **Income** (`income.html`) - Income tracking
- **Investments** (`investments.html`) - Investment portfolio
- **Loans** (`loans.html`) - Loan management
- **Insurance** (`insurance.html`) - Insurance policies
- **Expenses** (`expenses.html`) - Expense tracking
- **Goals** (`goals.html`) - Goal planning
- **Calendar** (`calendar.html`) - Financial calendar
- **Retirement** (`retirement.html`) - Retirement planning
- **Simulation** (`simulation.html`) - Monte Carlo simulation
- **Reports** (`reports.html`) - Reports and exports
- **Admin Panel** (`admin.html`) - User and feature management
- **Preferences** (`preferences.html`) - User preferences
- **Settings** (`settings.html`) - Application settings
- **My Account** (`account.html`) - Account and subscription info

---

## 🎨 User Experience Features

### Navigation
- Sidebar navigation with feature-based visibility
- Active page highlighting
- Role badges (FREE/PRO/ADMIN)
- User profile display
- Logout functionality

### Data Entry
- Modal-based forms
- Inline editing
- Validation and error handling
- Success/error toast notifications
- Amount in words conversion (for amount fields)

### Visualizations
- Interactive charts (Chart.js)
- Hover tooltips
- Legend toggles
- Responsive design
- Color-coded indicators

### Responsive Design
- Tailwind CSS for styling
- Mobile-friendly layouts (desktop-optimized)
- Consistent design language
- Accessible color schemes

---

## 🔄 Data Flow

### Authentication Flow
1. User clicks "Sign in with Google"
2. Redirects to Google OAuth
3. Backend creates/updates user
4. Generates JWT token
5. Redirects to frontend with token
6. Frontend stores token and fetches user data
7. Feature access loaded from API

### Feature Access Flow
1. User logs in
2. Frontend calls `/api/auth/features`
3. Backend returns feature access (with per-user overrides)
4. Frontend applies restrictions:
   - Hides navigation items
   - Disables buttons
   - Checks page access
   - Applies type restrictions

### Data Entry Flow
1. User fills form
2. Frontend validates input
3. API call with JWT token
4. Backend validates and saves
5. Frontend updates UI
6. Toast notification shown

---

## 📊 Calculations & Projections

### Retirement Corpus Calculation
- Includes: PPF, EPF, Mutual Funds, FD, RD, Stocks, NPS, Cash
- Excludes: Gold, Real Estate, Crypto (illiquid/volatile)
- Accounts for:
  - Starting balances
  - Monthly/yearly contributions
  - Interest/return rates
  - Rate reduction over time (configurable)
  - Maturity inflows
  - Goal outflows

### Income Strategies
- **Simple Depletion**: Corpus divided by years
- **4% Safe Withdrawal**: 4% of corpus annually
- **Sustainable**: Corpus grows while providing income

### Monte Carlo Simulation
- Uses historical return distributions
- Accounts for volatility
- Provides percentile-based outcomes
- Uses SecureRandom for randomness

---

## 🚀 Deployment

### Backend
- Spring Boot JAR file
- MongoDB connection
- Environment variables for configuration
- Profile-based configuration (local/dev/prod)

### Frontend
- Static HTML/CSS/JS files
- Deployable to any web server
- No build process required
- CDN-based dependencies

---

## 📈 Code Quality Metrics

### Test Coverage
- **Overall Coverage**: 78%
- **Instructions**: 78%
- **Branches**: 52%
- **Lines**: 81%
- **Methods**: 89%
- **Classes**: 95%

### Test Status
- **Total Tests**: 311
- **Passing**: 311
- **Failures**: 0
- **Errors**: 0

### Code Quality Tools
- SpotBugs: All critical issues resolved
- PMD: Code quality checks passing
- Checkstyle: Code style compliance

---

## 🔮 Future Enhancements (Planned)

- Mobile app support
- Cross-platform investment aggregation
- Real-time market data integration
- Automated goal funding recommendations
- Email report delivery
- Cloud sync across devices
- Multi-currency support
- Tax planning features
- Estate planning tools

---

## 📞 Support & Contact

- **Email**: bansalitadvisory@gmail.com
- **Documentation**: See README.md, USAGE.md, REQUIREMENTS.md
- **Code Coverage Report**: `backend/target/site/jacoco/index.html`

---

**Retyrment** - Plan Your Financial Future with Confidence
