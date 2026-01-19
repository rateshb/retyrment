// Retyrment - Retirement Page with Inline Settings

// Current year for effective year dropdown
const CURRENT_YEAR = new Date().getFullYear();

// Default parameters
const defaultParams = {
    currentAge: 35,
    retirementAge: 60,
    lifeExpectancy: 85,
    inflation: 6,
    mfReturn: 12,
    epfReturn: 8.15,
    ppfReturn: 7.1,
    sipStepup: 10,
    effectiveFromYear: CURRENT_YEAR + 1,  // Next year (absolute)
    incomeStrategy: 'SUSTAINABLE',  // SUSTAINABLE, SAFE_4_PERCENT, SIMPLE_DEPLETION
    corpusReturnRate: 10,           // Return on corpus at retirement
    // Rate reduction settings (PPF/EPF rates decrease over time)
    enableRateReduction: true,
    rateReductionPercent: 0.5,      // Reduce by 0.5% every N years
    rateReductionYears: 5           // Apply reduction every 5 years
};

// Current parameters (will be loaded from localStorage)
let currentParams = { ...defaultParams };

// Cache for additional strategy data (used in Strategy Planner tab)
let strategyData = {
    netWorth: null,
    loans: [],
    goals: [],
    expenseOpportunities: [] // Investment opportunities from ending expenses
};

document.addEventListener('DOMContentLoaded', async () => {
    // Initialize tabs - ensure only income tab is visible on page load
    if (typeof window.switchAnalysisTab === 'function') {
        window.switchAnalysisTab('income');
    }
    
    initializeEffectiveYearDropdown();
    loadSavedParams();
    await applyRetirementFeatureRestrictions(); // Apply feature restrictions first
    loadRetirementData();
    applyProRestrictions();
    
    // Initialize collapsible matrix
    initializeCollapsibleMatrix();
});

// Initialize collapsible matrix functionality
function initializeCollapsibleMatrix() {
    const matrixHeader = document.getElementById('matrix-header');
    const matrixContent = document.getElementById('matrix-content');
    const matrixToggle = document.getElementById('matrix-toggle');
    
    if (matrixHeader && matrixContent && matrixToggle) {
        matrixHeader.addEventListener('click', () => {
            matrixContent.classList.toggle('hidden');
            matrixToggle.textContent = matrixContent.classList.contains('hidden') ? '▶' : '▼';
        });
    }
}

// Apply retirement page feature restrictions
async function applyRetirementFeatureRestrictions() {
    try {
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        
        // Hide Strategy Planner TAB if not enabled (default is false/restricted)
        // NOTE: Only control the tab button visibility, NOT the panel content
        // Panel visibility is controlled by switchAnalysisTab()
        const strategyTab = document.getElementById('tab-strategy');
        
        if (features.retirementStrategyPlannerTab !== true) {
            // Hide the tab button
            if (strategyTab) {
                strategyTab.style.display = 'none';
                strategyTab.classList.add('hidden');
            }
        } else {
            // Show the tab button (but panel stays hidden until tab is clicked)
            if (strategyTab) {
                strategyTab.style.display = '';
                strategyTab.classList.remove('hidden');
            }
        }
        
        // Hide Withdrawal Strategy TAB if not enabled (default is false/restricted)
        const withdrawalTab = document.getElementById('tab-withdrawal');
        
        if (features.retirementWithdrawalStrategyTab !== true) {
            // Hide the tab button
            if (withdrawalTab) {
                withdrawalTab.style.display = 'none';
                withdrawalTab.classList.add('hidden');
            }
        } else {
            // Show the tab button (but panel stays hidden until tab is clicked)
            if (withdrawalTab) {
                withdrawalTab.style.display = '';
                withdrawalTab.classList.remove('hidden');
            }
        }
    } catch (error) {
        console.error('Error applying retirement feature restrictions:', error);
    }
}

// Initialize effective year dropdown with next 10 years
function initializeEffectiveYearDropdown() {
    const select = document.getElementById('param-effective-year');
    if (!select) return;
    
    select.innerHTML = '';
    for (let i = 0; i <= 10; i++) {
        const year = CURRENT_YEAR + i;
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year + (i === 0 ? ' (Current)' : i === 1 ? ' (Next Year)' : ` (+${i} years)`);
        select.appendChild(option);
    }
    
    // Add change listener to update button state
    select.addEventListener('change', () => {
        const selectedYear = parseInt(select.value) || CURRENT_YEAR + 1;
        const relativeYear = selectedYear - CURRENT_YEAR;
        updateSaveButtonState(relativeYear);
        
        // Show analysis-only notice if year > current+1
        const notice = document.getElementById('analysis-only-notice');
        if (relativeYear > 1) {
            if (!notice) {
                const noticeDiv = document.createElement('div');
                noticeDiv.id = 'analysis-only-notice';
                noticeDiv.className = 'mt-2 p-2 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-700';
                noticeDiv.innerHTML = '⚠️ <strong>Analysis mode:</strong> Changes from year ' + selectedYear + ' onwards are for scenario analysis only and will not be saved.';
                select.parentElement.appendChild(noticeDiv);
            } else {
                notice.innerHTML = '⚠️ <strong>Analysis mode:</strong> Changes from year ' + selectedYear + ' onwards are for scenario analysis only and will not be saved.';
                notice.classList.remove('hidden');
            }
        } else if (notice) {
            notice.classList.add('hidden');
        }
    });
}

// Tab switching for analysis section
// Make sure it's in global scope for onclick handlers
window.switchAnalysisTab = function switchAnalysisTab(tab) {
    // Update tab buttons
    ['income', 'gap', 'expenses', 'maturing', 'ending-expenses', 'strategy', 'withdrawal'].forEach(t => {
        const tabBtn = document.getElementById(`tab-${t}`);
        const panel = document.getElementById(`panel-${t}`);
        if (!tabBtn || !panel) return;
        if (t === tab) {
            tabBtn.className = 'flex-1 px-6 py-3 text-sm font-medium text-primary-600 border-b-2 border-primary-500 bg-primary-50/50';
            panel.classList.remove('hidden');
        } else {
            tabBtn.className = 'flex-1 px-6 py-3 text-sm font-medium text-slate-500 hover:text-slate-700 border-b-2 border-transparent';
            panel.classList.add('hidden');
        }
    });
    
    // Load strategy planner data when tab is selected
    if (tab === 'strategy') {
        if (window.cachedRetirementData) {
            // Ensure strategy data is loaded before rendering
            loadStrategyDataAndRender(window.cachedRetirementData);
        } else {
            document.getElementById('strategy-allocation-breakdown').innerHTML = 
                '<div class="text-center py-4 text-slate-400">Loading data... Please wait.</div>';
        }
    }
    
    // Load ending expenses data when tab is selected
    if (tab === 'ending-expenses') {
        loadEndingExpensesData();
    }
    
    // Load withdrawal strategy data when tab is selected
    if (tab === 'withdrawal') {
        renderWithdrawalSchedule();
    }
};

// Ensure function is available globally for onclick handlers
if (typeof window !== 'undefined') {
    window.switchAnalysisTab = switchAnalysisTab;
}

// Load strategy data and then render
async function loadStrategyDataAndRender(retirementData) {
    console.log('loadStrategyDataAndRender called with:', retirementData);
    
    // Show loading state
    const breakdownEl = document.getElementById('strategy-allocation-breakdown');
    const timelineEl = document.getElementById('strategy-timeline');
    const scenariosEl = document.getElementById('strategy-scenarios');
    
    if (breakdownEl) breakdownEl.innerHTML = '<div class="text-center py-4 text-slate-400">Loading...</div>';
    if (timelineEl) timelineEl.innerHTML = '<div class="text-center py-4 text-slate-400">Loading...</div>';
    if (scenariosEl) scenariosEl.innerHTML = '<div class="text-center py-4 text-slate-400">Loading...</div>';
    
    try {
        // Load additional data if not already loaded
        if (!strategyData.netWorth) {
            console.log('Fetching netWorth, loans, goals, expenseOpportunities...');
            
            // Get current params for expense opportunities
            const params = getParamsFromInputs();
            const currentAge = params.currentAge || 35;
            const retirementAge = params.retirementAge || 60;
            
            const [netWorth, loans, goals, expenseOpportunitiesResponse] = await Promise.all([
                api.analysis.getNetWorth().catch(e => { console.error('getNetWorth error:', e); return { assetBreakdown: {} }; }),
                api.loans.getAll().catch(e => { console.error('getLoans error:', e); return []; }),
                api.goals.getAll().catch(e => { console.error('getGoals error:', e); return []; }),
                api.expenses.getInvestmentOpportunities(currentAge, retirementAge).catch(e => { console.error('getExpenseOpportunities error:', e); return {}; })
            ]);
            
            // API returns { freedUpByYear: [...], totalMonthlyFreedUpByRetirement: ..., etc }
            const expenseOpportunities = expenseOpportunitiesResponse?.freedUpByYear || [];
            
            console.log('Fetched data:', { netWorth, loans, goals, expenseOpportunities });
            strategyData.netWorth = netWorth || { assetBreakdown: {} };
            strategyData.loans = loans || [];
            strategyData.goals = goals || [];
            strategyData.expenseOpportunities = expenseOpportunities;
        }
        
        // Now render (even if some data is missing)
        console.log('Calling renderStrategyPlanner...');
        renderStrategyPlanner(retirementData);
        console.log('renderStrategyPlanner completed');
    } catch (error) {
        console.error('Error loading strategy data:', error);
        // Still try to render with available data
        try {
            renderStrategyPlanner(retirementData);
        } catch (renderError) {
            console.error('Error rendering strategy planner:', renderError);
            if (breakdownEl) breakdownEl.innerHTML = '<div class="text-center py-4 text-danger-500">Error loading data. Please refresh.</div>';
        }
    }
}

// Toggle chart visibility and render chart when expanded
function toggleChart() {
    const container = document.getElementById('chart-container');
    const icon = document.getElementById('chart-toggle-icon');
    container.classList.toggle('hidden');
    icon.textContent = container.classList.contains('hidden') ? '▼' : '▲';
    
    // If chart is now visible, render it with cached data
    if (!container.classList.contains('hidden')) {
        const cachedData = typeof getCachedRetirementData === 'function' ? getCachedRetirementData() : null;
        if (cachedData) {
            createRetirementChart('retirement-chart', cachedData);
        }
    }
}

// Select income strategy from card click
async function selectIncomeStrategy(strategy) {
    // Update the hidden select element if it exists (might be in settings panel)
    const strategySelect = document.getElementById('param-income-strategy');
    if (strategySelect) {
        strategySelect.value = strategy;
    }
    
    // Update current params
    currentParams.incomeStrategy = strategy;
    highlightSelectedStrategy(strategy);
    
    // Immediately recalculate to update income projection and GAP analysis
    showToast('Updating with ' + getStrategyName(strategy) + '...');
    await loadRetirementData();
    showToast('Updated!');
}

// Get friendly strategy name
function getStrategyName(strategy) {
    switch (strategy) {
        case 'SIMPLE_DEPLETION': return 'Simple Depletion';
        case 'SAFE_4_PERCENT': return '4% Safe Withdrawal';
        case 'SUSTAINABLE': return 'Sustainable Income';
        default: return strategy;
    }
}

