// Retyrment - Monte Carlo Simulation Page

let simulationChart = null;

document.addEventListener('DOMContentLoaded', () => {
    checkProAccess();
    if (auth.isAuthenticated()) {
        const user = auth.getUser();
        const isPro = user && (user.role === 'PRO' || user.role === 'ADMIN' || 
                               user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN');
        if (isPro) {
            document.getElementById('simulation-content').classList.remove('hidden');
        } else {
            document.getElementById('pro-check').classList.remove('hidden');
            document.getElementById('simulation-content').classList.add('hidden');
        }
    } else {
        window.location.href = 'login.html';
    }
});

function checkProAccess() {
    if (!auth.isAuthenticated()) {
        window.location.href = 'login.html';
        return;
    }
    
    const user = auth.getUser();
    const isPro = user && (user.role === 'PRO' || user.role === 'ADMIN' || 
                           user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN');
    
    if (!isPro) {
        document.getElementById('pro-check').classList.remove('hidden');
        document.getElementById('simulation-content').classList.add('hidden');
    }
}

async function runSimulation() {
    const simulations = parseInt(document.getElementById('simulations').value) || 1000;
    const years = parseInt(document.getElementById('years').value) || 10;
    
    // Validate inputs
    if (simulations < 100 || simulations > 10000) {
        alert('Number of simulations must be between 100 and 10,000');
        return;
    }
    
    if (years < 1 || years > 50) {
        alert('Projection years must be between 1 and 50');
        return;
    }
    
    // Show loading
    document.getElementById('loading').classList.remove('hidden');
    document.getElementById('results').classList.add('hidden');
    document.getElementById('btn-run').disabled = true;
    
    try {
        const result = await api.analysis.runMonteCarlo(simulations, years);
        
        // Hide loading, show results
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('results').classList.remove('hidden');
        document.getElementById('btn-run').disabled = false;
        
        // Update summary cards
        document.getElementById('percentile10').textContent = formatCurrency(result.percentile10, true);
        document.getElementById('percentile50').textContent = formatCurrency(result.percentile50, true);
        document.getElementById('percentile90').textContent = formatCurrency(result.percentile90, true);
        
        // Update detailed percentiles
        document.getElementById('p10').textContent = formatCurrency(result.percentile10, true);
        document.getElementById('p25').textContent = formatCurrency(result.percentile25, true);
        document.getElementById('p50').textContent = formatCurrency(result.percentile50, true);
        document.getElementById('p75').textContent = formatCurrency(result.percentile75, true);
        document.getElementById('p90').textContent = formatCurrency(result.percentile90, true);
        document.getElementById('average').textContent = formatCurrency(result.average, true);
        
        // Create/update chart
        createSimulationChart(result);
        
    } catch (error) {
        console.error('Simulation error:', error);
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('btn-run').disabled = false;
        alert('Failed to run simulation. Please try again.');
    }
}

function createSimulationChart(data) {
    const ctx = document.getElementById('simulation-chart');
    if (!ctx) return;
    
    // Destroy existing chart if it exists
    if (simulationChart) {
        simulationChart.destroy();
    }
    
    const percentileValues = [
        data.percentile10,
        data.percentile25,
        data.percentile50,
        data.percentile75,
        data.percentile90
    ];
    
    const percentileLabels = ['10th', '25th', '50th (Median)', '75th', '90th'];
    
    simulationChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: percentileLabels,
            datasets: [{
                label: 'Portfolio Value',
                data: percentileValues,
                backgroundColor: [
                    'rgba(239, 68, 68, 0.7)',  // Red for 10th
                    'rgba(245, 158, 11, 0.7)', // Amber for 25th
                    'rgba(99, 102, 241, 0.7)', // Primary for 50th
                    'rgba(34, 211, 153, 0.7)', // Green for 75th
                    'rgba(16, 185, 129, 0.7)'  // Success for 90th
                ],
                borderColor: [
                    'rgba(239, 68, 68, 1)',
                    'rgba(245, 158, 11, 1)',
                    'rgba(99, 102, 241, 1)',
                    'rgba(34, 211, 153, 1)',
                    'rgba(16, 185, 129, 1)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return '₹' + context.parsed.y.toLocaleString('en-IN');
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return '₹' + (value / 100000).toFixed(1) + 'L';
                        }
                    },
                    title: {
                        display: true,
                        text: 'Portfolio Value'
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Percentile'
                    }
                }
            }
        }
    });
}

function formatCurrency(amount, showSymbol = false) {
    if (amount === null || amount === undefined) return '₹0';
    const formatted = Math.round(amount).toLocaleString('en-IN');
    return showSymbol ? '₹' + formatted : formatted;
}

function showUpgradePrompt() {
    window.location.href = 'pricing.html';
}
