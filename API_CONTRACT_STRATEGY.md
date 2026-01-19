# API Contract Strategy & Technology Recommendations

## Part 1: Preventing Frontend-Backend API Mismatches

### The Problem
We've encountered numerous issues:
- Enum mismatches (`STOCKS` vs `STOCK`, `HOME_LOAN` vs `HOME`)
- Field name discrepancies (`amount` vs `monthlyAmount`, `premium` vs `annualPremium`)
- Missing fields in frontend expectations
- HTTP response handling (204 vs 200)

### Solution 1: OpenAPI/Swagger Specification (Recommended)

**What it does**: Define your API contract in a single YAML/JSON file, then generate both server stubs and client code.

```yaml
# openapi.yaml
openapi: 3.0.3
info:
  title: Retyrment API
  version: 1.0.0

components:
  schemas:
    Expense:
      type: object
      required:
        - category
        - monthlyAmount
      properties:
        id:
          type: string
        category:
          $ref: '#/components/schemas/ExpenseCategory'
        monthlyAmount:
          type: number
        
    ExpenseCategory:
      type: string
      enum:
        - RENT
        - UTILITIES
        - GROCERIES
        - TRANSPORT
        - ENTERTAINMENT
        - SCHOOL_FEE
        - COLLEGE_FEE
        # ... all valid values
```

**Implementation for Retyrment**:

1. Add to `backend/pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

2. Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

3. Export OpenAPI spec and generate TypeScript types:
```bash
# Generate TypeScript interfaces from OpenAPI
npx openapi-typescript http://localhost:8080/v3/api-docs -o frontend/js/types/api.d.ts
```

### Solution 2: Contract Testing with Pact

**What it does**: Frontend writes "contracts" defining what it expects. Backend verifies it can fulfill those contracts.

```javascript
// frontend/tests/contracts/expense.pact.js
const { Pact } = require('@pact-foundation/pact');

describe('Expense API Contract', () => {
  it('should return expenses with correct structure', async () => {
    await provider.addInteraction({
      state: 'user has expenses',
      uponReceiving: 'a request for expenses',
      withRequest: {
        method: 'GET',
        path: '/api/expenses',
      },
      willRespondWith: {
        status: 200,
        body: eachLike({
          id: string(),
          category: term({ matcher: 'RENT|UTILITIES|GROCERIES', generate: 'RENT' }),
          monthlyAmount: decimal(),
          description: string(),
        }),
      },
    });
  });
});
```

```java
// Backend verifies the contract
@Provider("RetyrmentAPI")
@PactFolder("pacts")
public class ExpenseContractTest {
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
```

### Solution 3: Shared Type Definitions

Create a shared schema that both frontend and backend use:

```
retyrment/
├── shared/
│   └── schemas/
│       ├── expense.schema.json
│       ├── income.schema.json
│       └── enums.json
├── backend/
│   └── (generates Java classes from schemas)
└── frontend/
    └── (generates TypeScript types from schemas)
```

**enums.json**:
```json
{
  "ExpenseCategory": [
    "RENT", "UTILITIES", "GROCERIES", "TRANSPORT", 
    "ENTERTAINMENT", "SCHOOL_FEE", "COLLEGE_FEE",
    "TUITION", "COACHING", "BOOKS_SUPPLIES", "HOSTEL",
    "HEALTHCARE", "SHOPPING", "DINING", "TRAVEL",
    "SUBSCRIPTIONS", "CHILDCARE", "DAYCARE", 
    "ELDERLY_CARE", "MAINTENANCE", "SOCIETY_CHARGES",
    "INSURANCE_PREMIUM", "OTHER"
  ],
  "InsuranceType": [
    "TERM", "HEALTH", "VEHICLE", "HOME", "ULIP", "OTHER"
  ],
  "LoanType": [
    "HOME", "VEHICLE", "PERSONAL", "EDUCATION", "GOLD", "OTHER"
  ]
}
```

### Solution 4: Integration Tests (Quick Win)

Add API integration tests that run on every build:

```java
// backend/src/test/java/com/retyrment/integration/ApiContractTest.java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApiContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void expenseEndpoint_shouldReturnCorrectStructure() {
        ResponseEntity<List<Map<String, Object>>> response = 
            restTemplate.exchange("/api/expenses", GET, 
                withAuth(), new ParameterizedTypeReference<>() {});
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        if (!response.getBody().isEmpty()) {
            Map<String, Object> expense = response.getBody().get(0);
            // Verify expected fields exist
            assertThat(expense).containsKeys(
                "id", "category", "monthlyAmount", "description"
            );
            // Verify enum values are valid
            assertThat(expense.get("category").toString())
                .matches("RENT|UTILITIES|GROCERIES|...");
        }
    }
    
    @Test
    void incomeEndpoint_shouldReturnCorrectStructure() {
        // Similar test for income
    }
}
```

### Solution 5: API Versioning

Never break existing clients:

```java
// Version in URL
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseControllerV1 { }

@RestController  
@RequestMapping("/api/v2/expenses")
public class ExpenseControllerV2 { }