// Apply pro restrictions
function applyProRestrictions() {
    const user = auth.getUser();
    const isPro = user && (user.role === 'PRO' || user.role === 'ADMIN');
    
    if (!isPro) {
        // Show PRO badges only for FREE users
        const user = auth.getUser();
        const isPro = user && (user.isPro || user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN');
        if (!isPro) {
            document.querySelectorAll('.pro-badge').forEach(el => el.classList.remove('hidden'));
        } else {
            document.querySelectorAll('.pro-badge').forEach(el => el.classList.add('hidden'));
        }
        
        // Hide settings panel for free users
        const settingsPanel = document.getElementById('settings-panel');
        if (settingsPanel) {
            settingsPanel.innerHTML = `
                <div class="card p-6 border-2 border-amber-200 bg-amber-50">
                    <div class="flex items-center gap-4">
                        <div class="text-4xl">⭐</div>
                        <div>
                            <h3 class="text-lg font-semibold text-amber-800">Pro Feature</h3>
                            <p class="text-sm text-amber-700">Upgrade to Pro to adjust retirement assumptions and see different scenarios.</p>
                        </div>
                        <button onclick="showUpgradePrompt()" class="ml-auto px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg font-medium">
                            Upgrade
                        </button>
                    </div>
                </div>
            `;
        }
    }
}

// Handle adjust settings button
function handleAdjustSettings() {
    const user = auth.getUser();
    if (user && (user.role === 'PRO' || user.role === 'ADMIN')) {
        toggleSettings();
    } else {
        showUpgradePrompt();
    }
}

// Handle export button
function handleExportExcel() {
    const user = auth.getUser();
    if (user && (user.role === 'PRO' || user.role === 'ADMIN')) {
        downloadExcel();
    } else {
        showUpgradePrompt();
    }
}

function loadSavedParams() {
    const saved = localStorage.getItem('retyrment_retirement_params');
    if (saved) {
        currentParams = { ...defaultParams, ...JSON.parse(saved) };
    }
    updateParamInputs();
}

function updateParamInputs() {
    document.getElementById('param-current-age').value = currentParams.currentAge;
    document.getElementById('param-retirement-age').value = currentParams.retirementAge;
    document.getElementById('param-life-expectancy').value = currentParams.lifeExpectancy;
    document.getElementById('param-inflation').value = currentParams.inflation;
    document.getElementById('param-mf-return').value = currentParams.mfReturn;
    document.getElementById('param-epf-return').value = currentParams.epfReturn;
    document.getElementById('param-ppf-return').value = currentParams.ppfReturn;
    document.getElementById('param-sip-stepup').value = currentParams.sipStepup;
    // effectiveFromYear is stored as relative offset, convert to absolute year for dropdown
    const effectiveAbsoluteYear = CURRENT_YEAR + (currentParams.effectiveFromYear || 1);
    document.getElementById('param-effective-year').value = effectiveAbsoluteYear;
    const strategySelect = document.getElementById('param-income-strategy');
    if (strategySelect) {
        strategySelect.value = currentParams.incomeStrategy;
    }
    document.getElementById('param-corpus-return').value = currentParams.corpusReturnRate;
    
    // Rate reduction settings
    const enableRateReduction = document.getElementById('param-enable-rate-reduction');
    if (enableRateReduction) enableRateReduction.checked = currentParams.enableRateReduction !== false;
    const rateReductionPercent = document.getElementById('param-rate-reduction-percent');
    if (rateReductionPercent) rateReductionPercent.value = currentParams.rateReductionPercent || 0.5;
    const rateReductionYears = document.getElementById('param-rate-reduction-years');
    if (rateReductionYears) rateReductionYears.value = currentParams.rateReductionYears || 5;
}

function getParamsFromInputs() {
    // Helper function to safely get element value
    const getValue = (id, defaultValue, parser = parseFloat) => {
        const el = document.getElementById(id);
        if (!el) return defaultValue;
        const value = parser(el.value);
        return isNaN(value) ? defaultValue : value;
    };
    
    const getIntValue = (id, defaultValue) => getValue(id, defaultValue, parseInt);
    
    // Get the selected absolute year and convert to relative year for backend
    const effectiveYearEl = document.getElementById('param-effective-year');
    const selectedYear = effectiveYearEl ? parseInt(effectiveYearEl.value) || CURRENT_YEAR + 1 : CURRENT_YEAR + 1;
    const relativeYear = selectedYear - CURRENT_YEAR; // Convert to offset from current year
    
    // Rate reduction settings
    const enableRateReductionEl = document.getElementById('param-enable-rate-reduction');
    const rateReductionPercentEl = document.getElementById('param-rate-reduction-percent');
    const rateReductionYearsEl = document.getElementById('param-rate-reduction-years');
    
    return {
        currentAge: getIntValue('param-current-age', currentParams.currentAge || defaultParams.currentAge),
        retirementAge: getIntValue('param-retirement-age', currentParams.retirementAge || defaultParams.retirementAge),
        lifeExpectancy: getIntValue('param-life-expectancy', currentParams.lifeExpectancy || defaultParams.lifeExpectancy),
        inflation: getValue('param-inflation', currentParams.inflation || defaultParams.inflation),
        mfReturn: getValue('param-mf-return', currentParams.mfReturn || defaultParams.mfReturn),
        epfReturn: getValue('param-epf-return', currentParams.epfReturn || defaultParams.epfReturn),
        ppfReturn: getValue('param-ppf-return', currentParams.ppfReturn || defaultParams.ppfReturn),
        sipStepup: getValue('param-sip-stepup', currentParams.sipStepup || defaultParams.sipStepup),
        effectiveFromYear: relativeYear, // Backend expects relative year (0 = current, 1 = next, etc.)
        incomeStrategy: (document.getElementById('param-income-strategy')?.value) || currentParams.incomeStrategy || defaultParams.incomeStrategy,
        corpusReturnRate: getValue('param-corpus-return', currentParams.corpusReturnRate || defaultParams.corpusReturnRate),
        // Rate reduction settings
        enableRateReduction: enableRateReductionEl ? enableRateReductionEl.checked : (currentParams.enableRateReduction !== undefined ? currentParams.enableRateReduction : defaultParams.enableRateReduction),
        rateReductionPercent: rateReductionPercentEl ? parseFloat(rateReductionPercentEl.value) : (currentParams.rateReductionPercent !== undefined ? currentParams.rateReductionPercent : defaultParams.rateReductionPercent),
        rateReductionYears: rateReductionYearsEl ? parseInt(rateReductionYearsEl.value) : (currentParams.rateReductionYears !== undefined ? currentParams.rateReductionYears : defaultParams.rateReductionYears)
    };
}

function toggleSettings() {
    const panel = document.getElementById('settings-panel');
    const wasHidden = panel.classList.contains('hidden');
    panel.classList.toggle('hidden');
    
    // If opening, scroll to it and focus first input
    if (wasHidden) {
        panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
        setTimeout(() => {
            const firstInput = panel.querySelector('input, select');
            if (firstInput) firstInput.focus();
        }, 300);
    }
}

function resetDefaults() {
    currentParams = { ...defaultParams };
    updateParamInputs();
    showToast('Reset to default values');
}

async function recalculate() {
    const params = getParamsFromInputs();
    const isAnalysisOnly = params.effectiveFromYear > 1;
    
    // Update currentParams for this calculation
    currentParams = params;
    
    // Only save to localStorage if effectiveFromYear is 0 or 1 (Current or Next Year)
    if (!isAnalysisOnly) {
        localStorage.setItem('retyrment_retirement_params', JSON.stringify(currentParams));
        showToast('Recalculating and saving parameters...');
    } else {
        showToast(`📊 Analysis mode (from ${CURRENT_YEAR + params.effectiveFromYear}) - not saving`);
    }
    
    await loadRetirementData();
    
    if (isAnalysisOnly) {
        showToast('📊 Analysis complete - settings not saved');
    } else {
        showToast('✅ Projection updated and saved!');
    }
    
    // Update save button visibility
    updateSaveButtonState(params.effectiveFromYear);
}

// Update the save/recalculate button based on effective year
function updateSaveButtonState(effectiveFromYear) {
    const recalculateBtn = document.querySelector('button[onclick="recalculate()"]');
    if (recalculateBtn) {
        if (effectiveFromYear > 1) {
            recalculateBtn.innerHTML = '🔄 Analyze (No Save)';
            recalculateBtn.classList.remove('btn-primary');
            recalculateBtn.classList.add('bg-amber-500', 'hover:bg-amber-600', 'text-white');
        } else {
            recalculateBtn.innerHTML = '🔄 Recalculate';
            recalculateBtn.classList.add('btn-primary');
            recalculateBtn.classList.remove('bg-amber-500', 'hover:bg-amber-600', 'text-white');
        }
    }
}

async function loadRetirementData() {
    try {
        // Build params from current state (including any UI changes)
        const params = getParamsFromInputs();
        
        // Merge with current params to preserve any unsaved changes
        const requestParams = { ...currentParams, ...params };
        
        // Send parameters to backend for calculation
        const data = await api.post('/retirement/calculate', requestParams);
        
        // Cache the data for strategy planner
        window.cachedRetirementData = data;
        
        // Load additional data for strategy planner
        loadStrategyData(data);
        
        // Render the matrix table
        renderRetirementMatrix(data);
        
        // Render GAP analysis
        renderGapAnalysis(data.gapAnalysis);
        
        // Render maturing investments before retirement
        renderMaturingInvestments(data.maturingBeforeRetirement);
        
        // Create/update the chart - always call to cache data
        // If chart is visible, it will render; if hidden, data is cached for when expanded
        createRetirementChart('retirement-chart', data);
        
        // Force chart refresh if it's visible
        const chartContainer = document.getElementById('chart-container');
        if (chartContainer && !chartContainer.classList.contains('hidden')) {
            // Chart is visible, ensure it's updated
            console.log('Chart visible, refreshing with new data');
        }
    } catch (error) {
        console.error('Error loading retirement data:', error);
        // Fallback to default matrix endpoint
        try {
            const data = await api.retirement.getMatrix();
            window.cachedRetirementData = data;
            renderRetirementMatrix(data);
            renderGapAnalysis(data.gapAnalysis);
            renderMaturingInvestments(data.maturingBeforeRetirement);
        } catch (e) {
            document.getElementById('matrix-body').innerHTML = `
                <tr><td colspan="9" class="px-4 py-8 text-center text-danger-500">Error loading data. Is the server running?</td></tr>
            `;
        }
    }
}

function renderGapAnalysis(gap) {
    if (!gap) return;
    
    // Update status badge in tab
    const statusBadge = document.getElementById('gap-status-badge');
    if (gap.isOnTrack) {
        statusBadge.className = 'ml-2 px-2 py-0.5 text-xs rounded-full bg-success-100 text-success-700';
        statusBadge.textContent = '✓';
    } else {
        statusBadge.className = 'ml-2 px-2 py-0.5 text-xs rounded-full bg-danger-100 text-danger-700';
        statusBadge.textContent = '⚠';
    }
    
    // Update corpus cards
    document.getElementById('gap-required-corpus').textContent = formatCurrency(gap.requiredCorpus, true);
    document.getElementById('gap-projected-corpus').textContent = formatCurrency(gap.projectedCorpus, true);
    
    // Update required note with strategy explanation
    const strategyNote = gap.strategyExplanation || 'Based on strategy';
    document.getElementById('gap-required-note').textContent = strategyNote;
    
    // Update gap card
    const gapCard = document.getElementById('gap-card');
    const gapAmount = document.getElementById('gap-amount');
    const gapPercent = document.getElementById('gap-percent');
    
    if (gap.corpusGap > 0) {
        gapCard.className = 'rounded-lg p-3 text-center bg-danger-50';
        gapAmount.className = 'text-lg font-bold text-danger-600';
        gapAmount.textContent = '-' + formatCurrency(gap.corpusGap, true);
        gapPercent.className = 'text-xs text-danger-500';
        gapPercent.textContent = `${gap.gapPercent}% short`;
    } else {
        gapCard.className = 'rounded-lg p-3 text-center bg-success-50';
        gapAmount.className = 'text-lg font-bold text-success-600';
        gapAmount.textContent = '+' + formatCurrency(Math.abs(gap.corpusGap), true);
        gapPercent.className = 'text-xs text-success-500';
        gapPercent.textContent = `${Math.abs(gap.gapPercent)}% surplus`;
    }
    
    // Render expense projection table with insurance breakdown
    const expenseBody = document.getElementById('expense-projection-body');
    const monthlyInsurance = gap.monthlyInsurancePremiums || 0;
    const householdExpenses = gap.currentMonthlyExpenses || 0;
    
    if (gap.expenseProjection && gap.expenseProjection.length > 0) {
        // Header showing breakdown
        let headerNote = '';
        if (monthlyInsurance > 0) {
            headerNote = `
            <tr class="bg-blue-50 text-xs border-b border-blue-200">
                <td colspan="3" class="px-2 py-2 text-blue-700">
                    <div class="flex justify-between items-center">
                        <span>💡 <strong>Expense Components:</strong></span>
                        <span>Household ₹${formatNumber(householdExpenses)} + Insurance ₹${formatNumber(monthlyInsurance)}/mo</span>
                    </div>
                </td>
            </tr>`;
        } else if (householdExpenses > 0) {
            // No insurance continuing after retirement
            headerNote = `
            <tr class="bg-amber-50 text-xs border-b border-amber-200">
                <td colspan="3" class="px-2 py-2 text-amber-700">
                    <div class="flex justify-between items-center">
                        <span>⚠️ Only household expenses. Add Personal Health Insurance for complete projection.</span>
                        <a href="insurance.html" class="underline text-amber-800">Add Insurance →</a>
                    </div>
                </td>
            </tr>`;
        }
        
        expenseBody.innerHTML = headerNote + gap.expenseProjection.map(proj => {
            const insurancePart = proj.insurancePremium || 0;
            const householdPart = proj.householdExpense || proj.monthlyExpense;
            
            return `
            <tr class="${proj.label ? 'bg-primary-50 font-semibold' : ''} hover:bg-slate-50">
                <td class="px-2 py-1.5 text-slate-700">
                    ${proj.label || `Year ${proj.year}`}
                </td>
                <td class="px-2 py-1.5 text-right">
                    <div class="font-mono text-slate-800">${formatCurrency(proj.monthlyExpense)}</div>
                    ${insurancePart > 0 ? `<div class="text-xs text-slate-500">(₹${formatNumber(householdPart)} + ₹${formatNumber(insurancePart)} ins)</div>` : ''}
                </td>
                <td class="px-2 py-1.5 text-right font-mono text-slate-700">${formatCurrency(proj.yearlyExpense)}</td>
            </tr>`;
        }).join('');
    } else {
        expenseBody.innerHTML = `
            <tr>
                <td colspan="3" class="px-2 py-4 text-center text-slate-400">
                    <div class="mb-2">No expenses recorded</div>
                    <a href="expenses.html" class="text-primary-600 text-sm underline">Add monthly expenses →</a>
                </td>
            </tr>`;
    }
    
    // Render suggestions (compact)
    const suggestionsDiv = document.getElementById('gap-suggestions');
    let suggestionsHTML = '';
    
    // Show continuing insurance if any
    if (gap.continuingInsurance && gap.continuingInsurance.length > 0) {
        suggestionsHTML += `<div class="mb-3 p-2 bg-blue-50 rounded-lg border border-blue-200">
            <div class="text-xs font-semibold text-blue-700 mb-1">🏥 Insurance Continuing After Retirement</div>
            <div class="text-xs text-blue-600 space-y-1">
                ${gap.continuingInsurance.map(ins => {
                    const typeLabel = ins.healthType === 'GROUP' ? '(Group)' : 
                                     ins.healthType === 'PERSONAL' ? '(Personal)' : 
                                     ins.healthType === 'FAMILY_FLOATER' ? '(Floater)' : '';
                    return `<div class="flex justify-between">
                        <span>${ins.name} ${typeLabel}</span>
                        <span class="font-mono">${formatCurrency(ins.annualPremium)}/yr</span>
                    </div>`;
                }).join('')}
                <div class="pt-1 border-t border-blue-200 font-semibold flex justify-between">
                    <span>Total Monthly</span>
                    <span class="font-mono">${formatCurrency(gap.monthlyInsurancePremiums)}</span>
                </div>
            </div>
        </div>`;
    }
    
    if (gap.suggestions && gap.suggestions.length > 0) {
        suggestionsHTML += gap.suggestions.map(s => {
            let textColor = 'text-slate-600';
            if (s.impact === 'high') textColor = 'text-amber-700';
            else if (s.impact === 'positive') textColor = 'text-success-700';
            
            return `<div class="flex items-center gap-2 ${textColor}"><span>${s.icon}</span><span>${s.title}: ${s.description}</span></div>`;
        }).join('');
    }
    
    suggestionsDiv.innerHTML = suggestionsHTML;
}

// Helper to format numbers
function formatNumber(num) {
    if (!num) return '0';
    if (num >= 10000000) return (num / 10000000).toFixed(2) + 'Cr';
    if (num >= 100000) return (num / 100000).toFixed(2) + 'L';
    return num.toLocaleString('en-IN');
}

// ==================== SIP STEP-UP OPTIMIZATION ====================

// Render SIP Step-Up Optimization card
function renderSipStepUpOptimization(optimization, currentAge, summary) {
    const container = document.getElementById('sip-stepup-optimization');
    if (!container || !optimization) {
        if (container) container.classList.add('hidden');
        return;
    }
    
    container.classList.remove('hidden');
    
    // Get strategy info to show proper context
    const selectedStrategy = summary?.incomeStrategy || currentParams.incomeStrategy || 'SUSTAINABLE';
    const withdrawalRate = summary?.withdrawalRate || 8;
    const monthlyExpense = summary?.monthlyExpenseAtRetirement || 0;
    
    // Calculate what corpus the 4% rule would require (conservative baseline)
    const required4PercentCorpus = monthlyExpense * 12 / 0.04; // Annual expense / 4%
    const requiredCorpusFromStrategy = optimization.requiredCorpus || 0;
    
    // Check if the optimization is based on aggressive assumptions
    const isAggressive = selectedStrategy === 'SUSTAINABLE' && withdrawalRate > 6;
    const optimalCorpus = optimization.scenarios?.find(s => s.stopYear === optimization.optimalStopYear)?.projectedCorpus || 0;
    const meetsConservative = optimalCorpus >= required4PercentCorpus;
    
    // Update recommendation with context
    const recommendationEl = document.getElementById('stepup-recommendation');
    if (recommendationEl) {
        if (isAggressive && !meetsConservative) {
            recommendationEl.innerHTML = `
                <span class="text-amber-700">⚠️ Based on ${withdrawalRate}% withdrawal (optimistic). 
                With 4% rule, you'd need ${formatCurrency(required4PercentCorpus, true)} corpus.</span>
            `;
        } else {
            recommendationEl.textContent = optimization.recommendation || 'No optimization data available';
        }
    }
    
    // Update stats
    document.getElementById('stepup-initial-sip').textContent = formatCurrency(optimization.sipAtStart);
    document.getElementById('stepup-full-sip').textContent = formatCurrency(optimization.sipAtFullStepUp);
    document.getElementById('stepup-optimal-sip').textContent = formatCurrency(optimization.sipAtOptimalStop);
    document.getElementById('stepup-relief').textContent = formatCurrency(optimization.monthlyReliefFromStoppingEarly);
    
    // Render scenarios table with additional context
    const scenariosEl = document.getElementById('stepup-scenarios');
    if (scenariosEl && optimization.scenarios) {
        const currentYear = new Date().getFullYear();
        const requiredCorpus = optimization.requiredCorpus || 0;
        const optimalStopYear = optimization.optimalStopYear;
        
        // Add header note if using aggressive assumptions
        let tableHTML = '';
        if (isAggressive) {
            tableHTML += `
                <tr class="bg-amber-50">
                    <td colspan="5" class="px-3 py-2 text-xs text-amber-700">
                        ⚠️ Target based on ${withdrawalRate}% withdrawal. Conservative (4% rule) needs: <strong>${formatCurrency(required4PercentCorpus, true)}</strong>
                    </td>
                </tr>
            `;
        }
        
        tableHTML += optimization.scenarios.map(scenario => {
            const isOptimal = scenario.stopYear === optimalStopYear;
            const calendarYear = currentYear + scenario.stopYear;
            const age = (currentAge || 35) + scenario.stopYear;
            const surplus = scenario.surplus || 0;
            
            // Check if this scenario meets 4% rule requirement
            const meetsConservativeReq = scenario.projectedCorpus >= required4PercentCorpus;
            const rowClass = isOptimal 
                ? (meetsConservativeReq ? 'bg-emerald-50 font-semibold' : 'bg-amber-100 font-semibold')
                : (scenario.meetsTarget ? '' : 'bg-red-50');
            
            return `
                <tr class="${rowClass}">
                    <td class="px-3 py-2 text-slate-700">
                        ${calendarYear} (Age ${age})
                        ${isOptimal ? `<span class="ml-1 ${meetsConservativeReq ? 'text-emerald-600' : 'text-amber-600'}">← ${meetsConservativeReq ? 'Optimal' : 'Risky'}</span>` : ''}
                    </td>
                    <td class="px-3 py-2 text-right font-mono text-slate-600">${formatCurrency(scenario.finalSipAtStop)}</td>
                    <td class="px-3 py-2 text-right font-mono text-slate-700">${formatCurrency(scenario.projectedCorpus, true)}</td>
                    <td class="px-3 py-2 text-right font-mono ${surplus >= 0 ? 'text-emerald-600' : 'text-danger-600'}">
                        ${surplus >= 0 ? '+' : ''}${formatCurrency(surplus, true)}
                    </td>
                    <td class="px-3 py-2 text-center">
                        ${isAggressive 
                            ? (meetsConservativeReq ? '<span class="text-emerald-600">✓✓</span>' : (scenario.meetsTarget ? '<span class="text-amber-500">⚠️</span>' : '<span class="text-danger-500">✗</span>'))
                            : (scenario.meetsTarget ? '<span class="text-emerald-600">✓</span>' : '<span class="text-danger-500">✗</span>')}
                    </td>
                </tr>
            `;
        }).join('');
        
        scenariosEl.innerHTML = tableHTML;
    }
}

// Toggle step-up details visibility
function toggleStepUpDetails() {
    const details = document.getElementById('stepup-details');
    if (details) {
        details.classList.toggle('hidden');
    }
}

// Highlight the selected income strategy card (single selection only)
function highlightSelectedStrategy(strategy) {
    // Get all strategy cards
    const depletionCard = document.getElementById('income-depletion')?.closest('.rounded-lg');
    const safeCard = document.getElementById('income-4percent')?.closest('.rounded-lg');
    const sustainableCard = document.getElementById('sustainable-card');
    
    const allCards = [depletionCard, safeCard, sustainableCard].filter(Boolean);
    
    // Remove any existing highlights from ALL cards
    allCards.forEach(el => {
        el.classList.remove('ring-2', 'ring-primary-500', 'ring-offset-2', 'bg-primary-100');
    });
    
    // Map strategy to card element
    let selectedCard;
    switch (strategy) {
        case 'SIMPLE_DEPLETION':
            selectedCard = depletionCard;
            break;
        case 'SAFE_4_PERCENT':
            selectedCard = safeCard;
            break;
        case 'SUSTAINABLE':
        default:
            selectedCard = sustainableCard;
            break;
    }
    
    // Add highlight to selected card only
    if (selectedCard) {
        selectedCard.classList.add('ring-2', 'ring-primary-500', 'ring-offset-2');
    }
}

// Render retirement income projection table (compact)
function renderIncomeProjection(projection) {
    const tbody = document.getElementById('income-projection-body');
    
    if (!projection || projection.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" class="px-2 py-2 text-center text-slate-400">Add investments</td></tr>`;
        return;
    }
    
    tbody.innerHTML = projection.map((row, idx) => `
        <tr class="${idx === 0 ? 'bg-success-50 font-semibold' : ''}">
            <td class="px-2 py-1 text-slate-700">${row.year === 0 ? 'Retire' : `+${row.year}y`}</td>
            <td class="px-2 py-1 text-slate-600">${row.age}</td>
            <td class="px-2 py-1 text-right font-mono text-slate-700">${formatCurrency(row.corpus, true)}</td>
            <td class="px-2 py-1 text-right font-mono text-success-700">${formatCurrency(row.monthlyIncome)}</td>
        </tr>
    `).join('');
}

function renderRetirementMatrix(data) {
    const summary = data.summary || {};
    const matrix = data.matrix || [];

    const yearsToRetire = (summary.retirementAge || currentParams.retirementAge) - (summary.currentAge || currentParams.currentAge);
    
    // Update summary cards
    document.getElementById('current-age').textContent = summary.currentAge || currentParams.currentAge;
    document.getElementById('years-to-retire').textContent = yearsToRetire;
    document.getElementById('final-corpus').textContent = formatCurrency(summary.finalCorpus, true);
    
    // Render SIP Step-Up Optimization (pass summary for strategy context)
    renderSipStepUpOptimization(summary.sipStepUpOptimization, summary.currentAge || currentParams.currentAge, summary);
    
    // Update Required Corpus card (from GAP analysis)
    const gapData = data.gapAnalysis || {};
    const requiredCorpus = gapData.requiredCorpus || 0;
    const corpusGap = gapData.corpusGap || 0;
    
    document.getElementById('required-corpus').textContent = formatCurrency(requiredCorpus, true);
    
    // Style the required corpus card based on gap status
    const requiredCorpusCard = document.getElementById('required-corpus-card');
    const gapLabel = document.getElementById('corpus-gap-label');
    
    if (corpusGap > 0) {
        // Shortfall
        requiredCorpusCard.className = 'card p-4 border-danger-200 bg-danger-50';
        gapLabel.className = 'text-xs text-danger-600';
        gapLabel.textContent = `Gap: -${formatCurrency(corpusGap, true)} (${gapData.gapPercent || 0}% short)`;
    } else {
        // On track or surplus
        requiredCorpusCard.className = 'card p-4 border-success-200 bg-success-50';
        gapLabel.className = 'text-xs text-success-600';
        gapLabel.textContent = `Surplus: +${formatCurrency(Math.abs(corpusGap), true)}`;
    }
    
    // Update primary income display using selected strategy
    const selectedIncome = summary.selectedMonthlyIncome || summary.monthlyIncomeFromCorpus || (summary.finalCorpus || 0) * 0.08 / 12;
    document.getElementById('monthly-income').textContent = formatCurrency(selectedIncome);
    
    // Update the income note with selected strategy
    const incomeNote = document.getElementById('income-note');
    if (incomeNote && summary.selectedStrategyName) {
        incomeNote.textContent = `(${summary.selectedStrategyName})`;
    }
    
    // Update income breakdown cards
    document.getElementById('income-depletion').textContent = formatCurrency(summary.monthlyRetirementIncome) + '/mo';
    document.getElementById('income-4percent').textContent = formatCurrency(summary.monthlyIncome4Percent) + '/mo';
    document.getElementById('income-sustainable').textContent = formatCurrency(summary.monthlyIncomeFromCorpus) + '/mo';
    
    // Update sustainable label with actual rates
    const corpusReturn = summary.corpusReturnRate || 10;
    const withdrawal = summary.withdrawalRate || 8;
    const sustainableLabel = document.getElementById('sustainable-label');
    const sustainableNote = document.getElementById('sustainable-note');
    if (sustainableLabel) {
        sustainableLabel.textContent = `${corpusReturn}% Return, ${withdrawal}% Withdrawal`;
    }
    if (sustainableNote) {
        const netGrowth = corpusReturn - withdrawal;
        sustainableNote.textContent = netGrowth >= 0 ? `Corpus grows at ~${netGrowth}%` : `Corpus depletes at ~${Math.abs(netGrowth)}%`;
    }
    
    // Highlight selected strategy card
    highlightSelectedStrategy(summary.incomeStrategy);
    
    // Render retirement income projection table
    renderIncomeProjection(summary.retirementIncomeProjection);

    // Render matrix table
    const tbody = document.getElementById('matrix-body');

    if (matrix.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="10" class="px-4 py-8 text-center text-slate-500">
                Add investments to see your retirement projection.
            </td></tr>
        `;
        return;
    }

    // Get starting balances for tooltip info
    const startBal = data.summary?.startingBalances || {};
    
    // Get step-up optimization data
    const stepUpOptimization = data.summary?.sipStepUpOptimization || {};
    const optimalStopYear = stepUpOptimization.optimalStopYear;
    const currentYear = new Date().getFullYear();
    
    // Get step-up info for calculating "if stopped" values
    const sipStepUpPercent = data.summary?.sipStepUpPercent || 10; // Default 10% step-up
    const mfRate = matrix[0]?.mfRate || 12;
    
    // Get the SIP at the stop year (this becomes flat if we stop)
    const sipAtStopYear = optimalStopYear !== null ? (matrix[optimalStopYear]?.mfSip || 0) : 0;
    
    // Calculate what SIP and corpus would be IF step-up was STOPPED (flat)
    // The matrix already shows WITH step-up, so we calculate the savings from stopping
    function calculateIfStopped(yearIdx) {
        if (optimalStopYear === null || yearIdx <= optimalStopYear) {
            return { flatSip: null, flatCorpus: null }; // No comparison needed
        }
        
        // If stopped, SIP stays flat at the stop year value
        const flatSip = sipAtStopYear;
        
        // Current SIP (with step-up continuing) from matrix
        const currentSip = matrix[yearIdx]?.mfSip || 0;
        
        // Calculate corpus difference from lower SIP
        // How much LESS corpus we'd have if SIP was flat instead of stepped-up
        const yearsAfterStop = yearIdx - optimalStopYear;
        const sipSavings = currentSip - flatSip; // Monthly savings from stopping step-up
        
        // Rough estimate of corpus reduction (less contributions + less growth)
        const avgSipDiff = sipSavings / 2; // Average difference over the period
        const corpusReduction = avgSipDiff * 12 * yearsAfterStop * (1 + mfRate/100);
        
        return {
            flatSip: Math.round(flatSip),
            flatCorpus: Math.round((matrix[yearIdx]?.netCorpus || 0) - corpusReduction),
            sipSavings: Math.round(sipSavings)
        };
    }
    
    tbody.innerHTML = matrix.map((row, idx) => {
        // Calculate inflows this year (insurance maturity + investment maturity)
        const hasInflow = row.insuranceMaturity > 0 || row.investmentMaturity > 0;
        const totalInflow = (row.insuranceMaturity || 0) + (row.investmentMaturity || 0);
        
        // Determine step-up status for this row
        const isStepUpActive = row.sipStepUpActive;
        const isStopYear = optimalStopYear !== null && idx === optimalStopYear;
        const isAfterStop = optimalStopYear !== null && idx > optimalStopYear;
        
        // Calculate "if stopped" values for comparison (what would happen if we stopped step-up)
        const ifStopped = calculateIfStopped(idx);
        
        let stepUpDisplay = '';
        if (idx === 0) {
            stepUpDisplay = '<span class="text-slate-400">-</span>';
        } else if (isStopYear) {
            stepUpDisplay = '<span class="px-2 py-0.5 text-xs rounded bg-amber-100 text-amber-700 font-medium">🛑 STOP</span>';
        } else if (isStepUpActive || (optimalStopYear !== null && idx < optimalStopYear)) {
            stepUpDisplay = '<span class="px-2 py-0.5 text-xs rounded bg-emerald-100 text-emerald-700">📈 +10%</span>';
        } else if (isAfterStop) {
            // Show continuing step-up indicator (matrix continues step-up)
            stepUpDisplay = '<span class="px-2 py-0.5 text-xs rounded bg-emerald-100 text-emerald-700">📈 +10%</span>';
        } else {
            stepUpDisplay = '<span class="text-slate-400">-</span>';
        }
        
        // Show current SIP (with step-up) and what it would be IF STOPPED (flat) in grey
        const sipDisplay = isAfterStop && ifStopped.flatSip ? `
            ${formatCurrency(row.mfSip)}/mo
            <div class="text-xs text-slate-400">↘ ${formatCurrency(ifStopped.flatSip)}/mo</div>
        ` : `${formatCurrency(row.mfSip)}/mo`;
        
        // Show current corpus (with step-up) and what it would be IF STOPPED in grey
        const corpusDisplay = isAfterStop && ifStopped.flatCorpus ? `
            ${formatCurrency(row.netCorpus, true)}
            <div class="text-xs text-slate-400">↘ ${formatCurrency(ifStopped.flatCorpus, true)}</div>
        ` : formatCurrency(row.netCorpus, true);
        
        return `
        <tr class="hover:bg-slate-50 ${row.goalOutflow > 0 ? 'bg-danger-50' : ''} ${hasInflow ? 'bg-emerald-50' : ''} ${isStopYear ? 'bg-amber-50 border-l-4 border-amber-400' : ''}">
            <td class="px-4 py-3 font-medium text-slate-800">${row.year}</td>
            <td class="px-4 py-3 text-slate-600">${row.age}</td>
            <td class="px-4 py-3 text-right font-mono text-slate-700">
                ${formatCurrency((row.ppfBalance || 0) + (row.epfBalance || 0), true)}
                <div class="text-xs text-slate-400">${row.ppfRate || 7}%/${row.epfRate || 8}%</div>
            </td>
            <td class="px-4 py-3 text-right font-mono text-slate-700">${formatCurrency(row.mfBalance, true)}</td>
            <td class="px-4 py-3 text-right font-mono text-slate-700 group relative">
                ${formatCurrency(row.otherLiquidBalance || 0, true)}
                ${idx === 0 && startBal.otherLiquidTotal > 0 ? `
                <div class="text-xs text-slate-400">FD+RD+Stock+Cash</div>
                <div class="hidden group-hover:block absolute z-10 bg-slate-800 text-white text-xs p-2 rounded shadow-lg -left-4 top-full mt-1 whitespace-nowrap">
                    FD: ${formatCurrency(startBal.fd || 0)}<br>
                    RD: ${formatCurrency(startBal.rd || 0)}<br>
                    Stocks: ${formatCurrency(startBal.stocks || 0)}<br>
                    Cash: ${formatCurrency(startBal.cash || 0)}
                </div>` : ''}
            </td>
            <td class="px-4 py-3 text-right font-mono text-slate-500">${sipDisplay}</td>
            <td class="px-4 py-3 text-center">${stepUpDisplay}</td>
            <td class="px-4 py-3 text-right font-mono ${hasInflow ? 'text-emerald-600' : 'text-slate-400'}">
                ${hasInflow ? '+' + formatCurrency(totalInflow, true) : '-'}
                ${row.maturingPolicies?.length ? `<div class="text-xs text-emerald-500">${row.maturingPolicies.join(', ')}</div>` : ''}
                ${row.maturingInvestments?.length ? `<div class="text-xs text-emerald-500">${row.maturingInvestments.join(', ')}</div>` : ''}
            </td>
            <td class="px-4 py-3 text-right font-mono ${row.goalOutflow > 0 ? 'text-danger-600' : 'text-slate-400'}">
                ${row.goalOutflow > 0 ? '-' + formatCurrency(row.goalOutflow, true) : '-'}
                ${row.goalsThisYear?.length ? `<div class="text-xs text-slate-500">${row.goalsThisYear.join(', ')}</div>` : ''}
            </td>
            <td class="px-4 py-3 text-right font-mono font-bold text-primary-600">${corpusDisplay}</td>
        </tr>
    `}).join('');
}

// Render investments/insurance maturing before retirement
function renderMaturingInvestments(data) {
    const container = document.getElementById('maturing-investments-container');
    if (!container) return;
    
    if (!data || (data.investmentCount === 0 && data.insuranceCount === 0)) {
        container.innerHTML = `
            <div class="text-center py-6 text-slate-400">
                <div class="text-4xl mb-2">📅</div>
                <div>No investments or policies maturing before retirement</div>
                <div class="text-xs mt-1">FD, RD, PPF with maturity dates will appear here</div>
            </div>
        `;
        return;
    }
    
    let html = `
        <div class="mb-4 p-4 bg-gradient-to-r from-emerald-50 to-teal-50 rounded-xl border border-emerald-200">
            <div class="flex items-center justify-between">
                <div>
                    <div class="text-sm text-emerald-600 font-medium">Available for Reinvestment</div>
                    <div class="text-2xl font-bold text-emerald-700">${formatCurrency(data.totalMaturingBeforeRetirement, true)}</div>
                </div>
                <div class="text-right">
                    <div class="text-xs text-slate-500">Before retirement</div>
                    <div class="text-sm text-slate-600">${data.investmentCount} investments, ${data.insuranceCount} policies</div>
                </div>
            </div>
        </div>
    `;
    
    // Investments table
    if (data.maturingInvestments && data.maturingInvestments.length > 0) {
        html += `
            <div class="mb-4">
                <h4 class="text-sm font-semibold text-slate-700 mb-2">💰 Maturing Investments</h4>
                <div class="overflow-x-auto">
                    <table class="w-full text-sm">
                        <thead class="bg-slate-100">
                            <tr>
                                <th class="px-3 py-2 text-left text-slate-600">Investment</th>
                                <th class="px-3 py-2 text-left text-slate-600">Type</th>
                                <th class="px-3 py-2 text-right text-slate-600">Matures</th>
                                <th class="px-3 py-2 text-right text-slate-600">Current</th>
                                <th class="px-3 py-2 text-right text-slate-600">Expected Value</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100">
                            ${data.maturingInvestments.map(inv => `
                                <tr class="hover:bg-slate-50">
                                    <td class="px-3 py-2 font-medium text-slate-800">${inv.name}</td>
                                    <td class="px-3 py-2">
                                        <span class="px-2 py-0.5 text-xs rounded-full ${getTypeColor(inv.type)}">${inv.type}</span>
                                    </td>
                                    <td class="px-3 py-2 text-right text-slate-600">
                                        ${formatDate(inv.maturityDate)}
                                        <div class="text-xs text-slate-400">${inv.yearsToMaturity}y</div>
                                    </td>
                                    <td class="px-3 py-2 text-right font-mono text-slate-600">${formatCurrency(inv.currentValue)}</td>
                                    <td class="px-3 py-2 text-right font-mono font-semibold text-emerald-600">${formatCurrency(inv.expectedMaturityValue)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    // Insurance policies table
    if (data.maturingInsurance && data.maturingInsurance.length > 0) {
        html += `
            <div>
                <h4 class="text-sm font-semibold text-slate-700 mb-2">🛡️ Maturing Insurance Policies</h4>
                <div class="overflow-x-auto">
                    <table class="w-full text-sm">
                        <thead class="bg-slate-100">
                            <tr>
                                <th class="px-3 py-2 text-left text-slate-600">Policy</th>
                                <th class="px-3 py-2 text-left text-slate-600">Type</th>
                                <th class="px-3 py-2 text-right text-slate-600">Matures</th>
                                <th class="px-3 py-2 text-right text-slate-600">Fund Value</th>
                                <th class="px-3 py-2 text-right text-slate-600">Expected Maturity</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100">
                            ${data.maturingInsurance.map(ins => `
                                <tr class="hover:bg-slate-50">
                                    <td class="px-3 py-2 font-medium text-slate-800">${ins.name}</td>
                                    <td class="px-3 py-2">
                                        <span class="px-2 py-0.5 text-xs rounded-full bg-purple-100 text-purple-700">${ins.type}</span>
                                    </td>
                                    <td class="px-3 py-2 text-right text-slate-600">
                                        ${formatDate(ins.maturityDate)}
                                        <div class="text-xs text-slate-400">${ins.yearsToMaturity}y</div>
                                    </td>
                                    <td class="px-3 py-2 text-right font-mono text-slate-600">${formatCurrency(ins.currentFundValue)}</td>
                                    <td class="px-3 py-2 text-right font-mono font-semibold text-emerald-600">${formatCurrency(ins.expectedMaturityValue)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }
    
    html += `
        <div class="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200 text-xs text-blue-700">
            💡 <strong>Tip:</strong> These funds will become available before your retirement and can be reinvested for higher returns or used for goals.
        </div>
    `;
    
    container.innerHTML = html;
}

// Helper function for investment type colors
function getTypeColor(type) {
    const colors = {
        'FD': 'bg-blue-100 text-blue-700',
        'RD': 'bg-indigo-100 text-indigo-700',
        'PPF': 'bg-green-100 text-green-700',
        'MUTUAL_FUND': 'bg-purple-100 text-purple-700',
        'NPS': 'bg-orange-100 text-orange-700'
    };
    return colors[type] || 'bg-slate-100 text-slate-700';
}

// Format date helper
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-IN', { month: 'short', year: 'numeric' });
}

// ==================== WITHDRAWAL STRATEGY ====================

// Render personalized withdrawal schedule based on backend API
async function renderWithdrawalSchedule() {
    const container = document.getElementById('withdrawal-schedule');
    if (!container) return;
    
    container.innerHTML = '<div class="text-center py-4 text-slate-400">Loading your withdrawal strategy...</div>';
    
    try {
        // If cached data not available, fetch it first
        let retirementData = window.cachedRetirementData;
        if (!retirementData || !retirementData.summary) {
            console.log('Fetching matrix data for withdrawal strategy...');
            retirementData = await api.retirement.getMatrix();
            window.cachedRetirementData = retirementData;
        }
        
        const matrixSummary = retirementData?.summary || {};
        const currentAge = matrixSummary.currentAge || currentParams.currentAge || 35;
        const retirementAge = matrixSummary.retirementAge || currentParams.retirementAge || 60;
        const lifeExpectancy = matrixSummary.lifeExpectancy || currentParams.lifeExpectancy || 85;
        
        // Get the MATRIX's projected corpus (includes SIP step-up, etc.)
        // Matrix uses 'finalCorpus' in summary, or 'projectedCorpus' in gapAnalysis
        const gapAnalysis = retirementData?.gapAnalysis || {};
        const matrixProjectedCorpus = matrixSummary.finalCorpus || gapAnalysis.projectedCorpus || 0;
        
        console.log('Withdrawal Strategy - Matrix corpus:', matrixProjectedCorpus, 'finalCorpus:', matrixSummary.finalCorpus, 'gapAnalysis.projectedCorpus:', gapAnalysis.projectedCorpus);
        
        // Fetch withdrawal strategy from backend
        const strategy = await api.retirement.getWithdrawalStrategy(currentAge, retirementAge, lifeExpectancy);
        
        if (!strategy || !strategy.phases || strategy.phases.length === 0) {
            container.innerHTML = '<div class="text-center py-4 text-slate-400">Add investments to see your personalized withdrawal schedule.</div>';
            return;
        }
        
        const phases = strategy.phases;
        const strategySummary = strategy.summary || {};
        
        // Use MATRIX's projected corpus for sustainability calculation (more accurate with SIP step-up)
        const corpusToUse = matrixProjectedCorpus > 0 ? matrixProjectedCorpus : (strategySummary.totalCorpusAtRetirement || 0);
        
        // Get corpus values
        const backendCorpus = strategySummary.totalCorpusAtRetirement || 0;
        
        // Get SIP step-up optimization data
        const sipOptimization = matrixSummary.sipStepUpOptimization || {};
        const corpusIfStopped = sipOptimization.corpusAtOptimalStop || backendCorpus;
        const optimalStopYear = sipOptimization.optimalStopYear;
        const canStopEarly = sipOptimization.canStopEarly;
        
        // Calculate total phase values
        let totalPhaseValue = 0;
        phases.forEach(p => totalPhaseValue += (p.total || 0));
        
        // Build the phase summary table with better explanations
        let scheduleHTML = `
            <div class="overflow-x-auto">
                <table class="w-full text-sm">
                    <thead class="bg-slate-100">
                        <tr>
                            <th class="px-3 py-2 text-left text-slate-600">Phase</th>
                            <th class="px-3 py-2 text-left text-slate-600">Age Range</th>
                            <th class="px-3 py-2 text-left text-slate-600">Assets to Use</th>
                            <th class="px-3 py-2 text-right text-slate-600">Projected Value</th>
                            <th class="px-3 py-2 text-left text-slate-600">Notes</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-100">
        `;
        
        const colorMap = { emerald: 'emerald', blue: 'blue', purple: 'purple' };
        let runningTotal = corpusToUse; // Track remaining corpus
        
        for (let i = 0; i < phases.length; i++) {
            const phase = phases[i];
            const color = colorMap[phase.color] || 'slate';
            const assetNames = phase.assets?.map(a => a.name || a.type).slice(0, 3).join(', ') || 'None';
            const moreCount = (phase.assets?.length || 0) - 3;
            
            // Calculate note based on phase status
            let phaseNote = '';
            const phaseTotal = phase.total || 0;
            
            if (phaseTotal === 0 && i === 0) {
                phaseNote = '<span class="text-amber-600">No liquid assets - use SIP corpus</span>';
            } else if (phaseTotal === 0 && i > 0) {
                phaseNote = '<span class="text-blue-600">Continue from previous phase corpus</span>';
            } else if (phase.yearsCovered >= 10) {
                phaseNote = '<span class="text-emerald-600">✓ Well covered</span>';
            } else if (phase.yearsCovered < 5) {
                phaseNote = '<span class="text-amber-600">⚠️ May need supplement</span>';
            } else {
                phaseNote = '<span class="text-slate-500">Partial coverage</span>';
            }
            
            scheduleHTML += `
                <tr class="bg-${color}-50">
                    <td class="px-3 py-2">
                        <span class="px-2 py-1 text-xs rounded bg-${color}-200 text-${color}-700 font-medium">Phase ${phase.priority}</span>
                    </td>
                    <td class="px-3 py-2 text-slate-700">${phase.suggestedAgeRange || 'N/A'}</td>
                    <td class="px-3 py-2 text-slate-600">
                        ${assetNames}${moreCount > 0 ? ` +${moreCount} more` : ''}
                    </td>
                    <td class="px-3 py-2 text-right font-mono text-${color}-700 font-medium">${formatCurrency(phaseTotal, true)}</td>
                    <td class="px-3 py-2 text-xs">${phaseNote}</td>
                </tr>
            `;
        }
        
        // Show both corpus values - WITH step-up vs IF STOPPED
        const corpusDifference = corpusToUse - backendCorpus;
        const showDifference = Math.abs(corpusDifference) > 100000; // Show if >1L difference
        
        scheduleHTML += `
                    </tbody>
                </table>
            </div>
            
            <!-- Corpus Comparison: With Step-Up vs If Stopped -->
            <div class="mt-4 grid grid-cols-2 gap-4">
                <div class="p-4 bg-emerald-50 rounded-lg border border-emerald-200">
                    <div class="text-xs text-emerald-600 mb-1">📈 With Continued SIP Step-Up</div>
                    <div class="text-xl font-bold text-emerald-700">${formatCurrency(corpusToUse, true)}</div>
                    ${showDifference ? `<div class="text-xs text-emerald-600">Investments: ${formatCurrency(backendCorpus, true)} + SIPs: ${formatCurrency(corpusDifference, true)}</div>` : ''}
                    <div class="text-xs text-slate-500 mt-1">If you continue ${matrixSummary.sipStepUpPercent || 10}% annual SIP increase</div>
                </div>
                <div class="p-4 ${canStopEarly ? 'bg-amber-50 border-amber-200' : 'bg-slate-50 border-slate-200'} rounded-lg border">
                    <div class="text-xs ${canStopEarly ? 'text-amber-600' : 'text-slate-500'} mb-1">🛑 If SIP Step-Up Stops ${optimalStopYear ? `(Year ${optimalStopYear})` : ''}</div>
                    <div class="text-xl font-bold ${canStopEarly ? 'text-amber-700' : 'text-slate-700'}">${formatCurrency(corpusIfStopped, true)}</div>
                    <div class="text-xs ${canStopEarly ? 'text-amber-600' : 'text-slate-500'}">
                        ${canStopEarly 
                            ? `Save ${formatCurrency(sipOptimization.monthlyReliefFromStoppingEarly || 0)}/mo after stopping` 
                            : 'Keep step-up to maximize corpus'}
                    </div>
                    <div class="text-xs text-slate-500 mt-1">Difference: ${formatCurrency(corpusToUse - corpusIfStopped, true)} less</div>
                </div>
            </div>
            
            <!-- Important Note about Phase Coverage -->
            <div class="mt-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <div class="flex items-start gap-2 text-blue-700">
                    <span>💡</span>
                    <div class="text-sm">
                        <strong>How it works:</strong> Your total corpus (${formatCurrency(corpusToUse, true)}) funds ALL phases. 
                        Phases show which assets to withdraw FIRST, not separate pools. 
                        If Phase 1/2 assets run out, the remaining corpus continues to fund expenses.
                    </div>
                </div>
            </div>
        `;
        
        // Add monthly withdrawal summary
        // Use MATRIX's inflated expense for consistency with GAP Analysis
        // Backend sends 'inflatedMonthlyExpenses' in gapAnalysis which is the same expense used for required corpus calc
        const inflatedExpenseFromGap = gapAnalysis.inflatedMonthlyExpenses || 0;
        const expenseFromWithdrawalAPI = strategySummary.monthlyExpenseAtRetirement || 0;
        
        // Use GAP Analysis expense for consistency - this is what required corpus is based on
        const monthlyExpense = inflatedExpenseFromGap > 0 ? inflatedExpenseFromGap : expenseFromWithdrawalAPI;
        
        console.log('Withdrawal Strategy - Expense comparison:', {
            fromGapAnalysis_inflated: inflatedExpenseFromGap,
            fromWithdrawalAPI: expenseFromWithdrawalAPI,
            usingExpense: monthlyExpense,
            requiredCorpus: gapAnalysis.requiredCorpus,
            selectedStrategy: matrixSummary.incomeStrategy
        });
        
        // Get selected income strategy from matrix data
        const selectedStrategy = matrixSummary.incomeStrategy || currentParams.incomeStrategy || 'SUSTAINABLE';
        const corpusReturnRate = matrixSummary.corpusReturnRate || 10;
        const withdrawalRate = matrixSummary.withdrawalRate || 8;
        const yearsInRetirement = lifeExpectancy - retirementAge;
        
        // Calculate withdrawals based on SELECTED strategy (not hardcoded 4%)
        let strategyWithdrawal, strategyLabel, strategyDescription;
        
        if (selectedStrategy === 'SIMPLE_DEPLETION') {
            // Simple depletion: Corpus / Years / 12 months
            strategyWithdrawal = Math.round(corpusToUse / yearsInRetirement / 12);
            strategyLabel = 'Simple Depletion';
            strategyDescription = `Corpus ÷ ${yearsInRetirement} years`;
        } else if (selectedStrategy === 'SAFE_4_PERCENT') {
            // 4% rule
            strategyWithdrawal = Math.round(corpusToUse * 0.04 / 12);
            strategyLabel = '4% Safe Withdrawal';
            strategyDescription = '4% annually (most conservative)';
        } else {
            // SUSTAINABLE - uses custom rates
            strategyWithdrawal = Math.round(corpusToUse * (withdrawalRate / 100) / 12);
            strategyLabel = `${corpusReturnRate}% Return, ${withdrawalRate}% Withdrawal`;
            strategyDescription = `Net ${corpusReturnRate - withdrawalRate >= 0 ? '+' : ''}${corpusReturnRate - withdrawalRate}% corpus growth`;
        }
        
        // Also calculate 4% rule for comparison (conservative baseline)
        const safe4PercentWithdrawal = Math.round(corpusToUse * 0.04 / 12);
        
        // Determine sustainability based on selected strategy
        const isSustainable = strategyWithdrawal >= monthlyExpense;
        const shortfall = strategyWithdrawal - monthlyExpense;
        
        // Check if using aggressive assumptions (sustainable with >6% withdrawal rate)
        const isAggressive = selectedStrategy === 'SUSTAINABLE' && withdrawalRate > 6;
        const is4PercentSustainable = safe4PercentWithdrawal >= monthlyExpense;
        
        scheduleHTML += `
            <div class="mt-4 grid grid-cols-3 gap-3">
                <div class="p-3 bg-slate-50 rounded-lg text-center">
                    <div class="text-xs text-slate-500">Monthly Expense (Inflated)</div>
                    <div class="text-lg font-bold text-slate-700">${formatCurrency(monthlyExpense)}</div>
                </div>
                <div class="p-3 bg-primary-50 rounded-lg text-center border-2 border-primary-300">
                    <div class="text-xs text-primary-600">${strategyLabel}</div>
                    <div class="text-lg font-bold text-primary-700">${formatCurrency(strategyWithdrawal)}</div>
                    <div class="text-xs text-primary-500">${strategyDescription}</div>
                </div>
                <div class="p-3 ${isSustainable ? 'bg-emerald-50' : 'bg-amber-50'} rounded-lg text-center">
                    <div class="text-xs ${isSustainable ? 'text-emerald-600' : 'text-amber-600'}">Status</div>
                    <div class="text-lg font-bold ${isSustainable ? 'text-emerald-700' : 'text-amber-700'}">
                        ${isSustainable ? '✓ Sustainable' : '⚠️ Review Needed'}
                    </div>
                </div>
            </div>
        `;
        
        // Show comparison with 4% rule if using aggressive strategy
        if (isAggressive) {
            const conservativeShortfall = safe4PercentWithdrawal - monthlyExpense;
            scheduleHTML += `
                <div class="mt-3 p-3 bg-amber-50 rounded-lg border border-amber-200">
                    <div class="flex items-center gap-2 text-amber-700 mb-2">
                        <span>⚠️</span>
                        <span class="text-sm font-medium">Aggressive Assumptions Warning</span>
                    </div>
                    <div class="text-xs text-amber-700 mb-2">
                        Your selected strategy assumes ${corpusReturnRate}% returns and ${withdrawalRate}% withdrawal. 
                        This is optimistic - markets may not always deliver ${corpusReturnRate}% returns.
                    </div>
                    <div class="grid grid-cols-2 gap-2 text-xs">
                        <div class="p-2 bg-white rounded">
                            <div class="text-slate-500">Conservative (4% Rule)</div>
                            <div class="font-bold ${is4PercentSustainable ? 'text-emerald-700' : 'text-danger-600'}">${formatCurrency(safe4PercentWithdrawal)}/mo</div>
                            <div class="${is4PercentSustainable ? 'text-emerald-600' : 'text-danger-500'}">
                                ${is4PercentSustainable ? '✓ Covers expenses' : `₹${formatCurrency(Math.abs(conservativeShortfall))} short`}
                            </div>
                        </div>
                        <div class="p-2 bg-white rounded">
                            <div class="text-slate-500">Your Strategy (${withdrawalRate}%)</div>
                            <div class="font-bold ${isSustainable ? 'text-emerald-700' : 'text-danger-600'}">${formatCurrency(strategyWithdrawal)}/mo</div>
                            <div class="${isSustainable ? 'text-emerald-600' : 'text-danger-500'}">
                                ${isSustainable ? '✓ Covers expenses' : `₹${formatCurrency(Math.abs(shortfall))} short`}
                            </div>
                        </div>
                    </div>
                    <div class="mt-2 text-xs text-amber-600">
                        💡 The 4% rule has historically survived 30+ year retirements. Higher withdrawal rates increase the risk of running out of money.
                    </div>
                </div>
            `;
        }
        
        // Add simple status message (only if not showing aggressive warning)
        if (shortfall < 0 && !isAggressive) {
            scheduleHTML += `
                <div class="mt-4 p-3 bg-amber-50 rounded-lg border border-amber-200">
                    <div class="flex items-center gap-2 text-amber-700">
                        <span>⚠️</span>
                        <span class="text-sm">Your withdrawal (${strategyLabel}) is ${formatCurrency(Math.abs(shortfall))}/mo less than projected expenses. 
                        Consider: increasing SIP, extending working years, or reducing retirement expenses.</span>
                    </div>
                </div>
            `;
        } else if (shortfall >= 0 && !isAggressive) {
            scheduleHTML += `
                <div class="mt-4 p-3 bg-emerald-50 rounded-lg border border-emerald-200">
                    <div class="flex items-center gap-2 text-emerald-700">
                        <span>✓</span>
                        <span class="text-sm">Your corpus supports ${formatCurrency(shortfall)}/mo above expenses using ${strategyLabel}. 
                        This gives you buffer for inflation, healthcare, and unexpected expenses.</span>
                    </div>
                </div>
            `;
        }
        
        // Detailed asset breakdown per phase
        scheduleHTML += `
            <div class="mt-6">
                <h5 class="font-semibold text-slate-700 mb-3">📋 Detailed Asset Withdrawal Guide</h5>
                <div class="space-y-3">
        `;
        
        for (const phase of phases) {
            if (!phase.assets || phase.assets.length === 0) continue;
            
            const color = colorMap[phase.color] || 'slate';
            scheduleHTML += `
                <div class="p-3 bg-${color}-50 rounded-lg border border-${color}-200">
                    <div class="flex items-center gap-2 mb-2">
                        <span class="px-2 py-0.5 text-xs rounded bg-${color}-200 text-${color}-700 font-medium">Phase ${phase.priority}</span>
                        <span class="text-sm font-medium text-${color}-800">${phase.name}</span>
                    </div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs">
            `;
            
            for (const asset of phase.assets.slice(0, 6)) {
                scheduleHTML += `
                    <div class="p-2 bg-white rounded border border-${color}-100">
                        <div class="flex justify-between items-center">
                            <span class="font-medium text-slate-700">${asset.name || asset.type}</span>
                            <span class="font-mono text-${color}-700">${formatCurrency(asset.projectedValueAtRetirement, true)}</span>
                        </div>
                        ${asset.withdrawalTip ? `<div class="text-slate-500 mt-1">💡 ${asset.withdrawalTip}</div>` : ''}
                    </div>
                `;
            }
            
            if (phase.assets.length > 6) {
                scheduleHTML += `<div class="p-2 text-slate-500">+${phase.assets.length - 6} more assets...</div>`;
            }
            
            scheduleHTML += `
                    </div>
                </div>
            `;
        }
        
        scheduleHTML += `
                </div>
            </div>
        `;
        
        // Tax optimization tips from backend
        if (strategy.taxOptimizationTips && strategy.taxOptimizationTips.length > 0) {
            scheduleHTML += `
                <div class="mt-6 p-4 bg-gradient-to-r from-amber-50 to-yellow-50 rounded-lg border border-amber-200">
                    <h5 class="font-semibold text-amber-800 mb-3">🎯 Tax Optimization Tips</h5>
                    <div class="grid grid-cols-2 gap-3">
            `;
            
            for (const tip of strategy.taxOptimizationTips) {
                scheduleHTML += `
                    <div class="p-2 bg-white rounded border border-amber-100">
                        <div class="font-medium text-sm text-slate-700">${tip.title}</div>
                        <div class="text-xs text-slate-500 mt-1">${tip.description}</div>
                        <div class="text-xs text-amber-600 mt-1 font-medium">${tip.savingsEstimate}</div>
                    </div>
                `;
            }
            
            scheduleHTML += `
                    </div>
                </div>
            `;
        }
        
        container.innerHTML = scheduleHTML;
        
    } catch (error) {
        console.error('Error rendering withdrawal schedule:', error);
        container.innerHTML = '<div class="text-center py-4 text-danger-500">Error loading data. Please refresh.</div>';
    }
}

// ==================== ENDING EXPENSES ====================

// Load and render ending expenses (time-bound expenses with investment opportunities)
async function loadEndingExpensesData() {
    const container = document.getElementById('ending-expenses-container');
    if (!container) return;
    
    container.innerHTML = '<div class="text-center py-6 text-slate-400">Loading...</div>';
    
    try {
        const params = getParamsFromInputs();
        const currentAge = params.currentAge || 35;
        const retirementAge = params.retirementAge || 60;
        
        const response = await api.expenses.getInvestmentOpportunities(currentAge, retirementAge);
        // API returns { freedUpByYear: [...], totalMonthlyFreedUpByRetirement: ..., etc }
        const opportunities = response?.freedUpByYear || [];
        
        renderEndingExpenses(container, opportunities, currentAge, retirementAge);
    } catch (error) {
        console.error('Error loading ending expenses:', error);
        container.innerHTML = `
            <div class="text-center py-6 text-slate-400">
                <div class="text-4xl mb-2">🎓</div>
                <div>No time-bound expenses found</div>
                <div class="text-xs mt-1">Add school fees, tuition, or other temporary expenses to see investment opportunities</div>
                <a href="expenses.html" class="mt-4 inline-block px-4 py-2 bg-primary-500 text-white rounded-lg text-sm">
                    Add Expense →
                </a>
            </div>
        `;
    }
}

// Render ending expenses with investment opportunity analysis
function renderEndingExpenses(container, opportunities, currentAge, retirementAge) {
    if (!opportunities || opportunities.length === 0) {
        container.innerHTML = `
            <div class="text-center py-6 text-slate-400">
                <div class="text-4xl mb-2">🎓</div>
                <div>No time-bound expenses found</div>
                <div class="text-xs mt-1">Add school fees, tuition, or other temporary expenses to see investment opportunities</div>
                <a href="expenses.html" class="mt-4 inline-block px-4 py-2 bg-primary-500 text-white rounded-lg text-sm">
                    Add Expense →
                </a>
            </div>
        `;
        return;
    }
    
    // Calculate totals
    // Backend returns: monthlyFreedUp, potentialCorpusAt12Percent
    let totalFreedUpMonthly = 0;
    let totalPotentialCorpus = 0;
    opportunities.forEach(opp => {
        totalFreedUpMonthly += opp.monthlyFreedUp || opp.freedMonthlyAmount || 0;
        totalPotentialCorpus += opp.potentialCorpusAt12Percent || opp.potentialCorpusIfInvested || 0;
    });
    
    const currentYear = new Date().getFullYear();
    const retirementYear = currentYear + (retirementAge - currentAge);
    
    let html = `
        <!-- Summary Banner -->
        <div class="mb-6 p-4 bg-gradient-to-r from-purple-50 to-indigo-50 rounded-xl border border-purple-200">
            <div class="flex items-center justify-between">
                <div>
                    <div class="text-sm text-purple-600 font-medium">💡 Investment Opportunity from Ending Expenses</div>
                    <div class="text-2xl font-bold text-purple-700">${formatCurrency(totalPotentialCorpus, true)}</div>
                    <div class="text-xs text-purple-500">Potential additional corpus if invested</div>
                </div>
                <div class="text-right">
                    <div class="text-sm text-slate-600">${opportunities.length} expense(s) ending before retirement</div>
                    <div class="text-lg font-semibold text-slate-700">${formatCurrency(totalFreedUpMonthly)}/mo freed up</div>
                </div>
            </div>
        </div>
        
        <!-- Explanation -->
        <div class="mb-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <h4 class="text-sm font-semibold text-blue-700 mb-2">📚 How This Works</h4>
            <p class="text-sm text-blue-600">
                Time-bound expenses (like school fees, coaching) will end at a certain date. 
                When they end, that money can be redirected to investments. 
                If invested at 12% returns, this freed-up amount can generate significant corpus by retirement!
            </p>
        </div>
        
        <!-- Timeline -->
        <h4 class="text-lg font-semibold text-slate-800 mb-4">📅 Expense End Timeline</h4>
        <div class="space-y-4 mb-6">
    `;
    
    // Sort by end year (backend returns 'year', frontend may use 'endsInYear')
    opportunities.sort((a, b) => (a.year || a.endsInYear || 0) - (b.year || b.endsInYear || 0));
    
    opportunities.forEach(opp => {
        // Backend returns: year, monthlyFreedUp, potentialCorpusAt12Percent, endingExpenses[]
        const endYear = opp.year || opp.endsInYear || currentYear;
        const yearsLeft = endYear - currentYear;
        const yearsToInvest = retirementYear - endYear;
        
        // Get expense names from endingExpenses array
        const expenseNames = (opp.endingExpenses || []).map(e => e.name).join(', ') || opp.expenseName || 'Expense';
        const categories = (opp.endingExpenses || []).map(e => e.category).filter(Boolean);
        const category = categories[0] || opp.category || '';
        const categoryIcon = getCategoryIcon(category);
        const categoryDisplay = category ? category.replace(/_/g, ' ') : 'Other';
        
        // Use correct field names
        const monthlyFreedUp = opp.monthlyFreedUp || opp.freedMonthlyAmount || 0;
        const potentialCorpus = opp.potentialCorpusAt12Percent || opp.potentialCorpusIfInvested || 0;
        
        html += `
            <div class="flex items-center gap-4 p-4 bg-white rounded-lg border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
                <div class="w-16 h-16 rounded-full bg-purple-100 text-purple-700 flex flex-col items-center justify-center flex-shrink-0">
                    <span class="text-lg font-bold">${endYear}</span>
                    <span class="text-xs">${yearsLeft}y left</span>
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                        <span class="text-lg">${categoryIcon}</span>
                        <span class="font-semibold text-slate-800 truncate">${expenseNames}</span>
                        <span class="px-2 py-0.5 text-xs rounded-full bg-slate-100 text-slate-600">${categoryDisplay}</span>
                    </div>
                    <div class="text-sm text-slate-500 mt-1">
                        ${formatCurrency(monthlyFreedUp)}/month will be available after ${endYear}
                    </div>
                </div>
                <div class="text-right flex-shrink-0">
                    <div class="font-mono text-lg font-bold text-emerald-600">+${formatCurrency(potentialCorpus, true)}</div>
                    <div class="text-xs text-slate-400">If invested for ${yearsToInvest} years @ 12%</div>
                </div>
            </div>
        `;
    });
    
    html += `
        </div>
        
        <!-- Action Card -->
        <div class="p-4 bg-emerald-50 rounded-lg border border-emerald-200">
            <div class="flex items-center justify-between">
                <div>
                    <h4 class="font-semibold text-emerald-800">🚀 Recommended Action</h4>
                    <p class="text-sm text-emerald-600 mt-1">
                        When these expenses end, set up automatic SIP transfers for the freed-up amount. 
                        This will significantly boost your retirement corpus!
                    </p>
                </div>
                <button onclick="switchAnalysisTab('strategy')" class="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 flex-shrink-0">
                    Add to Strategy →
                </button>
            </div>
        </div>
        
        <!-- Calculation Breakdown -->
        <div class="mt-6 p-4 bg-slate-50 rounded-lg">
            <h4 class="text-sm font-semibold text-slate-700 mb-3">📊 Calculation Assumptions</h4>
            <div class="grid grid-cols-3 gap-4 text-sm">
                <div>
                    <span class="text-slate-500">Investment Return:</span>
                    <span class="font-medium text-slate-700 ml-2">12% p.a.</span>
                </div>
                <div>
                    <span class="text-slate-500">Retirement Year:</span>
                    <span class="font-medium text-slate-700 ml-2">${retirementYear}</span>
                </div>
                <div>
                    <span class="text-slate-500">Retirement Age:</span>
                    <span class="font-medium text-slate-700 ml-2">${retirementAge}</span>
                </div>
            </div>
        </div>
    `;
    
    container.innerHTML = html;
}

// Helper function to get category icon
function getCategoryIcon(category) {
    const icons = {
        'SCHOOL_FEE': '🎒',
        'COLLEGE_FEE': '🎓',
        'TUITION': '📖',
        'COACHING': '✏️',
        'BOOKS_SUPPLIES': '📕',
        'HOSTEL': '🏨',
        'EDUCATION': '📚',
        'CHILDCARE': '👶',
        'DAYCARE': '🧒',
        'ELDERLY_CARE': '👴',
        'RENT': '🏠',
        'OTHER': '📋'
    };
    return icons[category] || '📋';
}

// ==================== STRATEGY PLANNER ====================

// Load additional data needed for strategy planner
async function loadStrategyData(retirementData) {
    try {
        // Get current params for expense opportunities
        const params = getParamsFromInputs();
        const currentAge = params.currentAge || 35;
        const retirementAge = params.retirementAge || 60;
        
        const [netWorth, loans, goals, expenseOpportunitiesResponse] = await Promise.all([
            api.analysis.getNetWorth().catch(() => null),
            api.loans.getAll().catch(() => []),
            api.goals.getAll().catch(() => []),
            api.expenses.getInvestmentOpportunities(currentAge, retirementAge).catch(() => ({}))
        ]);
        
        // API returns { freedUpByYear: [...], totalMonthlyFreedUpByRetirement: ..., etc }
        const expenseOpportunities = expenseOpportunitiesResponse?.freedUpByYear || [];
        
        strategyData.netWorth = netWorth;
        strategyData.loans = loans || [];
        strategyData.goals = goals || [];
        strategyData.expenseOpportunities = expenseOpportunities;
        
        // If strategy tab is visible, render it
        const strategyPanel = document.getElementById('panel-strategy');
        if (strategyPanel && !strategyPanel.classList.contains('hidden')) {
            renderStrategyPlanner(retirementData);
        }
    } catch (error) {
        console.error('Error loading strategy data:', error);
    }
}

// Render the Strategy Planner tab
function renderStrategyPlanner(data) {
    if (!data) {
        console.error('No data provided to renderStrategyPlanner');
        return;
    }
    
    console.log('Rendering Strategy Planner with data:', data);
    
    const gapAnalysis = data.gapAnalysis || {};
    const summary = data.summary || {};
    const maturingData = data.maturingBeforeRetirement || {};
    
    console.log('Gap Analysis:', gapAnalysis);
    console.log('Summary:', summary);
    console.log('Strategy Data:', strategyData);
    
    // 1. Render Corpus Status
    renderCorpusStatus(summary, gapAnalysis);
    
    // 2. Render Monthly Savings Allocation
    renderSavingsAllocation(gapAnalysis, summary);
    
    // 3. Render Action Timeline
    renderActionTimeline(data, strategyData, gapAnalysis);
    
    // 4. Render What-If Scenarios
    renderWhatIfScenarios(summary, gapAnalysis, maturingData);
}

// Render Corpus Status Summary
function renderCorpusStatus(summary, gapAnalysis) {
    const projectedEl = document.getElementById('strategy-projected');
    const requiredEl = document.getElementById('strategy-required');
    const gapEl = document.getElementById('strategy-gap');
    const gapBox = document.getElementById('strategy-gap-box');
    const statusBadge = document.getElementById('strategy-status-badge');
    
    const projected = summary.finalCorpus || 0;
    const required = gapAnalysis.requiredCorpus || 0;
    const gap = gapAnalysis.corpusGap || 0;
    const isOnTrack = gapAnalysis.isOnTrack;
    
    projectedEl.textContent = formatCurrency(projected, true);
    requiredEl.textContent = formatCurrency(required, true);
    
    if (isOnTrack || gap <= 0) {
        gapEl.textContent = '+' + formatCurrency(Math.abs(gap), true);
        gapEl.className = 'text-lg font-bold text-emerald-600';
        gapBox.className = 'bg-emerald-50 rounded-lg p-4 text-center border border-emerald-200';
        statusBadge.textContent = '✅ On Track';
        statusBadge.className = 'text-sm text-emerald-600 bg-emerald-100 px-2 py-1 rounded-full';
    } else {
        gapEl.textContent = '-' + formatCurrency(gap, true);
        gapEl.className = 'text-lg font-bold text-danger-600';
        gapBox.className = 'bg-danger-50 rounded-lg p-4 text-center border border-danger-200';
        statusBadge.textContent = '⚠️ Shortfall';
        statusBadge.className = 'text-sm text-danger-600 bg-danger-100 px-2 py-1 rounded-full';
    }
}

// Render Monthly Savings Allocation
function renderSavingsAllocation(gapAnalysis, summary) {
    const monthlySavingsEl = document.getElementById('strategy-monthly-savings');
    const breakdownEl = document.getElementById('strategy-allocation-breakdown');
    
    if (!monthlySavingsEl || !breakdownEl) {
        console.error('Missing DOM elements for savings allocation');
        return;
    }
    
    console.log('Rendering savings allocation with:', gapAnalysis);
    
    // Use the correctly calculated values from backend
    const monthlyIncome = gapAnalysis.monthlyIncome || 0;
    const totalExpenses = gapAnalysis.totalCurrentMonthlyExpenses || gapAnalysis.currentMonthlyExpenses || 0;
    const monthlyEMI = gapAnalysis.monthlyEMI || 0;
    const monthlySIP = gapAnalysis.monthlySIP || 0;
    
    // Net savings = Income - Expenses - EMIs (SIPs are investments, not expenses)
    const netMonthlySavings = gapAnalysis.netMonthlySavings || (monthlyIncome - totalExpenses - monthlyEMI);
    const availableSavings = gapAnalysis.availableMonthlySavings || (netMonthlySavings - monthlySIP);
    
    monthlySavingsEl.innerHTML = `
        ${formatCurrency(netMonthlySavings)}
        <div class="text-xs text-slate-500 font-normal mt-1">
            Income: ${formatCurrency(monthlyIncome)} − Expenses: ${formatCurrency(totalExpenses)} − EMIs: ${formatCurrency(monthlyEMI)}
        </div>
    `;
    
    if (netMonthlySavings <= 0) {
        breakdownEl.innerHTML = `
            <div class="text-center py-4 text-danger-500">
                <div class="text-2xl mb-2">⚠️</div>
                <div class="font-medium">Your expenses + EMIs exceed income.</div>
                <div class="text-sm mt-2">Focus on reducing expenses or increasing income first.</div>
                <div class="text-xs mt-2 text-slate-500">
                    Income: ${formatCurrency(monthlyIncome)} | Expenses: ${formatCurrency(totalExpenses)} | EMIs: ${formatCurrency(monthlyEMI)}
                </div>
            </div>
        `;
        return;
    }
    
    const monthlySavings = netMonthlySavings;
    
    // Calculate recommended allocation
    const netWorthData = strategyData?.netWorth || {};
    const assetBreakdown = netWorthData.assetBreakdown || {};
    const currentCash = assetBreakdown.CASH || 0;
    const monthlyExpenses = totalExpenses > 0 ? totalExpenses : 50000; // Default if no expenses
    const emergencyFund = monthlyExpenses * 6;
    const emergencyGap = Math.max(0, emergencyFund - currentCash);
    
    const corpusGap = gapAnalysis.corpusGap || 0;
    const yearsToRetirement = summary.yearsToRetirement || 25;
    const monthsToRetirement = yearsToRetirement * 12;
    
    // Required monthly to close corpus gap (assuming 10% returns)
    const requiredMonthlySIP = corpusGap > 0 ? calculateRequiredSIP(corpusGap, 10, yearsToRetirement) : 0;
    
    // Active loans EMI
    const loansData = strategyData?.loans || [];
    const totalEMI = Array.isArray(loansData) ? loansData.reduce((sum, l) => sum + (l.emi || 0), 0) : 0;
    
    // Goals
    const goalsData = strategyData?.goals || [];
    const activeGoals = Array.isArray(goalsData) ? goalsData.filter(g => g.targetAmount > 0) : [];
    const goalsSavings = activeGoals.length > 0 ? monthlySavings * 0.1 : 0;
    
    // Allocation logic
    let allocations = [];
    let remaining = monthlySavings;
    
    // 1. Emergency Fund (if gap exists) - Priority 1
    if (emergencyGap > 0) {
        const emergencyAllocation = Math.min(remaining * 0.3, Math.ceil(emergencyGap / 12));
        allocations.push({
            name: '🆘 Emergency Fund',
            amount: emergencyAllocation,
            description: `Build ${formatCurrency(emergencyFund)} emergency fund (currently ${formatCurrency(currentCash)})`,
            priority: 'High',
            color: 'bg-amber-100 border-amber-300'
        });
        remaining -= emergencyAllocation;
    }
    
    // 2. Loan Prepayment (if significant savings possible) - Priority 2
    if (totalEMI > 0 && remaining > totalEMI * 0.2) {
        const prepaymentAllocation = Math.min(remaining * 0.2, totalEMI);
        allocations.push({
            name: '🏦 Loan Prepayment',
            amount: prepaymentAllocation,
            description: `Prepay loans to save interest and free up ${formatCurrency(totalEMI)}/mo sooner`,
            priority: 'Medium',
            color: 'bg-blue-100 border-blue-300'
        });
        remaining -= prepaymentAllocation;
    }
    
    // 3. Retirement Corpus (main allocation) - Priority 3
    if (corpusGap > 0) {
        const corpusAllocation = Math.min(remaining * 0.6, requiredMonthlySIP);
        allocations.push({
            name: '🏖️ Retirement Corpus',
            amount: corpusAllocation,
            description: `Invest in MF/NPS to close ${formatCurrency(corpusGap, true)} gap`,
            priority: 'High',
            color: 'bg-primary-100 border-primary-300'
        });
        remaining -= corpusAllocation;
    } else {
        // Surplus scenario
        allocations.push({
            name: '🏖️ Retirement (Already On Track!)',
            amount: remaining * 0.5,
            description: 'Continue investing to build even larger corpus',
            priority: 'Low',
            color: 'bg-emerald-100 border-emerald-300'
        });
        remaining -= remaining * 0.5;
    }
    
    // 4. Goals - Priority 4
    if (activeGoals.length > 0) {
        allocations.push({
            name: '🎯 Goals Fund',
            amount: Math.min(remaining * 0.3, remaining),
            description: `For ${activeGoals.length} active goal(s): ${activeGoals.slice(0, 2).map(g => g.name).join(', ')}`,
            priority: 'Medium',
            color: 'bg-purple-100 border-purple-300'
        });
        remaining -= Math.min(remaining * 0.3, remaining);
    }
    
    // 5. Flexible/Discretionary
    if (remaining > 0) {
        allocations.push({
            name: '💰 Flexible Savings',
            amount: remaining,
            description: 'Additional savings for opportunities or lifestyle',
            priority: 'Low',
            color: 'bg-slate-100 border-slate-300'
        });
    }
    
    // Render allocations
    breakdownEl.innerHTML = allocations.map(alloc => `
        <div class="flex items-center gap-3 p-3 ${alloc.color} border rounded-lg">
            <div class="flex-1">
                <div class="flex items-center justify-between">
                    <span class="font-medium text-slate-800">${alloc.name}</span>
                    <span class="font-bold text-slate-700">${formatCurrency(alloc.amount)}/mo</span>
                </div>
                <div class="text-xs text-slate-600 mt-1">${alloc.description}</div>
            </div>
            <span class="text-xs px-2 py-1 rounded ${alloc.priority === 'High' ? 'bg-danger-100 text-danger-700' : alloc.priority === 'Medium' ? 'bg-amber-100 text-amber-700' : 'bg-slate-200 text-slate-600'}">${alloc.priority}</span>
        </div>
    `).join('');
}

// Render Action Timeline
function renderActionTimeline(retirementData, stratData, gapAnalysis) {
    const timelineEl = document.getElementById('strategy-timeline');
    if (!timelineEl) return;
    
    const matrix = retirementData?.matrix || [];
    const maturingData = retirementData?.maturingBeforeRetirement || {};
    const summary = retirementData?.summary || {};
    const currentYear = new Date().getFullYear();
    const yearsToRetirement = summary.yearsToRetirement || 25;
    const retirementYear = currentYear + yearsToRetirement;
    
    let timelineItems = [];
    
    // 1. Add loan end dates (with null safety)
    const loans = stratData?.loans || [];
    loans.forEach(loan => {
        if (loan.endDate) {
            const endYear = new Date(loan.endDate).getFullYear();
            if (endYear > currentYear && endYear < currentYear + 30) {
                timelineItems.push({
                    year: endYear,
                    type: 'loan_end',
                    icon: '🎉',
                    title: `${loan.name || 'Loan'} Paid Off`,
                    description: `${formatCurrency(loan.emi || 0)}/mo becomes available for investment`,
                    action: 'Redirect EMI to MF SIP',
                    actionColor: 'text-emerald-600'
                });
            }
        }
    });
    
    // 2. Add investment maturities
    if (maturingData.maturingInvestments) {
        maturingData.maturingInvestments.forEach(inv => {
            if (inv.maturityDate) {
                const year = new Date(inv.maturityDate).getFullYear();
                timelineItems.push({
                    year: year,
                    type: 'investment_maturity',
                    icon: '💰',
                    title: `${inv.name} Matures`,
                    description: `${formatCurrency(inv.expectedMaturityValue)} available`,
                    action: 'Reinvest in higher-return assets',
                    actionColor: 'text-primary-600'
                });
            }
        });
    }
    
    // 3. Add insurance maturities
    if (maturingData.maturingInsurance) {
        maturingData.maturingInsurance.forEach(ins => {
            if (ins.maturityDate) {
                const year = new Date(ins.maturityDate).getFullYear();
                timelineItems.push({
                    year: year,
                    type: 'insurance_maturity',
                    icon: '🛡️',
                    title: `${ins.name} Matures`,
                    description: `${formatCurrency(ins.expectedMaturityValue)} available`,
                    action: 'Add to retirement corpus',
                    actionColor: 'text-purple-600'
                });
            }
        });
    }
    
    // 4. Add goal milestones (with null safety)
    const goals = stratData?.goals || [];
    goals.forEach(goal => {
        if (goal.targetYear && goal.targetYear > currentYear) {
            timelineItems.push({
                year: goal.targetYear,
                type: 'goal',
                icon: '🎯',
                title: goal.name,
                description: `${formatCurrency(goal.targetAmount)} needed`,
                action: 'Ensure funds are available',
                actionColor: 'text-amber-600'
            });
        }
    });
    
    // 5. Add expense end dates (time-bound expenses)
    const expenseOpportunities = stratData?.expenseOpportunities || [];
    expenseOpportunities.forEach(opp => {
        // Backend returns: year, monthlyFreedUp, potentialCorpusAt12Percent, endingExpenses[]
        const oppYear = opp.year || opp.endsInYear;
        if (oppYear && oppYear > currentYear && oppYear < retirementYear) {
            const expenseNames = (opp.endingExpenses || []).map(e => e.name).join(', ') || 'Expense';
            timelineItems.push({
                year: oppYear,
                type: 'expense_end',
                icon: '🎓',
                title: `${expenseNames} Ends`,
                description: `${formatCurrency(opp.monthlyFreedUp || opp.freedMonthlyAmount || 0)}/mo freed up`,
                action: `Invest for ${formatCurrency(opp.potentialCorpusAt12Percent || opp.potentialCorpusIfInvested || 0, true)} corpus`,
                actionColor: 'text-purple-600'
            });
        }
    });
    
    // 5. Add corpus gap closure point (if on track)
    const corpusGap = gapAnalysis.corpusGap || 0;
    if (corpusGap <= 0) {
        const summary = retirementData.summary || {};
        timelineItems.push({
            year: currentYear + (summary.yearsToRetirement || 25),
            type: 'retirement',
            icon: '🏖️',
            title: 'Retirement Target Met!',
            description: `Projected corpus: ${formatCurrency(summary.finalCorpus, true)}`,
            action: 'You\'re on track!',
            actionColor: 'text-emerald-600'
        });
    } else {
        // Find year when action needed
        timelineItems.push({
            year: currentYear,
            type: 'action_needed',
            icon: '⚠️',
            title: 'Action Required Now',
            description: `Increase SIP by ${formatCurrency(gapAnalysis.additionalSIPRequired || 5000)}/mo`,
            action: 'Close corpus gap',
            actionColor: 'text-danger-600'
        });
    }
    
    // Sort by year
    timelineItems.sort((a, b) => a.year - b.year);
    
    // Render timeline
    if (timelineItems.length === 0) {
        timelineEl.innerHTML = '<div class="text-center py-4 text-slate-400">No upcoming events</div>';
        return;
    }
    
    timelineEl.innerHTML = timelineItems.map((item, idx) => `
        <div class="flex gap-4 ${idx !== timelineItems.length - 1 ? 'pb-4 border-l-2 border-slate-200 ml-3' : 'ml-3'}">
            <div class="flex-shrink-0 -ml-5 w-8 h-8 rounded-full bg-white border-2 ${item.type === 'action_needed' ? 'border-danger-400' : 'border-slate-300'} flex items-center justify-center text-lg">
                ${item.icon}
            </div>
            <div class="flex-1 bg-slate-50 rounded-lg p-3 border border-slate-200">
                <div class="flex items-center justify-between mb-1">
                    <span class="font-semibold text-slate-800">${item.title}</span>
                    <span class="text-xs font-medium text-slate-500">${item.year}</span>
                </div>
                <div class="text-sm text-slate-600 mb-2">${item.description}</div>
                <div class="text-xs ${item.actionColor} font-medium">→ ${item.action}</div>
            </div>
        </div>
    `).join('');
}

// Render What-If Scenarios
function renderWhatIfScenarios(summary, gapAnalysis, maturingData) {
    const scenariosEl = document.getElementById('strategy-scenarios');
    if (!scenariosEl) return;
    
    const projected = summary.finalCorpus || 0;
    const required = gapAnalysis.requiredCorpus || 0;
    const gap = gapAnalysis.corpusGap || 0;
    const currentYear = new Date().getFullYear();
    const yearsToRetirement = summary.yearsToRetirement || 25;
    const retirementYear = currentYear + yearsToRetirement;
    
    // Get illiquid assets (with null safety)
    const netWorthData = strategyData?.netWorth || {};
    const assetBreakdown = netWorthData.assetBreakdown || {};
    const goldValue = assetBreakdown.GOLD || 0;
    const realEstateValue = assetBreakdown.REAL_ESTATE || 0;
    const illiquidTotal = goldValue + realEstateValue;
    
    // Get maturing investments
    const maturingTotal = maturingData?.totalMaturingBeforeRetirement || 0;
    
    // Calculate post-loan available (with null safety)
    const loansData = strategyData?.loans || [];
    const totalLoansEMI = Array.isArray(loansData) ? loansData.reduce((sum, l) => sum + (l.emi || 0), 0) : 0;
    const avgLoanYearsLeft = Array.isArray(loansData) && loansData.length > 0 
        ? Math.max(...loansData.map(l => {
            if (!l.endDate) return 0;
            return Math.max(0, new Date(l.endDate).getFullYear() - new Date().getFullYear());
          }))
        : 0;
    
    let scenarios = [];
    
    // Calculate optimal year to sell illiquid assets (if needed)
    function calculateOptimalSellYear(illiquidValue, gap, yearsToRet) {
        if (gap <= 0) return null; // Not needed
        // Find the year where selling illiquid + growth meets the gap
        for (let y = yearsToRet; y >= 0; y--) {
            const corpusAtYear = projected * Math.pow(1.10, y); // 10% growth
            const illiquidGrowth = illiquidValue * Math.pow(1.08, yearsToRet - y); // 8% for illiquid
            if ((corpusAtYear + illiquidGrowth) >= required) {
                return currentYear + (yearsToRet - y);
            }
        }
        return currentYear + yearsToRet; // At retirement if nothing else works
    }
    
    // Scenario 1: Sell Illiquid Assets
    if (illiquidTotal > 0) {
        const newCorpus = projected + illiquidTotal;
        const canMeet = newCorpus >= required;
        const optimalSellYear = gap > 0 ? calculateOptimalSellYear(illiquidTotal, gap, yearsToRetirement) : null;
        
        scenarios.push({
            id: 'sell_illiquid',
            icon: '🏠',
            title: 'Sell Illiquid Assets',
            description: `Gold (${formatCurrency(goldValue, true)}) + Real Estate (${formatCurrency(realEstateValue, true)})`,
            impact: `+${formatCurrency(illiquidTotal, true)} to corpus`,
            timing: optimalSellYear ? `📅 Optimal time: ${optimalSellYear} (${optimalSellYear - currentYear}y from now)` : null,
            result: canMeet ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpus, true)} short`,
            resultClass: canMeet ? 'text-emerald-600' : 'text-amber-600',
            bgClass: canMeet ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200',
            chartData: null, // Will be generated dynamically in showScenarioChart
            enabled: false,
            value: illiquidTotal,
            sellYear: optimalSellYear
        });
    }
    
    // Scenario 2: Reinvest All Maturities
    if (maturingTotal > 0) {
        // Calculate average maturity year from actual maturing investments
        const maturingInvestments = maturingData?.maturingInvestments || [];
        const maturingInsurance = maturingData?.maturingInsurance || [];
        let totalYears = 0;
        let count = 0;
        
        maturingInvestments.forEach(inv => {
            if (inv.maturityDate) {
                const maturityYear = new Date(inv.maturityDate).getFullYear();
                totalYears += (maturityYear - currentYear);
                count++;
            }
        });
        
        maturingInsurance.forEach(ins => {
            if (ins.maturityDate) {
                const maturityYear = new Date(ins.maturityDate).getFullYear();
                totalYears += (maturityYear - currentYear);
                count++;
            }
        });
        
        const avgMaturityYear = count > 0 ? currentYear + Math.round(totalYears / count) : currentYear + Math.floor(yearsToRetirement / 2);
        const avgYearsToMaturity = avgMaturityYear - currentYear;
        const yearsAfterMaturity = yearsToRetirement - avgYearsToMaturity;
        
        // Compound at 12% for remaining years after maturity
        const growthFactor = yearsAfterMaturity > 0 ? Math.pow(1.12, yearsAfterMaturity) : 1;
        const futureValue = maturingTotal * growthFactor;
        const newCorpus = projected + futureValue;
        const canMeet = newCorpus >= required;
        
        scenarios.push({
            id: 'reinvest_maturities',
            icon: '💰',
            title: 'Reinvest All Maturities',
            description: `Reinvest ${formatCurrency(maturingTotal, true)} at 12% returns`,
            impact: `+${formatCurrency(futureValue, true)} additional corpus`,
            timing: `📅 Average maturity: ${avgMaturityYear} (${avgYearsToMaturity}y from now)`,
            result: canMeet ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpus, true)} short`,
            resultClass: canMeet ? 'text-emerald-600' : 'text-amber-600',
            bgClass: canMeet ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200',
            chartData: null, // Will be generated dynamically in showScenarioChart
            enabled: false,
            value: maturingTotal,
            maturityYear: avgMaturityYear
        });
    }
    
    // Scenario 3: Redirect Loan EMIs After Payoff
    if (totalLoansEMI > 0 && avgLoanYearsLeft > 0) {
        const yearsAvailable = yearsToRetirement - avgLoanYearsLeft;
        if (yearsAvailable > 0) {
            const additionalCorpus = calculateSIPFutureValue(totalLoansEMI, 12, yearsAvailable);
            const newCorpus = projected + additionalCorpus;
            const canMeet = newCorpus >= required;
            scenarios.push({
                id: 'redirect_emi',
                icon: '🎯',
                title: 'Redirect Loan EMIs',
                description: `Invest ${formatCurrency(totalLoansEMI)}/mo for ${yearsAvailable}y after loans end`,
                impact: `+${formatCurrency(additionalCorpus, true)} additional corpus`,
                timing: `📅 Start from ${currentYear + avgLoanYearsLeft} (after loan ends)`,
                result: canMeet ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpus, true)} short`,
                resultClass: canMeet ? 'text-emerald-600' : 'text-amber-600',
                bgClass: canMeet ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200',
                chartData: null, // Will be generated dynamically in showScenarioChart
                enabled: false,
                value: totalLoansEMI,
                startYear: currentYear + avgLoanYearsLeft
            });
        }
    }
    
    // Scenario 4: Increase SIP by 20%
    const currentSIP = gapAnalysis.monthlySIP || summary.startingBalances?.mfSipMonthly || 10000;
    const increasedSIP = currentSIP * 1.2;
    const sipIncrease = currentSIP * 0.2;
    const additionalFromIncrease = calculateSIPFutureValue(sipIncrease, 12, yearsToRetirement);
    const newCorpusFromIncrease = projected + additionalFromIncrease;
    const canMeetWithIncrease = newCorpusFromIncrease >= required;
    
    scenarios.push({
        id: 'increase_sip',
        icon: '📈',
        title: 'Increase SIP by 20%',
        description: `From ${formatCurrency(currentSIP)}/mo to ${formatCurrency(increasedSIP)}/mo`,
        impact: `+${formatCurrency(additionalFromIncrease, true)} additional corpus`,
        timing: '📅 Start immediately',
        result: canMeetWithIncrease ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpusFromIncrease, true)} short`,
        resultClass: canMeetWithIncrease ? 'text-emerald-600' : 'text-amber-600',
        bgClass: canMeetWithIncrease ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200',
            chartData: null, // Will be generated dynamically in showScenarioChart
        enabled: false,
        value: sipIncrease
    });
    
    // Scenario 5: Invest Freed-Up Expenses (from time-bound expenses like school fees)
    const expenseOpportunities = strategyData?.expenseOpportunities || [];
    if (expenseOpportunities.length > 0) {
        // Calculate total potential corpus from all ending expenses
        // Backend returns: year, monthlyFreedUp, potentialCorpusAt12Percent, endingExpenses[]
        let totalFreedUpMonthly = 0;
        let totalPotentialCorpus = 0;
        let earliestEndYear = retirementYear;
        let latestEndYear = currentYear;
        
        expenseOpportunities.forEach(opp => {
            totalFreedUpMonthly += opp.monthlyFreedUp || opp.freedMonthlyAmount || 0;
            totalPotentialCorpus += opp.potentialCorpusAt12Percent || opp.potentialCorpusIfInvested || 0;
            const oppYear = opp.year || opp.endsInYear || retirementYear;
            if (oppYear < earliestEndYear) earliestEndYear = oppYear;
            if (oppYear > latestEndYear) latestEndYear = oppYear;
        });
        
        if (totalFreedUpMonthly > 0) {
            const newCorpusFromExpenses = projected + totalPotentialCorpus;
            const canMeetWithExpenses = newCorpusFromExpenses >= required;
            
            // Build description with expense names (from endingExpenses array)
            const allExpenseNames = expenseOpportunities.flatMap(o => 
                (o.endingExpenses || []).map(e => e.name) || [o.expenseName]
            ).filter(Boolean);
            const expenseNames = allExpenseNames.slice(0, 3).join(', ');
            const moreCount = allExpenseNames.length > 3 ? ` +${allExpenseNames.length - 3} more` : '';
            
            scenarios.push({
                id: 'freed_expenses',
                icon: '🎓',
                title: 'Invest Freed-Up Expenses',
                description: `${expenseNames}${moreCount} (${formatCurrency(totalFreedUpMonthly)}/mo total)`,
                impact: `+${formatCurrency(totalPotentialCorpus, true)} additional corpus`,
                timing: `📅 Expenses end: ${earliestEndYear}${latestEndYear > earliestEndYear ? ' - ' + latestEndYear : ''}`,
                result: canMeetWithExpenses ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpusFromExpenses, true)} short`,
                resultClass: canMeetWithExpenses ? 'text-emerald-600' : 'text-amber-600',
                bgClass: 'bg-purple-50 border-purple-200',
                chartData: null,
                enabled: false,
                value: totalFreedUpMonthly,
                potentialCorpus: totalPotentialCorpus,
                opportunities: expenseOpportunities,
                startYear: earliestEndYear
            });
        }
    }
    
    // Store scenarios for later use
    window.whatIfScenarios = scenarios;
    
    // Render scenarios
    if (scenarios.length === 0) {
        scenariosEl.innerHTML = '<div class="col-span-2 text-center py-4 text-slate-400">No scenarios available</div>';
        return;
    }
    
    scenariosEl.innerHTML = `
        ${scenarios.map(scenario => `
            <div class="${scenario.bgClass} border rounded-lg p-4 relative">
                <div class="absolute top-2 right-2">
                    <label class="inline-flex items-center cursor-pointer">
                        <input type="checkbox" id="enable-${scenario.id}" onchange="toggleScenario('${scenario.id}')" 
                               class="sr-only peer" ${scenario.enabled ? 'checked' : ''}>
                        <div class="relative w-9 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary-500"></div>
                    </label>
                </div>
                <div class="flex items-center gap-2 mb-2">
                    <span class="text-2xl">${scenario.icon}</span>
                    <span class="font-semibold text-slate-800">${scenario.title}</span>
                </div>
                <p class="text-sm text-slate-600 mb-2">${scenario.description}</p>
                ${scenario.timing ? `<div class="text-xs text-blue-600 mb-2">${scenario.timing}</div>` : ''}
                <div class="text-sm font-medium text-primary-600 mb-2">${scenario.impact}</div>
                <div class="text-sm font-semibold ${scenario.resultClass}">${scenario.result}</div>
                <button onclick="showScenarioChart('${scenario.id}')" class="mt-2 text-xs text-primary-600 hover:text-primary-700 underline">
                    📊 View projection
                </button>
            </div>
        `).join('')}
        
        <div class="col-span-2 mt-4 p-4 bg-primary-50 border border-primary-200 rounded-lg">
            <div class="flex items-center justify-between">
                <div>
                    <h4 class="font-semibold text-slate-800">💾 Save Your Strategy</h4>
                    <p class="text-sm text-slate-500">Enable the strategies you want to follow and save for tracking</p>
                </div>
                <button onclick="saveUserStrategy()" class="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700">
                    Save Strategy
                </button>
            </div>
        </div>
    `;
}

