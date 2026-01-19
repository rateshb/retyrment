// Retyrment - Insurance Recommendations Page
// Displays personalized health and term insurance recommendations

document.addEventListener('DOMContentLoaded', () => {
    // Check authentication first
    if (!auth.requireAuth()) return;
    
    // Initialize user info display
    if (typeof initUserDisplay === 'function') {
        initUserDisplay();
    }
    
    loadRecommendations();
});

async function loadRecommendations() {
    try {
        const recommendations = await api.get('/insurance/recommendations');
        console.log('Insurance recommendations:', recommendations);
        
        if (!recommendations) {
            showError('No recommendations data received');
            return;
        }
        
        renderRecommendations(recommendations);
    } catch (error) {
        console.error('Error loading recommendations:', error);
        showError(error.message || 'Failed to load recommendations');
    }
}

function renderRecommendations(data) {
    // Overall Score
    renderOverallScore(data.summary || {});
    
    // Urgent Actions
    renderUrgentActions(data.summary || {});
    
    // Health Insurance
    renderHealthRecommendation(data.healthRecommendation || {});
    
    // Term Insurance
    renderTermRecommendation(data.termRecommendation || {});
    
    // Check if family data exists
    const memberBreakdown = data.healthRecommendation?.memberBreakdown || [];
    if (memberBreakdown.length <= 2) {
        document.getElementById('no-family-warning').classList.remove('hidden');
    }
}

function renderOverallScore(summary) {
    const scoreDisplay = document.getElementById('score-display');
    const score = summary.overallScore || 0;
    
    let color, status;
    if (score >= 80) {
        color = 'text-green-600';
        status = '✅ Adequate';
    } else if (score >= 50) {
        color = 'text-amber-600';
        status = '⚠️ Needs Improvement';
    } else {
        color = 'text-red-600';
        status = '❌ Critical';
    }
    
    scoreDisplay.innerHTML = `
        <div class="text-5xl font-bold ${color}">${score}</div>
        <div class="text-sm ${color}">${status}</div>
        <div class="text-xs text-slate-400 mt-1">${summary.overallMessage || ''}</div>
    `;
}

function renderUrgentActions(summary) {
    const container = document.getElementById('urgent-actions');
    const list = document.getElementById('urgent-list');
    
    if (summary.urgentActions && summary.urgentActions.length > 0) {
        container.classList.remove('hidden');
        list.innerHTML = summary.urgentActions.map(action => 
            `<li class="flex items-start gap-2">
                <span class="text-red-600">•</span>
                <span>${action}</span>
            </li>`
        ).join('');
    } else {
        container.classList.add('hidden');
    }
}

