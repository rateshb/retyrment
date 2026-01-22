import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent } from '../components/ui';
import { api } from '../lib/api';
import { formatCurrency, formatDate } from '../lib/utils';
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight, Shield, TrendingUp, CreditCard, Target } from 'lucide-react';

interface CalendarEvent {
  date: string;
  title: string;
  type: 'insurance' | 'investment' | 'loan' | 'goal';
  amount?: number;
  description?: string;
}

export function Calendar() {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [viewMode, setViewMode] = useState<'month' | 'year'>('month');
  
  // Fetch data to generate calendar events
  const { data: insurances = [] } = useQuery({
    queryKey: ['insurance'],
    queryFn: api.insurance.getAll,
  });

  const { data: investments = [] } = useQuery({
    queryKey: ['investments'],
    queryFn: api.investments.getAll,
  });

  const { data: loans = [] } = useQuery({
    queryKey: ['loans'],
    queryFn: api.loans.getAll,
  });

  const { data: goals = [] } = useQuery({
    queryKey: ['goals'],
    queryFn: api.goals.getAll,
  });

  // Generate calendar events
  const currentYear = currentDate.getFullYear();
  const events: CalendarEvent[] = [
    // Insurance premiums (based on renewal month if provided)
    ...insurances.map(ins => {
      const renewalMonth = ins.renewalMonth || (ins.startDate ? new Date(ins.startDate).getMonth() + 1 : undefined);
      const date = renewalMonth
        ? `${currentYear}-${String(renewalMonth).padStart(2, '0')}-01`
        : ins.startDate;
      return {
        date,
        title: `${ins.policyName} Premium Due`,
        type: 'insurance' as const,
        amount: ins.annualPremium,
        description: ins.company,
      };
    }),
    // Investment maturities
    ...investments.filter(inv => inv.maturityDate).map(inv => ({
      date: inv.maturityDate!,
      title: `${inv.name} Maturity`,
      type: 'investment' as const,
      amount: inv.currentValue,
      description: inv.type,
    })),
    // Loan EMIs (monthly based on EMI day)
    ...loans.flatMap(loan => {
      const emiDay = loan.emiDay || (loan.startDate ? new Date(loan.startDate).getDate() : 1);
      const remainingMonths = loan.remainingMonths && loan.remainingMonths > 0 ? loan.remainingMonths : 12;
      const maxMonths = Math.min(12, remainingMonths);
      return Array.from({ length: maxMonths }, (_, idx) => {
        const monthIndex = idx;
        const daysInMonth = new Date(currentYear, monthIndex + 1, 0).getDate();
        const day = Math.min(Math.max(1, emiDay), daysInMonth);
        const date = `${currentYear}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        return {
          date,
          title: `${loan.name} EMI`,
          type: 'loan' as const,
          amount: loan.emi,
          description: loan.type,
        };
      });
    }),
    // Goals target dates
    ...goals.map(goal => ({
      date: `${goal.targetYear}-01-01`,
      title: goal.name,
      type: 'goal' as const,
      amount: goal.targetAmount,
      description: goal.description,
    })),
  ];

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfMonth = new Date(year, month, 1).getDay();
  const monthName = currentDate.toLocaleString('default', { month: 'long' });

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  const getEventsForDay = (day: number) => {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    return events.filter(e => e.date?.startsWith(dateStr));
  };

  const getEventIcon = (type: string) => {
    switch (type) {
      case 'insurance': return <Shield size={12} className="text-success-500" />;
      case 'investment': return <TrendingUp size={12} className="text-primary-500" />;
      case 'loan': return <CreditCard size={12} className="text-danger-500" />;
      case 'goal': return <Target size={12} className="text-warning-500" />;
      default: return null;
    }
  };

  const getEventBg = (type: string) => {
    switch (type) {
      case 'insurance': return 'bg-success-100 text-success-700';
      case 'investment': return 'bg-primary-100 text-primary-700';
      case 'loan': return 'bg-danger-100 text-danger-700';
      case 'goal': return 'bg-warning-100 text-warning-700';
      default: return 'bg-slate-100 text-slate-700';
    }
  };

  const monthlyTotals = useMemo(() => {
    const totals = Array.from({ length: 12 }, () => 0);
    events.forEach(event => {
      if (!event.date) return;
      const d = new Date(event.date);
      if (Number.isNaN(d.getTime()) || d.getFullYear() !== year) return;
      totals[d.getMonth()] += event.amount || 0;
    });
    return totals;
  }, [events, year]);

  // Get upcoming events for the sidebar
  const upcomingEvents = events
    .filter(e => new Date(e.date) >= new Date())
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, 10);

  return (
    <MainLayout
      title="Financial Calendar"
      subtitle="Track important financial dates"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="inline-flex rounded-lg border border-slate-200 bg-white p-1">
          <button
            className={`px-3 py-1.5 text-sm rounded-md ${viewMode === 'month' ? 'bg-primary-50 text-primary-600' : 'text-slate-600'}`}
            onClick={() => setViewMode('month')}
          >
            Month
          </button>
          <button
            className={`px-3 py-1.5 text-sm rounded-md ${viewMode === 'year' ? 'bg-primary-50 text-primary-600' : 'text-slate-600'}`}
            onClick={() => setViewMode('year')}
          >
            Year
          </button>
        </div>
        {viewMode === 'year' && (
          <div className="text-sm text-slate-500">Total outflow by month</div>
        )}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Calendar Grid */}
        <Card className="lg:col-span-3">
          <CardContent>
            {viewMode === 'month' ? (
              <>
                <div className="flex items-center justify-between mb-6">
                  <button onClick={prevMonth} className="p-2 hover:bg-slate-100 rounded-lg">
                    <ChevronLeft size={20} />
                  </button>
                  <h2 className="text-xl font-semibold text-slate-800">
                    {monthName} {year}
                  </h2>
                  <button onClick={nextMonth} className="p-2 hover:bg-slate-100 rounded-lg">
                    <ChevronRight size={20} />
                  </button>
                </div>

                <div className="grid grid-cols-7 gap-1 mb-2">
                  {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
                    <div key={day} className="text-center text-sm font-medium text-slate-500 py-2">
                      {day}
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-7 gap-1">
                  {Array.from({ length: firstDayOfMonth }).map((_, i) => (
                    <div key={`empty-${i}`} className="min-h-24 bg-slate-50 rounded-lg" />
                  ))}
                  {Array.from({ length: daysInMonth }).map((_, i) => {
                    const day = i + 1;
                    const dayEvents = getEventsForDay(day);
                    const isToday = new Date().toDateString() === new Date(year, month, day).toDateString();

                    return (
                      <div
                        key={day}
                        className={`min-h-24 p-2 rounded-lg border ${
                          isToday ? 'border-primary-500 bg-primary-50' : 'border-slate-200 hover:bg-slate-50'
                        }`}
                      >
                        <div className={`text-sm font-medium mb-1 ${isToday ? 'text-primary-600' : 'text-slate-700'}`}>
                          {day}
                        </div>
                        <div className="space-y-1">
                          {dayEvents.slice(0, 2).map((event, idx) => (
                            <div
                              key={idx}
                              className={`text-xs px-1.5 py-0.5 rounded truncate flex items-center gap-1 ${getEventBg(event.type)}`}
                              title={`${event.title} - ${event.amount ? formatCurrency(event.amount) : ''}`}
                            >
                              {getEventIcon(event.type)}
                              <span className="truncate">{event.title}</span>
                            </div>
                          ))}
                          {dayEvents.length > 2 && (
                            <div className="text-xs text-slate-500 pl-1">
                              +{dayEvents.length - 2} more
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            ) : (
              <>
                <div className="flex items-center justify-between mb-6">
                  <button onClick={() => setCurrentDate(new Date(year - 1, 0, 1))} className="p-2 hover:bg-slate-100 rounded-lg">
                    <ChevronLeft size={20} />
                  </button>
                  <h2 className="text-xl font-semibold text-slate-800">
                    {year} Outflows
                  </h2>
                  <button onClick={() => setCurrentDate(new Date(year + 1, 0, 1))} className="p-2 hover:bg-slate-100 rounded-lg">
                    <ChevronRight size={20} />
                  </button>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {Array.from({ length: 12 }).map((_, m) => (
                    <div key={m} className="p-3 border border-slate-200 rounded-lg bg-slate-50">
                      <div className="text-sm text-slate-500">
                        {new Date(year, m, 1).toLocaleString('default', { month: 'short' })}
                      </div>
                      <div className="text-lg font-semibold text-slate-800">
                        {formatCurrency(monthlyTotals[m] || 0, true)}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Upcoming Events Sidebar */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <CalendarIcon size={20} /> Upcoming
            </h3>
            <div className="space-y-3">
              {upcomingEvents.length === 0 ? (
                <p className="text-slate-400 text-center py-4">No upcoming events</p>
              ) : (
                upcomingEvents.map((event, idx) => (
                  <div key={idx} className={`p-3 rounded-lg ${getEventBg(event.type)}`}>
                    <div className="flex items-start gap-2">
                      {getEventIcon(event.type)}
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{event.title}</p>
                        <p className="text-xs opacity-75">{formatDate(event.date)}</p>
                        {event.amount && (
                          <p className="text-xs font-medium mt-1">{formatCurrency(event.amount)}</p>
                        )}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Legend */}
            <div className="mt-6 pt-4 border-t border-slate-200">
              <h4 className="text-sm font-medium text-slate-700 mb-2">Legend</h4>
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-xs">
                  <div className="w-3 h-3 rounded bg-success-500"></div>
                  <span className="text-slate-600">Insurance</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <div className="w-3 h-3 rounded bg-primary-500"></div>
                  <span className="text-slate-600">Investment</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <div className="w-3 h-3 rounded bg-danger-500"></div>
                  <span className="text-slate-600">Loan</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <div className="w-3 h-3 rounded bg-warning-500"></div>
                  <span className="text-slate-600">Goal</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  );
}

export default Calendar;