// Generate chart data for scenario showing baseline vs strategy-applied corpus
function generateScenarioChart(currentCorpus, projectedCorpus, scenario) {
    const currentYear = new Date().getFullYear();
    const yearsToRetirement = scenario.yearsToRetirement || 25;
    const annualReturn = 0.10; // 10% annual return
    const monthlyReturn = annualReturn / 12;
    
    // Get existing monthly SIP (baseline includes this)
    const existingMonthlySIP = scenario.existingMonthlySIP || 0;
    
    const data = [];
    
    // Calculate baseline corpus growth (WITH existing SIPs - this matches backend projection)
    let baselineCorpus = currentCorpus;
    
    // Calculate strategy-applied corpus growth (with strategy from deployment year)
    let strategyCorpus = currentCorpus;
    const deploymentYear = scenario.deploymentYear || currentYear;
    const deploymentYearOffset = Math.max(0, deploymentYear - currentYear);
    
    for (let y = 0; y <= yearsToRetirement; y++) {
        // Apply strategy at deployment year (before growth for lumpsum/reinvest)
        if (y === deploymentYearOffset && scenario.type) {
            if (scenario.type === 'lumpsum' && scenario.lumpsumValue) {
                // Add lumpsum at deployment year (before growth)
                strategyCorpus += scenario.lumpsumValue;
            } else if (scenario.type === 'reinvest' && scenario.reinvestValue) {
                // Add reinvestment at deployment year (before growth)
                strategyCorpus += scenario.reinvestValue;
            }
        }
        
        // Growth for both baseline and strategy
        if (y > 0) {
            // Baseline: existing corpus grows + existing SIP contributions
            // Apply monthly growth with existing SIP
            for (let m = 0; m < 12; m++) {
                baselineCorpus = baselineCorpus * (1 + monthlyReturn) + existingMonthlySIP;
            }
            
            // Strategy: same as baseline, but with additional SIP if strategy is 'sip'
            if (scenario.type === 'sip' && scenario.monthlySIP && y >= deploymentYearOffset) {
                // Add monthly SIP contributions (existing + incremental)
                for (let m = 0; m < 12; m++) {
                    strategyCorpus = strategyCorpus * (1 + monthlyReturn) + existingMonthlySIP + scenario.monthlySIP;
                }
            } else {
                // Same as baseline (existing SIP only)
                for (let m = 0; m < 12; m++) {
                    strategyCorpus = strategyCorpus * (1 + monthlyReturn) + existingMonthlySIP;
                }
            }
        }
        
        data.push({ 
            year: y, 
            corpus: Math.round(strategyCorpus),
            baselineCorpus: Math.round(baselineCorpus),
            difference: Math.round(strategyCorpus - baselineCorpus)
        });
    }
    
    return data;
}

