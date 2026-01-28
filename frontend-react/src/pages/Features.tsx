import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  ArrowLeft, ArrowRight, TrendingUp, PiggyBank, Shield, Target, 
  Calculator, BarChart3, Calendar, FileText, Users, Wallet
} from 'lucide-react';

export function Features() {
  useEffect(() => {
    document.title = 'Features | Retyrment';
  }, []);

  const features = [
    {
      icon: <TrendingUp size={32} />,
      title: 'Investment Tracking',
      description: 'Track all your investments in one place with real-time valuations and gain/loss calculations.',
      details: [
        'Mutual Funds, Stocks, ETFs',
        'PPF, EPF, NPS',
        'Fixed Deposits, Recurring Deposits',
        'Real Estate, Gold, Crypto',
        'SIP tracking with step-up',
        'Emergency fund tagging',
      ],
    },
    {
      icon: <PiggyBank size={32} />,
      title: 'Retirement Planning',
      description: 'Calculate your retirement corpus and plan your post-retirement life with confidence.',
      details: [
        'Year-by-year retirement matrix',
        'GAP analysis with recommendations',
        'Multiple income strategies (4% rule, depletion)',
        'SIP step-up optimization',
        'Withdrawal strategy planner',
        'Inflation-adjusted projections',
      ],
    },
    {
      icon: <Shield size={32} />,
      title: 'Insurance Advisor',
      description: 'Get personalized insurance recommendations based on your family composition.',
      details: [
        'Health insurance recommendations',
        'Term life insurance calculator',
        'Family floater vs individual analysis',
        'Super top-up suggestions',
        'Coverage gap identification',
        'Premium estimates',
      ],
    },
    {
      icon: <Target size={32} />,
      title: 'Goal Planning',
      description: 'Set financial goals and track your progress with automated milestone tracking.',
      details: [
        'Multiple goal types',
        'Progress visualization',
        'Recurring goals support',
        'Priority-based planning',
        'Inflation adjustment',
        'Funding recommendations',
      ],
    },
    {
      icon: <Calculator size={32} />,
      title: 'Net Worth Analysis',
      description: 'See your complete financial picture with detailed breakdowns.',
      details: [
        'Assets vs Liabilities',
        'Category-wise breakdown',
        'Historical tracking',
        'Liquid vs Illiquid assets',
        'Real-time calculations',
        'Visual charts',
      ],
    },
    {
      icon: <BarChart3 size={32} />,
      title: 'Monte Carlo Simulation',
      description: 'Understand the probability of achieving your financial goals.',
      details: [
        'Probabilistic projections',
        'Multiple scenarios',
        'Percentile analysis',
        'Success rate calculation',
        'Visual distributions',
        'What-if analysis',
      ],
    },
    {
      icon: <Calendar size={32} />,
      title: 'Financial Calendar',
      description: 'Never miss an important financial date with our smart calendar.',
      details: [
        'Insurance premium reminders',
        'EMI due dates',
        'Investment maturities',
        'Goal deadlines',
        'Tax payment dates',
        'Renewal alerts',
      ],
    },
    {
      icon: <FileText size={32} />,
      title: 'Reports & Export',
      description: 'Generate professional reports and export your data.',
      details: [
        'PDF financial summary',
        'Excel retirement matrix',
        'JSON data backup',
        'Custom date ranges',
        'Printable reports',
        'Data portability',
      ],
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Navigation */}
      <nav className="bg-white border-b border-slate-200 px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-slate-600 hover:text-primary-600">
            <ArrowLeft size={20} />
            <span>Back to Home</span>
          </Link>
          <Link 
            to="/login" 
            className="px-4 py-2 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 transition-colors"
          >
            Get Started
          </Link>
        </div>
      </nav>

      <div className="max-w-6xl mx-auto px-6 py-12">
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold text-slate-800 mb-4">
            Powerful Features for Your Financial Journey
          </h1>
          <p className="text-xl text-slate-600 max-w-2xl mx-auto">
            Everything you need to track, plan, and optimize your finances in one comprehensive platform.
          </p>
        </div>

        <div className="space-y-16">
          {features.map((feature, i) => (
            <div 
              key={i} 
              className={`flex flex-col md:flex-row gap-8 items-center ${i % 2 === 1 ? 'md:flex-row-reverse' : ''}`}
            >
              <div className="flex-1">
                <div className="w-16 h-16 rounded-2xl bg-primary-100 text-primary-600 flex items-center justify-center mb-4">
                  {feature.icon}
                </div>
                <h2 className="text-2xl font-bold text-slate-800 mb-3">{feature.title}</h2>
                <p className="text-lg text-slate-600 mb-4">{feature.description}</p>
                <ul className="grid grid-cols-2 gap-2">
                  {feature.details.map((detail, j) => (
                    <li key={j} className="flex items-center gap-2 text-slate-600">
                      <span className="w-1.5 h-1.5 rounded-full bg-primary-500"></span>
                      {detail}
                    </li>
                  ))}
                </ul>
              </div>
              <div className="flex-1">
                <div className="aspect-video bg-gradient-to-br from-slate-100 to-slate-200 rounded-2xl flex items-center justify-center">
                  <div className="text-slate-400 text-center">
                    <div className="w-20 h-20 mx-auto mb-2 rounded-xl bg-white shadow-lg flex items-center justify-center">
                      {feature.icon}
                    </div>
                    <p className="text-sm">{feature.title} Preview</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* CTA */}
        <div className="mt-20 text-center">
          <h2 className="text-3xl font-bold text-slate-800 mb-4">
            Ready to Transform Your Financial Planning?
          </h2>
          <p className="text-slate-600 mb-8">
            Join thousands of users who are already planning their financial future with Retyrment.
          </p>
          <Link 
            to="/login" 
            className="inline-flex items-center gap-2 px-8 py-4 bg-primary-600 text-white font-semibold rounded-xl hover:bg-primary-700 transition-all shadow-lg"
          >
            Start Free Trial <ArrowRight size={20} />
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Features;
