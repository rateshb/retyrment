// Retyrment PDF Export Module
// Uses jsPDF for PDF generation

const { jsPDF } = window.jspdf;

// Color palette for PDF
const pdfColors = {
    primary: [99, 102, 241],      // Indigo
    success: [16, 185, 129],      // Green
    warning: [245, 158, 11],      // Amber
    danger: [239, 68, 68],        // Red
    dark: [30, 41, 59],           // Slate 800
    medium: [100, 116, 139],      // Slate 500
    light: [241, 245, 249],       // Slate 100
    white: [255, 255, 255]
};

// Helper to format currency for PDF
function pdfFormatCurrency(amount, compact = false) {
    if (!amount && amount !== 0) return '₹0';
    if (compact) {
        if (amount >= 10000000) return '₹' + (amount / 10000000).toFixed(2) + ' Cr';
        if (amount >= 100000) return '₹' + (amount / 100000).toFixed(2) + ' L';
        if (amount >= 1000) return '₹' + (amount / 1000).toFixed(1) + ' K';
    }
    return '₹' + amount.toLocaleString('en-IN');
}

// Generate Financial Summary PDF
async function generateFinancialSummaryPDF() {
    showToast('Generating PDF report...');
    
    try {
        // Fetch all data
        const [netWorth, income, investments, loans, insurance, goals, expenses] = await Promise.all([
            api.analysis.getNetWorth().catch(() => null),
            api.income.getAll().catch(() => []),
            api.investments.getAll().catch(() => []),
            api.loans.getAll().catch(() => []),
            api.insurance.getAll().catch(() => []),
            api.goals.getAll().catch(() => []),
            api.expenses.getAll().catch(() => [])
        ]);

        const doc = new jsPDF();
        const pageWidth = doc.internal.pageSize.getWidth();
        const margin = 15;
        let y = 20;

        // Header
        doc.setFillColor(...pdfColors.primary);
        doc.rect(0, 0, pageWidth, 35, 'F');
        
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(24);
        doc.setFont('helvetica', 'bold');
        doc.text('Retyrment', margin, 18);
        
        doc.setFontSize(12);
        doc.setFont('helvetica', 'normal');
        doc.text('Financial Summary Report', margin, 28);
        
        doc.setFontSize(10);
        doc.text(new Date().toLocaleDateString('en-IN', { 
            year: 'numeric', month: 'long', day: 'numeric' 
        }), pageWidth - margin, 28, { align: 'right' });

        y = 50;

        // Net Worth Section
        doc.setTextColor(...pdfColors.dark);
        doc.setFontSize(16);
        doc.setFont('helvetica', 'bold');
        doc.text('Net Worth Overview', margin, y);
        y += 10;

        if (netWorth) {
            // Net worth cards
            const cardWidth = (pageWidth - 3 * margin) / 2;
            
            // Total Assets
            doc.setFillColor(...pdfColors.light);
            doc.roundedRect(margin, y, cardWidth, 25, 3, 3, 'F');
            doc.setFontSize(10);
            doc.setTextColor(...pdfColors.medium);
            doc.text('Total Assets', margin + 5, y + 8);
            doc.setFontSize(14);
            doc.setTextColor(...pdfColors.success);
            doc.setFont('helvetica', 'bold');
            doc.text(pdfFormatCurrency(netWorth.totalAssets, true), margin + 5, y + 18);

            // Total Liabilities
            doc.setFillColor(...pdfColors.light);
            doc.roundedRect(margin + cardWidth + margin/2, y, cardWidth, 25, 3, 3, 'F');
            doc.setFontSize(10);
            doc.setTextColor(...pdfColors.medium);
            doc.setFont('helvetica', 'normal');
            doc.text('Total Liabilities', margin + cardWidth + margin/2 + 5, y + 8);
            doc.setFontSize(14);
            doc.setTextColor(...pdfColors.danger);
            doc.setFont('helvetica', 'bold');
            doc.text(pdfFormatCurrency(netWorth.totalLiabilities, true), margin + cardWidth + margin/2 + 5, y + 18);

            y += 35;

            // Net Worth
            doc.setFillColor(...pdfColors.primary);
            doc.roundedRect(margin, y, pageWidth - 2 * margin, 25, 3, 3, 'F');
            doc.setTextColor(...pdfColors.white);
            doc.setFontSize(12);
            doc.setFont('helvetica', 'normal');
            doc.text('Net Worth', margin + 10, y + 10);
            doc.setFontSize(18);
            doc.setFont('helvetica', 'bold');
            doc.text(pdfFormatCurrency(netWorth.netWorth, true), margin + 10, y + 20);

            y += 35;
            
            // Asset Breakdown
            if (netWorth.assetBreakdown) {
                if (y > 240) { doc.addPage(); y = 20; }
                
                doc.setTextColor(...pdfColors.dark);
                doc.setFontSize(12);
                doc.setFont('helvetica', 'bold');
                doc.text('Asset Breakdown', margin, y);
                y += 8;
                
                doc.setFontSize(9);
                doc.setFont('helvetica', 'normal');
                doc.setTextColor(...pdfColors.medium);
                
                const assetTypes = ['MUTUAL_FUND', 'STOCK', 'FD', 'RD', 'PPF', 'EPF', 'NPS', 'GOLD', 'REAL_ESTATE', 'CASH', 'CRYPTO'];
                assetTypes.forEach(type => {
                    const value = netWorth.assetBreakdown[type] || 0;
                    if (value > 0) {
                        if (y > 270) { doc.addPage(); y = 20; }
                        const typeLabel = type.replace(/_/g, ' ');
                        doc.text(`• ${typeLabel}: ${pdfFormatCurrency(value, true)}`, margin + 5, y);
                        y += 6;
                    }
                });
                y += 10;
            }
        }

        // Cash Flow Analysis
        const totalMonthlyIncome = income.reduce((sum, i) => sum + (i.monthlyAmount || 0), 0);
        const totalMonthlyExpenses = expenses.reduce((sum, e) => sum + (e.monthlyAmount || 0), 0);
        const totalMonthlyEMI = loans.reduce((sum, l) => sum + (l.emi || 0), 0);
        const totalMonthlySIP = investments.reduce((sum, inv) => sum + (inv.monthlySip || 0), 0);
        const netMonthlySavings = totalMonthlyIncome - totalMonthlyExpenses - totalMonthlyEMI;
        const savingsRate = totalMonthlyIncome > 0 ? (netMonthlySavings / totalMonthlyIncome * 100) : 0;
        
        if (y > 200) { doc.addPage(); y = 20; }
        
        doc.setTextColor(...pdfColors.dark);
        doc.setFontSize(14);
        doc.setFont('helvetica', 'bold');
        doc.text('Monthly Cash Flow', margin, y);
        y += 10;
        
        const cardWidth = (pageWidth - 3 * margin) / 2;
        
        // Income Card
        doc.setFillColor(...pdfColors.success);
        doc.roundedRect(margin, y, cardWidth, 20, 3, 3, 'F');
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(9);
        doc.setFont('helvetica', 'normal');
        doc.text('Total Income', margin + 5, y + 8);
        doc.setFontSize(12);
        doc.setFont('helvetica', 'bold');
        doc.text(pdfFormatCurrency(totalMonthlyIncome), margin + 5, y + 16);
        
        // Expenses Card
        doc.setFillColor(...pdfColors.danger);
        doc.roundedRect(margin + cardWidth + margin/2, y, cardWidth, 20, 3, 3, 'F');
        doc.text('Total Expenses', margin + cardWidth + margin/2 + 5, y + 8);
        doc.setFontSize(12);
        doc.setFont('helvetica', 'bold');
        doc.text(pdfFormatCurrency(totalMonthlyExpenses), margin + cardWidth + margin/2 + 5, y + 16);
        
        y += 25;
        
        // EMIs Card
        doc.setFillColor(...pdfColors.warning);
        doc.roundedRect(margin, y, cardWidth, 20, 3, 3, 'F');
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(9);
        doc.setFont('helvetica', 'normal');
        doc.text('Total EMIs', margin + 5, y + 8);
        doc.setFontSize(12);
        doc.setFont('helvetica', 'bold');
        doc.text(pdfFormatCurrency(totalMonthlyEMI), margin + 5, y + 16);
        
        // Net Savings Card
        doc.setFillColor(...pdfColors.primary);
        doc.roundedRect(margin + cardWidth + margin/2, y, cardWidth, 20, 3, 3, 'F');
        doc.text('Net Savings', margin + cardWidth + margin/2 + 5, y + 8);
        doc.setFontSize(12);
        doc.setFont('helvetica', 'bold');
        doc.text(pdfFormatCurrency(netMonthlySavings), margin + cardWidth + margin/2 + 5, y + 16);
        
        y += 25;
        
        // Savings Rate
        doc.setTextColor(...pdfColors.dark);
        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');
        const savingsRateColor = savingsRate >= 30 ? pdfColors.success : savingsRate >= 20 ? pdfColors.warning : pdfColors.danger;
        doc.setFillColor(...savingsRateColor);
        doc.roundedRect(margin, y, pageWidth - 2 * margin, 15, 3, 3, 'F');
        doc.setTextColor(...pdfColors.white);
        doc.setFont('helvetica', 'bold');
        doc.text(`Savings Rate: ${savingsRate.toFixed(1)}%`, margin + 10, y + 10);
        
        y += 25;

        // Income Section
        if (income.length > 0) {
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Income Sources', margin, y);
            y += 8;

            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);

            const totalIncome = income.reduce((sum, i) => sum + (i.monthlyAmount || 0), 0);
            
            income.forEach(item => {
                if (y > 270) { doc.addPage(); y = 20; }
                const percent = totalIncome > 0 ? ((item.monthlyAmount || 0) / totalIncome * 100).toFixed(1) : 0;
                doc.text(`• ${item.source || 'Income'}: ${pdfFormatCurrency(item.monthlyAmount)}/mo (${percent}%)`, margin + 5, y);
                y += 6;
            });
            
            if (y > 260) { doc.addPage(); y = 20; }
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...pdfColors.dark);
            doc.text(`Total: ${pdfFormatCurrency(totalIncome)}/month`, margin + 5, y);
            doc.setFont('helvetica', 'normal');
            y += 10;
        }
        
        // Expenses Section
        if (expenses.length > 0) {
            if (y > 220) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Monthly Expenses', margin, y);
            y += 8;
            
            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            
            const totalExpenses = expenses.reduce((sum, e) => sum + (e.monthlyAmount || 0), 0);
            
            expenses.slice(0, 10).forEach(exp => {
                if (y > 270) { doc.addPage(); y = 20; }
                const percent = totalExpenses > 0 ? ((exp.monthlyAmount || 0) / totalExpenses * 100).toFixed(1) : 0;
                doc.text(`• ${exp.category || 'Expense'}: ${pdfFormatCurrency(exp.monthlyAmount)}/mo (${percent}%)`, margin + 5, y);
                y += 6;
            });
            
            if (expenses.length > 10) {
                if (y > 270) { doc.addPage(); y = 20; }
                doc.setFontSize(8);
                doc.text(`... and ${expenses.length - 10} more expenses`, margin + 5, y);
                y += 6;
            }
            
            if (y > 260) { doc.addPage(); y = 20; }
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...pdfColors.dark);
            doc.text(`Total: ${pdfFormatCurrency(totalExpenses)}/month`, margin + 5, y);
            doc.setFont('helvetica', 'normal');
            y += 10;
        }

        // Investments Section
        if (investments.length > 0) {
            if (y > 220) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Investment Portfolio', margin, y);
            y += 10;

            // Investment Summary
            const totalInvested = investments.reduce((sum, inv) => sum + (inv.investedAmount || 0), 0);
            const totalCurrent = investments.reduce((sum, inv) => sum + (inv.currentValue || 0), 0);
            const totalGain = totalCurrent - totalInvested;
            const totalReturnPercent = totalInvested > 0 ? (totalGain / totalInvested * 100) : 0;
            
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            doc.text(`Total Invested: ${pdfFormatCurrency(totalInvested, true)} | Current Value: ${pdfFormatCurrency(totalCurrent, true)} | Gain: ${pdfFormatCurrency(totalGain, true)} (${totalReturnPercent.toFixed(1)}%)`, margin, y);
            y += 8;
            
            // Table header
            doc.setFillColor(...pdfColors.light);
            doc.rect(margin, y, pageWidth - 2 * margin, 8, 'F');
            doc.setFontSize(8);
            doc.setTextColor(...pdfColors.dark);
            doc.setFont('helvetica', 'bold');
            doc.text('Type', margin + 3, y + 6);
            doc.text('Name', margin + 25, y + 6);
            doc.text('Invested', margin + 75, y + 6);
            doc.text('Current', margin + 105, y + 6);
            doc.text('Gain', margin + 135, y + 6);
            doc.text('Return%', margin + 160, y + 6);
            y += 10;

            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);

            investments.slice(0, 20).forEach(inv => {
                if (y > 270) { doc.addPage(); y = 20; }
                const gain = (inv.currentValue || 0) - (inv.investedAmount || 0);
                const returnPct = (inv.investedAmount || 0) > 0 ? (gain / inv.investedAmount * 100) : 0;
                const gainColor = gain >= 0 ? pdfColors.success : pdfColors.danger;
                
                doc.setFontSize(8);
                doc.text(inv.type || '-', margin + 3, y + 4);
                doc.text((inv.name || '-').substring(0, 18), margin + 25, y + 4);
                doc.text(pdfFormatCurrency(inv.investedAmount, true), margin + 75, y + 4);
                doc.text(pdfFormatCurrency(inv.currentValue, true), margin + 105, y + 4);
                doc.setTextColor(...gainColor);
                doc.text(pdfFormatCurrency(gain, true), margin + 135, y + 4);
                doc.text(`${returnPct.toFixed(1)}%`, margin + 160, y + 4);
                doc.setTextColor(...pdfColors.medium);
                y += 7;
            });

            if (investments.length > 15) {
                doc.setFontSize(8);
                doc.text(`... and ${investments.length - 15} more investments`, margin + 5, y + 4);
                y += 7;
            }
            y += 10;
        }

        // Loans Section
        if (loans.length > 0) {
            if (y > 220) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Active Loans', margin, y);
            y += 8;
            
            // Loan Summary
            const totalOutstanding = loans.reduce((sum, l) => sum + (l.outstandingAmount || 0), 0);
            const totalEMI = loans.reduce((sum, l) => sum + (l.emi || 0), 0);
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            doc.text(`Total Outstanding: ${pdfFormatCurrency(totalOutstanding, true)} | Total EMI: ${pdfFormatCurrency(totalEMI)}/month`, margin, y);
            y += 10;
            
            // Table header
            doc.setFillColor(...pdfColors.light);
            doc.rect(margin, y, pageWidth - 2 * margin, 8, 'F');
            doc.setFontSize(8);
            doc.setTextColor(...pdfColors.dark);
            doc.setFont('helvetica', 'bold');
            doc.text('Loan', margin + 3, y + 6);
            doc.text('Outstanding', margin + 50, y + 6);
            doc.text('EMI', margin + 95, y + 6);
            doc.text('Rate', margin + 120, y + 6);
            doc.text('Tenure', margin + 145, y + 6);
            y += 10;

            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            doc.setFontSize(8);

            loans.forEach(loan => {
                if (y > 270) { doc.addPage(); y = 20; }
                doc.text((loan.name || loan.type || 'Loan').substring(0, 20), margin + 3, y + 4);
                doc.text(pdfFormatCurrency(loan.outstandingAmount || 0, true), margin + 50, y + 4);
                doc.text(pdfFormatCurrency(loan.emi || 0), margin + 95, y + 4);
                doc.text(`${loan.interestRate || 0}%`, margin + 120, y + 4);
                doc.text(`${loan.tenureYears || 0}y`, margin + 145, y + 4);
                y += 7;
            });
            y += 10;
        }
        
        // Insurance Section
        if (insurance.length > 0) {
            if (y > 220) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Insurance Policies', margin, y);
            y += 8;
            
            const totalPremium = insurance.reduce((sum, ins) => sum + (ins.annualPremium || 0), 0);
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            doc.text(`Total Annual Premium: ${pdfFormatCurrency(totalPremium)} (${pdfFormatCurrency(totalPremium / 12)}/month)`, margin, y);
            y += 10;
            
            // Table header
            doc.setFillColor(...pdfColors.light);
            doc.rect(margin, y, pageWidth - 2 * margin, 8, 'F');
            doc.setFontSize(8);
            doc.setTextColor(...pdfColors.dark);
            doc.setFont('helvetica', 'bold');
            doc.text('Policy', margin + 3, y + 6);
            doc.text('Type', margin + 60, y + 6);
            doc.text('Sum Assured', margin + 95, y + 6);
            doc.text('Premium', margin + 135, y + 6);
            y += 10;

            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            doc.setFontSize(8);

            insurance.slice(0, 15).forEach(ins => {
                if (y > 270) { doc.addPage(); y = 20; }
                doc.text((ins.policyName || '-').substring(0, 25), margin + 3, y + 4);
                doc.text((ins.type || '-').substring(0, 12), margin + 60, y + 4);
                doc.text(pdfFormatCurrency(ins.sumAssured || 0, true), margin + 95, y + 4);
                doc.text(pdfFormatCurrency(ins.annualPremium || 0), margin + 135, y + 4);
                y += 7;
            });
            
            if (insurance.length > 15) {
                if (y > 270) { doc.addPage(); y = 20; }
                doc.setFontSize(8);
                doc.text(`... and ${insurance.length - 15} more policies`, margin + 5, y + 4);
                y += 7;
            }
            y += 10;
        }

        // Goals Section
        if (goals.length > 0) {
            if (y > 220) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Financial Goals', margin, y);
            y += 10;

            goals.forEach(goal => {
                if (y > 270) { doc.addPage(); y = 20; }
                const progress = goal.fundingPercent || 0;
                const progressColor = progress >= 100 ? pdfColors.success : progress >= 50 ? pdfColors.warning : pdfColors.danger;
                
                doc.setFontSize(10);
                doc.setTextColor(...pdfColors.dark);
                doc.text(`${goal.name}: ${pdfFormatCurrency(goal.targetAmount, true)} by ${goal.targetYear || '-'}`, margin + 5, y);
                
                // Progress bar
                doc.setFillColor(...pdfColors.light);
                doc.rect(margin + 5, y + 2, 80, 4, 'F');
                doc.setFillColor(...progressColor);
                doc.rect(margin + 5, y + 2, Math.min(80 * progress / 100, 80), 4, 'F');
                
                doc.setFontSize(8);
                doc.setTextColor(...pdfColors.medium);
                doc.text(`${progress.toFixed(0)}%`, margin + 90, y + 5);
                
                y += 12;
            });
        }

        // Footer
        doc.setFontSize(8);
        doc.setTextColor(...pdfColors.medium);
        doc.text('Generated by Retyrment • ' + new Date().toLocaleString('en-IN'), pageWidth / 2, 290, { align: 'center' });

        // Save
        doc.save(`Retyrment_Summary_${new Date().toISOString().split('T')[0]}.pdf`);
        showToast('PDF downloaded successfully!');

    } catch (error) {
        console.error('PDF generation error:', error);
        showToast('Error generating PDF: ' + error.message, 'error');
    }
}

