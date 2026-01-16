# WealthVision Backend

A Spring Boot backend for the WealthVision personal finance planning application.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.1**
- **MongoDB** (NoSQL Database)
- **Maven** (Build Tool)
- **Lombok** (Reduce Boilerplate)
- **Apache POI** (Excel Export)
- **iText** (PDF Generation)

## Prerequisites

1. **Java 17+** installed
2. **MongoDB** running locally or accessible remotely
3. **Maven 3.6+** (or use the included `mvnw` wrapper)

## Quick Start

### 1. Start MongoDB

Make sure MongoDB is running on `localhost:27017` (default).

```bash
# Windows
mongod --dbpath C:\data\db

# Linux/Mac
mongod --dbpath /data/db
```

### 2. Run the Application

**Windows:**
```bash
cd scripts
run-local.bat
```

**Linux/Mac:**
```bash
cd scripts
chmod +x run-local.sh
./run-local.sh
```

**Or using Maven directly:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Access the API

- Base URL: `http://localhost:8080/api`
- Example: `http://localhost:8080/api/investments`

## Environment Profiles

| Profile | Config File | Usage |
|---------|-------------|-------|
| `local` | `application-local.yml` | Local development |
| `dev` | `application-dev.yml` | Development server |
| `prod` | `application-prod.yml` | Production |

### Environment Variables (for dev/prod)

```bash
export MONGO_URI=mongodb://your-server:27017/wealthvision
export MONGO_DATABASE=wealthvision_prod
```

## API Endpoints

### Income
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/income` | Get all income sources |
| POST | `/api/income` | Add new income |
| PUT | `/api/income/{id}` | Update income |
| DELETE | `/api/income/{id}` | Delete income |

### Investments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/investments` | Get all investments |
| GET | `/api/investments/type/{type}` | Get by type |
| GET | `/api/investments/sips` | Get investments with SIP |
| POST | `/api/investments` | Add investment |
| PUT | `/api/investments/{id}` | Update investment |
| DELETE | `/api/investments/{id}` | Delete investment |

### Loans
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/loans` | Get all loans |
| GET | `/api/loans/{id}/amortization` | Get amortization schedule |
| POST | `/api/loans` | Add loan |
| PUT | `/api/loans/{id}` | Update loan |
| DELETE | `/api/loans/{id}` | Delete loan |

### Insurance
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/insurance` | Get all policies |
| GET | `/api/insurance/investment-linked` | Get ULIP/Endowment policies |
| POST | `/api/insurance` | Add policy |
| PUT | `/api/insurance/{id}` | Update policy |
| DELETE | `/api/insurance/{id}` | Delete policy |

### Expenses
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/expenses` | Get all expenses |
| GET | `/api/expenses/fixed` | Get fixed expenses |
| POST | `/api/expenses` | Add expense |
| PUT | `/api/expenses/{id}` | Update expense |
| DELETE | `/api/expenses/{id}` | Delete expense |

### Goals
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/goals` | Get all goals (sorted by year) |
| POST | `/api/goals` | Add goal |
| PUT | `/api/goals/{id}` | Update goal |
| DELETE | `/api/goals/{id}` | Delete goal |

### Calendar
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/calendar` | Get full year calendar |
| GET | `/api/calendar/month/{month}` | Get specific month |
| GET | `/api/calendar/upcoming` | Get next 30 days |
| POST | `/api/calendar` | Add manual entry |

### Retirement
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/retirement/matrix` | Get retirement matrix |
| POST | `/api/retirement/calculate` | Calculate with custom scenario |
| GET | `/api/retirement/scenarios` | Get saved scenarios |
| POST | `/api/retirement/scenarios` | Save scenario |

### Analysis
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analysis/networth` | Get current net worth |
| GET | `/api/analysis/projection?years=10` | Get projections |
| GET | `/api/analysis/goals` | Get goal analysis |
| GET | `/api/analysis/recommendations` | Get recommendations |
| GET | `/api/analysis/montecarlo` | Run Monte Carlo simulation |
| GET | `/api/analysis/summary` | Get full summary |

## Project Structure

```
backend/
├── src/main/java/com/wealthvision/
│   ├── WealthVisionApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   └── MongoConfig.java
│   ├── controller/
│   │   ├── IncomeController.java
│   │   ├── InvestmentController.java
│   │   ├── LoanController.java
│   │   ├── InsuranceController.java
│   │   ├── ExpenseController.java
│   │   ├── GoalController.java
│   │   ├── CalendarController.java
│   │   ├── RetirementController.java
│   │   └── AnalysisController.java
│   ├── model/
│   │   ├── Income.java
│   │   ├── Investment.java
│   │   ├── Loan.java
│   │   ├── Insurance.java
│   │   ├── Expense.java
│   │   ├── Goal.java
│   │   ├── CalendarEntry.java
│   │   ├── RetirementScenario.java
│   │   └── Settings.java
│   ├── repository/
│   │   └── *Repository.java
│   └── service/
│       ├── CalculationService.java
│       ├── AnalysisService.java
│       ├── CalendarService.java
│       └── RetirementService.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-dev.yml
│   └── application-prod.yml
├── scripts/
│   ├── run-local.bat / run-local.sh
│   ├── run-dev.bat / run-dev.sh
│   └── run-prod.bat / run-prod.sh
└── pom.xml
```

## Building for Production

```bash
mvn clean package -DskipTests
```

This creates `target/wealthvision-1.0.0.jar`

## Running in Production

```bash
java -Xms512m -Xmx1024m \
     -jar target/wealthvision-1.0.0.jar \
     --spring.profiles.active=prod
```

## Default Financial Assumptions

| Parameter | Default |
|-----------|---------|
| Inflation Rate | 6% |
| EPF Return | 8.15% |
| PPF Return | 7.1% |
| MF Equity Return | 12% |
| MF Debt Return | 7% |
| FD Return | 7% |
| Real Estate Return | 8% |
| Gold Return | 8% |
| Savings Return | 3.5% |

These can be configured in `application.yml` or overridden by user settings.

## License

MIT License