function renderHealthRecommendation(health) {
    // Summary cards
    document.getElementById('health-existing').textContent = formatCurrency(health.existingCover || 0);
    document.getElementById('health-recommended').textContent = formatCurrency(health.totalRecommendedCover || 0);
    document.getElementById('health-gap').textContent = formatCurrency(health.gap || 0);
    document.getElementById('health-premium').textContent = formatCurrency(health.totalEstimatedPremium || 0) + '/yr';
    
    // Breakdown
    const breakdown = document.getElementById('health-breakdown');
    if (!health.memberBreakdown || health.memberBreakdown.length === 0) {
        breakdown.innerHTML = '<p class="text-slate-500 text-center py-8">No recommendations available. Add family members first.</p>';
        return;
    }
    
    breakdown.innerHTML = health.memberBreakdown.map(member => {
        // Determine styling based on policy type
        let icon, bgClass, borderClass, titleColor, badge = '';
        if (member.isSupplementary || member.memberType === 'Super Top-Up') {
            icon = '🛡️';
            bgClass = 'bg-purple-50';
            borderClass = 'border-purple-200';
            titleColor = 'text-purple-800';
            badge = '<span class="ml-2 px-2 py-0.5 text-xs bg-purple-200 text-purple-700 rounded-full">Supplementary</span>';
        } else if (member.memberType?.includes('Family')) {
            icon = '👨‍👩‍👧‍👦';
            bgClass = 'bg-blue-50';
            borderClass = 'border-blue-200';
            titleColor = 'text-blue-800';
            badge = '<span class="ml-2 px-2 py-0.5 text-xs bg-blue-200 text-blue-700 rounded-full">Base Policy</span>';
        } else if (member.memberType?.includes('PARENT') || member.recommendedPolicyType?.includes('Senior')) {
            icon = '👴';
            bgClass = 'bg-amber-50';
            borderClass = 'border-amber-200';
            titleColor = 'text-amber-800';
            badge = '<span class="ml-2 px-2 py-0.5 text-xs bg-amber-200 text-amber-700 rounded-full">Separate Policy</span>';
        } else {
            icon = '🏥';
            bgClass = 'bg-slate-50';
            borderClass = 'border-slate-200';
            titleColor = 'text-slate-800';
        }
        
        return `
        <div class="p-4 mb-4 rounded-lg border ${bgClass} ${borderClass}">
            <div class="flex justify-between items-start mb-2">
                <div class="flex items-start gap-3">
                    <span class="text-2xl">${icon}</span>
                    <div>
                        <h4 class="font-semibold ${titleColor} flex items-center flex-wrap">
                            ${member.memberName || member.memberType}
                            ${badge}
                        </h4>
                        <p class="text-sm text-slate-500">${member.recommendedPolicyType}</p>
                    </div>
                </div>
                <div class="text-right">
                    <div class="text-xl font-bold ${member.isSupplementary ? 'text-purple-600' : 'text-success-600'}">${formatCurrency(member.recommendedCover)}</div>
                    ${member.estimatedPremium ? `<div class="text-xs text-slate-500">Est. ~${formatCurrency(member.estimatedPremium)}/yr</div>` : ''}
                </div>
            </div>
            <p class="text-sm text-slate-600 ml-11">${member.reasoning || ''}</p>
            ${member.isSupplementary ? `
                <div class="mt-2 ml-11 text-xs text-purple-600 italic">
                    ℹ️ Not included in total - this is layered coverage that extends your base policy
                </div>
            ` : ''}
            ${member.riskFactors && member.riskFactors.length > 0 ? `
                <div class="mt-3 ml-11 pt-2 border-t ${borderClass}">
                    <p class="text-xs text-amber-700 flex items-center gap-1">
                        <span>⚠️</span> ${member.riskFactors.join(' • ')}
                    </p>
                </div>
            ` : ''}
        </div>
    `}).join('');
    
    // Suggestions
    const suggestions = document.getElementById('health-suggestions');
    suggestions.innerHTML = (health.policySuggestions || []).map(s => 
        `<li class="flex items-start gap-2">
            <span>•</span>
            <span>${s}</span>
        </li>`
    ).join('');
}

