// Retyrment - User Preferences Page

let options = {};
let currentPreferences = {};

document.addEventListener('DOMContentLoaded', async () => {
    await loadOptions();
    await loadPreferences();
});

async function loadOptions() {
    try {
        options = await api.preferences.getOptions();
        populateDropdowns();
    } catch (error) {
        console.error('Error loading options:', error);
        // Use fallback options
        options = {
            currencies: [
                { code: 'INR', symbol: '₹', name: 'Indian Rupee' },
                { code: 'USD', symbol: '$', name: 'US Dollar' },
                { code: 'EUR', symbol: '€', name: 'Euro' },
                { code: 'GBP', symbol: '£', name: 'British Pound' }
            ],
            countries: [
                { code: 'IN', name: 'India', defaultCurrency: 'INR', fyStartMonth: 4 },
                { code: 'US', name: 'United States', defaultCurrency: 'USD', fyStartMonth: 1 },
                { code: 'UK', name: 'United Kingdom', defaultCurrency: 'GBP', fyStartMonth: 4 }
            ],
            dateFormats: [
                { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (31/12/2024)' },
                { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (12/31/2024)' },
                { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (2024-12-31)' }
            ],
            numberFormats: [
                { value: 'indian', label: 'Indian (12,34,567.00)' },
                { value: 'western', label: 'Western (1,234,567.00)' }
            ],
            themes: [
                { value: 'light', label: 'Light' },
                { value: 'dark', label: 'Dark' },
                { value: 'system', label: 'System Default' }
            ]
        };
        populateDropdowns();
    }
}

function populateDropdowns() {
    // Countries
    const countrySelect = document.getElementById('pref-country');
    countrySelect.innerHTML = options.countries.map(c => 
        `<option value="${c.code}">${c.name}</option>`
    ).join('');

    // Currencies
    const currencySelect = document.getElementById('pref-currency');
    currencySelect.innerHTML = options.currencies.map(c => 
        `<option value="${c.code}">${c.symbol} ${c.name} (${c.code})</option>`
    ).join('');

    // Date Formats
    const dateFormatSelect = document.getElementById('pref-dateFormat');
    dateFormatSelect.innerHTML = options.dateFormats.map(f => 
        `<option value="${f.value}">${f.label}</option>`
    ).join('');

    // Number Formats
    const numberFormatSelect = document.getElementById('pref-numberFormat');
    numberFormatSelect.innerHTML = options.numberFormats.map(f => 
        `<option value="${f.value}">${f.label}</option>`
    ).join('');

    // Themes
    const themeSelect = document.getElementById('pref-theme');
    themeSelect.innerHTML = options.themes.map(t => 
        `<option value="${t.value}">${t.label}</option>`
    ).join('');

    // Add country change listener to auto-set currency and FY start month
    countrySelect.addEventListener('change', function() {
        const selectedCountry = options.countries.find(c => c.code === this.value);
        if (selectedCountry) {
            document.getElementById('pref-currency').value = selectedCountry.defaultCurrency;
            document.getElementById('pref-financialYearStartMonth').value = selectedCountry.fyStartMonth;
        }
    });
}

async function loadPreferences() {
    try {
        currentPreferences = await api.preferences.get();
        populateForm(currentPreferences);
    } catch (error) {
        console.error('Error loading preferences:', error);
        showToast('Could not load preferences. Using defaults.', 'warning');
        // Use defaults
        populateForm({
            country: 'IN',
            currency: 'INR',
            dateFormat: 'DD/MM/YYYY',
            numberFormat: 'indian',
            theme: 'light',
            showAmountInWords: true,
            defaultInflationRate: 6.0,
            defaultEquityReturn: 12.0,
            defaultDebtReturn: 7.0,
            defaultRetirementAge: 60,
            financialYearStartMonth: 4,
            emailNotifications: true,
            paymentReminders: true,
            reminderDaysBefore: 3
        });
    }
}

function populateForm(prefs) {
    document.getElementById('pref-country').value = prefs.country || 'IN';
    document.getElementById('pref-currency').value = prefs.currency || 'INR';
    document.getElementById('pref-dateFormat').value = prefs.dateFormat || 'DD/MM/YYYY';
    document.getElementById('pref-numberFormat').value = prefs.numberFormat || 'indian';
    document.getElementById('pref-theme').value = prefs.theme || 'light';
    document.getElementById('pref-showAmountInWords').checked = prefs.showAmountInWords !== false;
    document.getElementById('pref-defaultInflationRate').value = prefs.defaultInflationRate || 6.0;
    document.getElementById('pref-defaultEquityReturn').value = prefs.defaultEquityReturn || 12.0;
    document.getElementById('pref-defaultDebtReturn').value = prefs.defaultDebtReturn || 7.0;
    document.getElementById('pref-defaultRetirementAge').value = prefs.defaultRetirementAge || 60;
    document.getElementById('pref-financialYearStartMonth').value = prefs.financialYearStartMonth || 4;
    document.getElementById('pref-emailNotifications').checked = prefs.emailNotifications !== false;
    document.getElementById('pref-paymentReminders').checked = prefs.paymentReminders !== false;
    document.getElementById('pref-reminderDaysBefore').value = prefs.reminderDaysBefore || 3;
}

function getFormValues() {
    return {
        country: document.getElementById('pref-country').value,
        currency: document.getElementById('pref-currency').value,
        dateFormat: document.getElementById('pref-dateFormat').value,
        numberFormat: document.getElementById('pref-numberFormat').value,
        theme: document.getElementById('pref-theme').value,
        showAmountInWords: document.getElementById('pref-showAmountInWords').checked,
        defaultInflationRate: parseFloat(document.getElementById('pref-defaultInflationRate').value) || 6.0,
        defaultEquityReturn: parseFloat(document.getElementById('pref-defaultEquityReturn').value) || 12.0,
        defaultDebtReturn: parseFloat(document.getElementById('pref-defaultDebtReturn').value) || 7.0,
        defaultRetirementAge: parseInt(document.getElementById('pref-defaultRetirementAge').value) || 60,
        financialYearStartMonth: parseInt(document.getElementById('pref-financialYearStartMonth').value) || 4,
        emailNotifications: document.getElementById('pref-emailNotifications').checked,
        paymentReminders: document.getElementById('pref-paymentReminders').checked,
        reminderDaysBefore: parseInt(document.getElementById('pref-reminderDaysBefore').value) || 3
    };
}

async function savePreferences() {
    try {
        const prefs = getFormValues();
        const saved = await api.preferences.update(prefs);
        currentPreferences = saved;
        
        // Store in localStorage for quick access by other pages
        localStorage.setItem('retyrment_preferences', JSON.stringify(saved));
        
        showToast('Preferences saved successfully!', 'success');
        
        // Apply theme if changed
        applyTheme(saved.theme);
        
    } catch (error) {
        console.error('Error saving preferences:', error);
        showToast('Error saving preferences: ' + error.message, 'error');
    }
}

function resetToDefaults() {
    if (confirm('Reset all preferences to defaults?')) {
        populateForm({
            country: 'IN',
            currency: 'INR',
            dateFormat: 'DD/MM/YYYY',
            numberFormat: 'indian',
            theme: 'light',
            showAmountInWords: true,
            defaultInflationRate: 6.0,
            defaultEquityReturn: 12.0,
            defaultDebtReturn: 7.0,
            defaultRetirementAge: 60,
            financialYearStartMonth: 4,
            emailNotifications: true,
            paymentReminders: true,
            reminderDaysBefore: 3
        });
        showToast('Preferences reset to defaults. Click Save to apply.', 'info');
    }
}

function applyTheme(theme) {
    // For now, we only support light theme
    // In the future, this can toggle dark mode
    if (theme === 'dark') {
        document.body.classList.add('dark-mode');
    } else {
        document.body.classList.remove('dark-mode');
    }
}

// Helper function to get user's currency symbol
function getUserCurrencySymbol() {
    const prefs = JSON.parse(localStorage.getItem('retyrment_preferences') || '{}');
    return prefs.currencySymbol || '₹';
}

// Helper function to format currency based on user preferences
function formatUserCurrency(amount, short = false) {
    const prefs = JSON.parse(localStorage.getItem('retyrment_preferences') || '{}');
    const symbol = prefs.currencySymbol || '₹';
    const format = prefs.numberFormat || 'indian';
    
    if (!amount || isNaN(amount)) return symbol + '0';
    const absAmount = Math.abs(amount);
    
    let formatted;
    if (short) {
        if (format === 'indian') {
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
            if (absAmount >= 1000000000) {
                formatted = (absAmount / 1000000000).toFixed(2) + 'B';
            } else if (absAmount >= 1000000) {
                formatted = (absAmount / 1000000).toFixed(2) + 'M';
            } else if (absAmount >= 1000) {
                formatted = (absAmount / 1000).toFixed(1) + 'K';
            } else {
                formatted = absAmount.toFixed(0);
            }
        }
    } else {
        if (format === 'indian') {
            formatted = absAmount.toLocaleString('en-IN');
        } else {
            formatted = absAmount.toLocaleString('en-US');
        }
    }
    
    return (amount < 0 ? '-' + symbol : symbol) + formatted;
}