// Show scenario projection chart
function showScenarioChart(scenarioId) {
    const scenario = window.whatIfScenarios?.find(s => s.id === scenarioId);
    if (!scenario) return;
    
    // Get current corpus from cached retirement data
    const retirementData = window.cachedRetirementData;
    if (!retirementData) {
        alert('Please load retirement data first');
        return;
    }
    
    const summary = retirementData.summary || {};
    const currentYear = new Date().getFullYear();
    
    // Get starting corpus (current corpus)
    const startingBalances = summary.startingBalances || {};
    const currentCorpus = (startingBalances.totalStarting || 0) + 
                         (startingBalances.otherLiquidTotal || 0);
    
    // Get projected corpus (final corpus without strategy)
    const projectedCorpus = summary.finalCorpus || 0;
    const yearsToRetirement = summary.yearsToRetirement || 25;
    
    // Get existing monthly SIP from gap analysis (baseline already includes this)
    const gapAnalysis = retirementData.gapAnalysis || {};
    const existingMonthlySIP = gapAnalysis.monthlySIP || 0;
    
    // Determine deployment year based on scenario
    let deploymentYear = currentYear;
    let scenarioType = 'lumpsum';
    let scenarioValue = scenario.value || 0;
    
    if (scenario.id === 'sell_illiquid') {
        deploymentYear = scenario.sellYear || currentYear + Math.floor(yearsToRetirement / 2);
        scenarioType = 'lumpsum';
        scenarioValue = scenario.value || 0;
    } else if (scenario.id === 'reinvest_maturities') {
        deploymentYear = scenario.maturityYear || (currentYear + Math.floor(yearsToRetirement / 2)); // Average maturity time
        scenarioType = 'reinvest';
        scenarioValue = scenario.value || 0;
    } else if (scenario.id === 'redirect_emi') {
        deploymentYear = scenario.startYear || currentYear + 5;
        scenarioType = 'sip';
        scenarioValue = scenario.value || 0;
    } else if (scenario.id === 'increase_sip') {
        deploymentYear = currentYear; // Start immediately
        scenarioType = 'sip';
        scenarioValue = scenario.value || 0; // This is the INCREMENTAL SIP (20% increase)
    } else if (scenario.id === 'freed_expenses') {
        deploymentYear = scenario.startYear || currentYear + 5;
        scenarioType = 'sip';
        scenarioValue = scenario.value || 0; // Monthly amount freed up from expenses
    }
    
    // Generate chart data with proper baseline vs strategy comparison
    const chartData = generateScenarioChart(currentCorpus, projectedCorpus, {
        yearsToRetirement: yearsToRetirement,
        deploymentYear: deploymentYear,
        type: scenarioType,
        lumpsumValue: scenarioType === 'lumpsum' ? scenarioValue : 0,
        reinvestValue: scenarioType === 'reinvest' ? scenarioValue : 0,
        monthlySIP: scenarioType === 'sip' ? scenarioValue : 0, // Incremental SIP
        existingMonthlySIP: existingMonthlySIP // Existing SIP for baseline
    });
    
    // Create modal with chart
    const modal = document.createElement('div');
    modal.className = 'fixed inset-0 bg-black/50 flex items-center justify-center z-50';
    modal.id = 'scenario-chart-modal';
    modal.innerHTML = `
        <div class="bg-white rounded-xl p-6 max-w-4xl w-full mx-4 max-h-[80vh] overflow-y-auto">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-semibold">${scenario.icon} ${scenario.title} - Projection</h3>
                <button onclick="document.getElementById('scenario-chart-modal').remove()" class="text-slate-400 hover:text-slate-600 text-2xl">✕</button>
            </div>
            <div class="mb-4 p-3 bg-blue-50 rounded-lg border border-blue-200 text-sm text-blue-700">
                <strong>Strategy Deployment:</strong> ${deploymentYear} (${deploymentYear - currentYear} years from now)
            </div>
            <div class="bg-slate-50 rounded-lg p-4 mb-4 overflow-x-auto">
                <table class="w-full text-sm">
                    <thead>
                        <tr class="border-b border-slate-200">
                            <th class="py-2 text-left text-slate-600">Year</th>
                            <th class="py-2 text-right text-slate-600">Current Corpus</th>
                            <th class="py-2 text-right text-slate-600">Baseline Corpus</th>
                            <th class="py-2 text-right text-slate-600">Strategy Corpus</th>
                            <th class="py-2 text-right text-slate-600">Difference</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${chartData.filter((d, i) => i % 5 === 0 || i === chartData.length - 1).map((d, idx, arr) => {
                            const prev = idx > 0 ? arr[idx - 1] : null;
                            const baselineGrowth = prev ? d.baselineCorpus - prev.baselineCorpus : 0;
                            const strategyGrowth = prev ? d.corpus - prev.corpus : 0;
                            const growthDiff = strategyGrowth - baselineGrowth;
                            
                            return `
                            <tr class="border-b border-slate-100 ${d.year === (deploymentYear - currentYear) ? 'bg-amber-50' : ''}">
                                <td class="py-2 text-slate-700 font-medium">${currentYear + d.year}${d.year === (deploymentYear - currentYear) ? ' ⭐' : ''}</td>
                                <td class="py-2 text-right font-mono text-slate-600">${d.year === 0 ? formatCurrency(d.corpus, true) : '-'}</td>
                                <td class="py-2 text-right font-mono text-slate-700">${formatCurrency(d.baselineCorpus, true)}</td>
                                <td class="py-2 text-right font-mono text-primary-700 font-semibold">${formatCurrency(d.corpus, true)}</td>
                                <td class="py-2 text-right font-mono ${d.difference >= 0 ? 'text-emerald-600' : 'text-slate-500'}">
                                    ${d.difference > 0 ? '+' : ''}${formatCurrency(d.difference, true)}
                                    ${prev ? `<div class="text-xs text-slate-400">(${growthDiff > 0 ? '+' : ''}${formatCurrency(growthDiff, true)} growth)</div>` : ''}
                                </td>
                            </tr>
                        `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
            <div class="text-sm text-slate-600 space-y-1">
                <div><strong>Impact:</strong> ${scenario.impact}</div>
                ${scenario.timing ? `<div><strong>Timing:</strong> ${scenario.timing}</div>` : ''}
                <div class="mt-2 p-2 bg-slate-100 rounded text-xs">
                    <strong>Note:</strong> Current Corpus is your starting balance. Baseline Corpus shows growth without strategy. 
                    Strategy Corpus shows growth with this strategy applied from ${deploymentYear}. 
                    Difference shows the additional corpus gained from the strategy.
                </div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.remove();
    });
}

// Toggle scenario selection
function toggleScenario(scenarioId) {
    const scenario = window.whatIfScenarios?.find(s => s.id === scenarioId);
    if (scenario) {
        scenario.enabled = !scenario.enabled;
    }
}

// Save user's selected strategy
async function saveUserStrategy() {
    const enabledScenarios = window.whatIfScenarios?.filter(s => s.enabled) || [];
    
    if (enabledScenarios.length === 0) {
        alert('Please enable at least one strategy to save');
        return;
    }
    
    const freedExpensesScenario = enabledScenarios.find(s => s.id === 'freed_expenses');
    
    const strategy = {
        selectedIncomeStrategy: currentParams.incomeStrategy || 'SUSTAINABLE',
        sellIlliquidAssets: enabledScenarios.some(s => s.id === 'sell_illiquid'),
        sellIlliquidAssetsYear: enabledScenarios.find(s => s.id === 'sell_illiquid')?.sellYear,
        illiquidAssetsValue: enabledScenarios.find(s => s.id === 'sell_illiquid')?.value,
        reinvestMaturities: enabledScenarios.some(s => s.id === 'reinvest_maturities'),
        expectedMaturitiesValue: enabledScenarios.find(s => s.id === 'reinvest_maturities')?.value,
        redirectLoanEMIs: enabledScenarios.some(s => s.id === 'redirect_emi'),
        loanEndYear: enabledScenarios.find(s => s.id === 'redirect_emi')?.startYear,
        monthlyEMIAmount: enabledScenarios.find(s => s.id === 'redirect_emi')?.value,
        increaseSIP: enabledScenarios.some(s => s.id === 'increase_sip'),
        sipIncreasePercent: 20,
        newSIPAmount: enabledScenarios.find(s => s.id === 'increase_sip')?.value,
        // New: Freed expenses investment opportunity
        investFreedExpenses: enabledScenarios.some(s => s.id === 'freed_expenses'),
        freedExpensesMonthly: freedExpensesScenario?.value,
        freedExpensesPotentialCorpus: freedExpensesScenario?.potentialCorpus,
        freedExpensesStartYear: freedExpensesScenario?.startYear,
        freedExpensesDetails: freedExpensesScenario?.opportunities?.map(o => ({
            name: o.expenseName,
            category: o.category,
            endYear: o.endsInYear,
            monthlyAmount: o.freedMonthlyAmount
        })),
        strategyNotes: `Selected ${enabledScenarios.length} strategies`
    };
    
    try {
        const saved = await api.post('/retirement/strategy', strategy);
        alert('✅ Strategy saved successfully! It will be shown on your dashboard.');
        
        // Store locally too
        localStorage.setItem('userStrategy', JSON.stringify(strategy));
    } catch (error) {
        console.error('Error saving strategy:', error);
        // Save locally even if API fails
        localStorage.setItem('userStrategy', JSON.stringify(strategy));
        alert('Strategy saved locally. You can view it on your dashboard.');
    }
}

// Calculate required SIP to reach a goal
function calculateRequiredSIP(targetAmount, annualReturn, years) {
    const monthlyRate = annualReturn / 100 / 12;
    const months = years * 12;
    // PMT formula: P = FV * r / ((1+r)^n - 1)
    const factor = (Math.pow(1 + monthlyRate, months) - 1) / monthlyRate;
    return targetAmount / factor;
}

// Calculate SIP future value
function calculateSIPFutureValue(monthlyAmount, annualReturn, years) {
    const monthlyRate = annualReturn / 100 / 12;
    const months = years * 12;
    // FV = P * ((1+r)^n - 1) / r * (1+r)
    return monthlyAmount * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
}
