# Retyrment

A comprehensive personal finance planning application built with Spring Boot and modern JavaScript.

![Retyrment](https://img.shields.io/badge/Retyrment-Personal%20Finance-6366f1)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![MongoDB](https://img.shields.io/badge/MongoDB-6.0-green)
![Chart.js](https://img.shields.io/badge/Chart.js-4.x-pink)

## 🌟 Features

### 🚨 High Priority Alerts Dashboard
- **Corpus Gap Analysis** - Instant visibility into retirement shortfall
- **Maturity Alerts** - FD, RD, PPF, insurance maturities available for reinvestment
- **Post-Loan Investment** - Track when EMIs end and funds become available
- **Savings Breakdown** - Emergency fund, corpus, and goal allocation guidance
- **Illiquid Asset Scenarios** - What-if analysis for selling Gold/Real Estate

### 📈 Complete Investment Tracking
Track all asset classes with proper corpus contribution:

| Asset Type | Corpus Contribution | Notes |
|------------|-------------------|-------|
| PPF | ✅ Yes | With yearly contributions |
| EPF | ✅ Yes | With monthly contributions |
| Mutual Funds | ✅ Yes | With SIP + step-up |
| NPS | ✅ Yes | With monthly contributions |
| FD (Fixed Deposit) | ✅ Yes | Interest rate growth |
| RD (Recurring Deposit) | ✅ Yes | Monthly contributions |
| Stocks | ✅ Yes | Market-linked returns |
| Cash/Savings | ✅ Yes | Included in starting balance |
| Gold | ❌ No | Illiquid - shown in net worth only |
| Real Estate | ❌ No | Illiquid - shown in net worth only |
| Crypto | ❌ No | Volatile - shown in net worth only |

### 🏖️ Smart Retirement Planning
- **Retirement Matrix** with PPF+EPF combined, MF, Other Liquid columns
- **Inflows Tracking** - Insurance maturities, FD/RD/PPF maturities in the timeline
- **Goal Outflows** - Visualize when goals impact your corpus
- **Rate Reduction** - PPF/EPF/FD rates decrease over time (configurable)
- **Income Strategies** - Simple Depletion, 4% Rule, Sustainable withdrawal
- **GAP Analysis** - Required vs projected with color-coded indicators

### 💰 Maturity Tracking (Before Retirement)
- Track FD, RD, PPF maturity dates
- Insurance policy maturities (ULIP, Endowment, Money-Back)
- Expected maturity values with projections
- "Available for Reinvestment" summary

### 🔐 User Management & Access Control
- **Google OAuth2** - Secure authentication
- **7-Day PRO Trial** - New users get PRO features free for 7 days
- **Time-Limited Roles** - Admins can grant temporary access with expiry dates
- **Automatic Role Expiry** - System automatically reverts expired roles
- **Per-User Feature Access** - Granular control over page visibility and feature access
- **Investment Type Restrictions** - Control which investment types users can add
- **Insurance Type Restrictions** - Block specific insurance types per user
- **Page & Tab Visibility** - Show/hide pages and tabs based on user permissions
- **Report Access Control** - Restrict PDF/Excel/JSON export and import per user

### Data Management
- 💰 **Income Tracking** - Multiple sources with annual increments
- 🏦 **Loan Management** - EMI tracking, post-loan investment opportunities
- 🛡️ **Insurance Policies** - Term, Health, ULIP, Endowment, Annuity, Money-Back
- 🛒 **Expense Tracking** - Fixed and variable categories
- 🎯 **Goal Planning** - With funding status and GAP analysis

### Reports & Export (PRO/Admin Controlled)
- 📑 **PDF Reports** - Financial summary, retirement matrix, calendar (admin-controlled access)
- 📊 **Excel Export** - Retirement matrix spreadsheet (admin-controlled access)
- 💾 **JSON Backup** - Full data backup and restore (admin-controlled access)
- 📥 **Data Import** - Restore from JSON backup (admin-controlled access)

### 🎲 Monte Carlo Simulation (PRO/Admin Controlled)
- Probabilistic financial projections
- Configurable simulation parameters (number of runs, projection years)
- Percentile-based outcome analysis (10th, 25th, 50th, 75th, 90th)
- Visual distribution charts

### 👤 My Account Page
- View subscription status (trial, PRO subscription)
- Check feature access permissions
- See allowed investment types and blocked insurance types
- View role expiry information

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 16+ (for frontend dev server)
- MongoDB 6.0+ (or Docker)

### 1. Start MongoDB
```bash
# Using Docker (recommended)
docker run -d -p 27017:27017 --name mongodb mongo:6.0

# Or native MongoDB
mongod --dbpath /data/db
```

### 2. Start Backend
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Backend runs at: `http://localhost:8080/api`

### 3. Start Frontend
```bash
cd frontend
npx http-server -p 5000 -c-1
```
Frontend runs at: `http://localhost:5000`

### 4. Open Browser
- Landing page: http://localhost:5000/landing.html
- Login: http://localhost:5000/login.html

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ HTML5    │ │ Tailwind │ │ Chart.js │ │ jsPDF    │       │
│  │ Pages    │ │ CSS      │ │ Charts   │ │ Reports  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│                           │                                  │
│                    REST API (JWT Auth)                       │
└─────────────────────────────┼───────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────┐
│                        Backend                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ Spring   │ │ Spring   │ │ OAuth2   │ │ Scheduled│       │
│  │ Boot 3   │ │ Security │ │ + JWT    │ │ Tasks    │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│                           │                                  │
│                       MongoDB                                │
└─────────────────────────────────────────────────────────────┘
```

## 📊 Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Runtime |
| Spring Boot | 3.2.1 | Framework |
| Spring Security | 6.x | Authentication |
| Spring Scheduler | - | Role expiry tasks |
| MongoDB | 6.0+ | Database |
| Apache POI | 5.x | Excel export |
| Lombok | 1.18+ | Boilerplate reduction |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| HTML5 | - | Structure |
| Tailwind CSS | 3.x | Styling |
| Chart.js | 4.x | Visualizations |
| jsPDF | 2.5.x | PDF generation |
| html2canvas | 1.4.x | Screenshot capture |
| Vanilla JS | ES6+ | Logic |

## 👥 User Roles & Access Control

| Role | Features | Duration |
|------|----------|----------|
| **FREE** | Basic tracking, data entry, projections | Unlimited |
| **PRO** | Custom assumptions, PDF exports, recommendations, maturity tracking | 7-day trial → Time-limited subscription |
| **ADMIN** | User management, role assignment, feature access control, system config | Permanent |

### Trial & Time-Limited Access
- New users automatically get 7-day PRO trial
- Admins can extend trials or grant temporary PRO access
- PRO subscriptions are time-limited (not lifetime)
- System automatically reverts expired roles and subscriptions

### Feature Access Control System
Admins can control per-user access to:
- **Pages**: Income, Investment, Loan, Insurance, Expense, Goals, Calendar, Retirement, Reports, Simulation, Preferences, Settings, Account
- **Investment Types**: Allow/restrict specific types (MF, PPF, EPF, FD, RD, Real Estate, Stocks, NPS, Gold, Crypto, Cash)
- **Insurance Types**: Block specific types (Vehicle, Pension, Life Savings, etc.)
- **Retirement Tabs**: Control access to Strategy Planner tab
- **Reports**: Enable/disable PDF, Excel, JSON export and data import

**Default Access:**
- Income, Loan, Expense, Settings, Account: Always visible to all users
- Investment, Insurance, Goals, Retirement: Visible by default
- Calendar, Reports, Simulation, Preferences: Restricted by default (admin-enabled)
- Strategy Planner Tab: Restricted by default
- Investment Types: MF, PPF, EPF, FD, RD, Real Estate allowed by default
- Insurance Types: Vehicle, Pension, Life Savings blocked by default

## 📈 Key Features Detail

### High Priority Alerts
The dashboard shows actionable insights:
1. **Corpus Shortfall** - With specific amount needed
2. **Reinvestment Opportunities** - Maturing investments before retirement
3. **Illiquid Asset Analysis** - Would selling Gold/RE help meet corpus?
4. **Post-Loan Funds** - EMIs that will end and become investable
5. **Emergency Fund Gap** - Months needed to complete emergency fund
6. **At-Risk Goals** - Goals that are underfunded

### Retirement Matrix
Enhanced table showing:
- **PPF + EPF** (combined) with individual rates
- **Mutual Funds** with SIP amounts
- **Other Liquid** (FD + RD + Stocks + Cash) with hover breakdown
- **Inflows** - Insurance & investment maturities (green)
- **Goal Outflows** - Withdrawals for goals (red)
- **Net Corpus** - Final projected balance

### Charts
- **Total Corpus Growth** - Default view (clean, simple)
- **Individual Components** - Click legend to show PPF+EPF, MF, Other Liquid
- **SIP Contributions** - Toggle to see investment patterns

## ⚙️ Configuration

### Backend (application.yml)
```yaml
app:
  frontend-url: http://localhost:5000
  admin:
    emails: admin@example.com
  defaults:
    inflation-rate: 6.0
    epf-return: 8.15
    ppf-return: 7.1
    mf-equity-return: 12.0
```

### Frontend (js/api.js)
```javascript
const API_BASE = 'http://localhost:8080/api';
```

## 🧪 Testing

### Run All Tests
```bash
cd backend
mvn test
```

### Code Coverage
```bash
mvn jacoco:report
# View: target/site/jacoco/index.html
```

Coverage excludes model/security/exception/config packages (focus on service/controller).

## 📦 Production Deployment

### Backend
```bash
cd backend
mvn clean package -DskipTests
java -jar target/retyrment-1.0.0.jar --spring.profiles.active=prod
```

### Frontend
Deploy static files to any web server (Nginx, Apache, S3, etc.)

### Environment Variables (Production)
```
SPRING_DATA_MONGODB_URI=mongodb://user:pass@host:27017/retyrment
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
APP_FRONTEND_URL=https://yourdomain.com
APP_JWT_SECRET=your-256-bit-secret
```

## 📄 License

MIT License - see LICENSE file for details.

## 📞 Support

For questions or support, contact: bansalitadvisory@gmail.com