function renderTermRecommendation(term) {
    // Summary cards
    document.getElementById('term-existing').textContent = formatCurrency(term.existingCover || 0);
    document.getElementById('term-recommended').textContent = formatCurrency(term.totalRecommendedCover || 0);
    document.getElementById('term-gap').textContent = formatCurrency(term.gap || 0);
    document.getElementById('term-premium').textContent = formatCurrency(term.estimatedAnnualPremium || 0) + '/yr';
    document.getElementById('term-coverage-age').textContent = `Age ${term.recommendedCoverageAge || 65}`;
    
    // Breakdown
    const breakdown = document.getElementById('term-breakdown');
    const b = term.breakdown;
    
    if (!b) {
        breakdown.innerHTML = '<p class="text-slate-500 text-center py-8">No breakdown available.</p>';
        return;
    }
    
    breakdown.innerHTML = `
        <div class="space-y-4">
            <!-- Income Replacement -->
            <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
                <div class="flex justify-between items-center mb-2">
                    <h4 class="font-medium text-blue-800">1. Income Replacement</h4>
                    <span class="text-xl font-bold text-blue-600">${formatCurrency(b.incomeReplacement)}</span>
                </div>
                <p class="text-sm text-blue-700">
                    Annual Income: ${formatCurrency(b.annualIncome)} × ${b.incomeMultiplier}x multiplier
                </p>
                <p class="text-xs text-blue-600 mt-1">${b.multiplierReason || ''}</p>
            </div>
            
            <!-- Liability Coverage -->
            <div class="p-4 bg-red-50 rounded-lg border border-red-200">
                <div class="flex justify-between items-center mb-2">
                    <h4 class="font-medium text-red-800">2. Liability Coverage</h4>
                    <span class="text-xl font-bold text-red-600">${formatCurrency(b.liabilityCoverage)}</span>
                </div>
                <p class="text-sm text-red-700">
                    Total outstanding loans and liabilities to be covered
                </p>
            </div>
            
            <!-- Expense Coverage -->
            <div class="p-4 bg-amber-50 rounded-lg border border-amber-200">
                <div class="flex justify-between items-center mb-2">
                    <h4 class="font-medium text-amber-800">3. Family Expense Coverage</h4>
                    <span class="text-xl font-bold text-amber-600">${formatCurrency(b.expenseCoverage)}</span>
                </div>
                <p class="text-sm text-amber-700">
                    Annual Expenses: ${formatCurrency(b.annualExpenses)} × ${b.expenseYears} years
                </p>
            </div>
            
            <!-- Children's Future -->
            ${b.childCount > 0 ? `
            <div class="p-4 bg-purple-50 rounded-lg border border-purple-200">
                <div class="flex justify-between items-center mb-2">
                    <h4 class="font-medium text-purple-800">4. Children's Future (Education + Marriage)</h4>
                    <span class="text-xl font-bold text-purple-600">${formatCurrency(b.childrenFutureCost)}</span>
                </div>
                <p class="text-sm text-purple-700">
                    For ${b.childCount} child(ren) - includes education and marriage costs adjusted for inflation
                </p>
            </div>
            ` : ''}
            
            <!-- Spouse Adjustment -->
            ${b.spouseAdjustmentReason ? `
            <div class="p-4 bg-green-50 rounded-lg border border-green-200">
                <div class="flex justify-between items-center mb-2">
                    <h4 class="font-medium text-green-800">5. Spouse Adjustment</h4>
                    <span class="text-sm font-medium text-green-600">Factor: ${(b.spouseAdjustmentFactor * 100).toFixed(0)}%</span>
                </div>
                <p class="text-sm text-green-700">${b.spouseAdjustmentReason}</p>
            </div>
            ` : ''}
            
            <!-- Total -->
            <div class="p-4 bg-slate-800 rounded-lg text-white">
                <div class="flex justify-between items-center">
                    <h4 class="font-medium">Total Recommended Coverage</h4>
                    <span class="text-2xl font-bold">${formatCurrency(term.totalRecommendedCover)}</span>
                </div>
            </div>
        </div>
    `;
    
    // Suggestions
    const suggestions = document.getElementById('term-suggestions');
    suggestions.innerHTML = (term.suggestions || []).map(s => 
        `<li class="flex items-start gap-2">
            <span>•</span>
            <span>${s}</span>
        </li>`
    ).join('');
}

function switchTab(tab) {
    // Update tab buttons
    document.querySelectorAll('[id^="tab-"]').forEach(btn => {
        btn.classList.remove('text-primary-600', 'border-b-2', 'border-primary-600', 'font-medium');
        btn.classList.add('text-slate-500');
    });
    document.getElementById(`tab-${tab}`).classList.add('text-primary-600', 'border-b-2', 'border-primary-600', 'font-medium');
    document.getElementById(`tab-${tab}`).classList.remove('text-slate-500');
    
    // Update panels
    document.getElementById('panel-health').classList.toggle('hidden', tab !== 'health');
    document.getElementById('panel-term').classList.toggle('hidden', tab !== 'term');
}

function showError(message) {
    document.getElementById('health-breakdown').innerHTML = `
        <div class="text-center py-8 text-red-500">
            <p>Error loading recommendations: ${message}</p>
            <button onclick="loadRecommendations()" class="mt-4 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700">
                Try Again
            </button>
        </div>
    `;
}

// Utility function
function formatCurrency(amount) {
    if (!amount || isNaN(amount)) return '₹0';
    if (amount >= 10000000) return `₹${(amount / 10000000).toFixed(2)} Cr`;
    if (amount >= 100000) return `₹${(amount / 100000).toFixed(2)} L`;
    return `₹${Math.round(amount).toLocaleString('en-IN')}`;
}
