import { Link } from 'react-router-dom';
import { ArrowLeft, Check, Star } from 'lucide-react';

export function Pricing() {
  const plans = [
    {
      name: 'Free',
      price: '₹0',
      period: 'forever',
      description: 'Perfect for getting started with financial planning',
      features: [
        'Track unlimited investments',
        'Income & expense tracking',
        'Basic retirement calculator',
        'Goal planning',
        'Net worth dashboard',
        'Mobile responsive',
      ],
      cta: 'Get Started',
      highlighted: false,
    },
    {
      name: 'Pro',
      price: '₹499',
      period: 'per month',
      description: 'Advanced features for serious financial planning',
      features: [
        'Everything in Free',
        'Monte Carlo simulation',
        'Advanced retirement matrix',
        'Insurance recommendations',
        'PDF & Excel exports',
        'Priority support',
        'Financial calendar',
        'Custom assumptions',
      ],
      cta: 'Start 7-Day Trial',
      highlighted: true,
    }/*,
    {
      name: 'Enterprise',
      price: 'Custom',
      period: 'contact us',
      description: 'For financial advisors and institutions',
      features: [
        'Everything in Pro',
        'Multi-user access',
        'White-label options',
        'API access',
        'Dedicated support',
        'Custom integrations',
        'SLA guarantee',
        'Training & onboarding',
      ],
      cta: 'Contact Sales',
      highlighted: false,
    },*/
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Navigation */}
      <nav className="bg-white border-b border-slate-200 px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center gap-4">
          <Link to="/" className="flex items-center gap-2 text-slate-600 hover:text-primary-600">
            <ArrowLeft size={20} />
            <span>Back to Home</span>
          </Link>
        </div>
      </nav>

      <div className="max-w-6xl mx-auto px-6 py-12">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-slate-800 mb-4">Simple, Transparent Pricing</h1>
          <p className="text-xl text-slate-600">
            Choose the plan that's right for your financial journey
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
          {plans.map((plan, i) => (
            <div 
              key={i} 
              className={`rounded-2xl p-8 ${
                plan.highlighted 
                  ? 'bg-gradient-to-br from-primary-600 to-primary-800 text-white ring-4 ring-primary-500/30 scale-105' 
                  : 'bg-white border border-slate-200'
              }`}
            >
              {plan.highlighted && (
                <div className="flex items-center gap-1 text-primary-200 text-sm font-medium mb-4">
                  <Star size={16} /> Most Popular
                </div>
              )}
              <h3 className={`text-2xl font-bold mb-2 ${plan.highlighted ? 'text-white' : 'text-slate-800'}`}>
                {plan.name}
              </h3>
              <div className="mb-4">
                <span className={`text-4xl font-bold ${plan.highlighted ? 'text-white' : 'text-slate-800'}`}>
                  {plan.price}
                </span>
                <span className={`text-sm ${plan.highlighted ? 'text-primary-200' : 'text-slate-500'}`}>
                  /{plan.period}
                </span>
              </div>
              <p className={`mb-6 ${plan.highlighted ? 'text-primary-100' : 'text-slate-600'}`}>
                {plan.description}
              </p>
              <ul className="space-y-3 mb-8">
                {plan.features.map((feature, j) => (
                  <li key={j} className="flex items-start gap-3">
                    <Check size={18} className={plan.highlighted ? 'text-primary-200' : 'text-success-500'} />
                    <span className={plan.highlighted ? 'text-primary-50' : 'text-slate-600'}>
                      {feature}
                    </span>
                  </li>
                ))}
              </ul>
              <Link
                to="/login"
                className={`block w-full py-3 text-center font-semibold rounded-xl transition-colors ${
                  plan.highlighted
                    ? 'bg-white text-primary-700 hover:bg-primary-50'
                    : 'bg-primary-600 text-white hover:bg-primary-700'
                }`}
              >
                {plan.cta}
              </Link>
            </div>
          ))}
        </div>

        {/* FAQ */}
        <div className="bg-white rounded-2xl border border-slate-200 p-8">
          <h2 className="text-2xl font-bold text-slate-800 mb-6">Frequently Asked Questions</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {[
              { q: 'Can I try Pro features before paying?', a: 'Yes! All new users get a free 7-day Pro trial with full access to all features.' },
              { q: 'Can I cancel anytime?', a: 'Absolutely. You can cancel your subscription anytime with no questions asked.' },
              { q: 'Is my financial data secure?', a: 'Yes, we use bank-level encryption and never share your data with third parties.' },
              { q: 'What payment methods do you accept?', a: 'We accept all major credit/debit cards and UPI payments.' },
            ].map((faq, i) => (
              <div key={i}>
                <h4 className="font-semibold text-slate-800 mb-2">{faq.q}</h4>
                <p className="text-slate-600">{faq.a}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Pricing;
