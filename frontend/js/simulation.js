// Retyrment - Monte Carlo Simulation Page

let simulationChart = null;
let canRunSimulationFeature = false;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.isLoggedIn()) {
        window.location.href = 'login.html';
        return;
    }
    
    checkSimulationAccess();
});

async function checkSimulationAccess() {
    if (!auth.isLoggedIn()) {
        window.location.href = 'login.html';
        return;
    }
    
    try {
        // Check feature access from backend
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        
        // Check page access
        if (features.simulationPage !== true) {
            // Page access denied - redirect to dashboard
            showToast('You do not have access to the Simulation page.', 'error');
            window.location.href = 'index.html';
            return;
        }
        
        // Check if user can run simulations
        canRunSimulationFeature = features.canRunSimulation === true;
        
        // Show the simulation content
        document.getElementById('simulation-content').classList.remove('hidden');
        document.getElementById('pro-check').classList.add('hidden');
        
        // Update run button based on canRunSimulation feature
        const runBtn = document.getElementById('btn-run');
        if (!canRunSimulationFeature) {
            runBtn.disabled = true;
            runBtn.classList.add('opacity-50', 'cursor-not-allowed');
            runBtn.title = 'You do not have permission to run simulations. Contact admin for access.';
            
            // Show a notice
            const notice = document.createElement('div');
            notice.className = 'bg-amber-50 border border-amber-200 text-amber-800 px-4 py-3 rounded-lg mb-4';
            notice.innerHTML = `
                <p class="font-medium">⚠️ Simulation Running Disabled</p>
                <p class="text-sm mt-1">You can view this page but cannot run simulations. Contact your administrator to enable this feature.</p>
            `;
            const contentDiv = document.getElementById('simulation-content');
            contentDiv.insertBefore(notice, contentDiv.firstChild);
        }
        
    } catch (error) {
        console.error('Error checking simulation access:', error);
        // Fallback to role-based check
        const user = auth.getUser();
        const isPro = user && (user.role === 'PRO' || user.role === 'ADMIN' || 
                               user.effectiveRole === 'PRO' || user.effectiveRole === 'ADMIN');
        
        if (isPro) {
            canRunSimulationFeature = true;
            document.getElementById('simulation-content').classList.remove('hidden');
        } else {
            document.getElementById('pro-check').classList.remove('hidden');
            document.getElementById('simulation-content').classList.add('hidden');
        }
    }
}

async function runSimulation() {
    // Check if user can run simulations
    if (!canRunSimulationFeature) {
        showToast('You do not have permission to run simulations. Contact admin for access.', 'error');
        return;
    }
    
    const simulations = parseInt(document.getElementById('simulations').value) || 1000;
    const years = parseInt(document.getElementById('years').value) || 10;
    
    // Validate inputs
    if (simulations < 100 || simulations > 10000) {
        showToast('Number of simulations must be between 100 and 10,000', 'error');
        return;
    }
    
    if (years < 1 || years > 50) {
        showToast('Projection years must be between 1 and 50', 'error');
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
