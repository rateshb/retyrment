// Retyrment Charts Module - Light Mode
const chartColors = {
    primary: '#6366f1',
    success: '#10b981',
    warning: '#f59e0b',
    danger: '#ef4444',
    purple: '#8b5cf6',
    pink: '#ec4899',
    blue: '#3b82f6',
    teal: '#14b8a6',
    orange: '#f97316',
    amber: '#f59e0b',
    gray: '#64748b'
};

const chartPalette = [
    chartColors.primary, chartColors.success, chartColors.warning,
    chartColors.purple, chartColors.pink, chartColors.blue,
    chartColors.teal, chartColors.orange
];

// Common chart options for light mode
const defaultOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: {
            labels: { color: '#475569', font: { family: 'Inter', weight: 500 } }
        }
    },
    scales: {
        x: {
            ticks: { color: '#64748b' },
            grid: { color: 'rgba(148, 163, 184, 0.2)' }
        },
        y: {
            ticks: { color: '#64748b' },
            grid: { color: 'rgba(148, 163, 184, 0.2)' }
        }
    }
};

// Chart instances store
const charts = {};

// Destroy existing chart if exists
function destroyChart(id) {
    if (charts[id]) {
        charts[id].destroy();
        delete charts[id];
    }
}

// Create Net Worth Pie Chart
function createNetWorthChart(canvasId, data) {
    destroyChart(canvasId);
    const ctx = document.getElementById(canvasId)?.getContext('2d');
    if (!ctx || !data) return;

    const breakdown = data.assetBreakdown || {};
    const labels = Object.keys(breakdown);
    const values = Object.values(breakdown);

    if (labels.length === 0) {
        // Show empty state
        return;
    }

    charts[canvasId] = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels.map(l => l.replace('_', ' ')),
            datasets: [{
                data: values,
                backgroundColor: chartPalette.slice(0, labels.length),
                borderWidth: 2,
                borderColor: '#ffffff',
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '60%',
            plugins: {
                legend: {
                    position: 'right',
                    labels: { color: '#475569', padding: 12, font: { size: 11, family: 'Inter' } }
                },
                tooltip: {
                    backgroundColor: '#1e293b',
                    titleColor: '#f1f5f9',
                    bodyColor: '#f1f5f9',
                    callbacks: {
                        label: (ctx) => `${ctx.label}: ${formatCurrency(ctx.raw)}`
                    }
                }
            }
        }
    });
}

// Create Projection Line Chart
function createProjectionChart(canvasId, projections) {
    destroyChart(canvasId);
    const ctx = document.getElementById(canvasId)?.getContext('2d');
    if (!ctx || !projections) return;

    const data = projections.projections || [];
    const labels = data.map(p => p.year);
    const values = data.map(p => p.projectedValue);

    if (labels.length === 0) return;

    charts[canvasId] = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: 'Projected Value',
                data: values,
                borderColor: chartColors.primary,
                backgroundColor: 'rgba(99, 102, 241, 0.1)',
                fill: true,
                tension: 0.4,
                pointRadius: 4,
                pointBackgroundColor: chartColors.primary,
                pointBorderColor: '#ffffff',
                pointBorderWidth: 2,
                pointHoverRadius: 6
            }]
        },
        options: {
            ...defaultOptions,
            plugins: {
                ...defaultOptions.plugins,
                tooltip: {
                    backgroundColor: '#1e293b',
                    callbacks: {
                        label: (ctx) => `Value: ${formatCurrency(ctx.raw)}`
                    }
                }
            },
            scales: {
                ...defaultOptions.scales,
                y: {
                    ...defaultOptions.scales.y,
                    ticks: {
                        color: '#64748b',
                        callback: (value) => formatCurrency(value, true)
                    }
                }
            }
        }
    });
}

