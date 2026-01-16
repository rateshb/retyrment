// Retyrment Common Utilities

// Check authentication on page load
async function checkAuth() {
    // Skip auth check on public pages
    const publicPages = ['login.html', 'landing.html', 'about.html', 'features.html', 'pricing.html', 'product.html', 'privacy.html', 'terms.html', 'disclaimer.html'];
    const currentPath = window.location.pathname;
    
    console.log('[Auth] Current path:', currentPath);
    
    if (publicPages.some(page => currentPath.includes(page))) {
        console.log('[Auth] Public page, skipping auth check');
        return;
    }
    
    // Check for token in URL (OAuth callback)
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');
    if (tokenFromUrl) {
        console.log('[Auth] Token found in URL, length:', tokenFromUrl.length);
        auth.setToken(tokenFromUrl);
        
        // Fetch user data from API
        try {
            console.log('[Auth] Fetching user data from /auth/me...');
            const response = await fetch('http://localhost:8080/api/auth/me', {
                headers: { 'Authorization': `Bearer ${tokenFromUrl}` }
            });
            console.log('[Auth] Response status:', response.status);
            if (response.ok) {
                const user = await response.json();
                console.log('[Auth] User data received:', user);
                auth.setUser(user);
                console.log('[Auth] User saved, redirecting to clean URL...');
                // Redirect to clean URL (remove token from URL)
                window.location.replace('index.html');
                return;
            } else {
                const errorText = await response.text();
                console.error('[Auth] Auth API returned:', response.status, errorText);
                auth.removeToken();
                localStorage.removeItem('retyrment_user');
                localStorage.removeItem('retyrment_features');
            }
        } catch (error) {
            console.error('[Auth] Error fetching user data:', error);
            auth.removeToken();
            localStorage.removeItem('retyrment_user');
            localStorage.removeItem('retyrment_features');
        }
    }
    
    // Check if already logged in
    const token = localStorage.getItem('retyrment_token');
    const user = localStorage.getItem('retyrment_user');
    console.log('[Auth] Token exists:', !!token, 'User exists:', !!user);
    
    // If we have token but no user, or if we have both, validate with backend
    if (token) {
        try {
            // Validate token with backend
            const response = await fetch('http://localhost:8080/api/auth/me', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            
            if (response.ok) {
                const userData = await response.json();
                auth.setUser(userData);
                console.log('[Auth] Token validated, user authenticated:', userData.email);
                // Display user profile if user info exists
                displayUserProfile();
                return;
            } else {
                // Token is invalid or expired
                console.log('[Auth] Token validation failed, clearing auth data');
                auth.removeToken();
                localStorage.removeItem('retyrment_user');
                localStorage.removeItem('retyrment_features');
            }
        } catch (error) {
            console.error('[Auth] Error validating token:', error);
            // Network error - don't clear auth, might be temporary
            if (user) {
                console.log('[Auth] Network error but user data exists, continuing...');
                displayUserProfile();
                return;
            }
        }
    }
    
    // Redirect to landing page if not authenticated
    if (!auth.isLoggedIn()) {
        console.log('[Auth] Not logged in, redirecting to landing...');
        window.location.replace('landing.html');
        return;
    }
    
    console.log('[Auth] User authenticated:', auth.getUser()?.email);
    // Display user profile if user info exists
    displayUserProfile();
}

// Display user profile in sidebar
function displayUserProfile() {
    const user = auth.getUser();
    const settingsLink = document.querySelector('a[data-page="settings"]');
    
    // Don't add profile twice
    if (document.querySelector('.user-profile-container')) return;
    
    if (settingsLink && user) {
        // Add user profile above settings
        const profileHtml = `
            <div class="user-profile-container flex items-center gap-3 px-3 py-2 mb-2">
                <img src="${user.picture || ''}" alt="" class="w-8 h-8 rounded-full" onerror="this.style.display='none'">
                <div class="flex-1 min-w-0">
                    <div class="text-sm font-medium text-slate-700 truncate">${user.name || 'User'}</div>
                    <div class="text-xs text-slate-400 truncate user-profile-email">${user.email || ''}</div>
                </div>
            </div>
        `;
        settingsLink.insertAdjacentHTML('beforebegin', profileHtml);
        
        // Add admin link if admin
        if (user.role === 'ADMIN') {
            addAdminLink();
        }
        
        // Add account link for all users
        addAccountLink();
    }
    
    // Add logout button
    const settingsContainer = document.querySelector('a[data-page="settings"]')?.parentElement;
    if (settingsContainer && !document.querySelector('.logout-btn')) {
        const logoutBtn = document.createElement('button');
        logoutBtn.className = 'logout-btn flex items-center gap-3 px-3 py-2 text-slate-500 hover:text-danger-500 rounded w-full text-left';
        logoutBtn.innerHTML = '<span>🚪</span> Logout';
        logoutBtn.onclick = () => auth.logout();
        settingsContainer.appendChild(logoutBtn);
    }
}

// Fetch user info and features from token
async function fetchUserInfo() {
    if (!auth.isLoggedIn()) return;
    
    try {
        // Fetch user info
        const response = await fetch('http://localhost:8080/api/auth/me', {
            headers: auth.getHeaders()
        });
        
        if (response.ok) {
            const user = await response.json();
            auth.setUser(user);
            displayUserProfile();
            
            // Fetch features
            const featuresResponse = await fetch('http://localhost:8080/api/auth/features', {
                headers: auth.getHeaders()
            });
            if (featuresResponse.ok) {
                const features = await featuresResponse.json();
                auth.setFeatures(features);
                applyFeatureRestrictions();
            }
        } else if (response.status === 401) {
            auth.logout();
        }
    } catch (error) {
        console.error('Failed to fetch user info:', error);
    }
}

// Apply feature restrictions based on user role and feature access
async function applyFeatureRestrictions() {
    const user = auth.getUser();
    if (!user) return;
    
    // Add role badge to user profile (use effectiveRole)
    const effectiveRole = user.effectiveRole || user.role;
    updateRoleBadge(effectiveRole);
    
    // Load feature access from API
    try {
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        auth.setFeatures(featuresResponse);
        
        // Debug: Log feature access for troubleshooting
        console.log('[Feature Access] Loaded features:', features);
        
        // Hide/show navigation items based on feature flags
        const navMap = {
            'dashboard': true,  // Dashboard always accessible for all users
            'income': features.incomePage,
            'investments': features.investmentPage,
            'loans': features.loanPage,
            'insurance': features.insurancePage,
            'expenses': features.expensePage,
            'goals': features.goalsPage,
            'calendar': features.calendarPage,
            'retirement': features.retirementPage,
            'reports': features.reportsPage,
            'simulation': features.simulationPage,
            'admin': features.adminPanel,
            'preferences': features.preferencesPage,
            'settings': features.settingsPage,
            'account': features.accountPage
        };
        
        // First, ensure all dynamic links are added (admin, account)
        // This must happen before we hide/show items
        if (user.role === 'ADMIN') {
            addAdminLink();
        }
        addAccountLink();
        
        // Wait a moment for DOM to update, then apply restrictions
        // Use requestAnimationFrame to ensure DOM is ready
        requestAnimationFrame(() => {
        // First, ensure all dynamic links are added (admin, account)
        // This must happen before we hide/show items
        if (user.role === 'ADMIN') {
            addAdminLink();
        }
        addAccountLink();
        
        // Wait a moment for DOM to update, then apply restrictions
        // Use requestAnimationFrame to ensure DOM is ready
        requestAnimationFrame(() => {
            // Hide navigation items that user doesn't have access to
            // Only show if explicitly true, hide otherwise (more secure)
            const navItems = document.querySelectorAll('[data-page]');
            console.log(`[Feature Access] Found ${navItems.length} navigation items to check`);
            
            navItems.forEach(item => {
                const page = item.dataset.page;
                const hasAccess = navMap[page];
                
                // Hide if not explicitly true (handles false, undefined, null)
                if (hasAccess === true) {
                    item.style.display = '';
                    item.style.visibility = '';
                    item.classList.remove('hidden');
                    console.log(`[Feature Access] Showing navigation item: ${page}`);
                } else {
                    item.style.display = 'none';
                    item.style.visibility = 'hidden';
                    item.classList.add('hidden');
                    console.log(`[Feature Access] Hiding navigation item: ${page} (access: ${hasAccess})`);
                }
            });
        });
        });
        
        // Check current page access
        const currentPage = getCurrentPage();
        const pageAccessMap = {
            'index': true,  // Dashboard always accessible
            'dashboard': true,
            'income': features.incomePage,
            'investments': features.investmentPage,
            'loans': features.loanPage,
            'insurance': features.insurancePage,
            'expenses': features.expensePage,
            'goals': features.goalsPage,
            'calendar': features.calendarPage,
            'retirement': features.retirementPage,
            'reports': features.reportsPage,
            'simulation': features.simulationPage,
            'admin': features.adminPanel,
            'preferences': features.preferencesPage,
            'settings': features.settingsPage,
            'account': features.accountPage
        };
        
        const hasPageAccess = pageAccessMap[currentPage];
        if (hasPageAccess === false) {
            showToast('You do not have access to this page.', 'error');
            window.location.href = 'index.html';
            return;
        }
        
        // Hide/disable pro features for free users
        const isPro = user && (user.isPro || user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN');
        
        if (user.role === 'FREE' && !user.isPro) {
            // Add upgrade prompts
            document.querySelectorAll('[data-pro-feature]').forEach(el => {
                el.classList.add('pro-locked');
                el.setAttribute('title', 'Upgrade to Pro to access this feature');
            });
            
            // Disable pro buttons
            document.querySelectorAll('[data-requires-pro]').forEach(btn => {
                btn.disabled = true;
                btn.classList.add('opacity-50', 'cursor-not-allowed');
                btn.onclick = (e) => {
                    e.preventDefault();
                    showUpgradePrompt();
                };
            });
        }
        
        // Hide PRO badges for PRO/ADMIN users
        if (isPro) {
            document.querySelectorAll('.pro-badge').forEach(el => el.classList.add('hidden'));
        } else {
            document.querySelectorAll('.pro-badge').forEach(el => el.classList.remove('hidden'));
        }
        
        // Apply feature-specific restrictions
        applyFeatureSpecificRestrictions(features);
        
        // Apply retirement tab restrictions (needs to run after features are loaded)
        // This will be called on every page, but only affects retirement.html
        if (features.retirementStrategyPlannerTab !== true) {
            // Use both immediate and delayed check to ensure it's hidden
            const hideStrategyTab = () => {
                const strategyTab = document.getElementById('tab-strategy');
                const strategyPanel = document.getElementById('panel-strategy');
                if (strategyTab) {
                    strategyTab.style.display = 'none';
                    strategyTab.style.visibility = 'hidden';
                    strategyTab.classList.add('hidden');
                }
                if (strategyPanel) {
                    strategyPanel.style.display = 'none';
                    strategyPanel.style.visibility = 'hidden';
                    strategyPanel.classList.add('hidden');
                }
            };
            hideStrategyTab();
            // Also try after a short delay in case DOM isn't ready
            setTimeout(hideStrategyTab, 100);
            setTimeout(hideStrategyTab, 500);
        }
        
    } catch (error) {
        console.error('Error loading feature access:', error);
    }
    
    // Note: Admin and account links are now added before applying restrictions
    // This ensures they're included in the restriction check
}

// Apply feature-specific restrictions (investment types, insurance types, etc.)
function applyFeatureSpecificRestrictions(features) {
    // Investment type restrictions
    // Always set it, even if empty array (to distinguish from undefined)
    if (features.allowedInvestmentTypes !== undefined) {
        window.allowedInvestmentTypes = features.allowedInvestmentTypes;
    } else {
        // If not set, initialize to undefined (will show all types)
        window.allowedInvestmentTypes = undefined;
    }
    
    // Insurance type restrictions
    // Always set it, even if empty array (to distinguish from undefined)
    if (features.blockedInsuranceTypes !== undefined) {
        window.blockedInsuranceTypes = features.blockedInsuranceTypes;
    } else {
        // If not set, initialize to undefined (will show all types)
        window.blockedInsuranceTypes = undefined;
    }
    
    // Retirement tab restrictions
    if (features.retirementStrategyPlannerTab !== true) {
        // Hide Strategy Planner tab in retirement page (default is false/restricted)
        const strategyTab = document.getElementById('tab-strategy');
        const strategyPanel = document.getElementById('panel-strategy');
        if (strategyTab) {
            strategyTab.style.display = 'none';
        }
        if (strategyPanel) {
            strategyPanel.style.display = 'none';
        }
    }
    
    // Report export restrictions
    if (features.canExportPdf !== true) {
        const pdfButtons = document.querySelectorAll('[data-export="pdf"]');
        pdfButtons.forEach(btn => {
            btn.disabled = true;
            btn.classList.add('opacity-50', 'cursor-not-allowed');
            btn.onclick = () => showUpgradePrompt();
        });
    } else {
        const pdfButtons = document.querySelectorAll('[data-export="pdf"]');
        pdfButtons.forEach(btn => {
            btn.disabled = false;
            btn.classList.remove('opacity-50', 'cursor-not-allowed');
        });
    }
    
    if (features.canExportExcel !== true) {
        const excelButtons = document.querySelectorAll('[data-export="excel"]');
        excelButtons.forEach(btn => {
            btn.disabled = true;
            btn.classList.add('opacity-50', 'cursor-not-allowed');
            btn.onclick = () => showUpgradePrompt();
        });
    } else {
        const excelButtons = document.querySelectorAll('[data-export="excel"]');
        excelButtons.forEach(btn => {
            btn.disabled = false;
            btn.classList.remove('opacity-50', 'cursor-not-allowed');
        });
    }
    
    if (features.canExportJson !== true) {
        const jsonButtons = document.querySelectorAll('[data-export="json"]');
        jsonButtons.forEach(btn => {
            btn.disabled = true;
            btn.classList.add('opacity-50', 'cursor-not-allowed');
            btn.onclick = () => showUpgradePrompt();
        });
    } else {
        const jsonButtons = document.querySelectorAll('[data-export="json"]');
        jsonButtons.forEach(btn => {
            btn.disabled = false;
            btn.classList.remove('opacity-50', 'cursor-not-allowed');
        });
    }
    
    if (features.canImportData !== true) {
        const importButtons = document.querySelectorAll('[data-import]');
        importButtons.forEach(btn => {
            btn.disabled = true;
            btn.classList.add('opacity-50', 'cursor-not-allowed');
            btn.onclick = () => showUpgradePrompt();
        });
    } else {
        const importButtons = document.querySelectorAll('[data-import]');
        importButtons.forEach(btn => {
            btn.disabled = false;
            btn.classList.remove('opacity-50', 'cursor-not-allowed');
        });
    }
}

// Update role badge in sidebar
function updateRoleBadge(role) {
    const badges = {
        'FREE': '<span class="text-xs px-2 py-0.5 bg-slate-200 text-slate-600 rounded-full">Free</span>',
        'PRO': '<span class="text-xs px-2 py-0.5 bg-gradient-to-r from-amber-400 to-orange-500 text-white rounded-full font-medium">⭐ Pro</span>',
        'ADMIN': '<span class="text-xs px-2 py-0.5 bg-gradient-to-r from-purple-500 to-indigo-600 text-white rounded-full font-medium">👑 Admin</span>'
    };
    
    const userEmailEl = document.querySelector('.user-profile-email');
    if (userEmailEl) {
        // Remove existing badge if any
        const existingBadge = userEmailEl.nextElementSibling;
        if (existingBadge && existingBadge.classList.contains('role-badge-container')) {
            existingBadge.remove();
        }
        // Add new badge
        const badgeContainer = document.createElement('div');
        badgeContainer.className = 'mt-1 role-badge-container';
        badgeContainer.innerHTML = badges[role] || badges.FREE;
        userEmailEl.insertAdjacentElement('afterend', badgeContainer);
    }
}

// Add admin link to sidebar (only if not already added)
function addAdminLink() {
    // Check if admin link already exists
    if (document.querySelector('a[data-page="admin"]')) {
        return; // Already added, don't duplicate
    }
    
    const analysisSection = document.querySelector('a[data-page="reports"]');
    if (analysisSection) {
        const adminLink = document.createElement('a');
        adminLink.href = 'admin.html';
        adminLink.className = 'nav-item flex items-center gap-3 px-5 py-3 text-slate-600 border-l-2 border-transparent';
        adminLink.setAttribute('data-page', 'admin');
        adminLink.innerHTML = '<span>👑</span> Admin Panel';
        analysisSection.parentNode.insertBefore(adminLink, analysisSection.nextSibling);
    }
}

// Add account link to sidebar (only if not already added)
function addAccountLink() {
    // Check if account link already exists
    if (document.querySelector('a[data-page="account"]')) {
        return; // Already added, don't duplicate
    }
    
    const settingsLink = document.querySelector('a[data-page="settings"]');
    if (settingsLink && settingsLink.parentElement) {
        const accountLink = document.createElement('a');
        accountLink.href = 'account.html';
        accountLink.className = 'nav-item flex items-center gap-3 px-3 py-2 text-slate-500 hover:text-slate-700 rounded';
        accountLink.setAttribute('data-page', 'account');
        accountLink.innerHTML = '<span>👤</span> My Account';
        settingsLink.parentElement.insertBefore(accountLink, settingsLink);
    }
}

// Show upgrade prompt
function showUpgradePrompt() {
    const modal = document.createElement('div');
    modal.className = 'fixed inset-0 flex items-center justify-center z-50';
    modal.style.background = 'rgba(15, 23, 42, 0.5)';
    modal.innerHTML = `
        <div class="bg-white rounded-xl border border-slate-200 w-full max-w-md p-6 shadow-2xl">
            <div class="text-center">
                <div class="text-5xl mb-4">⭐</div>
                <h3 class="text-xl font-bold text-slate-800 mb-2">Upgrade to Pro</h3>
                <p class="text-slate-500 mb-6">Unlock advanced features like recommendations, retirement planning, and report exports.</p>
                <div class="space-y-3">
                    <div class="flex items-center gap-2 text-left text-sm text-slate-600">
                        <span class="text-success-500">✓</span> AI-powered recommendations
                    </div>
                    <div class="flex items-center gap-2 text-left text-sm text-slate-600">
                        <span class="text-success-500">✓</span> Retirement planning with adjustable variables
                    </div>
                    <div class="flex items-center gap-2 text-left text-sm text-slate-600">
                        <span class="text-success-500">✓</span> Download PDF & Excel reports
                    </div>
                    <div class="flex items-center gap-2 text-left text-sm text-slate-600">
                        <span class="text-success-500">✓</span> Monte Carlo simulations
                    </div>
                </div>
                <div class="mt-6 flex gap-3">
                    <button onclick="this.closest('.fixed').remove()" class="flex-1 px-4 py-2 rounded-lg border border-slate-300 text-slate-600">Maybe Later</button>
                    <button onclick="contactForPro()" class="flex-1 px-4 py-2 rounded-lg btn-primary">Contact Us</button>
                </div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function contactForPro() {
    window.open('mailto:bansalitadvisory@gmail.com?subject=Retyrment Pro Upgrade Request', '_blank');
}

// Logout function
async function logout() {
    try {
        const token = auth.getToken();
        if (token) {
            await api.post('/auth/logout', {});
        }
    } catch (error) {
        console.error('Logout error:', error);
    } finally {
        // Clear all auth data
        auth.removeToken();
        localStorage.removeItem('retyrment_user');
        localStorage.removeItem('retyrment_features');
        // Redirect to landing page
        window.location.href = 'landing.html';
    }
}

// Get current page name from URL
function getCurrentPage() {
    const path = window.location.pathname;
    const page = path.substring(path.lastIndexOf('/') + 1).replace('.html', '');
    return page || 'index';
}

// Highlight active nav item
function initNavigation() {
    const currentPage = getCurrentPage();
    const pageMap = {
        'index': 'dashboard',
        'dashboard': 'dashboard',
        'income': 'income',
        'investments': 'investments',
        'loans': 'loans',
        'insurance': 'insurance',
        'expenses': 'expenses',
        'goals': 'goals',
        'calendar': 'calendar',
        'retirement': 'retirement',
        'reports': 'reports',
        'settings': 'settings'
    };
    
    const activePage = pageMap[currentPage] || 'dashboard';
    
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === activePage) {
            item.classList.add('active');
        }
    });
}

// Modal Functions
let currentEditId = null;
let currentFormFields = [];

function openModal(title, fields, data = {}) {
    // Check if standard modal exists (some pages use custom modals)
    const modalTitle = document.getElementById('modal-title');
    const fieldsContainer = document.getElementById('modal-fields');
    if (!modalTitle || !fieldsContainer) {
        console.log('Standard modal not found, page may use custom modal');
        return;
    }
    
    modalTitle.textContent = title;
    fieldsContainer.innerHTML = fields.map(field => {
        let value = data[field.name] !== undefined ? data[field.name] : (field.value || '');
        
        // Default date fields to today
        if (field.type === 'date' && !value) {
            value = getTodayDate();
        }
        
        if (field.type === 'select') {
            const fieldHint = field.hint ? `<div class="text-xs text-slate-400 mt-1">${field.hint}</div>` : '';
            const requiredStar = field.required ? '<span class="text-danger-500 ml-1">*</span>' : '';
            return `
                <div class="mb-4">
                    <label class="block text-sm text-gray-600 mb-2 font-medium">${field.label}${requiredStar}</label>
                    <select name="${field.name}" ${field.required ? 'required' : ''} 
                            class="w-full px-4 py-2.5 bg-white border border-gray-300 rounded-lg text-gray-800 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 focus:outline-none transition-all">
                        ${field.options.map(opt => `<option value="${opt}" ${value === opt ? 'selected' : ''}>${opt.replace(/_/g, ' ')}</option>`).join('')}
                    </select>
                    ${fieldHint}
                </div>
            `;
        }
        
        if (field.type === 'checkbox') {
            const checked = data[field.name] !== undefined ? data[field.name] : field.checked;
            const fieldHint = field.hint ? `<div class="text-xs text-slate-400 mt-1 ml-8">${field.hint}</div>` : '';
            return `
                <div class="mb-4">
                    <div class="flex items-center gap-3">
                        <input type="checkbox" name="${field.name}" id="${field.name}" ${checked ? 'checked' : ''}
                               class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500">
                        <label for="${field.name}" class="text-sm text-gray-700">${field.label}</label>
                    </div>
                    ${fieldHint}
                </div>
            `;
        }
        
        // Check if this is an amount field (exclude day/date fields)
        const fieldNameLower = field.name.toLowerCase();
        const isDayField = fieldNameLower.includes('day') || fieldNameLower.includes('date') || 
                           fieldNameLower.includes('month') || fieldNameLower.includes('year') ||
                           fieldNameLower.includes('age') || fieldNameLower.includes('tenure');
        const isAmountField = field.type === 'number' && !isDayField &&
            (fieldNameLower.includes('amount') || 
             fieldNameLower.includes('value') || 
             fieldNameLower.includes('premium') ||
             fieldNameLower.includes('emi') ||
             fieldNameLower.includes('sip') ||
             fieldNameLower.includes('contribution') ||
             fieldNameLower.includes('assured') ||
             fieldNameLower.includes('benefit'));
        
        const amountHint = isAmountField ? `<div class="text-xs text-primary-600 mt-1.5 amount-words italic" data-for="${field.name}"></div>` : '';
        const fieldHint = field.hint ? `<div class="text-xs text-slate-400 mt-1">${field.hint}</div>` : '';
        const onInputHandler = isAmountField ? `oninput="updateAmountWords('${field.name}', this.value)"` : '';
        const requiredStar = field.required ? '<span class="text-danger-500 ml-1">*</span>' : '';
        
        return `
            <div class="mb-4">
                <label class="block text-sm text-gray-600 mb-2 font-medium">${field.label}${requiredStar}</label>
                <input type="${field.type}" name="${field.name}" value="${value}"
                       ${field.required ? 'required' : ''}
                       ${field.placeholder ? `placeholder="${field.placeholder}"` : ''}
                       ${field.min !== undefined ? `min="${field.min}"` : ''}
                       ${field.max !== undefined ? `max="${field.max}"` : ''}
                       ${field.step ? `step="${field.step}"` : ''}
                       ${field.maxlength ? `maxlength="${field.maxlength}"` : ''}
                       ${onInputHandler}
                       class="w-full px-4 py-2.5 bg-white border border-gray-300 rounded-lg text-gray-800 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 focus:outline-none transition-all">
                ${amountHint}
                ${fieldHint}
            </div>
        `;
    }).join('');
    
    // Initialize amount words for existing values
    fields.forEach(field => {
        const value = data[field.name];
        if (value && typeof updateAmountWords === 'function') {
            setTimeout(() => updateAmountWords(field.name, value), 0);
        }
    });

    const modalEl = document.getElementById('modal');
    if (modalEl) {
        modalEl.classList.remove('hidden');
        modalEl.classList.add('flex');
    }
}

function closeModal() {
    const modalEl = document.getElementById('modal');
    if (modalEl) {
        modalEl.classList.add('hidden');
        modalEl.classList.remove('flex');
    }
    currentEditId = null;
}

// Update amount words display
function updateAmountWords(fieldName, value) {
    const wordsDiv = document.querySelector(`.amount-words[data-for="${fieldName}"]`);
    if (wordsDiv) {
        const numValue = parseFloat(value);
        if (numValue && numValue > 0) {
            wordsDiv.textContent = '💡 ' + numberToWords(numValue);
            wordsDiv.classList.remove('hidden');
        } else {
            wordsDiv.textContent = '';
            wordsDiv.classList.add('hidden');
        }
    }
}

function openAddModal(entityType, fields) {
    currentEditId = null;
    currentFormFields = fields;
    openModal(`Add ${entityType}`, fields);
}

async function openEditModal(entityType, endpoint, id, fields) {
    currentEditId = id;
    currentFormFields = fields;
    
    try {
        const data = await api.get(`${endpoint}/${id}`);
        openModal(`Edit ${entityType}`, fields, data);
    } catch (error) {
        showToast('Error loading data', 'error');
    }
}

function getFormData(form) {
    const formData = new FormData(form);
    const data = {};
    
    for (let [key, value] of formData.entries()) {
        const input = form.querySelector(`[name="${key}"]`);
        if (input && input.type === 'checkbox') {
            data[key] = input.checked;
        } else if (input && input.type === 'number') {
            data[key] = value ? parseFloat(value) : null;
        } else {
            data[key] = value || null;
        }
    }
    
    // Handle unchecked checkboxes
    form.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        if (!formData.has(cb.name)) {
            data[cb.name] = false;
        }
    });
    
    return data;
}

// Toast Notifications
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    
    const icon = document.getElementById('toast-icon');
    const msg = document.getElementById('toast-message');
    
    icon.textContent = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    msg.textContent = message;
    
    toast.classList.remove('hidden');
    
    setTimeout(() => {
        toast.classList.add('hidden');
    }, 3000);
}

// Delete confirmation
async function confirmDelete(endpoint, id, onSuccess) {
    if (!confirm('Are you sure you want to delete this item?')) return;
    
    try {
        await api.delete(`${endpoint}/${id}`);
        showToast('Deleted successfully!', 'success');
        if (onSuccess) onSuccess();
    } catch (error) {
        showToast('Error deleting item', 'error');
    }
}

// Export functions
async function exportJSON() {
    // Check feature access
    try {
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        if (features.canExportJson !== true) {
            showUpgradePrompt();
            return;
        }
    } catch (error) {
        console.error('Error checking feature access:', error);
        showUpgradePrompt();
        return;
    }
    
    try {
        const data = await api.export.json();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `retyrment_backup_${new Date().toISOString().split('T')[0]}.json`;
        a.click();
        URL.revokeObjectURL(url);
        showToast('Data exported successfully!');
    } catch (error) {
        showToast('Export failed: ' + error.message, 'error');
    }
}

async function importJSON(event) {
    // Check feature access
    try {
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        if (features.canImportData !== true) {
            showUpgradePrompt();
            // Reset file input
            event.target.value = '';
            return;
        }
    } catch (error) {
        console.error('Error checking feature access:', error);
        showUpgradePrompt();
        event.target.value = '';
        return;
    }
    
    const file = event.target.files[0];
    if (!file) return;
    
    try {
        const text = await file.text();
        const data = JSON.parse(text);
        await api.export.importJson(data);
        showToast('Data imported successfully!');
        location.reload();
    } catch (error) {
        showToast('Import failed: ' + error.message, 'error');
    }
}

function downloadPDF() {
    window.open(api.export.pdf(), '_blank');
    showToast('Generating PDF...');
}

async function downloadExcel() {
    try {
        showToast('Generating Excel...');
        const response = await fetch(api.export.excel(), {
            headers: {
                'Authorization': `Bearer ${auth.getToken()}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to generate Excel');
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Retyrment_Report_${new Date().toISOString().split('T')[0]}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        showToast('Excel downloaded successfully!');
    } catch (error) {
        console.error('Excel download error:', error);
        showToast('Error downloading Excel: ' + error.message, 'error');
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth();
    
    // Apply feature restrictions after auth check
    // This ensures restrictions are applied even if checkAuth doesn't call it
    if (auth.isLoggedIn()) {
        // Wait a bit for DOM to be fully ready
        await new Promise(resolve => setTimeout(resolve, 100));
        await applyFeatureRestrictions();
    }
    
    // Initialize navigation highlighting after restrictions are applied
    initNavigation();
    
    // Fetch user info if logged in but no user data
    if (auth.isLoggedIn() && !auth.getUser()) {
        fetchUserInfo();
    }
});
