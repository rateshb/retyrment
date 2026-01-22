// Settings management for VanillaJS frontend

// Default settings
const defaultSettings = {
    currentAge: 35,
    retirementAge: 60,
    lifeExpectancy: 85,
    inflationRate: 6.0,
    epfReturn: 8.15,
    ppfReturn: 7.1,
    mfEquityReturn: 12.0,
    mfDebtReturn: 7.0,
    fdReturn: 6.5,
    emergencyFundMonths: 6,
    sipStepup: 10
};

// Load settings from backend on page load
async function loadSettings() {
    try {
        const response = await fetch(`${API_BASE}/settings`, {
            headers: auth.getHeaders(),
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const settings = await response.json();
        
        if (settings) {
            // Populate form with loaded settings
            document.getElementById('current-age').value = settings.currentAge !== undefined ? settings.currentAge : defaultSettings.currentAge;
            document.getElementById('retirement-age').value = settings.retirementAge !== undefined ? settings.retirementAge : defaultSettings.retirementAge;
            document.getElementById('life-expectancy').value = settings.lifeExpectancy !== undefined ? settings.lifeExpectancy : defaultSettings.lifeExpectancy;
            document.getElementById('inflation').value = settings.inflationRate !== undefined ? settings.inflationRate : defaultSettings.inflationRate;
            document.getElementById('epf-return').value = settings.epfReturn !== undefined ? settings.epfReturn : defaultSettings.epfReturn;
            document.getElementById('ppf-return').value = settings.ppfReturn !== undefined ? settings.ppfReturn : defaultSettings.ppfReturn;
            document.getElementById('mf-equity-return').value = settings.mfEquityReturn !== undefined ? settings.mfEquityReturn : defaultSettings.mfEquityReturn;
            document.getElementById('mf-debt-return').value = settings.mfDebtReturn !== undefined ? settings.mfDebtReturn : defaultSettings.mfDebtReturn;
            document.getElementById('fd-return').value = settings.fdReturn !== undefined ? settings.fdReturn : defaultSettings.fdReturn;
            document.getElementById('emergency-months').value = settings.emergencyFundMonths !== undefined ? settings.emergencyFundMonths : defaultSettings.emergencyFundMonths;
            document.getElementById('sip-stepup').value = settings.sipStepup !== undefined ? settings.sipStepup : defaultSettings.sipStepup;
        } else {
            // No settings found, populate with defaults
            resetToDefaults();
        }
    } catch (error) {
        console.error('Error loading settings:', error);
        showToast('Failed to load settings. Using defaults.', 'error');
        resetToDefaults();
    }
}

// Save settings to backend
async function saveSettings() {
    const settings = {
        currentAge: parseInt(document.getElementById('current-age').value),
        retirementAge: parseInt(document.getElementById('retirement-age').value),
        lifeExpectancy: parseInt(document.getElementById('life-expectancy').value),
        inflationRate: parseFloat(document.getElementById('inflation').value),
        epfReturn: parseFloat(document.getElementById('epf-return').value),
        ppfReturn: parseFloat(document.getElementById('ppf-return').value),
        mfEquityReturn: parseFloat(document.getElementById('mf-equity-return').value),
        mfDebtReturn: parseFloat(document.getElementById('mf-debt-return').value),
        fdReturn: parseFloat(document.getElementById('fd-return').value),
        emergencyFundMonths: parseInt(document.getElementById('emergency-months').value),
        sipStepup: parseFloat(document.getElementById('sip-stepup').value)
    };

    try {
        // Use the api.settings.update function directly (it returns a promise)
        const response = await fetch(`${API_BASE}/settings`, {
            method: 'PUT',
            headers: auth.getHeaders(),
            body: JSON.stringify(settings),
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        showToast('Settings saved successfully!', 'success');
    } catch (error) {
        console.error('Error saving settings:', error);
        showToast('Failed to save settings', 'error');
    }
}

// Reset to default settings
function resetToDefaults() {
    document.getElementById('current-age').value = defaultSettings.currentAge;
    document.getElementById('retirement-age').value = defaultSettings.retirementAge;
    document.getElementById('life-expectancy').value = defaultSettings.lifeExpectancy;
    document.getElementById('inflation').value = defaultSettings.inflationRate;
    document.getElementById('epf-return').value = defaultSettings.epfReturn;
    document.getElementById('ppf-return').value = defaultSettings.ppfReturn;
    document.getElementById('mf-equity-return').value = defaultSettings.mfEquityReturn;
    document.getElementById('mf-debt-return').value = defaultSettings.mfDebtReturn;
    document.getElementById('fd-return').value = defaultSettings.fdReturn;
    document.getElementById('emergency-months').value = defaultSettings.emergencyFundMonths;
    document.getElementById('sip-stepup').value = defaultSettings.sipStepup;
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    loadSettings();
});