// Create Monthly Outflow Bar Chart
function createCalendarChart(canvasId, calendarData) {
    destroyChart(canvasId);
    const ctx = document.getElementById(canvasId)?.getContext('2d');
    if (!ctx || !calendarData) return;

    const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    const totals = calendarData.monthlyTotals || {};
    const values = months.map(m => totals[m] || 0);

    charts[canvasId] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: months,
            datasets: [{
                label: 'Monthly Outflow',
                data: values,
                backgroundColor: values.map(v => v > 100000 ? chartColors.danger : chartColors.primary),
                borderRadius: 6,
                borderSkipped: false
            }]
        },
        options: {
            ...defaultOptions,
            plugins: {
                ...defaultOptions.plugins,
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#1e293b',
                    callbacks: {
                        label: (ctx) => formatCurrency(ctx.raw)
                    }
                }
            },
            scales: {
                ...defaultOptions.scales,
                y: {
                    ...defaultOptions.scales.y,
                    ticks: {
                        color: '#64748b',
                        callback: (value) => formatCurrency(value, true)
                    }
                }
            }
        }
    });
}

// Create Goal Progress Chart
function createGoalProgressChart(canvasId, goalData, retirementMatrix) {
    destroyChart(canvasId);
    const ctx = document.getElementById(canvasId)?.getContext('2d');
    if (!ctx || !goalData) return;

    const goals = goalData.goals || [];
    if (goals.length === 0) return;

    // Check retirement matrix for actual goal fundability
    const matrix = retirementMatrix || [];
    const goalsWithShortfall = goals.map(g => {
        const goalYear = g.targetYear;
        const matrixRow = matrix.find(row => row.year === goalYear);
        const hasShortfall = matrixRow && matrixRow.netCorpus < 0 && matrixRow.goalOutflow > 0;
        
        let actualPercent = Math.min(g.fundingPercent || 0, 100);
        if (hasShortfall) {
            const goalAmount = matrixRow.goalOutflow || g.inflatedAmount || g.targetAmount;
            const shortfall = Math.abs(matrixRow.netCorpus);
            const fundable = Math.max(0, goalAmount - shortfall);
            actualPercent = goalAmount > 0 ? Math.min(100, (fundable / goalAmount) * 100) : 0;
        }
        
        return { ...g, actualPercent: Math.round(actualPercent), hasShortfall };
    });

    // Show warning if any goal has shortfall
    const warningEl = document.getElementById('goal-shortfall-warning');
    if (warningEl) {
        if (goalsWithShortfall.some(g => g.hasShortfall)) {
            warningEl.classList.remove('hidden');
        } else {
            warningEl.classList.add('hidden');
        }
    }

    const labels = goalsWithShortfall.map(g => g.name || 'Goal');
    const percentages = goalsWithShortfall.map(g => g.actualPercent);

    charts[canvasId] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                label: 'Funding %',
                data: percentages,
                backgroundColor: goalsWithShortfall.map(g => 
                    g.hasShortfall ? chartColors.danger :
                    g.actualPercent >= 100 ? chartColors.success : 
                    g.actualPercent >= 50 ? chartColors.warning : chartColors.danger
                ),
                borderRadius: 6,
                borderSkipped: false
            }]
        },
        options: {
            ...defaultOptions,
            indexAxis: 'y',
            plugins: {
                ...defaultOptions.plugins,
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const goal = goalsWithShortfall[context.dataIndex];
                            let label = context.parsed.x + '% funded';
                            if (goal.hasShortfall) {
                                label += ' (SHORTFALL!)';
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ...defaultOptions.scales.x,
                    max: 100,
                    ticks: { callback: (v) => v + '%', color: '#64748b' }
                },
                y: defaultOptions.scales.y
            }
        }
    });
}

// Store matrix data for deferred chart rendering
let cachedRetirementData = null;

