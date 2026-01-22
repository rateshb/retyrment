import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { MainLayout } from '../components/Layout';
import { Card, CardContent } from '../components/ui';
import { api } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { 
  Wallet, TrendingUp, Building2, PiggyBank, AlertTriangle, CheckCircle,
  Shield, Heart, Umbrella, Target, ArrowRight, Info
} from 'lucide-react';
import { formatCurrency } from '../lib/utils';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from 'recharts';

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#3b82f6', '#14b8a6', '#f97316'];

export function Dashboard() {
  const { user } = useAuthStore();
  const [hiddenAssets, setHiddenAssets] = useState<Set<string>>(new Set());
  
  const { data: networth, isLoading: networthLoading } = useQuery({
    queryKey: ['networth'],
    queryFn: api.analysis.networth,
  });

  const { data: recommendations } = useQuery({
    queryKey: ['recommendations'],
    queryFn: api.analysis.recommendations,
  });

  const { data: goalAnalysis } = useQuery({
    queryKey: ['goalAnalysis'],
    queryFn: api.analysis.goals,
  });

  const { data: investments = [] } = useQuery({
    queryKey: ['investments'],
    queryFn: api.investments.getAll,
  });

  const { data: insurances = [] } = useQuery({
    queryKey: ['insurance'],
    queryFn: api.insurance.getAll,
  });

  const { data: loans = [] } = useQuery({
    queryKey: ['loans'],
    queryFn: api.loans.getAll,
  });

  const { data: retirementData } = useQuery({
    queryKey: ['retirement'],
    queryFn: () => api.retirement.calculate({ currentAge: 35, retirementAge: 60, lifeExpectancy: 85 }),
  });

  const { data: maturingData } = useQuery({
    queryKey: ['maturingBeforeRetirement'],
    queryFn: () => api.retirement.getMaturing(35, 60),
  });

  const { data: savedStrategy } = useQuery({
    queryKey: ['retirementStrategy'],
    queryFn: api.retirement.getStrategy,
    retry: false,
  });

  const { data: insuranceRecs } = useQuery({
    queryKey: ['insuranceRecs'],
    queryFn: api.insuranceRecommendations.getOverall,
  });

  // Calculate critical areas
  const gapAnalysis = retirementData?.gapAnalysis || {};
  const monthlyExpenses = gapAnalysis.totalCurrentMonthlyExpenses || gapAnalysis.currentMonthlyExpenses || 0;
  
  // Emergency fund calculation
  const cashBalance = networth?.assetBreakdown?.CASH || 0;
  const emergencyFDs = investments.filter((inv: any) => (inv.type === 'FD' || inv.type === 'RD') && inv.isEmergencyFund);
  const emergencyFundValue = emergencyFDs.reduce((sum: number, inv: any) => sum + (inv.currentValue || inv.investedAmount || 0), 0);
  const emergencyFundTarget = monthlyExpenses > 0 ? monthlyExpenses * 6 : 0;
  const totalEmergencyFund = cashBalance + emergencyFundValue;
  const emergencyGap = Math.max(0, emergencyFundTarget - totalEmergencyFund);
  const emergencyFundMet = monthlyExpenses > 0 && totalEmergencyFund >= emergencyFundTarget;

  // Health coverage
  const healthRec = insuranceRecs?.healthRecommendation;
  const healthMet = healthRec ? (healthRec.gap <= 0 && healthRec.totalRecommendedCover > 0) : null;
  const hasHealthPolicy = insurances.some((policy: any) => policy.type === 'HEALTH');
  const hasNonGroupHealth = insurances.some((policy: any) => policy.type === 'HEALTH' && policy.healthType !== 'GROUP');
  const isGroupOnlyHealth = hasHealthPolicy && !hasNonGroupHealth;

  // Accidental cover check
  const accidentKeywords = ['accident', 'accidental', 'personal accident'];
  const accidentalCoverMet = insurances.some((policy: any) => {
    const name = (policy.policyName || policy.company || '').toLowerCase();
    return policy.type === 'OTHER' || accidentKeywords.some(k => name.includes(k));
  });

  // Retirement calculations
  const summary = retirementData?.summary || {};
  const projectedCorpus = summary.finalCorpus || 0;
  const requiredCorpus = gapAnalysis.requiredCorpus || 0;
  const corpusGap = gapAnalysis.corpusGap || 0;
  const readinessPercent = requiredCorpus > 0 ? Math.round((projectedCorpus / requiredCorpus) * 100) : 0;

  // Prepare chart data
  const assetBreakdownData = networth?.assetBreakdown 
    ? Object.entries(networth.assetBreakdown).map(([key, value]) => ({
        name: key.replace(/_/g, ' '),
        value: value as number,
      }))
    : [];
  const colorByAsset = useMemo(() => {
    const mapping = new Map<string, string>();
    assetBreakdownData.forEach((asset, index) => {
      mapping.set(asset.name, COLORS[index % COLORS.length]);
    });
    return mapping;
  }, [assetBreakdownData]);

  const visibleAssetBreakdown = useMemo(
    () => assetBreakdownData.filter(asset => !hiddenAssets.has(asset.name)),
    [assetBreakdownData, hiddenAssets]
  );

  // Check retirement matrix for actual goal fundability
  const retirementMatrix = retirementData?.matrix || [];
  const goalsData = (goalAnalysis?.goals || []).slice(0, 5).map((g: any) => {
    // Find if there's a negative corpus when this goal is due
    const goalYear = g.targetYear;
    const matrixRow = retirementMatrix.find((row: any) => row.year === goalYear);
    const hasShortfall = matrixRow && matrixRow.netCorpus < 0 && matrixRow.goalOutflow > 0;
    
    // If matrix shows shortfall, cap progress at actual fundable amount
    let actualPercent = Math.min(g.fundingPercent || 0, 100);
    if (hasShortfall) {
      // Calculate what percent can actually be funded
      const goalAmount = matrixRow.goalOutflow || g.inflatedAmount || g.targetAmount;
      const shortfall = Math.abs(matrixRow.netCorpus);
      const fundable = Math.max(0, goalAmount - shortfall);
      actualPercent = goalAmount > 0 ? Math.min(100, (fundable / goalAmount) * 100) : 0;
    }
    
    return {
      name: g.name?.substring(0, 15) || 'Goal',
      percent: Math.round(actualPercent),
      hasShortfall,
      shortfallAmount: hasShortfall ? Math.abs(matrixRow?.netCorpus || 0) : 0,
    };
  });

  const totalAssetsValue = assetBreakdownData.reduce((sum, item) => sum + (item.value || 0), 0);
  const visibleAssetsValue = visibleAssetBreakdown.reduce((sum, item) => sum + (item.value || 0), 0);
  const toggleAssetVisibility = (assetName: string) => {
    setHiddenAssets(prev => {
      const next = new Set(prev);
      if (next.has(assetName)) {
        next.delete(assetName);
      } else {
        next.add(assetName);
      }
      return next;
    });
  };

  // Generate alerts (match VanillaJS logic)
  const alerts: Array<{ type: string; icon: string; title: string; description: string; link: string; action: string }> = [];

  const gapAnalysisMonthlyIncome = gapAnalysis.monthlyIncome || 0;
  const monthlySavings = gapAnalysisMonthlyIncome - monthlyExpenses;

  let strategy = savedStrategy;
  if (!strategy) {
    try {
      const local = localStorage.getItem('userStrategy');
      if (local) {
        strategy = JSON.parse(local);
      }
    } catch {
      strategy = null;
    }
  }

  if (strategy && Object.keys(strategy || {}).length > 0) {
    const enabledStrategies: string[] = [];
    if (strategy.sellIlliquidAssets) enabledStrategies.push(`Sell Illiquid (${strategy.sellIlliquidAssetsYear || 'TBD'})`);
    if (strategy.reinvestMaturities) enabledStrategies.push('Reinvest Maturities');
    if (strategy.redirectLoanEMIs) enabledStrategies.push(`Redirect EMI (from ${strategy.loanEndYear || 'TBD'})`);
    if (strategy.increaseSIP) enabledStrategies.push('Increase SIP 20%');

    if (enabledStrategies.length > 0) {
      alerts.push({
        type: 'info',
        icon: '📋',
        title: 'Your Active Strategy',
        description: enabledStrategies.join(' • '),
        link: '/retirement',
        action: 'Update Strategy',
      });
    }
  }

  if (maturingData?.totalMaturingBeforeRetirement > 0) {
    const totalMaturing = maturingData.totalMaturingBeforeRetirement;
    const count = (maturingData.investmentCount || 0) + (maturingData.insuranceCount || 0);
    alerts.push({
      type: 'success',
      icon: '💰',
      title: `${formatCurrency(totalMaturing, true)} Available for Reinvestment`,
      description: `${count} investments/policies maturing before retirement. Consider reinvesting in higher-return assets.`,
      link: '/retirement',
      action: 'View Details',
    });
  }

  if (corpusGap > 0) {
    const illiquidValue = (networth?.assetBreakdown?.GOLD || 0) +
      (networth?.assetBreakdown?.REAL_ESTATE || 0);
    if (illiquidValue > 0) {
      const wouldMeetCorpus = (projectedCorpus + illiquidValue) >= requiredCorpus;
      if (wouldMeetCorpus) {
        alerts.push({
          type: 'warning',
          icon: '🏠',
          title: 'Illiquid Assets Can Cover Gap',
          description: `Selling Gold/Real Estate (${formatCurrency(illiquidValue, true)}) would help meet your corpus requirement.`,
          link: '/retirement',
          action: 'Plan Strategy',
        });
      }
    }

    const totalInflows = maturingData?.totalMaturingBeforeRetirement || 0;
    if (totalInflows > 0 && (projectedCorpus + totalInflows) >= requiredCorpus) {
      alerts.push({
        type: 'tip',
        icon: '📊',
        title: 'Reinvesting Maturities Helps',
        description: `If you reinvest ${formatCurrency(totalInflows, true)} from maturities, you can meet your corpus target.`,
        link: '/retirement',
        action: 'View Plan',
      });
    } else {
      alerts.push({
        type: 'danger',
        icon: '⚠️',
        title: `Corpus Shortfall: ${formatCurrency(corpusGap, true)}`,
        description: 'Increase SIP or consider additional investments to meet retirement goals.',
        link: '/retirement',
        action: 'Adjust Plan',
      });
    }
  }

  const activeLoans = loans.filter((l: any) => l.outstandingAmount > 0);
  if (activeLoans.length > 0) {
    const soonestLoan = activeLoans.filter((l: any) => l.endDate).sort((a: any, b: any) =>
      new Date(a.endDate as string).getTime() - new Date(b.endDate as string).getTime()
    )[0];
    if (soonestLoan && soonestLoan.endDate) {
      const yearsToEnd = new Date(soonestLoan.endDate as string).getFullYear() - new Date().getFullYear();
      if (yearsToEnd > 0 && yearsToEnd <= 10) {
        alerts.push({
          type: 'tip',
          icon: '🎯',
          title: `${formatCurrency(soonestLoan.emi || 0, true)}/mo Freed in ${yearsToEnd}y`,
          description: `After ${soonestLoan.name || 'loan'} ends, redirect EMI to investments for faster corpus growth.`,
          link: '/loans',
          action: 'View Loans',
        });
      }
    }
  }

  if (monthlySavings > 0 && requiredCorpus > 0) {
    const emergencyGap = Math.max(0, emergencyFundTarget - totalEmergencyFund);
    if (emergencyGap > 0) {
      alerts.push({
        type: 'warning',
        icon: '🆘',
        title: 'Emergency Fund Gap',
        description: `Need ${formatCurrency(emergencyGap, true)} more. Current: ${formatCurrency(totalEmergencyFund, true)} (Cash + Tagged FD/RD)`,
        link: '/investments',
        action: 'Tag FDs',
      });
    }

    const today = new Date();
    const sixMonthsFromNow = new Date();
    sixMonthsFromNow.setMonth(today.getMonth() + 6);

    const maturingSoonEmergencyFunds = investments.filter((inv: any) => {
      if ((inv.type !== 'FD' && inv.type !== 'RD') || !inv.isEmergencyFund) return false;
      if (!inv.maturityDate) return false;
      const maturityDate = new Date(inv.maturityDate);
      return maturityDate >= today && maturityDate <= sixMonthsFromNow;
    });

    if (maturingSoonEmergencyFunds.length > 0) {
      const totalMaturing = maturingSoonEmergencyFunds.reduce((sum: number, inv: any) =>
        sum + (inv.currentValue || inv.investedAmount || 0), 0);
      alerts.push({
        type: 'warning',
        icon: '⏰',
        title: 'Emergency Fund Maturing Soon',
        description: `${maturingSoonEmergencyFunds.length} emergency ${maturingSoonEmergencyFunds.length === 1 ? 'fund' : 'funds'} (${formatCurrency(totalMaturing, true)}) maturing in 6 months. Plan to reinvest to maintain emergency coverage.`,
        link: '/investments',
        action: 'Review',
      });
    }
  }

  const atRiskGoals = (goalAnalysis?.goals || []).filter((g: any) => g.status === 'UNFUNDED');
  if (atRiskGoals.length > 0) {
    alerts.push({
      type: 'danger',
      icon: '🎯',
      title: `${atRiskGoals.length} Goal(s) Underfunded`,
      description: atRiskGoals.slice(0, 2).map((g: any) => g.name).join(', ') + (atRiskGoals.length > 2 ? '...' : ''),
      link: '/goals',
      action: 'Review Goals',
    });
  }

  const criticalAreas = [
    {
      label: 'Health Cover',
      status: isGroupOnlyHealth ? false : healthMet,
      detail: isGroupOnlyHealth
        ? 'Group cover only (ends at retirement). Add personal/family policy.'
        : healthMet === null ? 'Add family/insurance data' : (healthMet ? 'Adequate coverage' : 'Coverage gap exists'),
      link: '/insurance-recommendations'
    },
    {
      label: 'Emergency Fund',
      status: monthlyExpenses > 0 ? emergencyFundMet : null,
      detail: monthlyExpenses > 0 
        ? `${formatCurrency(totalEmergencyFund)} / ${formatCurrency(emergencyFundTarget)}`
        : 'Add expenses to calculate',
      link: '/investments'
    },
    {
      label: 'Accidental Cover',
      status: accidentalCoverMet,
      detail: accidentalCoverMet ? 'Covered' : 'Not found',
      link: '/insurance'
    }
  ];

  const metCount = criticalAreas.filter(item => item.status === true).length;

  const monthlySurplus = gapAnalysis.netMonthlySavings
    ?? gapAnalysis.availableMonthlySavings
    ?? recommendations?.monthlySavings
    ?? (gapAnalysis.monthlyIncome || 0) - monthlyExpenses;

  const stats = [
    {
      label: 'Total Assets',
      value: networth?.totalAssets || 0,
      icon: <Wallet className="text-primary-500" size={24} />,
      color: 'bg-primary-50 border-primary-200',
    },
    {
      label: 'Monthly Surplus',
      value: monthlySurplus || 0,
      icon: <TrendingUp className="text-success-500" size={24} />,
      color: 'bg-success-50 border-success-200',
    },
    {
      label: 'Liabilities',
      value: networth?.totalLiabilities || 0,
      icon: <Building2 className="text-danger-500" size={24} />,
      color: 'bg-danger-50 border-danger-200',
    },
    {
      label: 'Net Worth',
      value: networth?.netWorth || 0,
      icon: <PiggyBank className="text-primary-600" size={24} />,
      color: 'bg-gradient-to-r from-primary-50 to-primary-100 border-primary-300',
      highlight: true,
    },
  ];

  return (
    <MainLayout 
      title={`Welcome back, ${user?.name?.split(' ')[0] || 'User'}!`}
      subtitle="Here's your financial overview"
    >
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {stats.map((stat, index) => (
          <Card 
            key={index} 
            className={`${stat.color} ${stat.highlight ? 'ring-2 ring-primary-500/20' : ''}`}
          >
            <CardContent className="flex items-center gap-4">
              <div className="p-3 rounded-xl bg-white/80 shadow-sm">
                {stat.icon}
              </div>
              <div>
                <p className="text-sm text-slate-500">{stat.label}</p>
                <p className={`text-2xl font-bold ${stat.highlight ? 'text-primary-700' : 'text-slate-800'}`}>
                  {networthLoading ? '...' : formatCurrency(stat.value, true)}
                </p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Retirement Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className={`border-l-4 ${corpusGap <= 0 ? 'border-l-success-500' : 'border-l-warning-500'}`}>
          <CardContent>
            <div className="flex items-center gap-4">
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl ${corpusGap <= 0 ? 'bg-success-100' : 'bg-warning-100'}`}>
                💰
              </div>
              <div>
                <p className="text-sm text-slate-500">Projected Corpus</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(projectedCorpus, true)}</p>
                <p className="text-xs text-slate-400">At age {summary.retirementAge || 60}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className={`border-l-4 ${corpusGap <= 0 ? 'border-l-success-500' : 'border-l-danger-500'}`}>
          <CardContent>
            <div className="flex items-center gap-4">
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl ${corpusGap <= 0 ? 'bg-success-100' : 'bg-danger-100'}`}>
                🎯
              </div>
              <div>
                <p className="text-sm text-slate-500">Required Corpus</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(requiredCorpus, true)}</p>
                <p className={`text-xs ${corpusGap > 0 ? 'text-danger-600' : 'text-success-600'}`}>
                  {corpusGap > 0 ? `Gap: -${formatCurrency(corpusGap)}` : `Surplus: +${formatCurrency(Math.abs(corpusGap))}`}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className={`border-l-4 ${readinessPercent >= 100 ? 'border-l-success-500' : readinessPercent >= 75 ? 'border-l-warning-500' : 'border-l-danger-500'}`}>
          <CardContent>
            <div className="flex items-center gap-4">
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl ${
                readinessPercent >= 100 ? 'bg-success-100' : readinessPercent >= 75 ? 'bg-warning-100' : 'bg-danger-100'
              }`}>
                {readinessPercent >= 100 ? '✅' : readinessPercent >= 75 ? '⚠️' : '❗'}
              </div>
              <div>
                <p className="text-sm text-slate-500">Retirement Readiness</p>
                <p className={`text-xl font-bold ${
                  readinessPercent >= 100 ? 'text-success-600' : readinessPercent >= 75 ? 'text-warning-600' : 'text-danger-600'
                }`}>{readinessPercent}%</p>
                <p className={`text-xs ${
                  readinessPercent >= 100 ? 'text-success-600' : readinessPercent >= 75 ? 'text-warning-600' : 'text-danger-600'
                }`}>
                  {readinessPercent >= 100 ? "You're on track! 🎉" : readinessPercent >= 75 ? 'Almost there' : 'Need to increase investments'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        {/* Critical Areas Check */}
        <Card>
          <CardContent>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-slate-800">Critical Areas Check</h3>
              <span className="text-sm text-slate-500 bg-slate-100 px-2 py-1 rounded">{metCount}/{criticalAreas.length} met</span>
            </div>
            <div className="space-y-3">
              {criticalAreas.map((item, i) => (
                <div key={i} className={`p-3 rounded-lg border ${
                  item.status === null ? 'border-slate-200 bg-slate-50' : 
                  item.status ? 'border-success-200 bg-success-50' : 'border-danger-200 bg-danger-50'
                }`}>
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-slate-600">{item.label}</span>
                    <span className={`text-sm font-medium ${
                      item.status === null ? 'text-slate-500' : item.status ? 'text-success-600' : 'text-danger-600'
                    }`}>
                      {item.status === null ? 'ℹ️ Unknown' : item.status ? '✅ Met' : '⚠️ Missing'}
                    </span>
                  </div>
                  <div className="text-sm text-slate-700">{item.detail}</div>
                  <Link to={item.link} className="text-xs text-primary-600 hover:text-primary-700 inline-block mt-2">
                    View →
                  </Link>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Emergency Fund Widget */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Emergency Fund</h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-2xl font-bold text-slate-800">{formatCurrency(totalEmergencyFund)}</p>
                  <p className="text-sm text-slate-500">Current Emergency Fund</p>
                </div>
                <div className="text-right">
                  <p className={`font-medium ${
                    monthlyExpenses === 0 ? 'text-slate-500' :
                    emergencyFundMet ? 'text-success-600' : 
                    totalEmergencyFund >= emergencyFundTarget * 0.5 ? 'text-warning-600' : 'text-danger-600'
                  }`}>
                    {monthlyExpenses === 0 ? 'ℹ️ Add expenses' :
                     emergencyFundMet ? '✅ Adequate' : 
                     totalEmergencyFund >= emergencyFundTarget * 0.5 ? '⚠️ Partial' : '🚨 Critical'}
                  </p>
                  <p className="text-sm text-slate-500">Target: {formatCurrency(emergencyFundTarget)}</p>
                </div>
              </div>

              {monthlyExpenses > 0 && (
                <div>
                  <div className="flex justify-between text-xs text-slate-600 mb-1">
                    <span>Progress</span>
                    <span>{Math.min(100, Math.round((totalEmergencyFund / emergencyFundTarget) * 100))}%</span>
                  </div>
                  <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                    <div 
                      className={`h-full transition-all duration-500 ${
                        emergencyFundMet ? 'bg-success-500' : 
                        totalEmergencyFund >= emergencyFundTarget * 0.5 ? 'bg-warning-500' : 'bg-danger-500'
                      }`}
                      style={{ width: `${Math.min(100, (totalEmergencyFund / emergencyFundTarget) * 100)}%` }}
                    />
                  </div>
                  {emergencyGap > 0 && <p className="text-xs text-slate-500 mt-1">Gap: {formatCurrency(emergencyGap)}</p>}
                </div>
              )}

              <div className="grid grid-cols-3 gap-2 pt-3 border-t border-slate-200">
                <div className="text-center">
                  <p className="text-xs text-slate-500">Cash</p>
                  <p className="font-semibold text-slate-700">{formatCurrency(cashBalance)}</p>
                </div>
                <div className="text-center">
                  <p className="text-xs text-slate-500">Tagged FDs</p>
                  <p className="font-semibold text-amber-600">{formatCurrency(emergencyFundValue)}</p>
                </div>
                <div className="text-center">
                  <p className="text-xs text-slate-500">Count</p>
                  <p className="font-semibold text-slate-700">{emergencyFDs.length}</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* High Priority Alerts */}
        <Card>
          <CardContent>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-slate-800">Priority Alerts</h3>
              <span className="text-sm text-slate-500">({alerts.length})</span>
            </div>
            {alerts.length > 0 ? (
              <div className="space-y-3 max-h-[300px] overflow-y-auto">
                {alerts.map((alert, i) => (
                  <div key={i} className={`flex items-start gap-3 p-3 rounded-lg border ${
                    alert.type === 'danger' ? 'bg-danger-50 border-danger-200' :
                    alert.type === 'warning' ? 'bg-amber-50 border-amber-200' :
                    alert.type === 'success' ? 'bg-emerald-50 border-emerald-200' :
                    'bg-blue-50 border-blue-200'
                  }`}>
                    <span className="text-xl flex-shrink-0">{alert.icon}</span>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-slate-800 text-sm">{alert.title}</p>
                      <p className="text-xs text-slate-600 mt-0.5">{alert.description}</p>
                    </div>
                    <Link to={alert.link} className="text-xs text-primary-600 hover:text-primary-700 whitespace-nowrap">
                      {alert.action} →
                    </Link>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-slate-400">
                <div className="text-4xl mb-2">✅</div>
                <p className="font-medium">All Good!</p>
                <p className="text-sm">No high priority alerts</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Asset Breakdown Chart */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Asset Breakdown</h3>
            {assetBreakdownData.length > 0 ? (
              <>
                <div className="h-[250px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={visibleAssetBreakdown}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={100}
                        paddingAngle={2}
                        dataKey="value"
                        labelLine={false}
                      >
                        {visibleAssetBreakdown.map((asset) => (
                          <Cell key={asset.name} fill={colorByAsset.get(asset.name) || COLORS[0]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => formatCurrency(value as number)} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                  {assetBreakdownData.map((asset) => {
                    const percent = visibleAssetsValue > 0 ? (asset.value / visibleAssetsValue) * 100 : 0;
                    const isHidden = hiddenAssets.has(asset.name);
                    return (
                      <button
                        key={asset.name}
                        type="button"
                        onClick={() => toggleAssetVisibility(asset.name)}
                        className="flex items-center justify-between gap-3 text-left hover:bg-slate-50 rounded-md px-1 py-1"
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <span
                            className="inline-block w-3 h-3 rounded-sm flex-shrink-0"
                            style={{ backgroundColor: colorByAsset.get(asset.name) || COLORS[0], opacity: isHidden ? 0.4 : 1 }}
                          />
                          <span className={`truncate ${isHidden ? 'text-slate-400 line-through' : 'text-slate-600'}`}>
                            {asset.name}
                          </span>
                        </div>
                        <div className="flex items-center gap-2 text-slate-700">
                          <span className={`text-xs ${isHidden ? 'text-slate-400' : ''}`}>
                            {percent.toFixed(1)}%
                          </span>
                          <span className={`font-medium ${isHidden ? 'text-slate-400' : ''}`}>
                            {formatCurrency(asset.value, true)}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
                {hiddenAssets.size > 0 && (
                  <div className="mt-2 text-xs text-slate-500">
                    Hidden assets are excluded from the chart. Click again to include.
                  </div>
                )}
              </>
            ) : (
              <p className="text-slate-400 text-center py-8">No assets yet. Add investments to see breakdown.</p>
            )}
          </CardContent>
        </Card>

        {/* Goal Progress Chart */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Goal Progress</h3>
            {goalsData.some((g: any) => g.hasShortfall) && (
              <div className="mb-4 p-3 bg-danger-50 border border-danger-200 rounded-lg flex items-start gap-2">
                <span className="text-danger-500">⚠️</span>
                <div className="text-sm text-danger-700">
                  <strong>Goal Shortfall Detected:</strong> Some goals cannot be fully funded from projected corpus. 
                  Check Year-by-Year matrix in Retirement page for details.
                </div>
              </div>
            )}
            {goalsData.length > 0 ? (
              <div className="h-[250px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={goalsData} layout="vertical" margin={{ left: 20 }}>
                    <XAxis type="number" domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                    <YAxis type="category" dataKey="name" width={100} />
                    <Tooltip 
                      formatter={(value: number, name: string, props: any) => {
                        const entry = props.payload;
                        if (entry.hasShortfall) {
                          return [`${value}% (Shortfall: ${formatCurrency(entry.shortfallAmount)})`, 'Funded'];
                        }
                        return [`${value}%`, 'Funded'];
                      }}
                    />
                    <Bar 
                      dataKey="percent" 
                      radius={[0, 4, 4, 0]}
                      fill="#6366f1"
                    >
                      {goalsData.map((entry: any, index: number) => (
                        <Cell 
                          key={`cell-${index}`} 
                          fill={entry.hasShortfall ? '#ef4444' : entry.percent >= 100 ? '#10b981' : entry.percent >= 50 ? '#f59e0b' : '#ef4444'} 
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <p className="text-slate-400 text-center py-8">No goals yet. Add goals to track progress.</p>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Recommendations */}
      <Card className="mt-6">
        <CardContent>
          <h3 className="text-lg font-semibold text-slate-800 mb-4">Recommendations</h3>
          <div className="space-y-3">
            {recommendations?.recommendations?.slice(0, 5).map((rec: any, index: number) => (
              <div 
                key={index} 
                className={`flex items-start gap-3 p-3 rounded-lg border ${
                  rec.type === 'danger' ? 'bg-danger-50 border-danger-200' :
                  rec.type === 'warning' ? 'bg-warning-50 border-warning-200' :
                  'bg-success-50 border-success-200'
                }`}
              >
                <span className="text-xl">{rec.icon}</span>
                <div>
                  <p className="font-medium text-slate-800">{rec.title}</p>
                  <p className="text-sm text-slate-600">{rec.description}</p>
                </div>
              </div>
            ))}
            {!recommendations?.recommendations?.length && (
              <div className="flex items-center gap-3 p-3 rounded-lg bg-success-50 border border-success-200">
                <CheckCircle className="text-success-500" />
                <p className="text-slate-700">You're on track! No urgent recommendations.</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </MainLayout>
  );
}