// Or version in header
@GetMapping(headers = "API-Version=2")
public List<ExpenseV2> getExpensesV2() { }
```

---

## Part 2: Technology Recommendations

### Current Stack Issues
| Component | Current | Problem |
|-----------|---------|---------|
| Frontend | Vanilla JS + HTML | No type safety, manual DOM manipulation, hard to maintain |
| State Management | Global variables | Race conditions, hard to debug |
| API Calls | Manual fetch | No type checking, error-prone |
| Styling | Custom CSS | Inconsistent, repetitive |

### Recommended Modern Stack

#### Option A: Full TypeScript Migration (Recommended)

```
┌─────────────────────────────────────────────────────────┐
│                    RECOMMENDED STACK                      │
├─────────────────────────────────────────────────────────┤
│  Frontend: React + TypeScript + TanStack Query           │
│  Backend: Spring Boot (keep current)                     │
│  Contract: OpenAPI + Generated Types                     │
│  Styling: Tailwind CSS or shadcn/ui                      │
│  State: Zustand or Redux Toolkit                         │
│  Build: Vite                                             │
└─────────────────────────────────────────────────────────┘
```

**Benefits**:
- **TypeScript**: Catch type mismatches at compile time
- **React**: Component-based, easier to maintain
- **TanStack Query**: Automatic caching, refetching, error handling
- **Generated Types**: API changes break the build, not production

**Example with generated types**:
```typescript
// Generated from OpenAPI
interface Expense {
  id: string;
  category: ExpenseCategory;
  monthlyAmount: number;
  description?: string;
}

type ExpenseCategory = 
  | 'RENT' 
  | 'UTILITIES' 
  | 'GROCERIES' 
  // ... all values from backend

// Usage - TypeScript catches errors!
const expense: Expense = {
  category: 'EDUCATION', // ❌ Error: Type '"EDUCATION"' is not assignable
  monthlyAmount: 1000,
};
```

#### Option B: Keep Vanilla JS, Add TypeScript Checking

Minimal change - add JSDoc types:

```javascript
// frontend/js/types.js
/**
 * @typedef {'RENT'|'UTILITIES'|'GROCERIES'|'TRANSPORT'} ExpenseCategory
 */

/**
 * @typedef {Object} Expense
 * @property {string} id
 * @property {ExpenseCategory} category
 * @property {number} monthlyAmount
 */

// frontend/js/expenses.js
/** @type {Expense[]} */
let expenses = [];

// IDE will now show errors for wrong types!
```

Add `jsconfig.json`:
```json
{
  "compilerOptions": {
    "checkJs": true,
    "strict": true
  },
  "include": ["frontend/js/**/*"]
}
```

#### Option C: GraphQL (If Building from Scratch)

```graphql
# Single source of truth for API
type Expense {
  id: ID!
  category: ExpenseCategory!
  monthlyAmount: Float!
  description: String
}

enum ExpenseCategory {
  RENT
  UTILITIES
  GROCERIES
  # All values defined here
}

type Query {
  expenses: [Expense!]!
  expense(id: ID!): Expense
}
```

**Benefits**:
- Frontend requests exactly what it needs
- Strong typing built-in
- Auto-generated TypeScript types
- No over-fetching or under-fetching

### Quick Wins You Can Implement Now

#### 1. Add Enum Validation Endpoint
```java
@RestController
@RequestMapping("/api/meta")
public class MetaController {
    
    @GetMapping("/enums")
    public Map<String, List<String>> getEnums() {
        return Map.of(
            "expenseCategories", Arrays.stream(ExpenseCategory.values())
                .map(Enum::name).toList(),
            "insuranceTypes", Arrays.stream(InsuranceType.values())
                .map(Enum::name).toList(),
            "loanTypes", Arrays.stream(LoanType.values())
                .map(Enum::name).toList()
        );
    }
}
```

```javascript
// frontend/js/enums.js
let ENUMS = {};

async function loadEnums() {
    ENUMS = await api.get('/meta/enums');
}

function populateDropdown(selectId, enumKey) {
    const select = document.getElementById(selectId);
    ENUMS[enumKey].forEach(value => {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = formatEnumLabel(value);
        select.appendChild(option);
    });
}
```

#### 2. Add Response DTOs with Validation
```java
@Data
public class ExpenseDTO {
    @NotNull
    private String id;
    
    @NotNull
    private ExpenseCategory category;
    
    @NotNull
    @Positive
    private Double monthlyAmount;
    
    private String description;
}
```

#### 3. Add API Documentation Comments
```java
@Operation(summary = "Get all expenses", 
           description = "Returns expenses for the authenticated user")
@ApiResponse(responseCode = "200", 
             content = @Content(schema = @Schema(implementation = ExpenseDTO.class)))
@GetMapping
public List<ExpenseDTO> getExpenses() { }
```

---

## Implementation Roadmap

### Phase 1: Immediate (This Week)
1. ✅ Add SpringDoc OpenAPI to backend
2. ✅ Create `/api/meta/enums` endpoint
3. ✅ Update frontend to fetch enums dynamically
4. ✅ Add integration tests for API structure

### Phase 2: Short-term (1-2 Weeks)
1. Generate TypeScript types from OpenAPI
2. Add JSDoc types to existing JavaScript
3. Enable TypeScript checking in IDE
4. Add contract tests for critical endpoints

### Phase 3: Medium-term (1-2 Months)
1. Migrate frontend to TypeScript
2. Consider React migration for complex pages
3. Implement TanStack Query for API calls
4. Add automated contract testing in CI/CD

### Phase 4: Long-term (Optional)
1. Consider GraphQL for complex data needs
2. Implement BFF (Backend for Frontend) pattern
3. Add real-time updates with WebSockets

---

## Summary

| Approach | Effort | Impact | Recommendation |
|----------|--------|--------|----------------|
| OpenAPI + SpringDoc | Low | High | ✅ Do First |
| Dynamic Enum Loading | Low | Medium | ✅ Do First |
| Integration Tests | Medium | High | ✅ Priority |
| TypeScript Migration | High | Very High | ✅ Plan for it |
| Contract Testing (Pact) | Medium | High | Consider |
| GraphQL | Very High | High | Only if rebuilding |

**Bottom Line**: Start with OpenAPI documentation + generated types. This gives you:
1. Auto-generated API docs
2. Type-safe client code
3. Breaking changes caught at build time
4. Single source of truth for API contracts