// Generate Retirement Report PDF
async function generateRetirementPDF() {
    showToast('Generating retirement report...');
    
    try {
        // Fetch retirement data
        const data = await api.post('/retirement/calculate', {
            currentAge: 35,
            retirementAge: 60,
            lifeExpectancy: 85,
            inflation: 6,
            mfReturn: 12,
            epfReturn: 8.15,
            ppfReturn: 7.1,
            sipStepup: 10,
            incomeStrategy: 'SUSTAINABLE'
        });

        const doc = new jsPDF();
        const pageWidth = doc.internal.pageSize.getWidth();
        const margin = 15;
        let y = 20;

        // Header
        doc.setFillColor(20, 184, 166); // Teal
        doc.rect(0, 0, pageWidth, 35, 'F');
        
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(24);
        doc.setFont('helvetica', 'bold');
        doc.text('Retirement Projection', margin, 18);
        
        doc.setFontSize(12);
        doc.setFont('helvetica', 'normal');
        doc.text('Retyrment Report', margin, 28);
        doc.text(new Date().toLocaleDateString('en-IN'), pageWidth - margin, 28, { align: 'right' });

        y = 50;

        const summary = data.summary || {};
        const matrix = data.matrix || [];
        const gap = data.gapAnalysis || {};
        const startingBalances = summary.startingBalances || {};
        const maturingData = data.maturingBeforeRetirement || {};

        // Summary Cards
        doc.setTextColor(...pdfColors.dark);
        doc.setFontSize(14);
        doc.setFont('helvetica', 'bold');
        doc.text('Retirement Summary', margin, y);
        y += 12;

        const summaryItems = [
            { label: 'Current Age', value: summary.currentAge || 35, unit: 'years' },
            { label: 'Retirement Age', value: summary.retirementAge || 60, unit: 'years' },
            { label: 'Years to Retirement', value: (summary.retirementAge || 60) - (summary.currentAge || 35), unit: 'years' },
            { label: 'Projected Corpus', value: pdfFormatCurrency(summary.finalCorpus, true), highlight: true },
            { label: 'Monthly Retirement Income', value: pdfFormatCurrency(summary.selectedMonthlyIncome || summary.monthlyIncomeFromCorpus), unit: '/month' },
            { label: 'Income Strategy', value: summary.selectedStrategyName || 'Sustainable', highlight: false }
        ];

        const cardWidth = (pageWidth - 4 * margin) / 3;
        let cardX = margin;
        let cardRow = 0;

        summaryItems.forEach((item, idx) => {
            if (idx > 0 && idx % 3 === 0) {
                y += 30;
                cardX = margin;
                cardRow++;
            }

            const bgColor = item.highlight ? pdfColors.primary : pdfColors.light;
            const textColor = item.highlight ? pdfColors.white : pdfColors.dark;
            
            doc.setFillColor(...bgColor);
            doc.roundedRect(cardX, y, cardWidth, 25, 3, 3, 'F');
            
            doc.setFontSize(9);
            doc.setTextColor(...(item.highlight ? pdfColors.white : pdfColors.medium));
            doc.setFont('helvetica', 'normal');
            doc.text(item.label, cardX + 5, y + 8);
            
            doc.setFontSize(12);
            doc.setTextColor(...textColor);
            doc.setFont('helvetica', 'bold');
            doc.text(String(item.value) + (item.unit ? ' ' + item.unit : ''), cardX + 5, y + 18);

            cardX += cardWidth + margin/2;
        });

        y += 40;
        
        // Starting Balances Breakdown
        if (startingBalances.totalStarting) {
            if (y > 200) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Current Asset Breakdown', margin, y);
            y += 8;
            
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            
            if (startingBalances.ppf) doc.text(`• PPF: ${pdfFormatCurrency(startingBalances.ppf, true)}`, margin + 5, y), y += 6;
            if (startingBalances.epf) doc.text(`• EPF: ${pdfFormatCurrency(startingBalances.epf, true)}`, margin + 5, y), y += 6;
            if (startingBalances.mutualFunds) doc.text(`• Mutual Funds: ${pdfFormatCurrency(startingBalances.mutualFunds, true)}`, margin + 5, y), y += 6;
            if (startingBalances.nps) doc.text(`• NPS: ${pdfFormatCurrency(startingBalances.nps, true)}`, margin + 5, y), y += 6;
            if (startingBalances.fd) doc.text(`• FD: ${pdfFormatCurrency(startingBalances.fd, true)}`, margin + 5, y), y += 6;
            if (startingBalances.rd) doc.text(`• RD: ${pdfFormatCurrency(startingBalances.rd, true)}`, margin + 5, y), y += 6;
            if (startingBalances.stocks) doc.text(`• Stocks: ${pdfFormatCurrency(startingBalances.stocks, true)}`, margin + 5, y), y += 6;
            if (startingBalances.cash) doc.text(`• Cash: ${pdfFormatCurrency(startingBalances.cash, true)}`, margin + 5, y), y += 6;
            
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...pdfColors.dark);
            doc.text(`Total Starting Corpus: ${pdfFormatCurrency(startingBalances.totalStarting, true)}`, margin + 5, y);
            y += 12;
        }
        
        // Maturing Investments
        if (maturingData.totalMaturingBeforeRetirement > 0) {
            if (y > 220) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Maturing Before Retirement', margin, y);
            y += 8;
            
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            doc.text(`Total Available: ${pdfFormatCurrency(maturingData.totalMaturingBeforeRetirement, true)}`, margin + 5, y);
            y += 8;
            
            if (maturingData.maturingInvestments && maturingData.maturingInvestments.length > 0) {
                doc.setFontSize(8);
                doc.text('Investments:', margin + 5, y);
                y += 6;
                maturingData.maturingInvestments.slice(0, 5).forEach(inv => {
                    if (y > 270) { doc.addPage(); y = 20; }
                    doc.text(`  • ${inv.name}: ${pdfFormatCurrency(inv.expectedMaturityValue, true)} (${inv.yearsToMaturity}y)`, margin + 10, y);
                    y += 6;
                });
            }
            
            if (maturingData.maturingInsurance && maturingData.maturingInsurance.length > 0) {
                if (y > 250) { doc.addPage(); y = 20; }
                doc.setFontSize(8);
                doc.text('Insurance Policies:', margin + 5, y);
                y += 6;
                maturingData.maturingInsurance.slice(0, 5).forEach(ins => {
                    if (y > 270) { doc.addPage(); y = 20; }
                    doc.text(`  • ${ins.name}: ${pdfFormatCurrency(ins.expectedMaturityValue, true)} (${ins.yearsToMaturity}y)`, margin + 10, y);
                    y += 6;
                });
            }
            y += 10;
        }

        // GAP Analysis
        if (gap) {
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('GAP Analysis', margin, y);
            y += 10;

            const gapColor = gap.corpusGap > 0 ? pdfColors.danger : pdfColors.success;
            const gapText = gap.corpusGap > 0 ? 'Shortfall' : 'Surplus';
            
            doc.setFillColor(...gapColor);
            doc.roundedRect(margin, y, pageWidth - 2 * margin, 20, 3, 3, 'F');
            doc.setTextColor(...pdfColors.white);
            doc.setFontSize(11);
            doc.setFont('helvetica', 'normal');
            doc.text(`Required: ${pdfFormatCurrency(gap.requiredCorpus, true)} | Projected: ${pdfFormatCurrency(gap.projectedCorpus, true)}`, margin + 10, y + 8);
            doc.setFont('helvetica', 'bold');
            doc.text(`${gapText}: ${pdfFormatCurrency(Math.abs(gap.corpusGap), true)} (${Math.abs(gap.gapPercent || 0).toFixed(1)}%)`, margin + 10, y + 16);
            
            y += 30;
        }

        // Matrix Table (first 15 rows)
        if (matrix.length > 0) {
            if (y > 180) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.text('Year-by-Year Projection', margin, y);
            y += 10;

            // Table header
            doc.setFillColor(...pdfColors.dark);
            doc.rect(margin, y, pageWidth - 2 * margin, 8, 'F');
            doc.setFontSize(7);
            doc.setTextColor(...pdfColors.white);
            doc.setFont('helvetica', 'bold');
            doc.text('Year', margin + 2, y + 6);
            doc.text('Age', margin + 18, y + 6);
            doc.text('PPF+EPF', margin + 30, y + 6);
            doc.text('MF', margin + 50, y + 6);
            doc.text('Other', margin + 65, y + 6);
            doc.text('Inflow', margin + 80, y + 6);
            doc.text('Outflow', margin + 95, y + 6);
            doc.text('Corpus', margin + 110, y + 6);
            y += 10;

            doc.setFont('helvetica', 'normal');
            
            matrix.slice(0, 25).forEach((row, idx) => {
                if (y > 275) { doc.addPage(); y = 20; }
                
                const bgColor = idx % 2 === 0 ? pdfColors.white : pdfColors.light;
                doc.setFillColor(...bgColor);
                doc.rect(margin, y - 4, pageWidth - 2 * margin, 7, 'F');
                
                doc.setTextColor(...pdfColors.dark);
                doc.setFontSize(7);
                doc.setFont('helvetica', 'normal');
                doc.text(String(row.year), margin + 2, y + 1);
                doc.text(String(row.age), margin + 18, y + 1);
                doc.text(pdfFormatCurrency((row.ppfBalance || 0) + (row.epfBalance || 0), true), margin + 30, y + 1);
                doc.text(pdfFormatCurrency(row.mfBalance, true), margin + 50, y + 1);
                doc.text(pdfFormatCurrency(row.otherLiquidBalance || 0, true), margin + 65, y + 1);
                
                const totalInflow = (row.insuranceMaturity || 0) + (row.investmentMaturity || 0);
                if (totalInflow > 0) {
                    doc.setTextColor(...pdfColors.success);
                    doc.text('+' + pdfFormatCurrency(totalInflow, true), margin + 80, y + 1);
                } else {
                    doc.setTextColor(...pdfColors.medium);
                    doc.text('-', margin + 80, y + 1);
                }
                
                if (row.goalOutflow > 0) {
                    doc.setTextColor(...pdfColors.danger);
                    doc.text('-' + pdfFormatCurrency(row.goalOutflow, true), margin + 95, y + 1);
                } else {
                    doc.setTextColor(...pdfColors.medium);
                    doc.text('-', margin + 95, y + 1);
                }
                
                doc.setTextColor(...pdfColors.primary);
                doc.setFont('helvetica', 'bold');
                doc.text(pdfFormatCurrency(row.netCorpus, true), margin + 110, y + 1);
                doc.setFont('helvetica', 'normal');
                
                y += 7;
            });

            if (matrix.length > 20) {
                doc.setFontSize(8);
                doc.setTextColor(...pdfColors.medium);
                doc.text(`... ${matrix.length - 20} more years until retirement`, margin + 5, y + 3);
            }
        }

        // Footer
        doc.setFontSize(8);
        doc.setTextColor(...pdfColors.medium);
        doc.text('Generated by Retyrment • ' + new Date().toLocaleString('en-IN'), pageWidth / 2, 290, { align: 'center' });

        // Save
        doc.save(`Retyrment_Retirement_${new Date().toISOString().split('T')[0]}.pdf`);
        showToast('Retirement PDF downloaded!');

    } catch (error) {
        console.error('PDF generation error:', error);
        showToast('Error generating PDF: ' + error.message, 'error');
    }
}

