import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, Input, Select, Modal, toast } from '../components/ui';
import { 
  analysisApi, 
  retirementApi, 
  investmentsApi, 
  expensesApi, 
  loansApi, 
  goalsApi, 
  incomeApi, 
  insuranceApi, 
  familyApi, 
  userDataApi, 
  settingsApi,
  Investment, 
  Loan, 
  Goal, 
  Expense, 
  Income, 
  Insurance, 
  FamilyMember 
} from '../lib/api';
import { formatCurrency, formatDate, amountInWordsHelper } from '../lib/utils';
import { calculateTotalRentalIncome, generateRealEstateRecommendations } from '../lib/realEstateUtils';
import { trackEvent } from '../lib/analytics';
import { useAuthStore } from '../stores/authStore';
import { 
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer, BarChart, Bar, LineChart, Line
} from 'recharts';
import { 
  Calculator, 
  TrendingUp, 
  Target, 
  Calendar, 
  PiggyBank, 
  Shield, 
  Home, 
  AlertTriangle, 
  ChevronRight, 
  Info, 
  Download, 
  RefreshCw, 
  Lightbulb,
  Building,
  DollarSign,
  Settings,
  ChevronDown,
  CheckCircle
} from 'lucide-react';

const CURRENT_YEAR = new Date().getFullYear();

const INCOME_STRATEGIES = [
  { value: 'SUSTAINABLE', label: 'Sustainable', description: 'Grows at ~10% with 8% withdrawals' },
  { value: 'SAFE_4_PERCENT', label: '4% Rule', description: 'Withdraw 4% of initial corpus yearly, inflation-adjusted' },
  { value: 'SIMPLE_DEPLETION', label: 'Depletion', description: 'Corpus depletes' },
];

const defaultParams = {
  currentAge: 35,
  retirementAge: 60,
  lifeExpectancy: 85,
  inflation: 6,
  mfReturn: 12,
  epfReturn: 8.15,
  ppfReturn: 7.1,
  sipStepup: 10,
  effectiveFromYear: 1,  // Relative year: 0 = current, 1 = next year, 2 = year after next, etc.
  incomeStrategy: 'SUSTAINABLE',
  corpusReturnRate: 10,
  enableRateReduction: true,
  rateReductionPercent: 0.5,
  rateReductionYears: 5,
};

// Primary navigation tabs
const PRIMARY_TABS = [
  { 
    id: 'overview', 
    label: 'Overview', 
    icon: '📊',
    description: 'Summary & Key Metrics'
  },
  { 
    id: 'projections', 
    label: 'Projections', 
    icon: '📈',
    description: 'Detailed Analysis'
  },
  { 
    id: 'strategy', 
    label: 'Strategy', 
    icon: '🎯',
    description: 'Planning & Optimization'
  },
  { 
    id: 'events', 
    label: 'Events', 
    icon: '📅',
    description: 'Timeline & Milestones'
  },
];

// Secondary navigation (sub-tabs) for each primary tab
const SECONDARY_TABS: Record<string, Array<{ id: string; label: string; feature?: string; alwaysShow?: boolean }>> = {
  projections: [
    { id: 'matrix', label: 'Year-by-Year', alwaysShow: true },
    { id: 'income', label: 'Income Strategy', alwaysShow: true },
    { id: 'gap', label: 'GAP Analysis', alwaysShow: true },
  ],
  strategy: [
    { id: 'strategy-planner', label: 'Strategy Planner', feature: 'retirementStrategyPlannerTab' },
    { id: 'withdrawal', label: 'Withdrawal Strategy', feature: 'retirementWithdrawalStrategyTab' },
  ],
  events: [
    { id: 'maturing', label: 'Maturing Before Retirement', alwaysShow: true },
    { id: 'expense-projection', label: 'Expense Projection', alwaysShow: true },
    { id: 'ending-expenses', label: 'Ending Expenses', alwaysShow: true },
  ],
};

