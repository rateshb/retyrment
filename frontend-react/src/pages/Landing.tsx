import { Link } from 'react-router-dom';
import { 
  TrendingUp, Shield, Target, PiggyBank, Calculator, 
  BarChart3, Users, CheckCircle, ArrowRight, Star
} from 'lucide-react';

export function Landing() {
  return (
    <div className="min-h-screen bg-slate-50">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center text-white font-bold text-xl shadow-lg">
              ₹
            </div>
            <span className="text-xl font-bold bg-gradient-to-r from-primary-500 to-primary-700 bg-clip-text text-transparent">
              Retyrment
            </span>
          </Link>
          <div className="hidden md:flex items-center gap-8">
            <Link to="/features" className="text-slate-600 hover:text-primary-600 transition-colors">Features</Link>
            <Link to="/pricing" className="text-slate-600 hover:text-primary-600 transition-colors">Pricing</Link>
            <Link to="/about" className="text-slate-600 hover:text-primary-600 transition-colors">About</Link>
            <Link to="/login" className="px-4 py-2 text-primary-600 font-medium hover:bg-primary-50 rounded-lg transition-colors">
              Login
            </Link>
            <Link to="/login" className="px-4 py-2 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 transition-colors shadow-md">
              Get Started
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-7xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-primary-100 text-primary-700 rounded-full text-sm font-medium mb-6">
            <Star size={16} /> Trusted by 10,000+ users
          </div>
          <h1 className="text-5xl md:text-6xl font-bold text-slate-800 mb-6 leading-tight">
            Plan Your Financial Future<br />
            <span className="bg-gradient-to-r from-primary-500 to-primary-700 bg-clip-text text-transparent">
              With Confidence
            </span>
          </h1>
          <p className="text-xl text-slate-600 max-w-2xl mx-auto mb-8">
            Track investments, plan retirement, manage insurance - all in one place. 
            Make smarter financial decisions with data-driven insights.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link 
              to="/login" 
              className="px-8 py-4 bg-primary-600 text-white font-semibold rounded-xl hover:bg-primary-700 transition-all shadow-lg shadow-primary-500/30 flex items-center justify-center gap-2"
            >
              Start Free Trial <ArrowRight size={20} />
            </Link>
            <Link 
              to="/features" 
              className="px-8 py-4 bg-white text-slate-700 font-semibold rounded-xl hover:bg-slate-100 transition-all border border-slate-200"
            >
              View Features
            </Link>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-20 px-6 bg-white">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-3xl font-bold text-center text-slate-800 mb-4">
            Everything You Need for Financial Success
          </h2>
          <p className="text-center text-slate-600 mb-12 max-w-2xl mx-auto">
            Comprehensive tools to track, analyze, and optimize your financial journey
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[
              { icon: <TrendingUp />, title: 'Investment Tracking', desc: 'Track all your investments - MF, stocks, FD, PPF, and more with real-time valuations.' },
              { icon: <PiggyBank />, title: 'Retirement Planning', desc: 'Calculate your retirement corpus, analyze gaps, and plan withdrawal strategies.' },
              { icon: <Shield />, title: 'Insurance Advisor', desc: 'Get personalized health and term insurance recommendations for your family.' },
              { icon: <Target />, title: 'Goal Planning', desc: 'Set financial goals and track progress with automated milestone tracking.' },
              { icon: <Calculator />, title: 'Net Worth Analysis', desc: 'See your complete financial picture with detailed asset and liability breakdown.' },
              { icon: <BarChart3 />, title: 'Monte Carlo Simulation', desc: 'Run probabilistic simulations to understand your financial future.' },
            ].map((feature, i) => (
              <div key={i} className="p-6 rounded-2xl border border-slate-200 hover:border-primary-200 hover:shadow-lg transition-all group">
                <div className="w-12 h-12 rounded-xl bg-primary-100 text-primary-600 flex items-center justify-center mb-4 group-hover:bg-primary-600 group-hover:text-white transition-colors">
                  {feature.icon}
                </div>
                <h3 className="text-lg font-semibold text-slate-800 mb-2">{feature.title}</h3>
                <p className="text-slate-600">{feature.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Social Proof */}
      <section className="py-20 px-6 bg-gradient-to-br from-primary-600 to-primary-800 text-white">
        <div className="max-w-7xl mx-auto text-center">
          <h2 className="text-3xl font-bold mb-12">Trusted by Thousands</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {[
              { value: '10K+', label: 'Active Users' },
              { value: '₹500Cr+', label: 'Assets Tracked' },
              { value: '50K+', label: 'Goals Set' },
              { value: '99.9%', label: 'Uptime' },
            ].map((stat, i) => (
              <div key={i}>
                <p className="text-4xl font-bold mb-2">{stat.value}</p>
                <p className="text-primary-200">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl font-bold text-slate-800 mb-4">
            Ready to Take Control of Your Finances?
          </h2>
          <p className="text-slate-600 mb-8">
            Join thousands of users who are already planning their financial future with Retyrment.
          </p>
          <Link 
            to="/login" 
            className="inline-flex items-center gap-2 px-8 py-4 bg-primary-600 text-white font-semibold rounded-xl hover:bg-primary-700 transition-all shadow-lg"
          >
            Get Started Free <ArrowRight size={20} />
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-400 py-12 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-8">
            <div>
              <h4 className="font-semibold text-white mb-4">Product</h4>
              <ul className="space-y-2">
                <li><Link to="/features" className="hover:text-white transition-colors">Features</Link></li>
                <li><Link to="/pricing" className="hover:text-white transition-colors">Pricing</Link></li>
                <li><Link to="/login" className="hover:text-white transition-colors">Login</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-white mb-4">Company</h4>
              <ul className="space-y-2">
                <li><Link to="/about" className="hover:text-white transition-colors">About Us</Link></li>
                {/*<li><Link to="/contact" className="hover:text-white transition-colors">Contact</Link></li>*/}
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-white mb-4">Legal</h4>
              <ul className="space-y-2">
                <li><Link to="/privacy" className="hover:text-white transition-colors">Privacy Policy</Link></li>
                <li><Link to="/terms" className="hover:text-white transition-colors">Terms of Service</Link></li>
                <li><Link to="/disclaimer" className="hover:text-white transition-colors">Disclaimer</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-white mb-4">Contact</h4>
              <p className="text-sm">bansalitadvisory@gmail.com</p>
            </div>
          </div>
          <div className="border-t border-slate-800 pt-8 text-center">
            <p>&copy; {new Date().getFullYear()} Retyrment. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default Landing;