// Generate Calendar PDF
async function generateCalendarPDF() {
    showToast('Generating calendar report...');
    
    try {
        const calendarData = await api.calendar.get();
        
        const doc = new jsPDF('landscape');
        const pageWidth = doc.internal.pageSize.getWidth();
        const pageHeight = doc.internal.pageSize.getHeight();
        const margin = 15;
        let y = 20;

        // Header
        doc.setFillColor(59, 130, 246); // Blue
        doc.rect(0, 0, pageWidth, 30, 'F');
        
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(20);
        doc.setFont('helvetica', 'bold');
        doc.text('Investment Calendar ' + new Date().getFullYear(), margin, 18);
        
        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');
        doc.text('Monthly Outflow Schedule', margin, 26);
        doc.text('Retyrment', pageWidth - margin, 18, { align: 'right' });

        y = 40;

        const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
        const entries = calendarData.entries || [];
        const totals = calendarData.monthlyTotals || {};
        
        // Calculate category totals
        const categoryTotals = {};
        entries.forEach(entry => {
            const category = entry.category || 'Other';
            if (!categoryTotals[category]) categoryTotals[category] = 0;
            Object.values(entry.months || {}).forEach(amount => {
                categoryTotals[category] += amount || 0;
            });
        });

        // Table header
        const colWidth = (pageWidth - 2 * margin - 80) / 12;
        
        doc.setFillColor(...pdfColors.dark);
        doc.rect(margin, y, pageWidth - 2 * margin, 10, 'F');
        
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(8);
        doc.setFont('helvetica', 'bold');
        doc.text('Item', margin + 3, y + 7);
        
        months.forEach((month, idx) => {
            doc.text(month, margin + 80 + idx * colWidth + colWidth/2, y + 7, { align: 'center' });
        });
        
        y += 12;

        // Table rows
        doc.setFont('helvetica', 'normal');
        
        entries.slice(0, 15).forEach((entry, rowIdx) => {
            const bgColor = rowIdx % 2 === 0 ? pdfColors.white : pdfColors.light;
            doc.setFillColor(...bgColor);
            doc.rect(margin, y - 2, pageWidth - 2 * margin, 8, 'F');
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(7);
            doc.text((entry.description || '-').substring(0, 20), margin + 3, y + 4);
            
            const entryMonths = entry.months || {};
            months.forEach((month, idx) => {
                const amount = entryMonths[month] || 0;
                if (amount > 0) {
                    doc.text(pdfFormatCurrency(amount, true), margin + 80 + idx * colWidth + colWidth/2, y + 4, { align: 'center' });
                }
            });
            
            y += 8;
        });

        // Totals row
        y += 2;
        doc.setFillColor(...pdfColors.primary);
        doc.rect(margin, y, pageWidth - 2 * margin, 10, 'F');
        doc.setTextColor(...pdfColors.white);
        doc.setFontSize(8);
        doc.setFont('helvetica', 'bold');
        doc.text('TOTAL', margin + 3, y + 7);
        
        months.forEach((month, idx) => {
            const total = totals[month] || 0;
            doc.text(pdfFormatCurrency(total, true), margin + 80 + idx * colWidth + colWidth/2, y + 7, { align: 'center' });
        });

        // Yearly total
        const yearlyTotal = Object.values(totals).reduce((a, b) => a + b, 0);
        
        // Category Breakdown
        y += 15;
        if (Object.keys(categoryTotals).length > 0) {
            if (y > pageHeight - 40) { doc.addPage(); y = 20; }
            
            doc.setTextColor(...pdfColors.dark);
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Category Breakdown', margin, y);
            y += 10;
            
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...pdfColors.medium);
            
            const sortedCategories = Object.entries(categoryTotals)
                .sort((a, b) => b[1] - a[1])
                .slice(0, 10);
            
            sortedCategories.forEach(([category, total]) => {
                if (y > pageHeight - 30) { doc.addPage(); y = 20; }
                const percent = yearlyTotal > 0 ? (total / yearlyTotal * 100).toFixed(1) : 0;
                doc.text(`• ${category}: ${pdfFormatCurrency(total)} (${percent}%)`, margin + 5, y);
                y += 6;
            });
            y += 5;
        }
        if (y > pageHeight - 30) { doc.addPage(); y = 20; }
        doc.setTextColor(...pdfColors.dark);
        doc.setFontSize(12);
        doc.setFont('helvetica', 'bold');
        doc.text(`Yearly Total: ${pdfFormatCurrency(yearlyTotal)}`, margin, y);

        // Footer
        doc.setFontSize(8);
        doc.setTextColor(...pdfColors.medium);
        doc.text('Generated by Retyrment • ' + new Date().toLocaleString('en-IN'), pageWidth / 2, pageHeight - 10, { align: 'center' });

        // Save
        doc.save(`Retyrment_Calendar_${new Date().getFullYear()}.pdf`);
        showToast('Calendar PDF downloaded!');

    } catch (error) {
        console.error('PDF generation error:', error);
        showToast('Error generating PDF: ' + error.message, 'error');
    }
}