export function Retirement() {
  const queryClient = useQueryClient();
  const { features } = useAuthStore();
  const [primaryTab, setPrimaryTab] = useState('overview');
  const [secondaryTab, setSecondaryTab] = useState<Record<string, string>>({
    projections: 'matrix',
    strategy: 'strategy-planner',
    events: 'maturing',
  });
  const [params, setParams] = useState(defaultParams);
  const [showSettings, setShowSettings] = useState(false);
  const [matrixExpanded, setMatrixExpanded] = useState(false);
  const [showSipMatrixModal, setShowSipMatrixModal] = useState(false);
  const [showScenarioModal, setShowScenarioModal] = useState(false);
  const [activeScenario, setActiveScenario] = useState<any>(null);
  const [enabledScenarios, setEnabledScenarios] = useState<Record<string, boolean>>({});
  const settingsRef = useRef<HTMLDivElement | null>(null);
  const hasAppliedSavedStrategy = useRef(false);
  const userSelectedIncomeStrategy = useRef(false);
  const hasSavedParamsRef = useRef(false);

  // Computed active section based on primary and secondary tabs
  const activeSection = primaryTab === 'overview' ? 'overview' : secondaryTab[primaryTab] || 'matrix';

  useEffect(() => {
    trackEvent('retirement_tab_view', {
      primary_tab: primaryTab,
      secondary_tab: primaryTab === 'overview' ? 'overview' : secondaryTab[primaryTab],
      active_section: activeSection,
    });
  }, [primaryTab, secondaryTab, activeSection]);

  // Load saved params - prioritize user settings from backend
  useEffect(() => {
    const loadSettings = async () => {
      let mergedParams = { ...defaultParams };
      
      // First, load from Settings page (backend user settings)
      try {
        const userSettings = await settingsApi.get();
        console.log('Loaded user settings from backend:', userSettings);
        
        // Map Settings page fields to Retirement params (use !== undefined to allow 0 or falsy values)
        if (userSettings) {
          if (userSettings.currentAge !== undefined) mergedParams.currentAge = userSettings.currentAge;
          if (userSettings.retirementAge !== undefined) mergedParams.retirementAge = userSettings.retirementAge;
          if (userSettings.lifeExpectancy !== undefined) mergedParams.lifeExpectancy = userSettings.lifeExpectancy;
          if (userSettings.inflationRate !== undefined) mergedParams.inflation = userSettings.inflationRate;
          if (userSettings.epfReturn !== undefined) mergedParams.epfReturn = userSettings.epfReturn;
          if (userSettings.ppfReturn !== undefined) mergedParams.ppfReturn = userSettings.ppfReturn;
          if (userSettings.mfEquityReturn !== undefined) mergedParams.mfReturn = userSettings.mfEquityReturn;
          if (userSettings.sipStepup !== undefined) mergedParams.sipStepup = userSettings.sipStepup;
        }
        
        console.log('Merged params after backend settings:', mergedParams);
      } catch (error) {
        console.error('Failed to load user settings:', error);
      }
      
      // Then, override ONLY Retirement-specific settings from localStorage
      // (NOT the basic settings that come from Settings page)
      const retirementSettings = localStorage.getItem('retirementParams');
      if (retirementSettings) {
        hasSavedParamsRef.current = true;
        const parsed = JSON.parse(retirementSettings);
        console.log('Loaded retirement params from localStorage:', parsed);
        
        // Migrate older absolute year values to relative offsets
        if (typeof parsed.effectiveFromYear === 'number' && parsed.effectiveFromYear > 1000) {
          const relative = parsed.effectiveFromYear - CURRENT_YEAR;
          parsed.effectiveFromYear = Math.max(0, relative);
          console.log('Migrated effectiveFromYear to relative:', parsed.effectiveFromYear);
        }
        
        // ONLY use localStorage for Retirement-specific fields, NOT basic settings
        // Basic settings (currentAge, retirementAge, inflation, epfReturn, ppfReturn, mfReturn, sipStepup) 
        // should come from Settings page backend
        const retirementOnlyFields = ['effectiveFromYear', 'incomeStrategy', 'corpusReturnRate', 
                                       'enableRateReduction', 'rateReductionPercent', 'rateReductionYears'];
        
        retirementOnlyFields.forEach(field => {
          if (parsed[field] !== undefined) {
            (mergedParams as any)[field] = parsed[field];
          }
        });
        
        console.log('Final merged params after localStorage (retirement-specific only):', mergedParams);
      }
      
      setParams(mergedParams);
      
      // Save the merged params to ensure they're persisted with backend defaults
      localStorage.setItem('retirementParams', JSON.stringify(mergedParams));
    };
    
    loadSettings();
  }, []);

  const { data: retirementData, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['retirement', params.currentAge, params.retirementAge, params.lifeExpectancy,
               params.inflation, params.mfReturn, params.epfReturn, params.ppfReturn,
               params.sipStepup, params.effectiveFromYear, params.corpusReturnRate,
               params.enableRateReduction, params.rateReductionPercent, params.rateReductionYears,
               params.incomeStrategy],
    queryFn: () => {
      console.log('Fetching retirement data with params:', params);
      return retirementApi.calculate(params);
    },
    staleTime: 0,
    gcTime: 0, // Don't cache the results
  });

  const { data: netWorth } = useQuery({
    queryKey: ['networth'],
    queryFn: analysisApi.networth,
  });

  const { data: loans = [] } = useQuery({
    queryKey: ['loans'],
    queryFn: loansApi.getAll,
  });

  const { data: goals = [] } = useQuery({
    queryKey: ['goals'],
    queryFn: goalsApi.getAll,
  });

  const { data: investments = [] } = useQuery({
    queryKey: ['investments'],
    queryFn: investmentsApi.getAll,
  });

  const { data: expenseOpportunities } = useQuery({
    queryKey: ['expenseOpportunities', params.currentAge, params.retirementAge],
    queryFn: () => expensesApi.getInvestmentOpportunities(params.currentAge, params.retirementAge),
  });

  const { data: savedStrategyData } = useQuery({
    queryKey: ['retirementStrategy'],
    queryFn: retirementApi.getStrategy,
    retry: false,
  });

  const handleParamChange = (key: string, value: any) => {
    const numericFields = ['currentAge', 'retirementAge', 'lifeExpectancy', 'inflation', 'mfReturn', 'sipStepup', 'epfReturn', 'ppfReturn', 'corpusReturnRate', 'effectiveFromYear', 'rateReductionPercent', 'rateReductionYears'];
    const parsedValue = numericFields.includes(key) ? Number(value) : value;
    const newParams = { ...params, [key]: parsedValue };
    setParams(newParams);
    
    // Only save Retirement-specific fields to localStorage (NOT basic settings from Settings page)
    const retirementOnlyFields = ['effectiveFromYear', 'incomeStrategy', 'corpusReturnRate', 
                                   'enableRateReduction', 'rateReductionPercent', 'rateReductionYears'];
    
    // Load existing localStorage, update only retirement-specific fields
    const savedStr = localStorage.getItem('retirementParams');
    const saved = savedStr ? JSON.parse(savedStr) : {};
    retirementOnlyFields.forEach(field => {
      if ((newParams as any)[field] !== undefined) {
        saved[field] = (newParams as any)[field];
      }
    });
    localStorage.setItem('retirementParams', JSON.stringify(saved));
    if (key === 'incomeStrategy') {
      userSelectedIncomeStrategy.current = true;
      try {
        const existing = localStorage.getItem('userStrategy');
        const parsed = existing ? JSON.parse(existing) : {};
        const merged = { ...parsed, selectedIncomeStrategy: parsedValue };
        localStorage.setItem('userStrategy', JSON.stringify(merged));
        // Persist selection so it doesn't revert on next load
        retirementApi.saveStrategy(merged).catch(() => {});
      } catch {
        const merged = { selectedIncomeStrategy: parsedValue };
        localStorage.setItem('userStrategy', JSON.stringify(merged));
        retirementApi.saveStrategy(merged).catch(() => {});
      }
    }
  };

  const handleRetireAtAge = (age: number) => {
    handleParamChange('retirementAge', age);
    setPrimaryTab('projections');
    setSecondaryTab(prev => ({ ...prev, projections: 'income' }));
  };

  const handleRecalculate = async () => {
    console.log('Recalculating with params:', params);
    // Clear all retirement queries and refetch
    queryClient.removeQueries({ queryKey: ['retirement'] });
    const result = await refetch();
    if (result.data) {
      toast.success('Calculations updated');
    } else {
      toast.error('Failed to recalculate');
    }
  };

  const handleSaveToSettings = async () => {
    try {
      // First fetch existing settings to preserve fields we're not changing
      let existingSettings: any = {};
      try {
        existingSettings = await settingsApi.get() || {};
      } catch (e) {
        console.log('No existing settings, will create new');
      }
      
      // Map Retirement params to Settings format, preserving existing values for fields not in Retirement
      const settingsData = {
        ...existingSettings,
        currentAge: params.currentAge,
        retirementAge: params.retirementAge,
        lifeExpectancy: params.lifeExpectancy,
        inflationRate: params.inflation,
        epfReturn: params.epfReturn,
        ppfReturn: params.ppfReturn,
        mfEquityReturn: params.mfReturn,
        sipStepup: params.sipStepup,
        // Set defaults for fields not in Retirement params if they don't exist
        mfDebtReturn: existingSettings.mfDebtReturn || 7.0,
        fdReturn: existingSettings.fdReturn || 6.5,
        emergencyFundMonths: existingSettings.emergencyFundMonths || 6,
      };
      
      await settingsApi.update(settingsData);
      queryClient.invalidateQueries({ queryKey: ['user-settings'] });
      toast.success('Settings saved successfully! These will be used as defaults.');
    } catch (error) {
      console.error('Failed to save settings:', error);
      toast.error('Failed to save settings to database');
    }
  };

  useEffect(() => {
    if (showSettings) {
      settingsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [showSettings]);

  useEffect(() => {
    const applySavedStrategy = (strategy: any) => {
      if (!strategy || Object.keys(strategy).length === 0) return;
      const enabled: Record<string, boolean> = {};
      if (strategy.sellIlliquidAssets) enabled.sell_illiquid = true;
      if (strategy.reinvestMaturities) enabled.reinvest_maturities = true;
      if (strategy.redirectLoanEMIs) enabled.redirect_emi = true;
      if (strategy.increaseSIP) enabled.increase_sip = true;
      if (strategy.investFreedExpenses) enabled.freed_expenses = true;
      setEnabledScenarios(prev => ({ ...prev, ...enabled }));
      const savedParams = localStorage.getItem('retirementParams');
      const hasIncomeInParams = savedParams ? !!JSON.parse(savedParams)?.incomeStrategy : false;
      if (!userSelectedIncomeStrategy.current && !hasIncomeInParams && strategy.selectedIncomeStrategy) {
        handleParamChange('incomeStrategy', strategy.selectedIncomeStrategy);
      }
    };

    if (hasAppliedSavedStrategy.current) return;
    if (savedStrategyData && Object.keys(savedStrategyData).length > 0) {
      localStorage.setItem('userStrategy', JSON.stringify(savedStrategyData));
      applySavedStrategy(savedStrategyData);
      hasAppliedSavedStrategy.current = true;
      return;
    }

    const local = localStorage.getItem('userStrategy');
    if (local) {
      try {
        applySavedStrategy(JSON.parse(local));
        hasAppliedSavedStrategy.current = true;
      } catch {
        // ignore invalid localStorage data
      }
    }
  }, [savedStrategyData]);

  // Filter secondary tabs based on feature access
  const getVisibleSecondaryTabs = (primaryId: string) => {
    const tabs = SECONDARY_TABS[primaryId] || [];
    return tabs.filter(tab => {
      if (tab.alwaysShow) return true;
      if (tab.feature && features) {
        return (features as unknown as Record<string, boolean>)[tab.feature] === true;
      }
      return false;
    });
  };

  // Handle tab switching
  const handlePrimaryTabChange = (tabId: string) => {
    setPrimaryTab(tabId);
    // If switching to a tab with secondary tabs, ensure we have a valid secondary tab selected
    if (SECONDARY_TABS[tabId]) {
      const visibleTabs = getVisibleSecondaryTabs(tabId);
      if (visibleTabs.length > 0 && !secondaryTab[tabId]) {
        setSecondaryTab(prev => ({ ...prev, [tabId]: visibleTabs[0].id }));
      }
    }
  };

  const handleSecondaryTabChange = (primaryId: string, secondaryId: string) => {
    setSecondaryTab(prev => ({ ...prev, [primaryId]: secondaryId }));
  };

  const selectIncomeStrategy = (strategyValue: string) => {
    handleParamChange('incomeStrategy', strategyValue);
  };

  const summary = retirementData?.summary || {};
  const gapAnalysis = retirementData?.gapAnalysis || {};
  const matrix = retirementData?.matrix || [];
  
  // Debug logging
  console.log('Retirement Data:', { 
    matrixLength: matrix.length, 
    incomeProjectionLength: (summary.retirementIncomeProjection || []).length,
    paramsCurrentAge: params.currentAge,
    paramsRetirementAge: params.retirementAge,
    summaryCurrentAge: summary.currentAge,
    summaryRetirementAge: summary.retirementAge,
    firstMatrixYear: matrix[0]?.year,
    lastMatrixYear: matrix[matrix.length - 1]?.year
  });
  const maturingBeforeRetirement = retirementData?.maturingBeforeRetirement || {};
  const moneyBackPayouts = maturingBeforeRetirement.moneyBackPayouts || [];
  const moneyBackCount = maturingBeforeRetirement.moneyBackCount || moneyBackPayouts.length || 0;
  const expenseProjection = gapAnalysis.expenseProjection || [];
  const endingExpenses = gapAnalysis.endingExpensesBeforeRetirement || [];
  const incomeProjection = summary.retirementIncomeProjection || [];

  // SIP Step-up optimization data
  const sipStepUpOptimization = summary.sipStepUpOptimization || {};
  
  // Calculate key metrics
  const projectedCorpus = summary.finalCorpus || 0;
  const requiredCorpus = gapAnalysis.requiredCorpus || 0;
  const corpusGap = gapAnalysis.corpusGap || 0;
  
  // Get monthly SIP from step-up optimization or first matrix row
  const monthlySIP = sipStepUpOptimization.sipAtStart || (matrix[0]?.mfSip) || 0;
  const sipAtFullStepUp = sipStepUpOptimization.sipAtFullStepUp || monthlySIP;
  const sipAtOptimalStop = sipStepUpOptimization.sipAtOptimalStop || monthlySIP;
  
  // Debug SIP step-up values
  console.log('SIP Step-up Debug:', {
    sipAtStart: sipStepUpOptimization.sipAtStart,
    sipAtFullStepUp: sipStepUpOptimization.sipAtFullStepUp,
    sipAtOptimalStop: sipStepUpOptimization.sipAtOptimalStop,
    monthlySIP,
    sipStepup: params.sipStepup,
    effectiveFromYear: params.effectiveFromYear,
    yearsToRetirement: params.retirementAge - params.currentAge,
    optimization: sipStepUpOptimization
  });
  const optimalStopYear = sipStepUpOptimization.optimalStopYear;
  const canStopEarly = sipStepUpOptimization.canStopEarly;
  const corpusAtOptimalStop = sipStepUpOptimization.corpusAtOptimalStop || projectedCorpus;
  const sipScenarios = sipStepUpOptimization.scenarios || [];
  const sipStepUpPercent = summary.sipStepUpPercent || params.sipStepup;
  const mfRate = matrix[0]?.mfRate || params.mfReturn;
  const sipAtStopYear = optimalStopYear !== null && matrix[optimalStopYear]
    ? (matrix[optimalStopYear]?.mfSip || 0)
    : 0;
  const annuityMonthlyIncome = summary.annuityMonthlyIncome || 0;

  const calculateIfStopped = (yearIdx: number) => {
    if (optimalStopYear === null || yearIdx <= optimalStopYear) {
      return null;
    }

    const flatSip = sipAtStopYear;
    const currentSip = matrix[yearIdx]?.mfSip || 0;
    const yearsAfterStop = yearIdx - optimalStopYear;
    const sipSavings = currentSip - flatSip;
    const avgSipDiff = sipSavings / 2;
    const corpusReduction = avgSipDiff * 12 * yearsAfterStop * (1 + mfRate / 100);

    return {
      flatSip: Math.round(flatSip),
      flatCorpus: Math.round((matrix[yearIdx]?.netCorpus || 0) - corpusReduction),
    };
  };

  const getTypeBadgeClass = (type?: string) => {
    switch ((type || '').toUpperCase()) {
      case 'FD':
        return 'bg-blue-100 text-blue-700';
      case 'RD':
        return 'bg-indigo-100 text-indigo-700';
      case 'PPF':
        return 'bg-green-100 text-green-700';
      case 'MUTUAL_FUND':
        return 'bg-purple-100 text-purple-700';
      case 'NPS':
        return 'bg-orange-100 text-orange-700';
      default:
        return 'bg-slate-100 text-slate-700';
    }
  };

  const calculateRequiredSIP = (targetAmount: number, annualReturn: number, years: number) => {
    const monthlyRate = annualReturn / 100 / 12;
    const months = years * 12;
    if (monthlyRate <= 0 || months <= 0) return 0;
    const factor = (Math.pow(1 + monthlyRate, months) - 1) / monthlyRate;
    return targetAmount / factor;
  };

  const calculateSIPFutureValue = (monthlyAmount: number, annualReturn: number, years: number) => {
    const monthlyRate = annualReturn / 100 / 12;
    const months = years * 12;
    if (monthlyRate <= 0 || months <= 0) return 0;
    return monthlyAmount * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
  };

  const retirementYears = summary.retirementYears || (summary.lifeExpectancy ? (summary.lifeExpectancy - (summary.retirementAge || params.retirementAge)) : 0);
  const withdrawalRate = summary.withdrawalRate || 8;
  const corpusReturnRate = summary.corpusReturnRate || params.corpusReturnRate || 10;
  const fallbackMonthlyRetirementIncome = projectedCorpus > 0 && retirementYears > 0
    ? projectedCorpus / retirementYears / 12
    : 0;
  const fallbackMonthlyIncome4Percent = projectedCorpus > 0 ? (projectedCorpus * 0.04) / 12 : 0;
  const fallbackMonthlyIncomeFromCorpus = projectedCorpus > 0 ? (projectedCorpus * (withdrawalRate / 100)) / 12 : 0;

  // Income strategy calculations (fallback to summary if gap analysis doesn't provide)
  const incomeStrategies = {
    SUSTAINABLE: gapAnalysis.sustainableIncome || summary.monthlyIncomeFromCorpus || fallbackMonthlyIncomeFromCorpus,
    SAFE_4_PERCENT: gapAnalysis.safe4PercentIncome || summary.monthlyIncome4Percent || fallbackMonthlyIncome4Percent,
    SIMPLE_DEPLETION: gapAnalysis.depletionIncome || summary.monthlyRetirementIncome || fallbackMonthlyRetirementIncome,
  };

  // Prepare corpus growth chart data - combine pre-retirement (matrix) and post-retirement (incomeProjection)
  // Match VanillaJS exactly: subtract current year's inflow only for pre-retirement
  const preRetirementData = matrix.map((row: any) => {
    const netCorpus = row.netCorpus || 0;
    const inflow = row.totalInflow || 0;
    
    return {
      year: row.year,
      age: row.age,
      // VanillaJS: netCorpus - inflow (current year only)
      corpus: Math.max(0, netCorpus - inflow),
      ppf: row.ppfBalance || 0,
      epf: row.epfBalance || 0,
      mf: row.mfBalance || 0,
      totalInflow: inflow,
      insuranceMaturity: row.insuranceMaturity || 0,
      investmentMaturity: row.investmentMaturity || 0,
      moneyBackPayout: row.moneyBackPayout || 0,
      isPreRetirement: true,
    };
  });

  // Post-retirement data - interpolate to fill all years (backend sends sparse data at intervals)
  const retirementYear = CURRENT_YEAR + (params.retirementAge - params.currentAge);
  const yearsInRetirement = params.lifeExpectancy - params.retirementAge;
  
  // Create yearly post-retirement data by interpolating between sparse data points
  const sparsePostRetirement = incomeProjection.slice(1);
  const postRetirementData: any[] = [];
  
  for (let yearOffset = 1; yearOffset <= yearsInRetirement; yearOffset++) {
    const actualYear = retirementYear + yearOffset;
    const age = params.retirementAge + yearOffset;
    
    // Find the two data points to interpolate between
    let lowerPoint = sparsePostRetirement[0];
    let upperPoint = sparsePostRetirement[sparsePostRetirement.length - 1];
    
    for (let i = 0; i < sparsePostRetirement.length - 1; i++) {
      if (sparsePostRetirement[i].year <= yearOffset && sparsePostRetirement[i + 1].year >= yearOffset) {
        lowerPoint = sparsePostRetirement[i];
        upperPoint = sparsePostRetirement[i + 1];
        break;
      }
    }
    
    // Linear interpolation for corpus value (VanillaJS uses raw corpus, no subtraction)
    let corpus = 0;
    if (lowerPoint && upperPoint && upperPoint.year !== lowerPoint.year) {
      const ratio = (yearOffset - lowerPoint.year) / (upperPoint.year - lowerPoint.year);
      corpus = (lowerPoint.corpus || 0) + ratio * ((upperPoint.corpus || 0) - (lowerPoint.corpus || 0));
    } else if (lowerPoint) {
      corpus = lowerPoint.corpus || 0;
    }
    
    postRetirementData.push({
      year: actualYear,
      age,
      corpus: Math.max(0, corpus),
      ppf: undefined,
      epf: undefined,
      mf: undefined,
      totalInflow: 0,
      insuranceMaturity: 0,
      investmentMaturity: 0,
      moneyBackPayout: 0,
      isPreRetirement: false,
    });
  }

  // Combine pre and post retirement data, adding maturity marker field
  // Maturity marker Y value = corpus value (so marker sits ON the line)
  const corpusChartData = [...preRetirementData, ...postRetirementData].map((row: any) => ({
    ...row,
    maturityMarker: row.totalInflow > 0 ? row.corpus : null,
  }));
  
  // Filter maturity events for the table display
  const maturityEvents = preRetirementData.filter((row: any) => (row.totalInflow || 0) > 0);

  // Starting balances from summary
  const startingBalances = summary.startingBalances || {};
  const totalAssets = startingBalances.totalStarting || 0;
  const monthlyInsurancePremiums = gapAnalysis.monthlyInsurancePremiums || 0;
  const householdExpenses = gapAnalysis.currentMonthlyExpenses || 0;
  const monthlyFreedUpByRetirement = gapAnalysis.monthlyFreedUpByRetirement || 0;
  const potentialCorpusFromFreedUpExpenses = gapAnalysis.potentialCorpusFromFreedUpExpenses || 0;

  const yearsToRetirement = summary.yearsToRetirement || (params.retirementAge - params.currentAge);
  const currentYear = new Date().getFullYear();

  const monthlyIncome = gapAnalysis.monthlyIncome || 0;
  const totalRentalIncome = summary.monthlyRentalIncomeAtRetirement ||0;
  const effectiveMonthlyIncome = monthlyIncome + totalRentalIncome;
  const totalExpenses = gapAnalysis.totalCurrentMonthlyExpenses || gapAnalysis.currentMonthlyExpenses || 0;
  const monthlyEMI = gapAnalysis.monthlyEMI || 0;
  const monthlySIPFromGap = gapAnalysis.monthlySIP || 0;
  const netMonthlySavings = gapAnalysis.netMonthlySavings || (effectiveMonthlyIncome - totalExpenses - monthlyEMI);
  const availableMonthlySavings = gapAnalysis.availableMonthlySavings || (netMonthlySavings - monthlySIPFromGap);

  const assetBreakdown = netWorth?.assetBreakdown || {};
  const sellableAssets = netWorth?.sellableAssets || {};
  const illiquidValue = (sellableAssets.GOLD || 0) + (sellableAssets.REAL_ESTATE || 0);
  const maturingTotal = maturingBeforeRetirement.totalMaturingBeforeRetirement || 0;
  const totalEMI = Array.isArray(loans) ? loans.reduce((sum: number, l: any) => sum + (l.emi || 0), 0) : 0;
  const expenseOpportunitiesByYear = expenseOpportunities?.freedUpByYear || [];

  const emergencyFundTagged = (Array.isArray(investments) ? investments : [])
    .filter((inv: any) => (inv.type === 'FD' || inv.type === 'RD') && inv.isEmergencyFund)
    .reduce((sum: number, inv: any) => sum + (inv.currentValue || inv.investedAmount || 0), 0);

  const strategyAllocations = (() => {
    const allocations: Array<{ name: string; amount: number; description: string; priority: 'High' | 'Medium' | 'Low'; color: string }> = [];
    if (netMonthlySavings <= 0) return allocations;

    const monthlyExpensesForEmergency = totalExpenses > 0 ? totalExpenses : 50000;
    const emergencyFund = monthlyExpensesForEmergency * 6;
    const currentCash = assetBreakdown.CASH || 0;
    const currentEmergencyFund = currentCash + emergencyFundTagged;
    const emergencyGap = Math.max(0, emergencyFund - currentEmergencyFund);
    const requiredMonthlySIP = corpusGap > 0 ? calculateRequiredSIP(corpusGap, 10, yearsToRetirement) : 0;

    const activeGoals = Array.isArray(goals) ? goals.filter((g: any) => (g.targetAmount || 0) > 0) : [];
    let remaining = netMonthlySavings;

    if (emergencyGap > 0) {
      const emergencyAllocation = Math.min(remaining * 0.3, Math.ceil(emergencyGap / 12));
      allocations.push({
        name: '🆘 Emergency Fund',
        amount: emergencyAllocation,
        description: `Build ${formatCurrency(emergencyFund)} emergency fund (currently ${formatCurrency(currentEmergencyFund)})`,
        priority: 'High',
        color: 'bg-amber-100 border-amber-300',
      });
      remaining -= emergencyAllocation;
    }

    if (totalEMI > 0 && remaining > totalEMI * 0.2) {
      const prepaymentAllocation = Math.min(remaining * 0.2, totalEMI);
      allocations.push({
        name: '🏦 Loan Prepayment',
        amount: prepaymentAllocation,
        description: `Prepay loans to save interest and free up ${formatCurrency(totalEMI)}/mo sooner`,
        priority: 'Medium',
        color: 'bg-blue-100 border-blue-300',
      });
      remaining -= prepaymentAllocation;
    }

    if (corpusGap > 0) {
      const corpusAllocation = Math.min(remaining * 0.6, requiredMonthlySIP);
      allocations.push({
        name: '🏖️ Retirement Corpus',
        amount: corpusAllocation,
        description: `Invest in MF/NPS to close ${formatCurrency(corpusGap, true)} gap`,
        priority: 'High',
        color: 'bg-primary-100 border-primary-300',
      });
      remaining -= corpusAllocation;
    } else if (remaining > 0) {
      allocations.push({
        name: '🏖️ Retirement (Already On Track!)',
        amount: remaining * 0.5,
        description: 'Continue investing to build even larger corpus',
        priority: 'Low',
        color: 'bg-emerald-100 border-emerald-300',
      });
      remaining -= remaining * 0.5;
    }

    if (activeGoals.length > 0 && remaining > 0) {
      const goalsAllocation = Math.min(remaining * 0.3, remaining);
      allocations.push({
        name: '🎯 Goals Fund',
        amount: goalsAllocation,
        description: `For ${activeGoals.length} goal(s): ${activeGoals.slice(0, 2).map((g: any) => g.name).join(', ')}`,
        priority: 'Medium',
        color: 'bg-purple-100 border-purple-300',
      });
      remaining -= goalsAllocation;
    }

    if (remaining > 0) {
      allocations.push({
        name: '💰 Flexible Savings',
        amount: remaining,
        description: 'Additional savings for opportunities or lifestyle',
        priority: 'Low',
        color: 'bg-slate-100 border-slate-300',
      });
    }

    return allocations;
  })();

  const timelineItems = (() => {
    const items: Array<{ year: number; icon: string; title: string; description: string; action: string; actionColor: string }> = [];

    (Array.isArray(loans) ? loans : []).forEach((loan: any) => {
      if (loan.endDate) {
        const endYear = new Date(loan.endDate).getFullYear();
        if (endYear > currentYear && endYear < currentYear + 30) {
          items.push({
            year: endYear,
            icon: '🎉',
            title: `${loan.name || 'Loan'} Paid Off`,
            description: `${formatCurrency(loan.emi || 0)}/mo becomes available for investment`,
            action: 'Redirect EMI to MF SIP',
            actionColor: 'text-emerald-600',
          });
        }
      }
    });

    (maturingBeforeRetirement.maturingInvestments || []).forEach((inv: any) => {
      if (inv.maturityDate) {
        items.push({
          year: new Date(inv.maturityDate).getFullYear(),
          icon: '💰',
          title: `${inv.name} Matures`,
          description: `${formatCurrency(inv.expectedMaturityValue || 0)} available`,
          action: 'Reinvest in higher-return assets',
          actionColor: 'text-primary-600',
        });
      }
    });

    (maturingBeforeRetirement.maturingInsurance || []).forEach((ins: any) => {
      if (ins.maturityDate) {
        items.push({
          year: new Date(ins.maturityDate).getFullYear(),
          icon: '🛡️',
          title: `${ins.name} Matures`,
          description: `${formatCurrency(ins.expectedMaturityValue || 0)} available`,
          action: 'Add to retirement corpus',
          actionColor: 'text-purple-600',
        });
      }
    });

    (Array.isArray(goals) ? goals : []).forEach((goal: any) => {
      if (goal.targetYear && goal.targetYear > currentYear) {
        items.push({
          year: goal.targetYear,
          icon: '🎯',
          title: goal.name,
          description: `${formatCurrency(goal.targetAmount || 0)} needed`,
          action: 'Ensure funds are available',
          actionColor: 'text-amber-600',
        });
      }
    });

    (expenseOpportunitiesByYear || []).forEach((opp: any) => {
      const oppYear = opp.year || opp.endsInYear;
      if (oppYear && oppYear > currentYear && oppYear < currentYear + yearsToRetirement) {
        const expenseNames = (opp.endingExpenses || []).map((e: any) => e.name).join(', ') || 'Expense';
        items.push({
          year: oppYear,
          icon: '🎓',
          title: `${expenseNames} Ends`,
          description: `${formatCurrency(opp.monthlyFreedUp || opp.freedMonthlyAmount || 0)}/mo freed up`,
          action: `Invest for ${formatCurrency(opp.potentialCorpusAt12Percent || opp.potentialCorpusIfInvested || 0, true)} corpus`,
          actionColor: 'text-purple-600',
        });
      }
    });

    if (corpusGap <= 0) {
      items.push({
        year: currentYear + yearsToRetirement,
        icon: '🏖️',
        title: 'Retirement Target Met!',
        description: `Projected corpus: ${formatCurrency(summary.finalCorpus || 0, true)}`,
        action: "You're on track!",
        actionColor: 'text-emerald-600',
      });
    } else {
      items.push({
        year: currentYear,
        icon: '⚠️',
        title: 'Action Required Now',
        description: `Increase SIP by ${formatCurrency(gapAnalysis.additionalSIPRequired || 0)}/mo`,
        action: 'Close corpus gap',
        actionColor: 'text-danger-600',
      });
    }

    return items.sort((a, b) => a.year - b.year);
  })();

  const whatIfScenarios = (() => {
    const scenarios: Array<{
      id: string;
      icon: string;
      title: string;
      description: string;
      impact: string;
      timing?: string;
      result: string;
      resultClass: string;
      bgClass: string;
      type: 'lumpsum' | 'reinvest' | 'sip';
      value: number;
      deploymentYear: number;
      potentialCorpus?: number;
      opportunities?: Array<any>;
    }> = [];
    const projected = summary.finalCorpus || 0;
    const required = gapAnalysis.requiredCorpus || 0;
    const gap = gapAnalysis.corpusGap || 0;

    const calculateOptimalSellYear = (illiquidTotal: number, gapAmount: number) => {
      if (gapAmount <= 0) return null;
      for (let y = yearsToRetirement; y >= 0; y -= 1) {
        const corpusAtYear = projected * Math.pow(1.10, y);
        const illiquidGrowth = illiquidTotal * Math.pow(1.08, yearsToRetirement - y);
        if (corpusAtYear + illiquidGrowth >= required) {
          return currentYear + (yearsToRetirement - y);
        }
      }
      return currentYear + yearsToRetirement;
    };

    if (illiquidValue > 0) {
      const newCorpus = projected + illiquidValue;
      const canMeet = newCorpus >= required;
      const optimalSellYear = calculateOptimalSellYear(illiquidValue, gap);
      scenarios.push({
        id: 'sell_illiquid',
        icon: '🏠',
        title: 'Sell Illiquid Assets',
        description: `Gold + Real Estate total ${formatCurrency(illiquidValue, true)}`,
        impact: `+${formatCurrency(illiquidValue, true)} to corpus`,
        timing: optimalSellYear ? `📅 Optimal time: ${optimalSellYear}` : undefined,
        result: canMeet ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpus, true)} short`,
        resultClass: canMeet ? 'text-emerald-600' : 'text-amber-600',
        bgClass: canMeet ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200',
        type: 'lumpsum',
        value: illiquidValue,
        deploymentYear: optimalSellYear || (currentYear + Math.floor(yearsToRetirement / 2)),
      });
    }

    if (maturingTotal > 0) {
      const newCorpus = projected + maturingTotal;
      const canMeet = newCorpus >= required;
      const maturityYears: number[] = []
        .concat((maturingBeforeRetirement.maturingInvestments || []).map((inv: any) => inv.maturityDate ? new Date(inv.maturityDate).getFullYear() : null))
        .concat((maturingBeforeRetirement.maturingInsurance || []).map((ins: any) => ins.maturityDate ? new Date(ins.maturityDate).getFullYear() : null))
        .filter((y: number | null): y is number => !!y);
      const avgMaturityYear = maturityYears.length > 0
        ? Math.round(maturityYears.reduce((sum, y) => sum + y, 0) / maturityYears.length)
        : currentYear + Math.floor(yearsToRetirement / 2);
      scenarios.push({
        id: 'reinvest_maturities',
        icon: '💰',
        title: 'Reinvest Maturities',
        description: `${formatCurrency(maturingTotal, true)} maturing before retirement`,
        impact: `+${formatCurrency(maturingTotal, true)} if reinvested`,
        result: canMeet ? '✅ Would meet required corpus' : `⚠️ Still ${formatCurrency(required - newCorpus, true)} short`,
        resultClass: canMeet ? 'text-emerald-600' : 'text-amber-600',
        bgClass: canMeet ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200',
        type: 'reinvest',
        value: maturingTotal,
        deploymentYear: avgMaturityYear,
      });
    }

    if (totalEMI > 0) {
      const loanEndYears = (Array.isArray(loans) ? loans : [])
        .map((l: any) => l.endDate ? new Date(l.endDate).getFullYear() : null)
        .filter((y: number | null): y is number => !!y)
        .sort((a, b) => a - b);
      const startYear = loanEndYears[0] || (currentYear + 5);
      scenarios.push({
        id: 'redirect_emi',
        icon: '💳',
        title: 'Redirect EMIs to SIP',
        description: `Current EMIs total ${formatCurrency(totalEMI)}/mo`,
        impact: `+${formatCurrency(totalEMI)}/mo SIP potential`,
        result: '✅ Can accelerate corpus growth',
        resultClass: 'text-emerald-600',
        bgClass: 'bg-emerald-50 border-emerald-200',
        type: 'sip',
        value: totalEMI,
        deploymentYear: startYear,
      });
    }

    if (monthlySIP > 0) {
      const increased = Math.round(monthlySIP * 0.2);
      scenarios.push({
        id: 'increase_sip',
        icon: '📈',
        title: 'Increase SIP by 20%',
        description: `Current SIP ${formatCurrency(monthlySIP)}/mo`,
        impact: `+${formatCurrency(increased)}/mo incremental SIP`,
        result: gap > 0 ? '✅ Helps reduce corpus gap' : '✅ Builds additional surplus',
        resultClass: 'text-emerald-600',
        bgClass: 'bg-emerald-50 border-emerald-200',
        type: 'sip',
        value: Math.round(monthlySIPFromGap * 0.2),
        deploymentYear: currentYear,
      });
    }

    if ((expenseOpportunities?.totalMonthlyFreedUpByRetirement || 0) > 0) {
      const startYear = expenseOpportunitiesByYear.length > 0
        ? expenseOpportunitiesByYear[0].year
        : currentYear + 5;
      scenarios.push({
        id: 'freed_expenses',
        icon: '🎓',
        title: 'Invest Ending Expenses',
        description: `${formatCurrency(expenseOpportunities.totalMonthlyFreedUpByRetirement || 0)}/mo freed up`,
        impact: `+${formatCurrency(expenseOpportunities.totalPotentialCorpus || 0, true)} potential corpus`,
        result: '✅ Convert freed cash into investments',
        resultClass: 'text-emerald-600',
        bgClass: 'bg-emerald-50 border-emerald-200',
        type: 'sip',
        value: expenseOpportunities.totalMonthlyFreedUpByRetirement || 0,
        deploymentYear: startYear,
        potentialCorpus: expenseOpportunities.totalPotentialCorpus,
        opportunities: expenseOpportunitiesByYear,
      });
    }

    return scenarios;
  })();

  const generateScenarioChartData = (scenario: any) => {
    const annualReturn = 0.10;
    const monthlyReturn = annualReturn / 12;
    const years = yearsToRetirement;
    const deploymentYearOffset = Math.max(0, scenario.deploymentYear - currentYear);
    const existingMonthlySIP = monthlySIPFromGap;

    const startingCorpus = (startingBalances.totalStarting || 0) + (startingBalances.otherLiquidTotal || 0);
    let baselineCorpus = startingCorpus;
    let strategyCorpus = startingCorpus;

    const data = [];
    for (let y = 0; y <= years; y += 1) {
      if (y === deploymentYearOffset) {
        if (scenario.type === 'lumpsum' && scenario.value) {
          strategyCorpus += scenario.value;
        }
        if (scenario.type === 'reinvest' && scenario.value) {
          strategyCorpus += scenario.value;
        }
      }

      if (y > 0) {
        for (let m = 0; m < 12; m += 1) {
          baselineCorpus = baselineCorpus * (1 + monthlyReturn) + existingMonthlySIP;
        }

        if (scenario.type === 'sip' && scenario.value && y >= deploymentYearOffset) {
          for (let m = 0; m < 12; m += 1) {
            strategyCorpus = strategyCorpus * (1 + monthlyReturn) + existingMonthlySIP + scenario.value;
          }
        } else {
          for (let m = 0; m < 12; m += 1) {
            strategyCorpus = strategyCorpus * (1 + monthlyReturn) + existingMonthlySIP;
          }
        }
      }

      data.push({
        year: currentYear + y,
        baselineCorpus: Math.round(baselineCorpus),
        strategyCorpus: Math.round(strategyCorpus),
        difference: Math.round(strategyCorpus - baselineCorpus),
      });
    }

    return data;
  };

  const getScenarioImpact = (scenario: any) => {
    const data = generateScenarioChartData(scenario);
    const last = data[data.length - 1];
    return {
      impact: last?.difference || 0,
      baseline: last?.baselineCorpus || 0,
      strategy: last?.strategyCorpus || 0,
    };
  };

  const handleSaveStrategy = async () => {
    const enabledList = whatIfScenarios.filter(s => enabledScenarios[s.id]);
    if (enabledList.length === 0) {
      toast.error('Please enable at least one strategy to save');
      return;
    }

    const getScenario = (id: string) => enabledList.find(s => s.id === id);
    const freedScenario = getScenario('freed_expenses');
    const freedDetails = (freedScenario?.opportunities || []).flatMap((opp: any) =>
      (opp.endingExpenses || []).map((exp: any) => ({
        name: exp.name,
        category: exp.category,
        endYear: opp.year,
        monthlyAmount: exp.monthlyAmount,
      }))
    );

    const strategy = {
      selectedIncomeStrategy: params.incomeStrategy || 'SUSTAINABLE',
      sellIlliquidAssets: !!getScenario('sell_illiquid'),
      sellIlliquidAssetsYear: getScenario('sell_illiquid')?.deploymentYear,
      illiquidAssetsValue: getScenario('sell_illiquid')?.value,
      reinvestMaturities: !!getScenario('reinvest_maturities'),
      expectedMaturitiesValue: getScenario('reinvest_maturities')?.value,
      redirectLoanEMIs: !!getScenario('redirect_emi'),
      loanEndYear: getScenario('redirect_emi')?.deploymentYear,
      monthlyEMIAmount: getScenario('redirect_emi')?.value,
      increaseSIP: !!getScenario('increase_sip'),
      sipIncreasePercent: 20,
      newSIPAmount: getScenario('increase_sip')?.value,
      investFreedExpenses: !!getScenario('freed_expenses'),
      freedExpensesMonthly: freedScenario?.value,
      freedExpensesPotentialCorpus: freedScenario?.potentialCorpus,
      freedExpensesStartYear: freedScenario?.deploymentYear,
      freedExpensesDetails: freedDetails,
      strategyNotes: `Selected ${enabledList.length} strategies`,
    };

    try {
      await retirementApi.saveStrategy(strategy);
      localStorage.setItem('userStrategy', JSON.stringify(strategy));
      toast.success('Strategy saved successfully');
    } catch (error: any) {
      localStorage.setItem('userStrategy', JSON.stringify(strategy));
      toast.success('Strategy saved locally');
    }
  };

  return (
    <MainLayout 
      title="Retirement Planning"
      subtitle="Plan your financial future"
    >
      <div className="flex items-center justify-end gap-3 mb-4">
        <Button
          onClick={() => {
            console.log('Toggle settings panel');
            setShowSettings(prev => !prev);
          }}
          variant="secondary"
          size="sm"
          aria-expanded={showSettings}
        >
          <Settings size={16} className="mr-1" /> Settings
        </Button>
        <span className="text-xs text-slate-400">
          {showSettings ? 'Settings: open' : 'Settings: closed'}
        </span>
      </div>
      {/* Settings Panel */}
      {showSettings && (
        <Card className="mb-6" ref={settingsRef}>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Calculation Parameters</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4 mb-4">
              <Input
                label="Current Age"
                type="number"
                value={params.currentAge}
                onChange={e => handleParamChange('currentAge', Number(e.target.value))}
              />
              <Input
                label="Retirement Age"
                type="number"
                value={params.retirementAge}
                onChange={e => handleParamChange('retirementAge', Number(e.target.value))}
              />
              <Input
                label="Life Expectancy"
                type="number"
                value={params.lifeExpectancy}
                onChange={e => handleParamChange('lifeExpectancy', Number(e.target.value))}
              />
              <Input
                label="Inflation (%)"
                type="number"
                value={params.inflation}
                onChange={e => handleParamChange('inflation', Number(e.target.value))}
                step="0.1"
              />
              <Input
                label="MF Return (%)"
                type="number"
                value={params.mfReturn}
                onChange={e => handleParamChange('mfReturn', Number(e.target.value))}
                step="0.1"
              />
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
              <Input
                label="EPF Return (%)"
                type="number"
                value={params.epfReturn}
                onChange={e => handleParamChange('epfReturn', Number(e.target.value))}
                step="0.1"
              />
              <Input
                label="PPF Return (%)"
                type="number"
                value={params.ppfReturn}
                onChange={e => handleParamChange('ppfReturn', Number(e.target.value))}
                step="0.1"
              />
              <Input
                label="SIP Step-up (%)"
                type="number"
                value={params.sipStepup}
                onChange={e => handleParamChange('sipStepup', Number(e.target.value))}
              />
              <Select
                label="Step-up Start Year"
                value={params.effectiveFromYear.toString()}
                onChange={e => handleParamChange('effectiveFromYear', Number(e.target.value))}
              >
                <option value="0">{CURRENT_YEAR} (Current)</option>
                <option value="1">{CURRENT_YEAR + 1} (Next Year)</option>
                <option value="2">{CURRENT_YEAR + 2}</option>
                <option value="3">{CURRENT_YEAR + 3}</option>
                <option value="5">{CURRENT_YEAR + 5}</option>
              </Select>
            </div>
            <div className="mt-4 rounded-lg border border-slate-200 p-4 bg-slate-50">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <p className="text-sm font-semibold text-slate-700">Rate Reduction (PPF/EPF/FD/RD)</p>
                  <p className="text-xs text-slate-500">Reduce rates over time to model declining returns</p>
                </div>
                <label className="flex items-center gap-2 text-sm text-slate-600">
                  <input
                    type="checkbox"
                    checked={params.enableRateReduction}
                    onChange={e => handleParamChange('enableRateReduction', e.target.checked)}
                  />
                  Enable
                </label>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                <Input
                  label="Reduction (%)"
                  type="number"
                  value={params.rateReductionPercent}
                  onChange={e => handleParamChange('rateReductionPercent', Number(e.target.value))}
                  step="0.1"
                />
                <Input
                  label="Every N years"
                  type="number"
                  value={params.rateReductionYears}
                  onChange={e => handleParamChange('rateReductionYears', Number(e.target.value))}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-4">
              <Button 
                variant="secondary" 
                onClick={async () => {
                  localStorage.removeItem('retirementParams');
                  
                  // Load from Settings page backend if available, otherwise use defaults
                  let resetParams = { ...defaultParams };
                  try {
                    const userSettings = await settingsApi.get();
                    if (userSettings.currentAge) resetParams.currentAge = userSettings.currentAge;
                    if (userSettings.retirementAge) resetParams.retirementAge = userSettings.retirementAge;
                    if (userSettings.lifeExpectancy) resetParams.lifeExpectancy = userSettings.lifeExpectancy;
                    if (userSettings.inflationRate) resetParams.inflation = userSettings.inflationRate;
                    if (userSettings.epfReturn) resetParams.epfReturn = userSettings.epfReturn;
                    if (userSettings.ppfReturn) resetParams.ppfReturn = userSettings.ppfReturn;
                    if (userSettings.mfEquityReturn) resetParams.mfReturn = userSettings.mfEquityReturn;
                    if (userSettings.sipStepup) resetParams.sipStepup = userSettings.sipStepup;
                  } catch (error) {
                    console.error('Failed to load user settings:', error);
                  }
                  
                  setParams(resetParams);
                  toast.success('Reset to Settings page values');
                }}
              >
                Reset to Defaults
              </Button>
              <Button 
                variant="secondary"
                onClick={handleSaveToSettings}
              >
                Save to Settings
              </Button>
              <Button onClick={handleRecalculate} isLoading={isFetching}>
                {isFetching ? 'Calculating...' : 'Recalculate'}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Primary Tab Navigation */}
      <div className="mb-4">
        <div className="flex gap-1 border-b border-slate-200">
          {PRIMARY_TABS.map(tab => (
            <button
              key={tab.id}
              onClick={() => handlePrimaryTabChange(tab.id)}
              className={`flex items-center gap-2 px-6 py-3 text-sm font-medium border-b-2 transition-all ${
                primaryTab === tab.id
                  ? 'border-primary-500 text-primary-700 bg-primary-50/50'
                  : 'border-transparent text-slate-600 hover:text-primary-600 hover:border-slate-300'
              }`}
            >
              <span className="text-lg">{tab.icon}</span>
              <div className="flex flex-col items-start">
                <span>{tab.label}</span>
                {primaryTab === tab.id && (
                  <span className="text-xs text-slate-500 font-normal">{tab.description}</span>
                )}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Secondary Tab Navigation (for tabs with sub-sections) */}
      {SECONDARY_TABS[primaryTab] && (
        <div className="mb-6">
          <div className="flex gap-2 px-1">
            {getVisibleSecondaryTabs(primaryTab).map(tab => (
              <button
                key={tab.id}
                onClick={() => handleSecondaryTabChange(primaryTab, tab.id)}
                className={`px-4 py-2 text-sm rounded-lg transition-colors ${
                  secondaryTab[primaryTab] === tab.id
                    ? 'bg-primary-100 text-primary-700 font-medium'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Main Content */}
      <div className="w-full">
          {/* Summary Cards - Always Visible */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <Card className={`border-l-4 ${corpusGap <= 0 ? 'border-l-success-500' : 'border-l-danger-500'}`}>
              <CardContent className="py-3">
                <p className="text-xs text-slate-500">Projected Corpus</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(projectedCorpus, true)}</p>
                <p className="text-xs text-slate-400">At age {params.retirementAge}</p>
              </CardContent>
            </Card>
            <Card className={`border-l-4 ${corpusGap <= 0 ? 'border-l-success-500' : 'border-l-danger-500'}`}>
              <CardContent className="py-3">
                <p className="text-xs text-slate-500">Required Corpus</p>
                <p className="text-xl font-bold text-slate-800">{formatCurrency(requiredCorpus, true)}</p>
                <p className={`text-xs ${corpusGap > 0 ? 'text-danger-600' : 'text-success-600'}`}>
                  {corpusGap > 0 ? `Gap: ${formatCurrency(corpusGap, true)}` : `Surplus: ${formatCurrency(Math.abs(corpusGap), true)}`}
                </p>
              </CardContent>
            </Card>
            <Card className="border-l-4 border-l-primary-500">
              <CardContent className="py-3">
                <p className="text-xs text-slate-500">Monthly SIP</p>
                <p className="text-xl font-bold text-primary-600">{formatCurrency(monthlySIP, true)}</p>
                <p className="text-xs text-slate-400">
                  {params.sipStepup}% step-up to {formatCurrency(sipAtFullStepUp, true)}
                </p>
                {canStopEarly && optimalStopYear && (
                  <p className="text-xs text-amber-600 mt-1">
                    💡 Can stop step-up in year {optimalStopYear}
                  </p>
                )}
              </CardContent>
            </Card>
            <Card className="border-l-4 border-l-amber-500">
              <CardContent className="py-3">
                <p className="text-xs text-slate-500">Years to Retirement</p>
                <p className="text-xl font-bold text-amber-600">{params.retirementAge - params.currentAge}</p>
                <p className="text-xs text-slate-400">Year {CURRENT_YEAR + (params.retirementAge - params.currentAge)}</p>
              </CardContent>
            </Card>
          </div>

          {/* OVERVIEW SECTION */}
          {activeSection === 'overview' && (
            <div className="space-y-6">
              {/* Corpus Growth Chart */}
              <Card>
                <CardContent>
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold text-slate-800">Corpus Growth Projection</h3>
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                      <TrendingUp size={16} className="text-success-500" />
                      <span>Projected at {params.mfReturn}% return</span>
                    </div>
                  </div>
                  
                  {corpusChartData.length > 0 ? (
                    <div className="h-[350px]">
                      <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={corpusChartData}>
                          <defs>
                            <linearGradient id="corpusGradient" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                              <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                            </linearGradient>
                          </defs>
                          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                          <XAxis 
                            dataKey="year" 
                            stroke="#64748b" 
                            fontSize={11}
                            tickFormatter={(v) => `'${String(v).slice(-2)}`}
                            domain={['dataMin', 'dataMax']}
                            type="number"
                            allowDataOverflow={false}
                          />
                          <YAxis 
                            stroke="#64748b" 
                            fontSize={11}
                            tickFormatter={(v) => v >= 10000000 ? `${(v/10000000).toFixed(1)}Cr` : `${(v/100000).toFixed(0)}L`}
                          />
                          <Tooltip 
                            formatter={(value: number | undefined, name: string | undefined, props: any) => {
                              if (name === 'Maturity Events') {
                                const inflow = props?.payload?.totalInflow || 0;
                                if (inflow > 0) return [formatCurrency(inflow, true), 'Maturity Inflow'];
                                return null;
                              }
                              if (value === null || value === undefined) return null;
                              return [formatCurrency(value), name || ''];
                            }}
                            labelFormatter={(label) => {
                              const dataPoint = corpusChartData.find((d: any) => d.year === label);
                              return `Year ${label} (Age ${dataPoint?.age || ''})`;
                            }}
                          />
                          <Legend />
                          <Area 
                            type="monotone" 
                            dataKey="corpus" 
                            name="Total Corpus"
                            stroke="#10b981" 
                            fill="url(#corpusGradient)"
                            strokeWidth={2}
                          />
                          <Line
                            type="monotone"
                            dataKey="maturityMarker"
                            name="Maturity Events"
                            stroke="#f59e0b"
                            strokeWidth={0}
                            legendType="circle"
                            dot={(props: any) => {
                              if (props.payload?.maturityMarker === null || props.payload?.maturityMarker === undefined) return <></>;
                              return <circle cx={props.cx} cy={props.cy} r={8} fill="#ffffff" stroke="#f59e0b" strokeWidth={3} />;
                            }}
                            activeDot={{ r: 10, fill: '#f59e0b', stroke: '#ffffff', strokeWidth: 2 }}
                            isAnimationActive={false}
                            connectNulls={false}
                          />
                          <Line 
                            type="monotone" 
                            dataKey="mf" 
                            name="Mutual Funds"
                            stroke="#8b5cf6" 
                            strokeWidth={1.5}
                            dot={false}
                            connectNulls={false}
                          />
                          <Line 
                            type="monotone" 
                            dataKey="epf" 
                            name="EPF"
                            stroke="#3b82f6" 
                            strokeWidth={1}
                            strokeDasharray="5 5"
                            dot={false}
                            connectNulls={false}
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    </div>
                  ) : (
                    <div className="h-[350px] flex items-center justify-center text-slate-400">
                      <p>Add investments to see corpus growth projection</p>
                    </div>
                  )}
                  {maturityEvents.length > 0 && (
                    <div className="mt-4">
                      <div className="text-sm font-medium text-slate-700 mb-2">Maturity Events (not included in line)</div>
                      <div className="overflow-x-auto rounded-lg border border-slate-200">
                        <table className="w-full text-xs">
                          <thead className="bg-slate-100">
                            <tr>
                              <th className="px-3 py-2 text-left text-slate-600">Year</th>
                              <th className="px-3 py-2 text-right text-slate-600">Total Inflow</th>
                              <th className="px-3 py-2 text-right text-slate-600">Investments</th>
                              <th className="px-3 py-2 text-right text-slate-600">Insurance</th>
                              <th className="px-3 py-2 text-right text-slate-600">Money-Back</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100">
                            {maturityEvents.map((row: any) => (
                              <tr key={row.year}>
                                <td className="px-3 py-2 text-slate-700">{row.year}</td>
                                <td className="px-3 py-2 text-right font-mono text-emerald-600">{formatCurrency(row.totalInflow, true)}</td>
                                <td className="px-3 py-2 text-right font-mono text-slate-600">{formatCurrency(row.investmentMaturity, true)}</td>
                                <td className="px-3 py-2 text-right font-mono text-slate-600">{formatCurrency(row.insuranceMaturity, true)}</td>
                                <td className="px-3 py-2 text-right font-mono text-slate-600">{formatCurrency(row.moneyBackPayout, true)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Current Assets & Retirement Readiness */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Current Starting Balances */}
                <Card>
                  <CardContent>
                    <h3 className="text-lg font-semibold text-slate-800 mb-4">Current Investment Balances</h3>
                    {totalAssets > 0 ? (
                      <div className="space-y-3">
                        {/* Grid of balances */}
                        <div className="grid grid-cols-2 gap-2">
                          {[
                            { label: 'Mutual Funds', value: startingBalances.mutualFunds, color: 'bg-purple-500' },
                            { label: 'EPF', value: startingBalances.epf, color: 'bg-green-500' },
                            { label: 'PPF', value: startingBalances.ppf, color: 'bg-blue-500' },
                            { label: 'NPS', value: startingBalances.nps, color: 'bg-orange-500' },
                            { label: 'FD', value: startingBalances.fd, color: 'bg-yellow-500' },
                            { label: 'RD', value: startingBalances.rd, color: 'bg-amber-500' },
                            { label: 'Stocks', value: startingBalances.stocks, color: 'bg-cyan-500' },
                            { label: 'Cash', value: startingBalances.cash, color: 'bg-lime-500' },
                          ].filter(item => item.value > 0).map((item, i) => (
                            <div key={i} className="flex items-center justify-between p-2 bg-slate-50 rounded-lg">
                              <div className="flex items-center gap-2">
                                <div className={`w-2 h-2 rounded-full ${item.color}`} />
                                <span className="text-sm text-slate-600">{item.label}</span>
                              </div>
                              <span className="text-sm font-semibold">{formatCurrency(item.value || 0)}</span>
                            </div>
                          ))}
                        </div>
                        
                        {/* Emergency Fund if exists */}
                        {startingBalances.emergencyFund > 0 && (
                          <div className="flex items-center justify-between p-2 bg-amber-50 border border-amber-200 rounded-lg">
                            <div className="flex items-center gap-2">
                              <span>🛡️</span>
                              <span className="text-sm text-amber-700">Emergency Fund (excluded)</span>
                            </div>
                            <span className="text-sm font-semibold text-amber-700">
                              {formatCurrency(startingBalances.emergencyFund)}
                            </span>
                          </div>
                        )}
                        
                        {/* Total */}
                        <div className="flex items-center justify-between p-3 bg-primary-50 border border-primary-200 rounded-lg">
                          <span className="font-medium text-primary-700">Total in Corpus</span>
                          <span className="text-lg font-bold text-primary-700">{formatCurrency(totalAssets, true)}</span>
                        </div>
                      </div>
                    ) : (
                      <div className="text-center py-6">
                        <p className="text-slate-400 mb-2">No investments found</p>
                        <p className="text-sm text-slate-400">Add investments to see your current allocation</p>
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* Retirement Readiness */}
                <Card>
                  <CardContent>
                    <h3 className="text-lg font-semibold text-slate-800 mb-4">Retirement Readiness</h3>
                    <div className="text-center py-4">
                      <div className={`inline-flex items-center justify-center w-24 h-24 rounded-full mb-4 ${
                        corpusGap <= 0 ? 'bg-success-100' : 'bg-danger-100'
                      }`}>
                        {corpusGap <= 0 ? (
                          <CheckCircle size={48} className="text-success-500" />
                        ) : (
                          <AlertTriangle size={48} className="text-danger-500" />
                        )}
                      </div>
                      <p className={`text-xl font-bold ${corpusGap <= 0 ? 'text-success-600' : 'text-danger-600'}`}>
                        {corpusGap <= 0 ? 'On Track!' : 'Needs Attention'}
                      </p>
                      <p className="text-sm text-slate-500 mt-2">
                        {corpusGap <= 0 
                          ? `You have a surplus of ${formatCurrency(Math.abs(corpusGap))}`
                          : `You need ${formatCurrency(corpusGap)} more to meet your goal`
                        }
                      </p>
                      {canStopEarly && optimalStopYear && (
                        <p className="text-xs text-amber-600 mt-2">
                          💡 Can stop SIP step-up in year {optimalStopYear}!
                        </p>
                      )}
                      {corpusGap > 0 && !canStopEarly && (
                        <p className="text-xs text-amber-600 mt-2">
                          💡 Continue {params.sipStepup}% SIP step-up to meet target
                        </p>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          )}

          {/* INCOME STRATEGY SECTION */}
          {activeSection === 'income' && (
            <Card>
              <CardContent>
                <h3 className="text-lg font-semibold text-slate-800 mb-4">Income at Retirement</h3>
                {totalRentalIncome > 0 && (
                  <div className="mb-4 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded-lg px-3 py-2">
                    Includes rental income: <strong>{formatCurrency(totalRentalIncome, true)}/mo</strong>
                  </div>
                )}
                {annuityMonthlyIncome > 0 && (
                  <div className="mb-4 text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-lg px-3 py-2">
                    Includes annuity income: <strong>{formatCurrency(annuityMonthlyIncome, true)}/mo</strong>
                  </div>
                )}
                
                {/* Income Strategy Cards */}
                <div className="grid grid-cols-3 gap-4 mb-6">
                  {INCOME_STRATEGIES.map((strategy) => {
                    const isSelected = params.incomeStrategy === strategy.value;
                    const totalIncome = incomeStrategies[strategy.value as keyof typeof incomeStrategies] || 0;
                    const corpusIncome = totalIncome - annuityMonthlyIncome - totalRentalIncome;
                    const strategyDescription = strategy.value === 'SUSTAINABLE'
                      ? `Grows at ~${Math.round(corpusReturnRate)}% with ${Math.round(withdrawalRate)}% withdrawals`
                      : strategy.value === 'SAFE_4_PERCENT'
                        ? `Withdraw 4% of initial corpus yearly, inflation-adjusted; assumes ~${Math.round(corpusReturnRate)}% return`
                        : strategy.description;
                    
                    return (
                      <div 
                        key={strategy.value}
                        onClick={() => selectIncomeStrategy(strategy.value)}
                        className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                          isSelected 
                            ? 'border-primary-500 bg-primary-50 shadow-lg' 
                            : 'border-slate-200 hover:border-slate-300 bg-white'
                        }`}
                      >
                        <div className={`text-sm font-medium ${isSelected ? 'text-primary-600' : 'text-slate-500'}`}>
                          {strategy.label}
                        </div>
                        <div className={`text-2xl font-bold my-1 ${isSelected ? 'text-primary-700' : 'text-slate-700'}`}>
                          {formatCurrency(totalIncome)}<span className="text-sm font-normal">/mo</span>
                        </div>
                        {/* Income Breakdown */}
                        <div className="mt-2 space-y-1">
                          <div className="flex justify-between text-xs">
                            <span className="text-slate-500">From Corpus:</span>
                            <span className={isSelected ? 'text-primary-600' : 'text-slate-600'}>{formatCurrency(corpusIncome)}</span>
                          </div>
                          {totalRentalIncome > 0 && (
                            <div className="flex justify-between text-xs">
                              <span className="text-blue-600">From Rental:</span>
                              <span className="text-blue-600">{formatCurrency(totalRentalIncome)}</span>
                            </div>
                          )}
                          {annuityMonthlyIncome > 0 && (
                            <div className="flex justify-between text-xs">
                              <span className="text-emerald-600">From Annuity:</span>
                              <span className="text-emerald-600">{formatCurrency(annuityMonthlyIncome)}</span>
                            </div>
                          )}
                        </div>
                        <div className="text-xs text-slate-400 mt-2">{strategyDescription}</div>
                        {strategy.value === 'SAFE_4_PERCENT' && (
                          <div className="text-[11px] text-slate-400 mt-1">
                            Uses a fixed, inflation-adjusted withdrawal from the initial corpus, so withdrawals don’t shrink when the corpus dips.
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Income Projection Table */}
                {incomeProjection.length > 0 && (
                  <div className="max-h-80 overflow-y-auto rounded-lg border border-slate-200">
                    <table className="w-full text-sm">
                      <thead className="sticky top-0 bg-slate-100">
                        <tr>
                          <th className="px-4 py-2 text-left font-medium text-slate-600">Year</th>
                          <th className="px-4 py-2 text-left font-medium text-slate-600">Age</th>
                          <th className="px-4 py-2 text-right font-medium text-slate-600">Corpus</th>
                          <th className="px-4 py-2 text-right font-medium text-slate-600">From Corpus</th>
                          {totalRentalIncome > 0 && (
                            <th className="px-4 py-2 text-right font-medium text-blue-600">From Rental (5% Annual Increment)</th>
                          )}
                          {annuityMonthlyIncome > 0 && (
                            <th className="px-4 py-2 text-right font-medium text-emerald-600">From Annuity</th>
                          )}
                          <th className="px-4 py-2 text-right font-medium text-slate-600">Total Income</th>
                          <th className="px-4 py-2 text-right font-medium text-amber-600">Required Expense</th>
                          <th className="px-4 py-2 text-right font-medium text-danger-600">Shortfall</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {incomeProjection.slice(0, 25).map((proj: any, i: number) => {
							
                          let totalMonthlyIncome = proj.monthlyIncome
                            ?? ((proj.withdrawal || 0) / 12);
                          totalMonthlyIncome = totalMonthlyIncome > 0
                            ? totalMonthlyIncome
                            : (params.incomeStrategy === 'SIMPLE_DEPLETION'
                              ? (proj.corpus && retirementYears > 0 ? (proj.corpus / Math.max(retirementYears - (proj.year || 0), 1) / 12) : 0)
                              : (params.incomeStrategy === 'SAFE_4_PERCENT'
                                ? (projectedCorpus * 0.04) / 12
                                : (proj.corpus || 0) * (withdrawalRate / 100) / 12));
                          const annuityIncome = proj.annuityMonthlyIncome || annuityMonthlyIncome || 0;
                          const corpusWithdrawal = totalMonthlyIncome - annuityIncome - proj.rentalMonthlyIncome;
                          
                          // Calculate required expense for this year (with inflation)
                          const baseMonthlyExpense = gapAnalysis.monthlyExpensesAtRetirement || gapAnalysis.inflatedMonthlyExpenses || 0;
                          const goalOutflowMonthly = (proj.goalOutflow || 0) / 12;
                          const requiredExpense = (baseMonthlyExpense * Math.pow(1 + params.inflation / 100, proj.year || 0)) + goalOutflowMonthly;
                          
                          // Calculate shortfall
                          const shortfall = Math.max(0, requiredExpense - totalMonthlyIncome);

                          return (
                          <tr key={i} className={`hover:bg-slate-50 ${i === 0 ? 'bg-success-50 font-semibold' : ''}`}>
                            <td className="px-4 py-2">{CURRENT_YEAR + (params.retirementAge - params.currentAge) + proj.year}</td>
                            <td className="px-4 py-2">{params.retirementAge + proj.year}</td>
                            <td className="px-4 py-2 text-right">{formatCurrency(proj.corpus)}</td>
                            <td className="px-4 py-2 text-right text-slate-600">{formatCurrency(corpusWithdrawal)}</td>
                            {proj.rentalMonthlyIncome > 0 && (
                              <td className="px-4 py-2 text-right text-blue-600">{formatCurrency(proj.rentalMonthlyIncome)}</td>
                            )}
                            {annuityMonthlyIncome > 0 && (
                              <td className="px-4 py-2 text-right text-emerald-600">{formatCurrency(annuityIncome)}</td>
                            )}
                            <td className="px-4 py-2 text-right font-medium text-primary-600">
                              {formatCurrency(totalMonthlyIncome)}
                            </td>
                            <td className="px-4 py-2 text-right text-amber-600">
                              <div className="flex flex-col items-end">
                                <span>{formatCurrency(requiredExpense)}</span>
                                {goalOutflowMonthly > 0 && (
                                  <span className="text-[11px] text-indigo-600 mt-0.5">
                                    (includes {formatCurrency(goalOutflowMonthly)} goal)
                                  </span>
                                )}
                              </div>
                            </td>
                            <td className={`px-4 py-2 text-right font-medium ${shortfall > 0 ? 'text-danger-600' : 'text-success-600'}`}>
                              {shortfall > 0 ? formatCurrency(shortfall) : '✓'}
                            </td>
                          </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* GAP ANALYSIS SECTION */}
          {activeSection === 'gap' && (
            <div className="space-y-6">
              <Card>
                <CardContent>
                  <h3 className="text-lg font-semibold text-slate-800 mb-4">GAP Analysis</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-3">
                      <div className="flex justify-between items-center p-3 bg-slate-50 rounded-lg">
                        <span className="text-slate-600">Current Monthly Expenses</span>
                        <span className="font-semibold">{formatCurrency(gapAnalysis.currentMonthlyExpenses || 0)}</span>
                      </div>
                      <div className="flex justify-between items-center p-3 bg-slate-50 rounded-lg">
                        <span className="text-slate-600">Expenses at Retirement</span>
                        <span className="font-semibold">{formatCurrency(gapAnalysis.monthlyExpensesAtRetirement || 0)}</span>
                      </div>
                      <div className="flex justify-between items-center p-3 bg-slate-50 rounded-lg">
                        <span className="text-slate-600">Annual at Retirement</span>
                        <span className="font-semibold">{formatCurrency(gapAnalysis.annualExpensesAtRetirement || 0)}</span>
                      </div>
                      <div className="flex justify-between items-center p-3 bg-primary-50 rounded-lg">
                        <span className="text-primary-600 font-medium">Income Strategy</span>
                        <span className="font-semibold text-primary-700">
                          {INCOME_STRATEGIES.find(s => s.value === params.incomeStrategy)?.label}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center justify-center">
                      <div className={`p-8 rounded-2xl text-center ${
                        corpusGap > 0 ? 'bg-danger-50 border-2 border-danger-200' : 'bg-success-50 border-2 border-success-200'
                      }`}>
                        {corpusGap > 0 ? (
                          <AlertTriangle className="mx-auto mb-2 text-danger-500" size={40} />
                        ) : (
                          <CheckCircle className="mx-auto mb-2 text-success-500" size={40} />
                        )}
                        <p className={`text-lg font-semibold ${corpusGap > 0 ? 'text-danger-700' : 'text-success-700'}`}>
                          {corpusGap > 0 ? 'Corpus Shortfall' : 'On Track!'}
                        </p>
                        <p className={`text-3xl font-bold my-2 ${corpusGap > 0 ? 'text-danger-600' : 'text-success-600'}`}>
                          {formatCurrency(Math.abs(corpusGap))}
                        </p>
                        <p className={`text-sm ${corpusGap > 0 ? 'text-danger-600' : 'text-success-600'}`}>
                          {corpusGap > 0 ? 'Additional savings needed' : 'Surplus above goal'}
                        </p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* SIP Step-Up Optimization */}
              {monthlySIP > 0 && (
                <Card className={`border-2 ${canStopEarly ? 'bg-amber-50 border-amber-200' : 'bg-emerald-50 border-emerald-200'}`}>
                  <CardContent>
                    <h4 className={`font-semibold mb-3 ${canStopEarly ? 'text-amber-800' : 'text-emerald-800'}`}>
                      💡 SIP Step-up Analysis ({params.sipStepup}% annual increase)
                    </h4>
                    <div className="grid grid-cols-2 gap-4 mb-4">
                      <div className="bg-white p-3 rounded-lg border">
                        <p className="text-xs text-slate-500">Starting SIP</p>
                        <p className="text-lg font-bold text-primary-600">{formatCurrency(monthlySIP)}/mo</p>
                      </div>
                      <div className="bg-white p-3 rounded-lg border">
                        <p className="text-xs text-slate-500">SIP at Retirement</p>
                        <p className="text-lg font-bold text-purple-600">{formatCurrency(sipAtFullStepUp)}/mo</p>
                      </div>
                    </div>
                    
                    {canStopEarly && optimalStopYear ? (
                      <div className="bg-amber-100 p-4 rounded-lg">
                        <p className="font-medium text-amber-900 mb-2">
                          🎯 You can stop step-up in year {optimalStopYear} ({CURRENT_YEAR + optimalStopYear})
                        </p>
                        <p className="text-sm text-amber-800">
                          Corpus at optimal stop: <strong>{formatCurrency(corpusAtOptimalStop)}</strong>
                        </p>
                        <p className="text-sm text-amber-800">
                          Monthly SIP savings after stop: <strong>{formatCurrency(sipAtFullStepUp - (sipStepUpOptimization.sipAtOptimalStop || sipAtFullStepUp))}/mo</strong>
                        </p>
                      </div>
                    ) : (
                      <div className="bg-emerald-100 p-4 rounded-lg">
                        <p className="text-sm text-emerald-800">
                          ✅ Continue SIP step-up until retirement for optimal corpus growth
                        </p>
                      </div>
                    )}
                    {sipScenarios.length > 0 && (
                      <div className="flex justify-end mt-4">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setShowSipMatrixModal(true)}
                        >
                          View full SIP analysis
                        </Button>
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}
              <Modal
                isOpen={showSipMatrixModal}
                onClose={() => setShowSipMatrixModal(false)}
                title="SIP Step-Up Scenario Matrix"
                size="xl"
              >
                {sipScenarios.length > 0 ? (
                  <div className="space-y-4">
                    <div className="text-sm text-slate-600">
                      Target corpus: <strong>{formatCurrency(sipStepUpOptimization.requiredCorpus || requiredCorpus)}</strong>
                    </div>
                    <div className="overflow-x-auto border border-slate-200 rounded-lg">
                      <table className="w-full text-sm">
                        <thead className="bg-slate-100">
                          <tr>
                            <th className="px-3 py-2 text-left font-medium text-slate-600">Stop Year</th>
                            <th className="px-3 py-2 text-right font-medium text-slate-600">Final SIP</th>
                            <th className="px-3 py-2 text-right font-medium text-slate-600">Projected Corpus</th>
                            <th className="px-3 py-2 text-right font-medium text-slate-600">vs Target</th>
                            <th className="px-3 py-2 text-center font-medium text-slate-600">Meets?</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {sipScenarios.map((scenario: any, idx: number) => {
                            const isOptimal = optimalStopYear !== null && scenario.stopYear === optimalStopYear;
                            const calendarYear = CURRENT_YEAR + scenario.stopYear;
                            const age = params.currentAge + scenario.stopYear;
                            const surplus = scenario.surplus || 0;
                            return (
                              <tr key={idx} className={isOptimal ? 'bg-emerald-50 font-semibold' : ''}>
                                <td className="px-3 py-2 text-slate-700">
                                  {calendarYear} (Age {age}) {isOptimal ? '← Optimal' : ''}
                                </td>
                                <td className="px-3 py-2 text-right">{formatCurrency(scenario.finalSipAtStop || 0)}</td>
                                <td className="px-3 py-2 text-right">{formatCurrency(scenario.projectedCorpus || 0)}</td>
                                <td className={`px-3 py-2 text-right ${surplus >= 0 ? 'text-emerald-600' : 'text-danger-600'}`}>
                                  {surplus >= 0 ? '+' : ''}{formatCurrency(surplus)}
                                </td>
                                <td className="px-3 py-2 text-center">
                                  {scenario.meetsTarget ? '✓' : '✗'}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">No SIP scenario data available.</p>
                )}
              </Modal>
            </div>
          )}

          {/* MATURING BEFORE RETIREMENT SECTION */}
          {activeSection === 'maturing' && (
            <Card>
              <CardContent>
                <h3 className="text-lg font-semibold text-slate-800 mb-4">Maturing Before Retirement</h3>
                {(!maturingBeforeRetirement.maturingInvestments?.length && !maturingBeforeRetirement.maturingInsurance?.length && !moneyBackPayouts.length) ? (
                  <div className="text-center py-6 text-slate-400">
                    <div className="text-4xl mb-2">📅</div>
                    <div>No investments or policies maturing before retirement</div>
                    <div className="text-xs mt-1">FD, RD, PPF with maturity dates will appear here</div>
                  </div>
                ) : (
                  <div className="space-y-6">
                    <div className="p-4 bg-gradient-to-r from-emerald-50 to-teal-50 rounded-xl border border-emerald-200">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-sm text-emerald-600 font-medium">Available for Reinvestment</div>
                          <div className="text-2xl font-bold text-emerald-700">
                            {formatCurrency(maturingBeforeRetirement.totalMaturingBeforeRetirement || 0)}
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-xs text-slate-500">Before retirement</div>
                          <div className="text-sm text-slate-600">
                            {(maturingBeforeRetirement.investmentCount || 0)} investments, {(maturingBeforeRetirement.insuranceCount || 0)} policies, {moneyBackCount} payouts
                          </div>
                        </div>
                      </div>
                    </div>

                    {maturingBeforeRetirement.maturingInvestments?.length > 0 && (
                      <div>
                        <h4 className="text-sm font-semibold text-slate-700 mb-2">💰 Maturing Investments</h4>
                        <div className="overflow-x-auto rounded-lg border border-slate-200">
                          <table className="w-full text-sm">
                            <thead className="bg-slate-100">
                              <tr>
                                <th className="px-3 py-2 text-left text-slate-600">Investment</th>
                                <th className="px-3 py-2 text-left text-slate-600">Type</th>
                                <th className="px-3 py-2 text-right text-slate-600">Matures</th>
                                <th className="px-3 py-2 text-right text-slate-600">Current</th>
                                <th className="px-3 py-2 text-right text-slate-600">Expected Value</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                              {maturingBeforeRetirement.maturingInvestments.map((inv: any, i: number) => (
                                <tr key={i} className="hover:bg-slate-50">
                                  <td className="px-3 py-2 font-medium text-slate-800">{inv.name}</td>
                                  <td className="px-3 py-2">
                                    <span className={`px-2 py-0.5 text-xs rounded-full ${getTypeBadgeClass(inv.type)}`}>
                                      {inv.type || 'OTHER'}
                                    </span>
                                  </td>
                                  <td className="px-3 py-2 text-right text-slate-600">
                                    {formatDate(inv.maturityDate)}
                                    <div className="text-xs text-slate-400">{inv.yearsToMaturity}y</div>
                                  </td>
                                  <td className="px-3 py-2 text-right font-mono text-slate-600">{formatCurrency(inv.currentValue || 0)}</td>
                                  <td className="px-3 py-2 text-right font-mono font-semibold text-emerald-600">{formatCurrency(inv.expectedMaturityValue || 0)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}

                    {maturingBeforeRetirement.maturingInsurance?.length > 0 && (
                      <div>
                        <h4 className="text-sm font-semibold text-slate-700 mb-2">🛡️ Maturing Insurance Policies</h4>
                        <div className="overflow-x-auto rounded-lg border border-slate-200">
                          <table className="w-full text-sm">
                            <thead className="bg-slate-100">
                              <tr>
                                <th className="px-3 py-2 text-left text-slate-600">Policy</th>
                                <th className="px-3 py-2 text-left text-slate-600">Type</th>
                                <th className="px-3 py-2 text-right text-slate-600">Matures</th>
                                <th className="px-3 py-2 text-right text-slate-600">Fund Value</th>
                                <th className="px-3 py-2 text-right text-slate-600">Expected Maturity</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                              {maturingBeforeRetirement.maturingInsurance.map((ins: any, i: number) => (
                                <tr key={i} className="hover:bg-slate-50">
                                  <td className="px-3 py-2 font-medium text-slate-800">{ins.name}</td>
                                  <td className="px-3 py-2">
                                    <span className="px-2 py-0.5 text-xs rounded-full bg-purple-100 text-purple-700">
                                      {ins.type || 'OTHER'}
                                    </span>
                                  </td>
                                  <td className="px-3 py-2 text-right text-slate-600">
                                    {formatDate(ins.maturityDate)}
                                    <div className="text-xs text-slate-400">{ins.yearsToMaturity}y</div>
                                  </td>
                                  <td className="px-3 py-2 text-right font-mono text-slate-600">{formatCurrency(ins.currentFundValue || 0)}</td>
                                  <td className="px-3 py-2 text-right font-mono font-semibold text-emerald-600">{formatCurrency(ins.expectedMaturityValue || 0)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}

                    {moneyBackPayouts.length > 0 && (
                      <div>
                        <h4 className="text-sm font-semibold text-slate-700 mb-2">💵 Money-Back Payouts</h4>
                        <div className="overflow-x-auto rounded-lg border border-slate-200">
                          <table className="w-full text-sm">
                            <thead className="bg-slate-100">
                              <tr>
                                <th className="px-3 py-2 text-left text-slate-600">Policy</th>
                                <th className="px-3 py-2 text-right text-slate-600">Year</th>
                                <th className="px-3 py-2 text-right text-slate-600">Policy Year</th>
                                <th className="px-3 py-2 text-right text-slate-600">Amount</th>
                                <th className="px-3 py-2 text-left text-slate-600">Notes</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                              {moneyBackPayouts.map((payout: any, i: number) => (
                                <tr key={i} className="hover:bg-slate-50">
                                  <td className="px-3 py-2 font-medium text-slate-800">{payout.policyName}</td>
                                  <td className="px-3 py-2 text-right text-slate-600">{payout.calendarYear}</td>
                                  <td className="px-3 py-2 text-right text-slate-600">{payout.policyYear || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono font-semibold text-emerald-600">{formatCurrency(payout.amount || 0)}</td>
                                  <td className="px-3 py-2 text-slate-600">{payout.description || ''}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}

                    <div className="p-3 bg-blue-50 rounded-lg border border-blue-200 text-xs text-blue-700">
                      💡 <strong>Tip:</strong> These funds become available before retirement and can be reinvested for higher returns.
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* EXPENSE PROJECTION SECTION */}
          {activeSection === 'expense-projection' && (
            <Card>
              <CardContent>
                <h3 className="text-lg font-semibold text-slate-800 mb-4">Expense Projection</h3>
                {expenseProjection.length > 0 ? (
                  <div className="space-y-4">
                    {monthlyInsurancePremiums > 0 ? (
                      <div className="p-3 bg-blue-50 rounded-lg border border-blue-200 text-xs text-blue-700 flex justify-between items-center">
                        <span>💡 <strong>Expense Components:</strong></span>
                        <span>Household {formatCurrency(householdExpenses)} + Insurance {formatCurrency(monthlyInsurancePremiums)}/mo</span>
                      </div>
                    ) : householdExpenses > 0 ? (
                      <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-xs text-amber-700 flex justify-between items-center">
                        <span>⚠️ Only household expenses. Add Personal Health Insurance for complete projection.</span>
                        <Button variant="ghost" size="sm" onClick={() => window.location.href = '/insurance'}>
                          Add Insurance →
                        </Button>
                      </div>
                    ) : null}
                    <div className="overflow-x-auto rounded-lg border border-slate-200">
                      <table className="w-full text-sm">
                        <thead className="bg-slate-100">
                          <tr>
                            <th className="px-3 py-2 text-left text-slate-600">Year</th>
                            <th className="px-3 py-2 text-right text-slate-600">Monthly Expense</th>
                            <th className="px-3 py-2 text-right text-slate-600">Yearly Expense</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {expenseProjection.map((proj: any, i: number) => {
                            const insurancePart = proj.insurancePremium || 0;
                            const householdPart = proj.householdExpense || proj.monthlyExpense || 0;
                            return (
                              <tr key={i} className={`${proj.label ? 'bg-primary-50 font-semibold' : ''} hover:bg-slate-50`}>
                                <td className="px-3 py-2 text-slate-700">
                                  {proj.label || `Year ${proj.year}`}
                                </td>
                                <td className="px-3 py-2 text-right">
                                  <div className="font-mono text-slate-800">{formatCurrency(proj.monthlyExpense || 0)}</div>
                                  {insurancePart > 0 && (
                                    <div className="text-xs text-slate-500">
                                      ({formatCurrency(householdPart)} + {formatCurrency(insurancePart)} ins)
                                    </div>
                                  )}
                                </td>
                                <td className="px-3 py-2 text-right font-mono text-slate-700">
                                  {formatCurrency(proj.yearlyExpense || 0)}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-6 text-slate-400">
                    <div className="mb-2">No expenses recorded</div>
                    <Button variant="ghost" size="sm" onClick={() => window.location.href = '/expenses'}>
                      Add monthly expenses →
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* ENDING EXPENSES SECTION */}
          {activeSection === 'ending-expenses' && (
            <Card>
              <CardContent>
                <h3 className="text-lg font-semibold text-slate-800 mb-4">Ending Expenses</h3>
                {endingExpenses.length > 0 ? (
                  <div className="space-y-6">
                    <div className="p-4 bg-gradient-to-r from-purple-50 to-indigo-50 rounded-xl border border-purple-200">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-sm text-purple-600 font-medium">💡 Investment Opportunity from Ending Expenses</div>
                          <div className="text-2xl font-bold text-purple-700">
                            {formatCurrency(potentialCorpusFromFreedUpExpenses)}
                          </div>
                          <div className="text-xs text-purple-500">Potential additional corpus if invested</div>
                        </div>
                        <div className="text-right">
                          <div className="text-sm text-slate-600">{endingExpenses.length} expense(s) ending before retirement</div>
                          <div className="text-lg font-semibold text-slate-700">{formatCurrency(monthlyFreedUpByRetirement)}/mo freed up</div>
                        </div>
                      </div>
                    </div>

                    <div className="overflow-x-auto rounded-lg border border-slate-200">
                      <table className="w-full text-sm">
                        <thead className="bg-slate-100">
                          <tr>
                            <th className="px-3 py-2 text-left text-slate-600">Expense</th>
                            <th className="px-3 py-2 text-left text-slate-600">Category</th>
                            <th className="px-3 py-2 text-right text-slate-600">Ends</th>
                            <th className="px-3 py-2 text-right text-slate-600">Monthly Freed</th>
                            <th className="px-3 py-2 text-right text-slate-600">Potential Corpus</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {endingExpenses
                            .slice()
                            .sort((a: any, b: any) => (a.endYear || 0) - (b.endYear || 0))
                            .map((exp: any, i: number) => (
                              <tr key={i} className="hover:bg-slate-50">
                                <td className="px-3 py-2 font-medium text-slate-800">
                                  {exp.name}
                                  {exp.dependentName && (
                                    <div className="text-xs text-slate-500">For {exp.dependentName}</div>
                                  )}
                                </td>
                                <td className="px-3 py-2 text-slate-600">
                                  {(exp.category || 'OTHER').replace(/_/g, ' ')}
                                </td>
                                <td className="px-3 py-2 text-right text-slate-600">
                                  {exp.endYear || '-'}
                                  {exp.yearsRemaining !== undefined && (
                                    <div className="text-xs text-slate-400">{exp.yearsRemaining}y left</div>
                                  )}
                                </td>
                                <td className="px-3 py-2 text-right font-mono text-slate-700">
                                  {formatCurrency(exp.monthlyAmount || 0)}
                                </td>
                                <td className="px-3 py-2 text-right font-mono font-semibold text-emerald-600">
                                  +{formatCurrency(exp.potentialCorpusIfInvested || 0)}
                                </td>
                              </tr>
                            ))}
                        </tbody>
                      </table>
                    </div>

                    <div className="p-4 bg-emerald-50 rounded-lg border border-emerald-200">
                      <h4 className="font-semibold text-emerald-800">🚀 Recommended Action</h4>
                      <p className="text-sm text-emerald-600 mt-1">
                        When these expenses end, set up automatic SIP transfers for the freed-up amount to boost your corpus.
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-6 text-slate-400">
                    <div className="mb-2">No time-bound expenses ending before retirement</div>
                    <Button variant="ghost" size="sm" onClick={() => window.location.href = '/expenses'}>
                      Add time-bound expenses →
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* YEAR-BY-YEAR MATRIX */}
          {activeSection === 'matrix' && (
            <Card>
              <CardContent>
                <div 
                  className="flex items-center justify-between cursor-pointer mb-4"
                  onClick={() => setMatrixExpanded(!matrixExpanded)}
                >
                  <h3 className="text-lg font-semibold text-slate-800">Year-by-Year Projection</h3>
                  <span className="text-slate-500">
                    {matrixExpanded ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
                  </span>
                </div>
                
                <div className={`overflow-x-auto ${matrixExpanded ? '' : 'max-h-48 overflow-y-hidden'}`}>
                  <table className="w-full min-w-[900px] text-sm">
                    <thead className="bg-slate-50 sticky top-0">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-600">Year</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-600">Age</th>
                        <th className="px-3 py-2 text-right font-semibold text-blue-600">
                          <div>PPF</div>
                          <div className="text-xs font-normal text-slate-400">({params.ppfReturn}%)</div>
                        </th>
                        <th className="px-3 py-2 text-right font-semibold text-green-600">
                          <div>EPF</div>
                          <div className="text-xs font-normal text-slate-400">({params.epfReturn}%)</div>
                        </th>
                        <th className="px-3 py-2 text-right font-semibold text-purple-600">
                          <div>MF</div>
                          <div className="text-xs font-normal text-slate-400">({params.mfReturn}%)</div>
                        </th>
                        <th className="px-3 py-2 text-right font-semibold text-slate-600">Other</th>
                        <th className="px-3 py-2 text-right font-semibold text-amber-600">SIP/mo</th>
                        <th className="px-3 py-2 text-center font-semibold text-indigo-600">Step-Up</th>
                        <th className="px-3 py-2 text-right font-semibold text-emerald-600">Inflows</th>
                        <th className="px-3 py-2 text-right font-semibold text-danger-600">Goal Outflow</th>
                        <th className="px-3 py-2 text-right font-semibold text-slate-600">Required Corpus</th>
                        <th className="px-3 py-2 text-right font-semibold text-primary-600 bg-primary-50">Net Corpus</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {matrix.length > 0 ? matrix.map((row: any, i: number) => {
                        const yearIndex = typeof row.year === 'number' ? row.year - CURRENT_YEAR : i;
                        const isOptimalStopYear = optimalStopYear !== null && yearIndex === optimalStopYear;
                        const isAfterStopYear = optimalStopYear !== null && yearIndex > optimalStopYear;
                        const stepUpEligible = yearIndex >= params.effectiveFromYear;
                        const isStepUpActive = row.sipStepUpActive ?? (stepUpEligible && !isOptimalStopYear);
                        const ifStopped = calculateIfStopped(i);
                        
                        return (
                          <tr key={i} className={`hover:bg-slate-50 ${row.age === params.retirementAge ? 'bg-amber-50 font-semibold' : ''} ${isOptimalStopYear ? 'bg-amber-100' : ''}`}>
                            <td className="px-3 py-2">{row.year}</td>
                            <td className="px-3 py-2">{row.age}</td>
                            <td className="px-3 py-2 text-right text-blue-600">{formatCurrency(row.ppfBalance || 0)}</td>
                            <td className="px-3 py-2 text-right text-green-600">{formatCurrency(row.epfBalance || 0)}</td>
                            <td className="px-3 py-2 text-right text-purple-600">{formatCurrency(row.mfBalance || 0)}</td>
                            <td className="px-3 py-2 text-right text-slate-600">{formatCurrency(row.otherLiquidBalance || 0)}</td>
                            <td className="px-3 py-2 text-right">
                              <div className="flex flex-col items-end">
                                <span className={isAfterStopYear ? 'text-slate-600' : 'text-amber-600'}>
                                  {formatCurrency(row.mfSip || 0)}/mo
                                </span>
                                {isAfterStopYear && ifStopped?.flatSip ? (
                                  <span className="text-xs text-slate-400">
                                    ↘ {formatCurrency(ifStopped.flatSip)}/mo
                                  </span>
                                ) : null}
                              </div>
                            </td>
                            <td className="px-3 py-2 text-center">
                              {isOptimalStopYear ? (
                                <span className="text-xs text-amber-600 font-medium">🛑 Stop</span>
                              ) : isStepUpActive ? (
                                <span className="text-xs text-emerald-600">+{params.sipStepup}%</span>
                              ) : (
                                <span className="text-xs text-slate-400">-</span>
                              )}
                            </td>
                            {/* Inflows Column */}
                            <td className="px-3 py-2 text-right">
                              {(row.totalInflow || 0) > 0 ? (
                                <div className="group relative">
                                  <span className="text-emerald-600 font-medium">+{formatCurrency(row.totalInflow)}</span>
                                  {/* Tooltip showing breakdown */}
                                  <div className="absolute right-0 bottom-full mb-1 hidden group-hover:block z-10 bg-slate-800 text-white text-xs rounded-lg p-2 whitespace-nowrap shadow-lg">
                                    <div className="font-semibold mb-1">Inflow Breakdown:</div>
                                    {(row.insuranceMaturity || 0) > 0 && (
                                      <div>Insurance Maturity: {formatCurrency(row.insuranceMaturity)}</div>
                                    )}
                                    {(row.investmentMaturity || 0) > 0 && (
                                      <div>Investment Maturity: {formatCurrency(row.investmentMaturity)}</div>
                                    )}
                                    {(row.moneyBackPayout || 0) > 0 && (
                                      <div>Money-Back Payout: {formatCurrency(row.moneyBackPayout)}</div>
                                    )}
                                  </div>
                                </div>
                              ) : (
                                <span className="text-slate-400">-</span>
                              )}
                            </td>
                            {/* Goals Column - shows outflow amount (already included in Net Corpus) */}
                            <td className="px-3 py-2 text-right">
                              {(row.goalOutflow || 0) > 0 ? (
                                <div className="flex items-center justify-end gap-1">
                                  <span className="text-danger-600">{formatCurrency(row.goalOutflow)}</span>
                                  {(row.totalInflow || 0) > 0 && (
                                    <div className="group relative">
                                      <span className="text-amber-500 cursor-help" title="Goal partially/fully offset by inflow">ℹ️</span>
                                      <div className="absolute right-0 bottom-full mb-1 hidden group-hover:block z-10 bg-amber-50 border border-amber-200 text-amber-800 text-xs rounded-lg p-2 whitespace-nowrap shadow-lg">
                                        <div className="font-semibold">Net impact on corpus:</div>
                                        <div className="text-danger-600">Goal outflow: {formatCurrency(row.goalOutflow)}</div>
                                        <div className="text-emerald-600">Inflow: {formatCurrency(row.totalInflow)}</div>
                                        <div className="border-t border-amber-200 mt-1 pt-1 font-semibold">
                                          Net: {(row.totalInflow || 0) >= (row.goalOutflow || 0) 
                                            ? <span className="text-emerald-600">+{formatCurrency((row.totalInflow || 0) - (row.goalOutflow || 0))}</span>
                                            : <span className="text-danger-600">{formatCurrency((row.goalOutflow || 0) - (row.totalInflow || 0))} used from corpus</span>
                                          }
                                        </div>
                                      </div>
                                    </div>
                                  )}
                                </div>
                              ) : (
                                <span className="text-slate-400">-</span>
                              )}
                            </td>
                            <td className="px-3 py-2 text-right">
                              <div className="flex flex-col items-end gap-1 text-xs">
                                <div className="flex items-center gap-2">
                                  <span className="text-slate-500">Sustainable</span>
                                  <span className="text-slate-700">{formatCurrency(row.requiredCorpusByStrategy?.SUSTAINABLE || 0, true)}</span>
                                  {row.canRetireByStrategy?.SUSTAINABLE && (
                                    <span className="text-emerald-600">✅</span>
                                  )}
                                </div>
                                <div className="flex items-center gap-2">
                                  <span className="text-slate-500">4% Rule</span>
                                  <span className="text-slate-700">{formatCurrency(row.requiredCorpusByStrategy?.SAFE_4_PERCENT || 0, true)}</span>
                                  {row.canRetireByStrategy?.SAFE_4_PERCENT && (
                                    <span className="text-emerald-600">✅</span>
                                  )}
                                </div>
                                <div className="flex items-center gap-2">
                                  <span className="text-slate-500">Depletion</span>
                                  <span className="text-slate-700">{formatCurrency(row.requiredCorpusByStrategy?.SIMPLE_DEPLETION || 0, true)}</span>
                                  {row.canRetireByStrategy?.SIMPLE_DEPLETION && (
                                    <span className="text-emerald-600">✅</span>
                                  )}
                                </div>
                              </div>
                            </td>
                            <td className={`px-3 py-2 text-right font-semibold bg-primary-50 ${(row.netCorpus || 0) < 0 ? 'text-danger-600' : 'text-primary-600'}`}>
                              <div className="flex flex-col items-end">
                                <div className="flex items-center gap-1">
                                  <span>{formatCurrency(row.netCorpus || 0)}</span>
                                  {(row.netCorpus || 0) < 0 && (row.goalOutflow || 0) > 0 && (
                                    <span className="text-danger-500" title="Goal cannot be fully funded from corpus!">⚠️</span>
                                  )}
                                </div>
                                {(row.netCorpus || 0) < 0 && (row.goalOutflow || 0) > 0 && (
                                  <span className="text-xs font-normal text-danger-500">
                                    Goal shortfall: {formatCurrency(Math.abs(row.netCorpus || 0))}
                                  </span>
                                )}
                                {isAfterStopYear && ifStopped?.flatCorpus ? (
                                  <span className="text-xs font-normal text-slate-400">
                                    ↘ {formatCurrency(ifStopped.flatCorpus)}
                                  </span>
                                ) : null}
                                {row.canRetireByStrategy?.[params.incomeStrategy] && (
                                  <button
                                    type="button"
                                    onClick={() => handleRetireAtAge(row.age)}
                                    className="mt-1 text-xs text-emerald-700 hover:text-emerald-800"
                                  >
                                    Retire at this age
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      }) : (
                        <tr>
                          <td colSpan={11} className="px-3 py-8 text-center text-slate-400">
                            Add investments to see year-by-year projections
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
                
                {!matrixExpanded && matrix.length > 5 && (
                  <p className="text-center text-sm text-slate-500 mt-2 cursor-pointer hover:text-primary-600" onClick={() => setMatrixExpanded(true)}>
                    Click to show all {matrix.length} rows
                  </p>
                )}
              </CardContent>
            </Card>
          )}

          {/* STRATEGY PLANNER */}
          {activeSection === 'strategy-planner' && (
            <Card>
              <CardContent>
                {!(features as unknown as Record<string, boolean>)?.retirementStrategyPlannerTab ? (
                  <div className="text-center py-8">
                    <div className="text-4xl mb-3">🔒</div>
                    <h3 className="text-lg font-semibold text-slate-700 mb-2">Strategy Planner Locked</h3>
                    <p className="text-slate-500">This feature requires PRO access. Contact your administrator.</p>
                  </div>
                ) : (
                  <>
                    <div className="flex items-center justify-between mb-6">
                      <div>
                        <h3 className="text-lg font-semibold text-slate-800">Strategy Planner</h3>
                        <p className="text-slate-500 text-sm">Actionable steps to close your retirement gap</p>
                      </div>
                      <span className={`text-xs px-2 py-1 rounded-full ${
                        corpusGap <= 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-danger-100 text-danger-700'
                      }`}>
                        {corpusGap <= 0 ? '✅ On Track' : '⚠️ Shortfall'}
                      </span>
                    </div>

                    {/* Corpus Status */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                      <div className="p-4 rounded-lg border border-slate-200 bg-slate-50">
                        <div className="text-xs text-slate-500">Projected Corpus</div>
                        <div className="text-lg font-bold text-slate-800">{formatCurrency(projectedCorpus, true)}</div>
                      </div>
                      <div className="p-4 rounded-lg border border-slate-200 bg-slate-50">
                        <div className="text-xs text-slate-500">Required Corpus</div>
                        <div className="text-lg font-bold text-slate-800">{formatCurrency(requiredCorpus, true)}</div>
                      </div>
                      <div className={`p-4 rounded-lg border ${corpusGap <= 0 ? 'bg-emerald-50 border-emerald-200' : 'bg-danger-50 border-danger-200'}`}>
                        <div className="text-xs text-slate-500">Gap / Surplus</div>
                        <div className={`text-lg font-bold ${corpusGap <= 0 ? 'text-emerald-600' : 'text-danger-600'}`}>
                          {corpusGap <= 0 ? '+' : '-'}{formatCurrency(Math.abs(corpusGap), true)}
                        </div>
                      </div>
                    </div>

                    {/* Monthly Savings Allocation */}
                    <div className="mb-8">
                      <h4 className="font-semibold text-slate-800 mb-3">Monthly Savings Allocation</h4>
                      <div className="p-4 rounded-lg border border-slate-200 bg-white">
                        <div className="text-xl font-bold text-slate-800">
                          {formatCurrency(netMonthlySavings)}
                          <div className="text-xs text-slate-500 font-normal mt-1">
                            Income: {formatCurrency(monthlyIncome)}
                            {totalRentalIncome > 0 && (
                              <span className="text-blue-600"> + Rental: {formatCurrency(totalRentalIncome)}</span>
                            )}
                            {' '}− Expenses: {formatCurrency(totalExpenses)} − EMIs: {formatCurrency(monthlyEMI)}
                          </div>
                          {totalRentalIncome > 0 && (
                            <div className="text-xs text-blue-500 font-normal mt-1">
                              Total Effective Income: {formatCurrency(effectiveMonthlyIncome)}
                            </div>
                          )}
                        </div>
                      </div>
                      {netMonthlySavings <= 0 ? (
                        <div className="text-center py-4 text-danger-500">
                          <div className="text-2xl mb-2">⚠️</div>
                          <div className="font-medium">Your expenses + EMIs exceed income.</div>
                          <div className="text-sm mt-2">Focus on reducing expenses or increasing income first.</div>
                        </div>
                      ) : (
                        <div className="space-y-3 mt-4">
                          {strategyAllocations.map((alloc, idx) => (
                            <div key={idx} className={`flex items-center gap-3 p-3 ${alloc.color} border rounded-lg`}>
                              <div className="flex-1">
                                <div className="flex items-center justify-between">
                                  <span className="font-medium text-slate-800">{alloc.name}</span>
                                  <span className="font-bold text-slate-700">{formatCurrency(alloc.amount)}/mo</span>
                                </div>
                                <div className="text-xs text-slate-600 mt-1">{alloc.description}</div>
                              </div>
                              <span className={`text-xs px-2 py-1 rounded ${
                                alloc.priority === 'High'
                                  ? 'bg-danger-100 text-danger-700'
                                  : alloc.priority === 'Medium'
                                  ? 'bg-amber-100 text-amber-700'
                                  : 'bg-slate-200 text-slate-600'
                              }`}>{alloc.priority}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* Action Timeline */}
                    <div className="mb-8">
                      <h4 className="font-semibold text-slate-800 mb-3">Action Timeline</h4>
                      {timelineItems.length === 0 ? (
                        <div className="text-center py-4 text-slate-400">No upcoming events</div>
                      ) : (
                        <div className="space-y-4">
                          {timelineItems.map((item, idx) => (
                            <div key={idx} className="flex gap-4">
                              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-white border-2 border-slate-300 flex items-center justify-center text-lg">
                                {item.icon}
                              </div>
                              <div className="flex-1 bg-slate-50 rounded-lg p-3 border border-slate-200">
                                <div className="flex items-center justify-between mb-1">
                                  <span className="font-semibold text-slate-800">{item.title}</span>
                                  <span className="text-xs font-medium text-slate-500">{item.year}</span>
                                </div>
                                <div className="text-sm text-slate-600 mb-2">{item.description}</div>
                                <div className={`text-xs ${item.actionColor} font-medium`}>→ {item.action}</div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* What-If Scenarios */}
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="font-semibold text-slate-800">What-If Scenarios</h4>
                        <Button size="sm" onClick={handleSaveStrategy}>
                          Save Strategy
                        </Button>
                      </div>
                      {whatIfScenarios.length === 0 ? (
                        <div className="text-center py-4 text-slate-400">No scenarios available</div>
                      ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {whatIfScenarios.map((scenario) => {
                            const impact = getScenarioImpact(scenario);
                            return (
                            <div key={scenario.id} className={`${scenario.bgClass} border rounded-lg p-4`}>
                              <div className="flex items-center gap-2 mb-2">
                                <label className="flex items-center gap-2 cursor-pointer">
                                  <input
                                    type="checkbox"
                                    checked={!!enabledScenarios[scenario.id]}
                                    onChange={() => setEnabledScenarios(prev => ({ ...prev, [scenario.id]: !prev[scenario.id] }))}
                                  />
                                  <span className="text-2xl">{scenario.icon}</span>
                                  <span className="font-semibold text-slate-800">{scenario.title}</span>
                                </label>
                              </div>
                              <p className="text-sm text-slate-600 mb-2">{scenario.description}</p>
                              {scenario.timing && <div className="text-xs text-blue-600 mb-2">{scenario.timing}</div>}
                              <div className="text-sm font-medium text-primary-600 mb-1">
                                Estimated corpus impact: +{formatCurrency(impact.impact, true)}
                              </div>
                              <div className="text-xs text-slate-500 mb-2">
                                At retirement: {formatCurrency(impact.baseline, true)} → {formatCurrency(impact.strategy, true)}
                              </div>
                              <div className={`text-sm font-semibold ${scenario.resultClass}`}>{scenario.result}</div>
                              <button
                                className="mt-2 text-xs text-primary-600 hover:text-primary-700 underline"
                                onClick={() => {
                                  setActiveScenario(scenario);
                                  setShowScenarioModal(true);
                                }}
                              >
                                View Projection
                              </button>
                            </div>
                          );
                          })}
                        </div>
                      )}
                    </div>
                    <Modal
                      isOpen={showScenarioModal}
                      onClose={() => setShowScenarioModal(false)}
                      title={activeScenario ? `${activeScenario.icon} ${activeScenario.title} - Projection` : 'Scenario Projection'}
                      size="xl"
                    >
                      {activeScenario ? (
                        <div className="space-y-4">
                          {(() => {
                            const impact = getScenarioImpact(activeScenario);
                            return (
                              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                                <div className="p-3 rounded-lg border border-slate-200 bg-slate-50">
                                  <div className="text-xs text-slate-500">Baseline Corpus</div>
                                  <div className="text-lg font-semibold text-slate-800">{formatCurrency(impact.baseline, true)}</div>
                                </div>
                                <div className="p-3 rounded-lg border border-emerald-200 bg-emerald-50">
                                  <div className="text-xs text-emerald-700">With Strategy</div>
                                  <div className="text-lg font-semibold text-emerald-700">{formatCurrency(impact.strategy, true)}</div>
                                </div>
                                <div className="p-3 rounded-lg border border-blue-200 bg-blue-50">
                                  <div className="text-xs text-blue-700">Difference (Impact)</div>
                                  <div className="text-lg font-semibold text-blue-700">+{formatCurrency(impact.impact, true)}</div>
                                  <div className="text-xs text-slate-500 mt-1">Difference at retirement</div>
                                </div>
                              </div>
                            );
                          })()}
                          <div className="p-3 bg-blue-50 rounded-lg border border-blue-200 text-sm text-blue-700">
                            <strong>Strategy Deployment:</strong> {activeScenario.deploymentYear} ({activeScenario.deploymentYear - currentYear} years from now)
                          </div>
                          <div className="h-[280px]">
                            <ResponsiveContainer width="100%" height="100%">
                              <LineChart data={generateScenarioChartData(activeScenario)}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                <XAxis dataKey="year" />
                                <YAxis tickFormatter={(v) => `${(v / 100000).toFixed(0)}L`} />
                                <Tooltip formatter={(value: number | undefined) => value !== undefined ? formatCurrency(value) : ''} />
                                <Legend />
                                <Line type="monotone" dataKey="baselineCorpus" name="Baseline Corpus" stroke="#94a3b8" strokeWidth={2} />
                                <Line type="monotone" dataKey="strategyCorpus" name="With Strategy" stroke="#10b981" strokeWidth={2} />
                              </LineChart>
                            </ResponsiveContainer>
                          </div>
                          <div className="overflow-x-auto rounded-lg border border-slate-200">
                            <table className="w-full text-sm">
                              <thead className="bg-slate-100">
                                <tr>
                                  <th className="px-3 py-2 text-left text-slate-600">Year</th>
                                  <th className="px-3 py-2 text-right text-slate-600">Baseline</th>
                                  <th className="px-3 py-2 text-right text-slate-600">With Strategy</th>
                                  <th className="px-3 py-2 text-right text-slate-600">Difference</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-100">
                                {generateScenarioChartData(activeScenario).map((row: any, idx: number) => (
                                  <tr key={idx} className="hover:bg-slate-50">
                                    <td className="px-3 py-2 text-slate-700">{row.year}</td>
                                    <td className="px-3 py-2 text-right">{formatCurrency(row.baselineCorpus, true)}</td>
                                    <td className="px-3 py-2 text-right">{formatCurrency(row.strategyCorpus, true)}</td>
                                    <td className="px-3 py-2 text-right text-emerald-600">+{formatCurrency(row.difference, true)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-slate-500">No scenario selected.</p>
                      )}
                    </Modal>
                  </>
                )}
              </CardContent>
            </Card>
          )}

          {/* WITHDRAWAL STRATEGY */}
          {activeSection === 'withdrawal' && (
            <Card>
              <CardContent>
                {!(features as unknown as Record<string, boolean>)?.retirementWithdrawalStrategyTab ? (
                  <div className="text-center py-8">
                    <div className="text-4xl mb-3">🔒</div>
                    <h3 className="text-lg font-semibold text-slate-700 mb-2">Withdrawal Strategy Locked</h3>
                    <p className="text-slate-500">This feature requires PRO access. Contact your administrator.</p>
                  </div>
                ) : (
                  <>
                    <h3 className="text-lg font-semibold text-slate-800 mb-4">Withdrawal Strategy</h3>
                    
                    <div className="p-4 bg-green-50 border border-green-200 rounded-xl mb-6">
                      <h4 className="font-semibold text-green-800 mb-2">Recommended Withdrawal Order</h4>
                      <ol className="list-decimal list-inside space-y-2 text-green-700">
                        <li><strong>Taxable Accounts First</strong> - Regular savings, FDs, stocks</li>
                        <li><strong>Tax-Free Sources</strong> - PPF maturity, LTCG</li>
                        <li><strong>Tax-Deferred Last</strong> - EPF withdrawal</li>
                      </ol>
                    </div>

                    {incomeProjection.length > 0 && (
                      <div>
                        <h4 className="font-medium text-slate-800 mb-3">Post-Retirement Projection</h4>
                        <div className="h-[300px]">
                          <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={incomeProjection.slice(0, 15)}>
                              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                              <XAxis 
                                dataKey="year" 
                                stroke="#64748b" 
                                fontSize={11}
                                tickFormatter={(v) => `+${v}y`}
                              />
                              <YAxis 
                                stroke="#64748b" 
                                fontSize={11}
                                tickFormatter={(v) => `${(v/100000).toFixed(0)}L`}
                              />
                              <Tooltip formatter={(value: number | undefined) => value !== undefined ? formatCurrency(value) : ''} />
                              <Legend />
                              <Bar dataKey="withdrawal" name="Annual Withdrawal" fill="#6366f1" radius={[4, 4, 0, 0]} />
                              <Bar dataKey="corpus" name="Remaining Corpus" fill="#10b981" radius={[4, 4, 0, 0]} />
                            </BarChart>
                          </ResponsiveContainer>
                        </div>
                      </div>
                    )}
                  </>
                )}
              </CardContent>
            </Card>
          )}
        </div>
    </MainLayout>
  );
}

export default Retirement;
