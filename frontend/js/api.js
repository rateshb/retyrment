// Retyrment API Service with Authentication
const API_BASE = 'http://localhost:8080/api';

// Auth helper
const auth = {
    getToken: () => localStorage.getItem('retyrment_token'),
    setToken: (token) => localStorage.setItem('retyrment_token', token),
    removeToken: () => localStorage.removeItem('retyrment_token'),
    isLoggedIn: () => {
        const token = localStorage.getItem('retyrment_token');
        const user = localStorage.getItem('retyrment_user');
        // Must have both token AND user data to be considered logged in
        return !!(token && user);
    },
    
    getHeaders: () => {
        const token = auth.getToken();
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    },
    
    // Redirect to landing page if not authenticated
    requireAuth: () => {
        if (!auth.isLoggedIn()) {
            window.location.href = 'landing.html';
            return false;
        }
        return true;
    },
    
    logout: () => {
        auth.removeToken();
        localStorage.removeItem('retyrment_user');
        localStorage.removeItem('retyrment_features');
        window.location.href = 'landing.html';
    },
    
    getUser: () => {
        const userStr = localStorage.getItem('retyrment_user');
        return userStr ? JSON.parse(userStr) : null;
    },
    
    setUser: (user) => {
        localStorage.setItem('retyrment_user', JSON.stringify(user));
    },
    
    // Role helpers
    isPro: () => {
        const user = auth.getUser();
        if (!user) return false;
        // Pro if role is PRO or ADMIN, or if in active trial
        if (user.role === 'PRO' || user.role === 'ADMIN') return true;
        if (user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN') return true;
        if (user.trial?.active) return true;
        return user.isPro === true;
    },
    
    isAdmin: () => {
        const user = auth.getUser();
        return user && user.role === 'ADMIN';
    },
    
    // Check if user is in trial period
    isInTrial: () => {
        const user = auth.getUser();
        return user?.trial?.active === true;
    },
    
    // Get trial days remaining
    getTrialDaysRemaining: () => {
        const user = auth.getUser();
        return user?.trial?.daysRemaining || 0;
    },
    
    isFree: () => {
        const user = auth.getUser();
        return user && user.role === 'FREE';
    },
    
    // Feature access
    getFeatures: () => {
        const featuresStr = localStorage.getItem('retyrment_features');
        return featuresStr ? JSON.parse(featuresStr) : null;
    },
    
    setFeatures: (features) => {
        localStorage.setItem('retyrment_features', JSON.stringify(features));
    },
    
    canAccess: (feature) => {
        const features = auth.getFeatures();
        return features && features.features && features.features[feature] === true;
    }
};

const api = {
    // Generic CRUD methods with authentication
    async get(endpoint) {
        try {
            // Check if user is logged in
            if (!auth.isLoggedIn()) {
                console.warn(`GET ${endpoint} - User not logged in, redirecting to login`);
                window.location.href = 'login.html';
                throw new Error('Not authenticated');
            }
            
            const response = await fetch(`${API_BASE}${endpoint}`, {
                headers: auth.getHeaders(),
                credentials: 'include'
            });
            
            if (response.status === 401) {
                console.warn(`GET ${endpoint} - Unauthorized, logging out`);
                auth.logout();
                throw new Error('Session expired');
            }
            if (!response.ok) {
                const errorText = await response.text().catch(() => '');
                throw new Error(`HTTP ${response.status}: ${errorText || 'Unknown error'}`);
            }
            // Handle empty responses (204 No Content)
            if (response.status === 204 || response.headers.get('content-length') === '0') {
                return null;
            }
            const text = await response.text();
            return text ? JSON.parse(text) : null;
        } catch (error) {
            // Only log if it's not a redirect
            if (error.message !== 'Not authenticated') {
                console.error(`GET ${endpoint} failed:`, error);
            }
            throw error;
        }
    },

    async post(endpoint, data) {
        try {
            // Check if user is logged in
            if (!auth.isLoggedIn()) {
                console.warn(`POST ${endpoint} - User not logged in, redirecting to login`);
                window.location.href = 'login.html';
                throw new Error('Not authenticated');
            }
            
            const response = await fetch(`${API_BASE}${endpoint}`, {
                method: 'POST',
                headers: auth.getHeaders(),
                body: JSON.stringify(data),
                credentials: 'include'
            });
            
            if (response.status === 401) {
                console.warn(`POST ${endpoint} - Unauthorized, logging out`);
                auth.logout();
                throw new Error('Session expired');
            }
            if (!response.ok) {
                const errorText = await response.text().catch(() => '');
                throw new Error(`HTTP ${response.status}: ${errorText || 'Unknown error'}`);
            }
            // Handle empty responses (204 No Content)
            if (response.status === 204 || response.headers.get('content-length') === '0') {
                return null;
            }
            const text = await response.text();
            return text ? JSON.parse(text) : null;
        } catch (error) {
            // Only log if it's not a redirect
            if (error.message !== 'Not authenticated') {
                console.error(`POST ${endpoint} failed:`, error);
            }
            throw error;
        }
    },

    async put(endpoint, data) {
        try {
            const response = await fetch(`${API_BASE}${endpoint}`, {
                method: 'PUT',
                headers: auth.getHeaders(),
                body: JSON.stringify(data)
            });
            if (response.status === 401) {
                auth.logout();
                throw new Error('Session expired');
            }
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            // Handle empty responses (204 No Content)
            if (response.status === 204 || response.headers.get('content-length') === '0') {
                return null;
            }
            const text = await response.text();
            return text ? JSON.parse(text) : null;
        } catch (error) {
            console.error(`PUT ${endpoint} failed:`, error);
            throw error;
        }
    },

    async delete(endpoint) {
        try {
            const response = await fetch(`${API_BASE}${endpoint}`, { 
                method: 'DELETE',
                headers: auth.getHeaders()
            });
            if (response.status === 401) {
                auth.logout();
                throw new Error('Session expired');
            }
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            
            // Try to parse JSON response, return true if no body
            const text = await response.text();
            if (text) {
                try {
                    return JSON.parse(text);
                } catch {
                    return true;
                }
            }
            return true;
        } catch (error) {
            console.error(`DELETE ${endpoint} failed:`, error);
            throw error;
        }
    },

    // Income
    income: {
        getAll: () => api.get('/income'),
        getActive: () => api.get('/income/active'),
        create: (data) => api.post('/income', data),
        update: (id, data) => api.put(`/income/${id}`, data),
        delete: (id) => api.delete(`/income/${id}`)
    },

    // Investments
    investments: {
        getAll: () => api.get('/investments'),
        getByType: (type) => api.get(`/investments/type/${type}`),
        getSIPs: () => api.get('/investments/sips'),
        create: (data) => api.post('/investments', data),
        update: (id, data) => api.put(`/investments/${id}`, data),
        delete: (id) => api.delete(`/investments/${id}`)
    },

    // Loans
    loans: {
        getAll: () => api.get('/loans'),
        getActive: () => api.get('/loans/active'),
        getAmortization: (id) => api.get(`/loans/${id}/amortization`),
        create: (data) => api.post('/loans', data),
        update: (id, data) => api.put(`/loans/${id}`, data),
        delete: (id) => api.delete(`/loans/${id}`)
    },

    // Insurance
    insurance: {
        getAll: () => api.get('/insurance'),
        getByType: (type) => api.get(`/insurance/type/${type}`),
        getInvestmentLinked: () => api.get('/insurance/investment-linked'),
        getRecommendations: () => api.get('/insurance/recommendations'),
        create: (data) => api.post('/insurance', data),
        update: (id, data) => api.put(`/insurance/${id}`, data),
        delete: (id) => api.delete(`/insurance/${id}`)
    },
    
    // Family Members
    family: {
        getAll: () => api.get('/family'),
        getSummary: () => api.get('/family/summary'),
        getSelf: () => api.get('/family/self'),
        getSpouse: () => api.get('/family/spouse'),
        getDependents: () => api.get('/family/dependents'),
        create: (data) => api.post('/family', data),
        update: (id, data) => api.put(`/family/${id}`, data),
        delete: (id) => api.delete(`/family/${id}`)
    },

    // Expenses
    expenses: {
        getAll: () => api.get('/expenses'),
        getFixed: () => api.get('/expenses/fixed'),
        getVariable: () => api.get('/expenses/variable'),
        getTimeBound: () => api.get('/expenses/time-bound'),
        getRecurring: () => api.get('/expenses/recurring'),
        getEducation: () => api.get('/expenses/education'),
        getByCategory: (category) => api.get(`/expenses/category/${category}`),
        getByDependent: (name) => api.get(`/expenses/dependent/${encodeURIComponent(name)}`),
        getSummary: () => api.get('/expenses/summary'),
        getInvestmentOpportunities: (currentAge = 35, retirementAge = 60) => 
            api.get(`/expenses/investment-opportunities?currentAge=${currentAge}&retirementAge=${retirementAge}`),
        getProjection: (currentAge = 35, retirementAge = 60, inflationRate = 6) => 
            api.get(`/expenses/projection?currentAge=${currentAge}&retirementAge=${retirementAge}&inflationRate=${inflationRate}`),
        getDependents: () => api.get('/expenses/dependents'),
        getFrequencyOptions: () => api.get('/expenses/options/frequencies'),
        getCategoryOptions: () => api.get('/expenses/options/categories'),
        create: (data) => api.post('/expenses', data),
        update: (id, data) => api.put(`/expenses/${id}`, data),
        delete: (id) => api.delete(`/expenses/${id}`)
    },

    // Goals
    goals: {
        getAll: () => api.get('/goals'),
        getRecurring: () => api.get('/goals/recurring'),
        create: (data) => api.post('/goals', data),
        update: (id, data) => api.put(`/goals/${id}`, data),
        delete: (id) => api.delete(`/goals/${id}`)
    },

    // Calendar
    calendar: {
        getYear: () => api.get('/calendar'),
        getMonth: (month) => api.get(`/calendar/month/${month}`),
        getUpcoming: () => api.get('/calendar/upcoming'),
        createEntry: (data) => api.post('/calendar', data),
        updateEntry: (id, data) => api.put(`/calendar/${id}`, data),
        deleteEntry: (id) => api.delete(`/calendar/${id}`)
    },

    // Retirement
    retirement: {
        getMatrix: () => api.get('/retirement/matrix'),
        calculate: (scenario) => api.post('/retirement/calculate', scenario),
        getScenarios: () => api.get('/retirement/scenarios'),
        getDefaultScenario: () => api.get('/retirement/scenarios/default'),
        saveScenario: (data) => api.post('/retirement/scenarios', data),
        deleteScenario: (id) => api.delete(`/retirement/scenarios/${id}`),
        getMaturingInvestments: (currentAge = 35, retirementAge = 60) => 
            api.get(`/retirement/maturing?currentAge=${currentAge}&retirementAge=${retirementAge}`),
        // User strategy
        getStrategy: () => api.get('/retirement/strategy'),
        saveStrategy: (data) => api.post('/retirement/strategy', data),
        deleteStrategy: () => api.delete('/retirement/strategy'),
        // Withdrawal strategy
        getWithdrawalStrategy: (currentAge = 35, retirementAge = 60, lifeExpectancy = 85) =>
            api.get(`/retirement/withdrawal-strategy?currentAge=${currentAge}&retirementAge=${retirementAge}&lifeExpectancy=${lifeExpectancy}`)
    },

    // Analysis
    analysis: {
        getNetWorth: () => api.get('/analysis/networth'),
        getProjections: (years = 10) => api.get(`/analysis/projection?years=${years}`),
        getGoalAnalysis: () => api.get('/analysis/goals'),
        getRecommendations: () => api.get('/analysis/recommendations'),
        runMonteCarlo: (sims = 1000, years = 10) => 
            api.get(`/analysis/montecarlo?simulations=${sims}&years=${years}`),
        getSummary: () => api.get('/analysis/summary')
    },

    // Export
    export: {
        json: () => api.get('/export/json'),
        importJson: (data) => api.post('/export/import/json', data),
        pdf: () => `${API_BASE}/export/pdf?token=${auth.getToken()}`,
        excel: () => `${API_BASE}/export/excel?token=${auth.getToken()}`
    },
    
    // Admin
    admin: {
        getUsers: () => api.get('/admin/users'),
        getUser: (id) => api.get(`/admin/users/${id}`),
        // updateRole now accepts: { role, durationDays?, reason? }
        updateRole: (id, data) => {
            // Support both old format (string) and new format (object)
            const body = typeof data === 'string' ? { role: data } : data;
            return api.put(`/admin/users/${id}/role`, body);
        },
        deleteUser: (id) => api.delete(`/admin/users/${id}`),
        searchUsers: (email) => api.get(`/admin/users/search?email=${email}`),
        extendTrial: (id, days) => api.put(`/admin/users/${id}/extend-trial`, { days }),
        removeRoleExpiry: (id) => api.delete(`/admin/users/${id}/role-expiry`),
        checkExpiredRoles: () => api.post('/admin/roles/check-expired', {}),
        // Feature access management
        getFeatureAccess: (id) => api.get(`/admin/users/${id}/features`),
        updateUserFeatures: (id, features) => api.put(`/admin/users/${id}/features`, features)
    },
    
    // Auth endpoints
    auth: {
        me: () => api.get('/auth/me'),
        features: () => api.get('/auth/features'),
        validate: (token) => api.post('/auth/validate', { token })
    },
    
    // User Preferences
    preferences: {
        get: () => api.get('/preferences'),
        update: (data) => api.put('/preferences', data),
        getOptions: () => api.get('/preferences/options')
    },
    
    // Insurance Recommendations
    insuranceRecommendations: {
        getHealth: (currentAge, retirementAge) => api.get(`/insurance/recommendations/health?currentAge=${currentAge}&retirementAge=${retirementAge}`),
        getTerm: (currentAge, retirementAge) => api.get(`/insurance/recommendations/term?currentAge=${currentAge}&retirementAge=${retirementAge}`)
    },
    
    // User Data Management
    userData: {
        getSummary: () => api.get('/user/data/summary'),
        deleteAll: (confirmation) => api.delete(`/user/data/all?confirmation=${confirmation}`)
    }
};

// Format currency in Indian format
function formatCurrency(amount, short = false) {
    if (!amount || isNaN(amount)) return '₹0';
    const absAmount = Math.abs(amount);
    let formatted;
    if (short) {
        if (absAmount >= 10000000) {
            formatted = (absAmount / 10000000).toFixed(2) + ' Cr';
        } else if (absAmount >= 100000) {
            formatted = (absAmount / 100000).toFixed(2) + ' L';
        } else if (absAmount >= 1000) {
            formatted = (absAmount / 1000).toFixed(1) + ' K';
        } else {
            formatted = absAmount.toFixed(0);
        }
    } else {
        formatted = absAmount.toLocaleString('en-IN');
    }
    return (amount < 0 ? '-₹' : '₹') + formatted;
}

// Format percentage
function formatPercent(value) {
    if (!value || isNaN(value)) return '0%';
    return value.toFixed(1) + '%';
}

// Format date
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

// Get today's date in YYYY-MM-DD format
function getTodayDate() {
    return new Date().toISOString().split('T')[0];
}

// Convert number to Indian words
function numberToWords(num) {
    if (!num || isNaN(num) || num === 0) return '';
    
    const ones = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
                  'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 
                  'Seventeen', 'Eighteen', 'Nineteen'];
    const tens = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];
    
    const numValue = Math.abs(Math.floor(num));
    
    if (numValue === 0) return 'Zero Rupees';
    
    function convertLessThanHundred(n) {
        if (n < 20) return ones[n];
        return tens[Math.floor(n / 10)] + (n % 10 ? ' ' + ones[n % 10] : '');
    }
    
    function convertLessThanThousand(n) {
        if (n < 100) return convertLessThanHundred(n);
        return ones[Math.floor(n / 100)] + ' Hundred' + (n % 100 ? ' ' + convertLessThanHundred(n % 100) : '');
    }
    
    let result = '';
    
    // Crores (1,00,00,000)
    if (numValue >= 10000000) {
        result += convertLessThanThousand(Math.floor(numValue / 10000000)) + ' Crore ';
    }
    
    // Lakhs (1,00,000)
    const lakhs = Math.floor((numValue % 10000000) / 100000);
    if (lakhs > 0) {
        result += convertLessThanHundred(lakhs) + ' Lakh ';
    }
    
    // Thousands (1,000)
    const thousands = Math.floor((numValue % 100000) / 1000);
    if (thousands > 0) {
        result += convertLessThanHundred(thousands) + ' Thousand ';
    }
    
    // Hundreds and below
    const remainder = numValue % 1000;
    if (remainder > 0) {
        result += convertLessThanThousand(remainder);
    }
    
    result = result.trim() + ' Rupees';
    return num < 0 ? 'Minus ' + result : result;
}