// Create Retirement Matrix Chart with Expenses and Goals
function createRetirementChart(canvasId, matrixData) {
    // Always cache the data for later use (when chart is expanded)
    if (matrixData) {
        cachedRetirementData = matrixData;
    }
    
    const canvas = document.getElementById(canvasId);
    if (!canvas) {
        console.warn('Chart canvas not found:', canvasId);
        return;
    }
    
    // Check if canvas is visible (parent container might be hidden)
    const container = canvas.closest('#chart-container');
    if (container && container.classList.contains('hidden')) {
        // Canvas is hidden, just cache data and return - chart will be created when expanded
        console.log('Chart container hidden, data cached for later');
        return;
    }
    
    // Destroy any existing chart first
    destroyChart(canvasId);
    
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        console.warn('Could not get canvas context');
        return;
    }
    
    if (!matrixData) {
        console.warn('No matrix data provided');
        return;
    }

    const matrix = matrixData.matrix || [];
    if (matrix.length === 0) return;
    
    const summary = matrixData.summary || {};
    const incomeProjection = summary.retirementIncomeProjection || [];

    // Combine pre-retirement (matrix) and post-retirement (income projection) data
    const preRetirementLabels = matrix.map(r => r.year);
    const postRetirementLabels = incomeProjection.slice(1).map(p => `${summary.retirementAge + p.year}`); // Skip year 0 as it overlaps
    const labels = [...preRetirementLabels, ...postRetirementLabels];
    
    // Extract goal outflows from matrix
    const goalOutflows = matrix.map(r => r.goalOutflow || 0);
    const hasGoals = goalOutflows.some(g => g > 0);
    
    // Get monthly SIP for expense trend visualization (pre-retirement only)
    const sipValues = matrix.map(r => (r.mfSip || 0) * 12);
    // Pad SIP values for post-retirement (no SIP after retirement)
    const sipValuesPadded = [...sipValues, ...incomeProjection.slice(1).map(() => 0)];
    
    // Get corpus values - pre-retirement from matrix (exclude one-time inflows), post-retirement from income projection
    const preRetirementCorpus = matrix.map(r => {
        const netCorpus = r.netCorpus || 0;
        const inflow = r.totalInflow || 0;
        return Math.max(0, netCorpus - inflow);
    });
    const postRetirementCorpus = incomeProjection.slice(1).map(p => p.corpus || 0);
    const combinedCorpus = [...preRetirementCorpus, ...postRetirementCorpus];
    
    // Individual component values (pre-retirement only, then null for post-retirement)
    const ppfValues = [...matrix.map(r => r.ppfBalance), ...incomeProjection.slice(1).map(() => null)];
    const epfValues = [...matrix.map(r => r.epfBalance), ...incomeProjection.slice(1).map(() => null)];
    const mfValues = [...matrix.map(r => r.mfBalance), ...incomeProjection.slice(1).map(() => null)];
    
    // Goal outflows padded for full chart
    const goalOutflowsPadded = [...goalOutflows, ...incomeProjection.slice(1).map(() => 0)];

    // Get other liquid assets for the chart
    const otherLiquidValues = [...matrix.map(r => r.otherLiquidBalance || 0), ...incomeProjection.slice(1).map(() => null)];
    
    const maturityInflows = [...matrix.map(r => r.totalInflow || 0), ...incomeProjection.slice(1).map(() => 0)];
    const maturityMarkers = maturityInflows.map((val, idx) => (val > 0 ? combinedCorpus[idx] : null));

    const datasets = [
        {
            label: 'Total Corpus',
            data: combinedCorpus,
            borderColor: chartColors.success,
            backgroundColor: 'rgba(16, 185, 129, 0.15)',
            fill: true,
            tension: 0.4,
            borderWidth: 3,
            pointRadius: 0
        },
        {
            label: 'Maturity Events',
            data: maturityMarkers,
            borderColor: '#f59e0b',
            backgroundColor: '#ffffff',
            showLine: false,
            pointRadius: 6,
            pointHoverRadius: 7,
            pointBorderWidth: 2
        },
        {
            label: 'PPF + EPF',
            data: ppfValues.map((p, i) => (p || 0) + (epfValues[i] || 0)),
            borderColor: chartColors.blue,
            backgroundColor: 'transparent',
            tension: 0.4,
            pointRadius: 0,
            borderWidth: 2,
            spanGaps: false,
            hidden: true  // Hidden by default
        },
        {
            label: 'Mutual Funds',
            data: mfValues,
            borderColor: chartColors.purple,
            backgroundColor: 'transparent',
            tension: 0.4,
            pointRadius: 0,
            borderWidth: 2,
            spanGaps: false,
            hidden: true  // Hidden by default
        },
        {
            label: 'Other Liquid (FD/RD/Stock/Cash)',
            data: otherLiquidValues,
            borderColor: chartColors.teal,
            backgroundColor: 'transparent',
            tension: 0.4,
            pointRadius: 0,
            borderWidth: 2,
            spanGaps: false,
            hidden: true  // Hidden by default
        },
        {
            label: 'Annual SIP',
            data: sipValuesPadded,
            borderColor: chartColors.amber,
            backgroundColor: 'rgba(245, 158, 11, 0.1)',
            fill: true,
            tension: 0.4,
            borderWidth: 2,
            pointRadius: 0,
            yAxisID: 'y1',
            hidden: true  // Hidden by default
        }
    ];
    
    // Add Goals dataset if there are goals
    if (hasGoals) {
        datasets.push({
            label: 'Goal Outflows',
            data: goalOutflowsPadded,
            type: 'bar',
            backgroundColor: 'rgba(239, 68, 68, 0.7)',
            borderColor: chartColors.danger,
            borderWidth: 1,
            yAxisID: 'y1',
            barPercentage: 0.4,
            categoryPercentage: 0.8
        });
    }
    
    // Find the retirement year index for annotation
    const retirementYearIndex = matrix.length - 1;

    charts[canvasId] = new Chart(ctx, {
        type: 'line',
        data: { labels, datasets },
        options: {
            ...defaultOptions,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                ...defaultOptions.plugins,
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        padding: 15,
                        font: { size: 11 }
                    }
                },
                tooltip: {
                    backgroundColor: '#1e293b',
                    callbacks: {
                        title: (items) => {
                            if (items.length === 0) return '';
                            const idx = items[0].dataIndex;
                            const isPostRetirement = idx >= matrix.length;
                            if (isPostRetirement) {
                                return `Year ${labels[idx]} (Post-Retirement)`;
                            }
                            return `Year ${labels[idx]}`;
                        },
                        label: (ctx) => {
                            const value = ctx.raw;
                            if (value === null) return null; // Don't show null values
                            if (ctx.dataset.label === 'Maturity Events') {
                                const inflow = maturityInflows[ctx.dataIndex] || 0;
                                return `Maturity Inflow: ${formatCurrency(inflow)}`;
                            }
                            if (ctx.dataset.label === 'Goal Outflows' && value > 0) {
                                // Find goal names for this year
                                const yearIndex = ctx.dataIndex;
                                if (yearIndex < matrix.length) {
                                    const row = matrix[yearIndex];
                                    const goalNames = row?.goalsThisYear?.join(', ') || 'Goal';
                                    return `${goalNames}: -${formatCurrency(value)}`;
                                }
                                return `Goal: -${formatCurrency(value)}`;
                            }
                            return `${ctx.dataset.label}: ${formatCurrency(value)}`;
                        }
                    }
                }
            },
            scales: {
                ...defaultOptions.scales,
                y: {
                    ...defaultOptions.scales.y,
                    position: 'left',
                    title: {
                        display: true,
                        text: 'Corpus Value',
                        color: '#64748b'
                    },
                    ticks: {
                        color: '#64748b',
                        callback: (value) => formatCurrency(value, true)
                    }
                },
                y1: {
                    position: 'right',
                    title: {
                        display: true,
                        text: 'SIP / Goals',
                        color: '#64748b'
                    },
                    grid: { drawOnChartArea: false },
                    ticks: {
                        color: '#64748b',
                        callback: (value) => formatCurrency(value, true)
                    }
                }
            }
        }
    });
}

// Get cached retirement data for deferred chart rendering
function getCachedRetirementData() {
    return cachedRetirementData;
}

// Create Income vs Expense Chart
function createIncomeExpenseChart(canvasId, income, expenses) {
    destroyChart(canvasId);
    const ctx = document.getElementById(canvasId)?.getContext('2d');
    if (!ctx) return;

    charts[canvasId] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Monthly'],
            datasets: [
                {
                    label: 'Income',
                    data: [income],
                    backgroundColor: chartColors.success,
                    borderRadius: 6
                },
                {
                    label: 'Expenses',
                    data: [expenses],
                    backgroundColor: chartColors.danger,
                    borderRadius: 6
                }
            ]
        },
        options: {
            ...defaultOptions,
            plugins: {
                ...defaultOptions.plugins,
                tooltip: {
                    backgroundColor: '#1e293b',
                    callbacks: {
                        label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.raw)}`
                    }
                }
            },
            scales: {
                ...defaultOptions.scales,
                y: {
                    ...defaultOptions.scales.y,
                    ticks: {
                        callback: (value) => formatCurrency(value, true)
                    }
                }
            }
        }
    });
}
