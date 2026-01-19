// Retyrment - Dashboard Page

document.addEventListener('DOMContentLoaded', async () => {
    // Wait a bit for checkAuth() to complete
    await new Promise(resolve => setTimeout(resolve, 100));
    
    // Check if user is authenticated before loading dashboard
    if (!auth.isLoggedIn()) {
        console.log('[Dashboard] User not authenticated, waiting for redirect...');
        return; // checkAuth() will handle the redirect
    }
    
    loadDashboard();
});

async function loadDashboard() {
    const loading = document.getElementById('loading');
    const content = document.getElementById('dashboard-content');
    const errorState = document.getElementById('error-state');

    // Double-check authentication before loading
    if (!auth.isLoggedIn()) {
        console.warn('[Dashboard] Not authenticated, redirecting...');
        window.location.href = 'login.html';
        return;
    }

    loading.classList.remove('hidden');
    content.classList.add('hidden');
    errorState.classList.add('hidden');

    // Check user role
    const user = auth.getUser();
    const isPro = user && (user.role === 'PRO' || user.role === 'ADMIN' || user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN');

    try {
        // Load basic data for all users
        const [netWorth, goalAnalysis, retirementData, loans, maturingData, investments, insuranceRecs, insurances] = await Promise.all([
            api.analysis.getNetWorth(),
            api.analysis.getGoalAnalysis(),
            api.post('/retirement/calculate', { currentAge: 35, retirementAge: 60, lifeExpectancy: 85 }).catch(() => null),
            api.loans.getAll().catch(() => []),
            api.retirement.getMaturingInvestments(35, 60).catch(() => null),
            api.investments.getAll().catch(() => []),
            api.insurance.getRecommendations().catch(() => null),
            api.insurance.getAll().catch(() => [])
        ]);

        // Update summary cards
        document.getElementById('net-worth').textContent = formatCurrency(netWorth?.netWorth || 0, true);
        document.getElementById('total-assets').textContent = formatCurrency(netWorth?.totalAssets || 0, true);
        document.getElementById('total-liabilities').textContent = formatCurrency(netWorth?.totalLiabilities || 0, true);

        // Update retirement planning cards
        updateRetirementCards(retirementData);

        // Render charts
        createNetWorthChart('networth-chart', netWorth);
        createGoalProgressChart('goals-chart', goalAnalysis);
        
        // Generate High Priority Alerts
        generateHighPriorityAlerts(retirementData, netWorth, loans, maturingData, goalAnalysis);

        // Update critical areas summary
        updateCriticalAreasSummary(retirementData, netWorth, investments, insuranceRecs, insurances);
        updateEmergencyFundWidget(retirementData, netWorth, investments);

        // Handle recommendations based on user role
        const recsContainer = document.getElementById('recommendations');
        
        if (isPro) {
            // Pro users get full recommendations
            try {
                const recommendations = await api.analysis.getRecommendations();
                document.getElementById('monthly-savings').textContent = formatCurrency(recommendations?.monthlySavings || 0, true);
                
                const recs = recommendations?.recommendations || [];
                if (recs.length > 0) {
                    recsContainer.innerHTML = recs.map(rec => {
                        const bgColor = rec.type === 'danger' ? 'bg-danger-50 border-danger-200' :
                                       rec.type === 'warning' ? 'bg-warning-50 border-warning-200' :
                                       rec.type === 'success' ? 'bg-success-50 border-success-200' :
                                       'bg-slate-50 border-slate-200';
                        return `
                            <div class="flex items-start gap-3 p-3 ${bgColor} border rounded-lg">
                                <span class="text-xl">${rec.icon}</span>
                                <div>
                                    <div class="font-medium text-slate-800">${rec.title}</div>
                                    <div class="text-sm text-slate-600">${rec.description}</div>
                                </div>
                            </div>
                        `;
                    }).join('');
                } else {
                    recsContainer.innerHTML = '<div class="text-slate-500">No recommendations yet. Add some data to get started!</div>';
                }
            } catch (e) {
                recsContainer.innerHTML = '<div class="text-slate-500">Unable to load recommendations</div>';
            }
        } else {
            // Free users see upgrade prompt
            document.getElementById('monthly-savings').textContent = '₹--';
            recsContainer.innerHTML = `
                <div class="text-center py-4">
                    <div class="text-4xl mb-3">⭐</div>
                    <div class="font-medium text-slate-800 mb-2">Pro Feature</div>
                    <p class="text-sm text-slate-500 mb-4">Get personalized financial recommendations with Pro</p>
                    <button onclick="showUpgradePrompt()" class="px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-lg text-sm font-medium">
                        Upgrade to Pro
                    </button>
                </div>
            `;
        }

        loading.classList.add('hidden');
        content.classList.remove('hidden');

    } catch (error) {
        // Don't show error if it's just an auth redirect
        if (error.message === 'Not authenticated' || error.message.includes('Session expired')) {
            console.log('[Dashboard] Authentication required, redirect handled');
            return; // Redirect already handled by api.js
        }
        
        console.error('Dashboard load error:', error);
        loading.classList.add('hidden');
        content.classList.add('hidden');
        errorState.classList.remove('hidden');
        
        // Show user-friendly error message
        const errorMessage = document.getElementById('error-message');
        if (errorMessage) {
            errorMessage.textContent = 'Failed to load dashboard. Please try refreshing the page.';
        }
    }
}

function updateCriticalAreasSummary(retirementData, netWorth, investments, insuranceRecs, insurances) {
    const list = document.getElementById('critical-areas-list');
    const countBadge = document.getElementById('critical-met-count');
    if (!list || !countBadge) return;

    const gapAnalysis = retirementData?.gapAnalysis || {};
    // Use totalCurrentMonthlyExpenses for current emergency fund calculation
    const monthlyExpenses = gapAnalysis.totalCurrentMonthlyExpenses || gapAnalysis.currentMonthlyExpenses || 0;

    // Emergency fund: cash + emergency FD
    const cashBalance = netWorth?.assetBreakdown?.CASH || 0;
    const emergencyFdValue = (investments || [])
        .filter(inv => inv.type === 'FD' && inv.isEmergencyFund)
        .reduce((sum, inv) => sum + (inv.currentValue || inv.investedAmount || 0), 0);
    const emergencyFundTarget = monthlyExpenses > 0 ? monthlyExpenses * 6 : 0;
    const emergencyFundAvailable = cashBalance + emergencyFdValue;
    const emergencyFundMet = monthlyExpenses > 0 && emergencyFundAvailable >= emergencyFundTarget;

    // Health coverage: based on insurance recommendations gap
    const healthRec = insuranceRecs?.healthRecommendation;
    const healthMet = healthRec ? (healthRec.gap <= 0 && healthRec.totalRecommendedCover > 0) : null;

    // Accidental cover: heuristic based on policy name/type
    const accidentKeywords = ['accident', 'accidental', 'personal accident'];
    const accidentalCoverMet = (insurances || []).some(policy => {
        const name = (policy.policyName || policy.company || '').toLowerCase();
        return policy.type === 'OTHER' || accidentKeywords.some(k => name.includes(k));
    });

    const items = [
        {
            label: 'Health Cover',
            status: healthMet,
            detail: healthRec ? (healthMet ? 'Adequate coverage' : 'Coverage gap exists') : 'Add family/insurance data',
            action: 'insurance-recommendations.html'
        },
        {
            label: 'Emergency Fund',
            status: monthlyExpenses > 0 ? emergencyFundMet : null,
            detail: monthlyExpenses > 0
                ? `${formatCurrency(emergencyFundAvailable, true)} / ${formatCurrency(emergencyFundTarget, true)}`
                : 'Add expenses to calculate',
            action: 'investments.html'
        },
        {
            label: 'Accidental Cover',
            status: accidentalCoverMet,
            detail: accidentalCoverMet ? 'Covered' : 'Not found',
            action: 'insurance.html'
        }
    ];

    const metCount = items.filter(item => item.status === true).length;
    countBadge.textContent = `${metCount}/${items.length} met`;

    list.innerHTML = items.map(item => {
        const statusText = item.status === null ? 'Unknown' : (item.status ? 'Met' : 'Missing');
        const statusClass = item.status === null ? 'text-slate-500' : (item.status ? 'text-success-600' : 'text-danger-600');
        const borderClass = item.status === null ? 'border-slate-200 bg-slate-50' : (item.status ? 'border-success-200 bg-success-50' : 'border-danger-200 bg-danger-50');
        const icon = item.status === null ? 'ℹ️' : (item.status ? '✅' : '⚠️');
        return `
            <div class="p-4 rounded-lg border ${borderClass}">
                <div class="flex items-center justify-between mb-1">
                    <div class="text-sm text-slate-600">${item.label}</div>
                    <span class="text-sm ${statusClass} font-medium">${icon} ${statusText}</span>
                </div>
                <div class="text-sm text-slate-700">${item.detail}</div>
                <a href="${item.action}" class="text-xs text-primary-600 hover:text-primary-700 inline-block mt-2">
                    View →
                </a>
            </div>
        `;
    }).join('');
}

// Update Emergency Fund widget with detailed breakdown
function updateEmergencyFundWidget(retirementData, netWorth, investments) {
    const widget = document.getElementById('emergency-fund-widget');
    if (!widget) return;

    const gapAnalysis = retirementData?.gapAnalysis || {};
    // Use totalCurrentMonthlyExpenses for current emergency fund calculation
    const monthlyExpenses = gapAnalysis.totalCurrentMonthlyExpenses || gapAnalysis.currentMonthlyExpenses || 0;
    const emergencyFundTarget = monthlyExpenses > 0 ? monthlyExpenses * 6 : 0;

    // Calculate components
    const cashBalance = netWorth?.assetBreakdown?.CASH || 0;
    const emergencyFDs = (investments || []).filter(inv => inv.type === 'FD' && inv.isEmergencyFund);
    const emergencyRDs = (investments || []).filter(inv => inv.type === 'RD' && inv.isEmergencyFund);
    
    const emergencyFdValue = emergencyFDs.reduce((sum, inv) => sum + (inv.currentValue || inv.investedAmount || 0), 0);
    const emergencyRdValue = emergencyRDs.reduce((sum, inv) => sum + (inv.currentValue || inv.investedAmount || 0), 0);
    
    const totalEmergencyFund = cashBalance + emergencyFdValue + emergencyRdValue;
    const gap = Math.max(0, emergencyFundTarget - totalEmergencyFund);
    const progressPercent = emergencyFundTarget > 0 ? Math.min(100, (totalEmergencyFund / emergencyFundTarget) * 100) : 0;
    
    // Status determination
    let statusClass, statusIcon, statusText;
    if (monthlyExpenses === 0) {
        statusClass = 'text-slate-500';
        statusIcon = 'ℹ️';
        statusText = 'Add expenses to calculate target';
    } else if (totalEmergencyFund >= emergencyFundTarget) {
        statusClass = 'text-success-600';
        statusIcon = '✅';
        statusText = 'Adequate emergency fund';
    } else if (totalEmergencyFund >= emergencyFundTarget * 0.5) {
        statusClass = 'text-warning-600';
        statusIcon = '⚠️';
        statusText = 'Partially funded';
    } else {
        statusClass = 'text-danger-600';
        statusIcon = '🚨';
        statusText = 'Critical: Low emergency fund';
    }

    widget.innerHTML = `
        <div class="space-y-4">
            <!-- Status Bar -->
            <div class="flex items-center justify-between">
                <div>
                    <div class="text-2xl font-bold text-slate-800">${formatCurrency(totalEmergencyFund, true)}</div>
                    <div class="text-sm text-slate-500">Current Emergency Fund</div>
                </div>
                <div class="text-right">
                    <div class="${statusClass} font-medium flex items-center gap-1 justify-end">
                        ${statusIcon} ${statusText}
                    </div>
                    <div class="text-sm text-slate-500 mt-1">Target: ${formatCurrency(emergencyFundTarget, true)}</div>
                </div>
            </div>

            <!-- Progress Bar -->
            ${monthlyExpenses > 0 ? `
                <div>
                    <div class="flex items-center justify-between text-xs text-slate-600 mb-1">
                        <span>Progress</span>
                        <span>${progressPercent.toFixed(1)}%</span>
                    </div>
                    <div class="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                        <div class="h-full ${progressPercent >= 100 ? 'bg-success-500' : progressPercent >= 50 ? 'bg-warning-500' : 'bg-danger-500'} transition-all duration-500"
                             style="width: ${progressPercent}%"></div>
                    </div>
                    ${gap > 0 ? `<div class="text-xs text-slate-500 mt-1">Gap: ${formatCurrency(gap, true)}</div>` : ''}
                </div>
            ` : ''}

            <!-- Breakdown -->
            <div class="grid grid-cols-3 gap-4 pt-4 border-t border-slate-200">
                <div class="text-center">
                    <div class="text-sm text-slate-500 mb-1">Cash</div>
                    <div class="font-semibold text-slate-700">${formatCurrency(cashBalance, true)}</div>
                </div>
                <div class="text-center">
                    <div class="text-sm text-slate-500 mb-1">Tagged FDs</div>
                    <div class="font-semibold text-amber-600">${formatCurrency(emergencyFdValue, true)}</div>
                    <div class="text-xs text-slate-400">${emergencyFDs.length} FD${emergencyFDs.length !== 1 ? 's' : ''}</div>
                </div>
                <div class="text-center">
                    <div class="text-sm text-slate-500 mb-1">Tagged RDs</div>
                    <div class="font-semibold text-amber-600">${formatCurrency(emergencyRdValue, true)}</div>
                    <div class="text-xs text-slate-400">${emergencyRDs.length} RD${emergencyRDs.length !== 1 ? 's' : ''}</div>
                </div>
            </div>

            <!-- Action hint -->
            ${(emergencyFDs.length === 0 && emergencyRDs.length === 0 && cashBalance < emergencyFundTarget) ? `
                <div class="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
                    💡 Tip: You can tag existing FDs or RDs as emergency fund on the <a href="investments.html" class="underline font-medium">Investments page</a>. Tagged funds will be excluded from retirement corpus calculations.
                </div>
            ` : ''}
        </div>
    `;
}

// Update retirement planning cards on dashboard
function updateRetirementCards(retirementData) {
    if (!retirementData) {
        // No retirement data available
        document.getElementById('projected-corpus').textContent = '₹0';
        document.getElementById('required-corpus-main').textContent = '₹0';
        document.getElementById('retirement-readiness').textContent = 'N/A';
        document.getElementById('retirement-status-note').textContent = 'Add investments to calculate';
        return;
    }
    
    const summary = retirementData.summary || {};
    const gapAnalysis = retirementData.gapAnalysis || {};
    
    // Projected corpus at retirement
    const projectedCorpus = summary.finalCorpus || 0;
    document.getElementById('projected-corpus').textContent = formatCurrency(projectedCorpus, true);
    document.getElementById('retirement-age-note').textContent = `At age ${summary.retirementAge || 60}`;
    
    // Required corpus
    const requiredCorpus = gapAnalysis.requiredCorpus || 0;
    document.getElementById('required-corpus-main').textContent = formatCurrency(requiredCorpus, true);
    
    // Style the required corpus card based on gap
    const corpusGap = gapAnalysis.corpusGap || 0;
    const requiredCard = document.getElementById('required-corpus-card-main');
    const requiredIcon = document.getElementById('required-corpus-icon');
    const gapNote = document.getElementById('corpus-gap-main');
    
    // Get strategy name for display
    const strategyName = gapAnalysis.incomeStrategy || 'SUSTAINABLE';
    const strategyLabel = strategyName === 'SIMPLE_DEPLETION' ? 'Simple Depletion' :
                          strategyName === 'SAFE_4_PERCENT' ? '4% Withdrawal' :
                          'Sustainable';
    
    if (corpusGap > 0) {
        // Shortfall
        requiredCard.className = 'card p-6 border-l-4 border-danger-500';
        requiredIcon.className = 'w-12 h-12 rounded-xl bg-danger-100 flex items-center justify-center text-2xl';
        gapNote.className = 'text-xs text-danger-600';
        gapNote.innerHTML = `Gap: -${formatCurrency(corpusGap, true)} <span class="text-slate-400">(${strategyLabel})</span>`;
    } else {
        // On track
        requiredCard.className = 'card p-6 border-l-4 border-success-500';
        requiredIcon.className = 'w-12 h-12 rounded-xl bg-success-100 flex items-center justify-center text-2xl';
        gapNote.className = 'text-xs text-success-600';
        gapNote.innerHTML = `Surplus: +${formatCurrency(Math.abs(corpusGap), true)} <span class="text-slate-400">(${strategyLabel})</span>`;
    }
    
    // Retirement readiness
    const statusCard = document.getElementById('retirement-status-card');
    const statusIcon = document.getElementById('retirement-status-icon');
    const readiness = document.getElementById('retirement-readiness');
    const statusNote = document.getElementById('retirement-status-note');
    
    const readinessPercent = requiredCorpus > 0 ? Math.round((projectedCorpus / requiredCorpus) * 100) : 0;
    
    if (readinessPercent >= 100) {
        statusCard.className = 'card p-6 border-l-4 border-success-500';
        statusIcon.className = 'w-12 h-12 rounded-xl bg-success-100 flex items-center justify-center text-2xl';
        statusIcon.textContent = '✅';
        readiness.className = 'text-2xl font-bold text-success-600';
        readiness.textContent = `${readinessPercent}%`;
        statusNote.className = 'text-xs text-success-600';
        statusNote.textContent = 'You\'re on track! 🎉';
    } else if (readinessPercent >= 75) {
        statusCard.className = 'card p-6 border-l-4 border-warning-500';
        statusIcon.className = 'w-12 h-12 rounded-xl bg-warning-100 flex items-center justify-center text-2xl';
        statusIcon.textContent = '⚠️';
        readiness.className = 'text-2xl font-bold text-warning-600';
        readiness.textContent = `${readinessPercent}%`;
        statusNote.className = 'text-xs text-warning-600';
        statusNote.textContent = 'Almost there, increase savings';
    } else {
        statusCard.className = 'card p-6 border-l-4 border-danger-500';
        statusIcon.className = 'w-12 h-12 rounded-xl bg-danger-100 flex items-center justify-center text-2xl';
        statusIcon.textContent = '❗';
        readiness.className = 'text-2xl font-bold text-danger-600';
        readiness.textContent = `${readinessPercent}%`;
        statusNote.className = 'text-xs text-danger-600';
        statusNote.textContent = 'Need to increase investments';
    }
}

// Generate High Priority Alerts based on financial data
async function generateHighPriorityAlerts(retirementData, netWorth, loans, maturingData, goalAnalysis) {
    const alertsContainer = document.getElementById('high-priority-alerts');
    const alertsCount = document.getElementById('alerts-count');
    const alerts = [];
    
    const gapAnalysis = retirementData?.gapAnalysis || {};
    const summary = retirementData?.summary || {};
    const projectedCorpus = summary.finalCorpus || 0;
    const requiredCorpus = gapAnalysis.requiredCorpus || 0;
    const corpusGap = gapAnalysis.corpusGap || 0;
    
    // Check for saved user strategy
    let savedStrategy = null;
    try {
        savedStrategy = await api.retirement.getStrategy().catch(() => null);
        if (!savedStrategy) {
            // Try local storage
            const local = localStorage.getItem('userStrategy');
            if (local) savedStrategy = JSON.parse(local);
        }
    } catch (e) {
        const local = localStorage.getItem('userStrategy');
        if (local) savedStrategy = JSON.parse(local);
    }
    
    // Show saved strategy if exists
    if (savedStrategy && Object.keys(savedStrategy).length > 0) {
        const enabledStrategies = [];
        if (savedStrategy.sellIlliquidAssets) enabledStrategies.push(`Sell Illiquid (${savedStrategy.sellIlliquidAssetsYear || 'TBD'})`);
        if (savedStrategy.reinvestMaturities) enabledStrategies.push('Reinvest Maturities');
        if (savedStrategy.redirectLoanEMIs) enabledStrategies.push(`Redirect EMI (from ${savedStrategy.loanEndYear || 'TBD'})`);
        if (savedStrategy.increaseSIP) enabledStrategies.push('Increase SIP 20%');
        
        if (enabledStrategies.length > 0) {
            alerts.push({
                type: 'info',
                icon: '📋',
                title: 'Your Active Strategy',
                description: enabledStrategies.join(' • '),
                action: 'Update Strategy',
                link: 'retirement.html'
            });
        }
    }
    
    // 1. Inflows before retirement - available for reinvestment
    if (maturingData && maturingData.totalMaturingBeforeRetirement > 0) {
        const totalMaturing = maturingData.totalMaturingBeforeRetirement;
        const count = (maturingData.investmentCount || 0) + (maturingData.insuranceCount || 0);
        alerts.push({
            type: 'success',
            icon: '💰',
            title: `${formatCurrency(totalMaturing, true)} Available for Reinvestment`,
            description: `${count} investments/policies maturing before retirement. Consider reinvesting in higher-return assets.`,
            action: 'View Details',
            link: 'retirement.html'
        });
    }
    
    // 2. Corpus Gap Analysis
    if (corpusGap > 0) {
        // Check if selling illiquid assets would help
        const illiquidValue = (netWorth?.assetBreakdown?.GOLD || 0) + 
                             (netWorth?.assetBreakdown?.REAL_ESTATE || 0);
        
        if (illiquidValue > 0) {
            const wouldMeetCorpus = (projectedCorpus + illiquidValue) >= requiredCorpus;
            if (wouldMeetCorpus) {
                alerts.push({
                    type: 'warning',
                    icon: '🏠',
                    title: 'Illiquid Assets Can Cover Gap',
                    description: `Selling Gold/Real Estate (${formatCurrency(illiquidValue, true)}) would help meet your corpus requirement.`,
                    action: 'Plan Strategy',
                    link: 'retirement.html'
                });
            }
        }
        
        // Add inflows to see if they help
        const totalInflows = maturingData?.totalMaturingBeforeRetirement || 0;
        if (totalInflows > 0 && (projectedCorpus + totalInflows) >= requiredCorpus) {
            alerts.push({
                type: 'tip',
                icon: '📊',
                title: 'Reinvesting Maturities Helps',
                description: `If you reinvest ${formatCurrency(totalInflows, true)} from maturities, you can meet your corpus target.`,
                action: 'View Plan',
                link: 'retirement.html'
            });
        } else if (corpusGap > 0) {
            alerts.push({
                type: 'danger',
                icon: '⚠️',
                title: `Corpus Shortfall: ${formatCurrency(corpusGap, true)}`,
                description: 'Increase SIP or consider additional investments to meet retirement goals.',
                action: 'Adjust Plan',
                link: 'retirement.html'
            });
        }
    }
    
    // 3. Post-Loan Investment Opportunity
    const activeLoans = Array.isArray(loans) ? loans.filter(l => l.outstandingAmount > 0) : [];
    if (activeLoans.length > 0) {
        const totalEMI = activeLoans.reduce((sum, l) => sum + (l.emi || 0), 0);
        const soonestEndDate = activeLoans
            .filter(l => l.endDate)
            .sort((a, b) => new Date(a.endDate) - new Date(b.endDate))[0];
        
        if (soonestEndDate && totalEMI > 0) {
            const endYear = new Date(soonestEndDate.endDate).getFullYear();
            const yearsToEnd = endYear - new Date().getFullYear();
            if (yearsToEnd > 0 && yearsToEnd <= 10) {
                alerts.push({
                    type: 'tip',
                    icon: '🎯',
                    title: `${formatCurrency(soonestEndDate.emi || 0)}/mo Freed in ${yearsToEnd}y`,
                    description: `After ${soonestEndDate.name || 'loan'} ends, redirect EMI to investments for faster corpus growth.`,
                    action: 'View Loans',
                    link: 'loans.html'
                });
            }
        }
    }
    
    // 4. Emergency Fund Check (includes tagged FDs + RDs)
    const monthlyIncome = gapAnalysis.monthlyIncome || 0;
    const monthlyExpensesForEmergency = gapAnalysis.totalCurrentMonthlyExpenses || gapAnalysis.currentMonthlyExpenses || 0;
    const monthlySavings = monthlyIncome - monthlyExpensesForEmergency;
    
    if (monthlySavings > 0 && requiredCorpus > 0) {
        const emergencyFundTarget = monthlyExpensesForEmergency * 6;
        const currentCash = netWorth?.assetBreakdown?.CASH || 0;
        
        // Include tagged emergency fund FDs and RDs
        const allInvestments = await api.investments.getAll().catch(() => []);
        const emergencyFdValue = allInvestments
            .filter(inv => (inv.type === 'FD' || inv.type === 'RD') && inv.isEmergencyFund)
            .reduce((sum, inv) => sum + (inv.currentValue || inv.investedAmount || 0), 0);
        
        const totalEmergencyFund = currentCash + emergencyFdValue;
        const emergencyGap = Math.max(0, emergencyFundTarget - totalEmergencyFund);
        
        if (emergencyGap > 0) {
            const monthsToEmergency = Math.ceil(emergencyGap / (monthlySavings * 0.3));
            alerts.push({
                type: 'warning',
                icon: '🆘',
                title: 'Emergency Fund Gap',
                description: `Need ${formatCurrency(emergencyGap, true)} more. Current: ${formatCurrency(totalEmergencyFund, true)} (Cash + Tagged FD/RD)`,
                action: 'Tag FDs',
                link: 'investments.html'
            });
        }
        
        // Check for emergency FDs/RDs maturing soon (within 6 months)
        const today = new Date();
        const sixMonthsFromNow = new Date();
        sixMonthsFromNow.setMonth(today.getMonth() + 6);
        
        const maturingSoonEmergencyFunds = allInvestments.filter(inv => {
            if ((inv.type !== 'FD' && inv.type !== 'RD') || !inv.isEmergencyFund) return false;
            if (!inv.maturityDate) return false;
            const maturityDate = new Date(inv.maturityDate);
            return maturityDate >= today && maturityDate <= sixMonthsFromNow;
        });
        
        if (maturingSoonEmergencyFunds.length > 0) {
            const totalMaturing = maturingSoonEmergencyFunds.reduce((sum, inv) => 
                sum + (inv.currentValue || inv.investedAmount || 0), 0);
            
            alerts.push({
                type: 'warning',
                icon: '⏰',
                title: 'Emergency Fund Maturing Soon',
                description: `${maturingSoonEmergencyFunds.length} emergency ${maturingSoonEmergencyFunds.length === 1 ? 'fund' : 'funds'} (${formatCurrency(totalMaturing, true)}) maturing in 6 months. Plan to reinvest to maintain emergency coverage.`,
                action: 'Review',
                link: 'investments.html'
            });
        }
    }
    
    // 5. Goals at risk
    const atRiskGoals = (goalAnalysis?.goals || []).filter(g => g.status === 'UNFUNDED');
    if (atRiskGoals.length > 0) {
        alerts.push({
            type: 'danger',
            icon: '🎯',
            title: `${atRiskGoals.length} Goal(s) Underfunded`,
            description: atRiskGoals.slice(0, 2).map(g => g.name).join(', ') + (atRiskGoals.length > 2 ? '...' : ''),
            action: 'Review Goals',
            link: 'goals.html'
        });
    }
    
    // Render alerts
    if (alerts.length > 0) {
        alertsCount.textContent = `(${alerts.length})`;
        alertsContainer.innerHTML = alerts.map(alert => {
            const bgColor = alert.type === 'danger' ? 'bg-danger-50 border-danger-200' :
                           alert.type === 'warning' ? 'bg-amber-50 border-amber-200' :
                           alert.type === 'success' ? 'bg-emerald-50 border-emerald-200' :
                           'bg-blue-50 border-blue-200';
            return `
                <div class="flex items-start gap-3 p-3 ${bgColor} border rounded-lg">
                    <span class="text-xl flex-shrink-0">${alert.icon}</span>
                    <div class="flex-1 min-w-0">
                        <div class="font-medium text-slate-800 text-sm">${alert.title}</div>
                        <div class="text-xs text-slate-600 mt-0.5">${alert.description}</div>
                    </div>
                    <a href="${alert.link}" class="text-xs text-primary-600 hover:text-primary-700 whitespace-nowrap">
                        ${alert.action} →
                    </a>
                </div>
            `;
        }).join('');
    } else {
        alertsCount.textContent = '(0)';
        alertsContainer.innerHTML = `
            <div class="text-center py-6 text-slate-400">
                <div class="text-4xl mb-2">✅</div>
                <div class="font-medium">All Good!</div>
                <div class="text-sm">No high priority alerts at this time</div>
            </div>
        `;
    }
}
