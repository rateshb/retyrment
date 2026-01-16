# WealthVision - User Guide

## Table of Contents
1. [Getting Started](#getting-started)
2. [Authentication](#authentication)
3. [Dashboard Overview](#dashboard-overview)
4. [Data Entry Pages](#data-entry-pages)
5. [Analysis & Planning](#analysis--planning)
6. [Reports & Export](#reports--export)
7. [Account Management](#account-management)
8. [Admin Features](#admin-features)

---

## Getting Started

### First Time Setup

1. **Visit the Landing Page**
   - Navigate to `http://localhost:5000/landing.html`
   - Review features and pricing

2. **Sign In with Google**
   - Click "Get Started Free" or "Sign In"
   - You'll be redirected to Google OAuth
   - After authentication, you'll automatically receive a 7-day PRO trial

3. **Explore the Dashboard**
   - Your dashboard shows net worth, projections, and high-priority alerts
   - Use the sidebar navigation to access different pages

---

## Authentication

### Login Process
1. Click "Sign In with Google" on the login page
2. Authorize WealthVision to access your Google account
3. You'll be redirected back with an authentication token
4. Your session is stored in browser localStorage

### Logout
- Click the "Logout" button in the sidebar (bottom section)
- This clears your session and redirects to the landing page

### Session Management
- Tokens are stored in browser localStorage
- Sessions persist across browser restarts
- Clear browser data to fully log out

---

## Dashboard Overview

The dashboard (`index.html`) provides a comprehensive financial overview:

### Key Sections

1. **Net Worth Summary**
   - Total assets, liabilities, and net worth
   - Asset allocation breakdown
   - Growth trends

2. **High Priority Alerts** (Replaces 10-year projection)
   - **Corpus Gap**: Retirement shortfall amount
   - **Reinvestment Opportunities**: Maturing investments before retirement
   - **Post-Loan Investment**: EMIs ending and funds becoming available
   - **Savings Breakdown**: Emergency fund, corpus, and goal allocation
   - **Illiquid Asset Analysis**: Would selling Gold/Real Estate help meet corpus?

3. **Monthly Savings Analysis**
   - Breakdown of how monthly savings can be allocated
   - Emergency fund requirements
   - Corpus contribution recommendations
   - Goal funding suggestions

4. **Quick Actions**
   - Add new investment, income, expense, etc.
   - Access to key pages

---

## Data Entry Pages

### 💰 Income Page
**Access**: Always visible to all users

**Features:**
- Add multiple income sources (salary, freelance, rental, etc.)
- Set monthly amount and annual increment percentage
- Mark income as active/inactive
- View total monthly income

**Fields:**
- Source Name
- Monthly Amount (₹)
- Annual Increment (%)
- Start Date
- Active Status

---

### 📈 Investment Page
**Access**: Visible by default (admin can restrict)

**Investment Types Available:**
- **Default Allowed**: Mutual Funds, PPF, EPF, FD, RD, Real Estate
- **Admin-Controlled**: Stocks, NPS, Gold, Crypto, Cash

**Features:**
- Track all investment types
- Add SIP details for mutual funds
- Set maturity dates for FD/RD/PPF
- View current value and expected returns
- Track corpus contribution

**Important Notes:**
- Only investment types allowed by admin are available in the dropdown
- Restricted types won't appear in the form
- All investments contribute to retirement corpus (except Gold, Real Estate, Crypto)

---

### 🏦 Loan Page
**Access**: Always visible to all users

**Features:**
- Add home loans, personal loans, car loans, etc.
- Track EMI, interest rate, tenure
- View amortization schedule
- See post-loan investment opportunities (when EMI ends)

**Fields:**
- Loan Type
- Principal Amount
- Interest Rate (%)
- Tenure (months)
- Start Date
- EMI Amount

---

### 🛡️ Insurance Page
**Access**: Visible by default (admin can restrict)

**Insurance Types:**
- **Available by Default**: Health, Term Life, ULIP, Endowment, Money-Back
- **Blocked by Default**: Vehicle, Pension, Life Savings (admin can unblock)

**Features:**
- Track all insurance policies
- Set premium frequency (monthly, quarterly, yearly)
- Track maturity dates for ULIP, Endowment, Money-Back
- View sum assured and fund value

**Important Notes:**
- Blocked insurance types won't appear in the dropdown
- Admin can customize which types are blocked per user

---

### 🛒 Expense Page
**Access**: Always visible to all users

**Features:**
- Track fixed expenses (rent, EMI, insurance premiums)
- Track variable expenses (groceries, utilities, entertainment)
- Categorize expenses
- View monthly expense breakdown

**Fields:**
- Description
- Category
- Amount (monthly)
- Type (Fixed/Variable)
- Start Date

---

### 🎯 Goals Page
**Access**: Visible by default (admin can restrict)

**Features:**
- Set financial goals (education, marriage, house, etc.)
- Set target amount and target year
- Track funding status
- View GAP analysis
- Set priority levels

**Fields:**
- Goal Name
- Target Amount (₹)
- Target Year
- Priority (High/Medium/Low)
- Recurrence (One-time/Recurring)

---

## Analysis & Planning

### 📅 Calendar Page
**Access**: Restricted by default (admin must enable)

**Features:**
- Year-round view of all financial commitments
- Monthly and yearly outflow visualization
- Track SIPs, insurance premiums, EMIs, goals
- Manual calendar entries for one-time payments

**View Options:**
- Full year calendar
- Monthly calendar
- Upcoming payments list

---

### 🏖️ Retirement Page
**Access**: Visible by default (admin can restrict)

**Tabs:**
1. **Retirement Matrix** (Always visible)
   - Year-by-year corpus projection
   - PPF+EPF combined column with individual ROIs
   - Mutual Funds column
   - Other Liquid column (FD, RD, Stocks, Cash) with hover breakdown
   - Inflows from maturing investments
   - Goal outflows
   - Net corpus calculation

2. **Strategy Planner** (Restricted by default, admin-enabled)
   - Analyze reinvestment opportunities
   - Illiquid asset sale scenarios
   - Post-loan investment planning
   - Save "What-If" strategies
   - View strategy impact on corpus

**Features:**
- Adjust retirement assumptions (age, inflation, returns)
- Select income strategy (Simple Depletion, 4% Rule, Sustainable)
- View GAP analysis
- See maturing investments before retirement
- Corpus growth charts

---

### 🎲 Monte Carlo Simulation Page
**Access**: Restricted by default (admin must enable)

**Features:**
- Run probabilistic financial projections
- Configure number of simulations (100-10,000)
- Set projection years (1-50)
- View percentile-based outcomes:
  - 10th percentile (worst case)
  - 25th percentile
  - 50th percentile (median)
  - 75th percentile
  - 90th percentile (best case)
- Visual distribution charts

**Usage:**
1. Set simulation parameters
2. Click "Run Simulation"
3. Review results and charts
4. Use for risk assessment and planning

---

### 📑 Reports Page
**Access**: Restricted by default (admin must enable)

**Available Reports:**
1. **Financial Summary PDF**
   - Net worth overview
   - Income sources breakdown
   - Investment portfolio with gains
   - Active loans summary
   - Insurance policies
   - Monthly expenses breakdown
   - Goal progress

2. **Retirement Report PDF**
   - Current asset breakdown
   - Maturing investments & inflows
   - Year-by-year projection matrix
   - GAP analysis
   - Income strategy details

3. **Calendar Report PDF**
   - 12-month outflow grid
   - Category breakdown
   - Top categories by amount
   - Monthly and yearly totals

4. **Excel Export**
   - Complete retirement matrix
   - All financial data in spreadsheet format

5. **JSON Backup**
   - Full data export for backup
   - Can be imported later to restore data

**Export Access:**
- PDF Export: Admin-controlled
- Excel Export: Admin-controlled
- JSON Export: Admin-controlled
- Data Import: Admin-controlled

---

## Account Management

### 👤 My Account Page
**Access**: Always visible to all users

**Sections:**

1. **Profile Information**
   - Name, Email
   - Account Role (FREE/PRO/ADMIN)
   - Effective Role (considering trial/subscription)

2. **Subscription Status**
   - Trial period (if active)
   - PRO subscription (if active)
   - Days remaining
   - Expiry dates

3. **Feature Access**
   - List of all pages and their access status
   - Shows which features are enabled/disabled

4. **Investment Types**
   - Shows which investment types you can add
   - Indicates allowed vs. restricted types

5. **Insurance Restrictions**
   - Shows which insurance types are blocked
   - Lists restricted types

---

### ⚙️ Preferences Page
**Access**: Restricted by default (admin must enable)

**Features:**
- Currency settings
- Country/region settings
- Theme preferences
- Notification preferences

---

### 🔧 Settings Page
**Access**: Always visible to all users

**Features:**
- Default assumptions (inflation rate, returns)
- Personal info (name, age, retirement age)
- View and update default calculation parameters

---

## Admin Features

### 👑 Admin Panel
**Access**: Admin users only

**Features:**

1. **User Management**
   - View all users
   - Search users by email
   - Change user roles (FREE/PRO/ADMIN)
   - Set time-limited role changes
   - Extend trial periods
   - Delete users

2. **Feature Access Management**
   - Control page visibility per user
   - Set allowed investment types
   - Set blocked insurance types
   - Enable/disable retirement tabs
   - Control report access (PDF/Excel/JSON/Import)

3. **Statistics Dashboard**
   - Total users count
   - Users by role (FREE/PRO/ADMIN)
   - Active trials count
   - Temporary roles count
   - Expiring roles (next 7 days)

4. **Role Management**
   - Grant temporary PRO access with expiry dates
   - Extend existing subscriptions
   - Remove role expiry (make permanent)
   - Force check and revert expired roles

---

## Feature Access Control

### How It Works

1. **Default Access**: New users get default access based on their role
   - FREE users: Basic pages only
   - PRO users: Most features enabled
   - ADMIN users: Full access

2. **Admin Overrides**: Admins can customize access for any user
   - Go to Admin Panel → Users → Click "Features" button
   - Toggle page visibility
   - Set investment/insurance restrictions
   - Control report access

3. **Page Visibility Rules**:
   - If any tab on a page is visible, the page becomes visible
   - Navigation items are automatically hidden for restricted pages
   - Direct URL access to restricted pages redirects to dashboard

4. **Investment Type Control**:
   - Default allowed: MF, PPF, EPF, FD, RD, Real Estate
   - Admin can add/remove types per user
   - Restricted types won't appear in dropdowns

5. **Insurance Type Control**:
   - Default blocked: Vehicle, Pension, Life Savings
   - Admin can block/unblock types per user
   - Blocked types won't appear in dropdowns

---

## Best Practices

### Data Entry
1. **Start with Income**: Enter all income sources first
2. **Add Investments**: Track all investments to get accurate corpus
3. **Set Goals**: Define your financial goals early
4. **Update Regularly**: Keep data current for accurate projections

### Retirement Planning
1. **Review Assumptions**: Adjust inflation and return rates based on your risk profile
2. **Check GAP Analysis**: Regularly review corpus shortfall
3. **Use Strategy Planner**: Explore different scenarios to meet corpus requirements
4. **Monitor Inflows**: Track maturing investments for reinvestment opportunities

### Reports
1. **Regular Backups**: Export JSON backup monthly
2. **PDF Reports**: Generate for financial reviews
3. **Excel Analysis**: Use for detailed spreadsheet analysis

---

## Troubleshooting

### Can't Access a Page
- Check if admin has enabled access for your account
- Visit "My Account" page to see your feature access
- Contact admin if you need access to a restricted feature

### Investment Type Not Showing
- The investment type may be restricted by admin
- Check "My Account" page to see allowed investment types
- Contact admin to request access

### Reports Not Working
- Reports require admin-enabled access
- Check "My Account" page for report permissions
- Ensure you have PRO subscription or admin has enabled reports

### Logout Not Working
- Clear browser localStorage manually
- Close and reopen browser
- Clear browser cache and cookies

---

## Support

For questions or issues:
- Email: bansalitadvisory@gmail.com
- Check "My Account" page for your access permissions
- Contact admin for feature access requests

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl/Cmd + K` | Quick search (if implemented) |
| `Esc` | Close modals |
| `Enter` | Submit forms |

---

## Mobile Access

Currently, WealthVision is optimized for desktop browsers:
- Chrome (latest) ✅
- Firefox (latest) ✅
- Safari ❌
- Edge ❌
- Mobile browsers ❌

Mobile support is planned for future releases.
