// WealthVision - Calendar Page

const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

document.addEventListener('DOMContentLoaded', () => {
    loadCalendarData();
});

async function loadCalendarData() {
    try {
        const data = await api.calendar.getYear();
        renderCalendar(data);
        document.getElementById('yearly-total').textContent = formatCurrency(data.yearlyGrandTotal);
        createCalendarChart('calendar-chart', data);
    } catch (error) {
        console.error('Error loading calendar:', error);
        document.getElementById('calendar-body').innerHTML = `
            <tr><td colspan="14" class="px-4 py-8 text-center text-danger-400">Error loading data. Is the server running?</td></tr>
        `;
    }
}

function renderCalendar(data) {
    const tbody = document.getElementById('calendar-body');
    const entries = data.entries || [];
    const totals = data.monthlyTotals || {};

    if (entries.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="14" class="px-4 py-8 text-center text-gray-500">
                No outflows yet. Add SIPs, EMIs, or insurance to see them here.
            </td></tr>
        `;
        return;
    }

    let html = entries.map(entry => {
        const monthCells = months.map(m => {
            const val = entry.months?.[m] || 0;
            return `<td class="px-3 py-3 text-right font-mono text-sm ${val > 0 ? 'text-gray-200' : 'text-gray-600'}">
                ${val > 0 ? formatCurrency(val, true) : '-'}
            </td>`;
        }).join('');

        return `
            <tr class="hover:bg-dark-600">
                <td class="px-4 py-3 sticky left-0 bg-dark-700">
                    <div class="font-medium">${entry.description}</div>
                    <div class="text-xs text-gray-500">${entry.category}</div>
                </td>
                ${monthCells}
                <td class="px-4 py-3 text-right font-mono font-medium text-accent-400">
                    ${formatCurrency(entry.yearlyTotal, true)}
                </td>
            </tr>
        `;
    }).join('');

    // Grand total row
    const totalCells = months.map(m => 
        `<td class="px-3 py-3 text-right font-mono text-warning-400">${formatCurrency(totals[m] || 0, true)}</td>`
    ).join('');

    html += `
        <tr class="bg-dark-800 font-bold">
            <td class="px-4 py-3 sticky left-0 bg-dark-800">GRAND TOTAL</td>
            ${totalCells}
            <td class="px-4 py-3 text-right font-mono text-accent-400">${formatCurrency(data.yearlyGrandTotal, true)}</td>
        </tr>
    `;

    tbody.innerHTML = html;
}
