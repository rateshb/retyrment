import { Link } from 'react-router-dom';
import { ArrowLeft, Users, Target, Shield, Heart } from 'lucide-react';

export function About() {
  return (
    <div className="min-h-screen bg-slate-50">
      {/* Navigation */}
      <nav className="bg-white border-b border-slate-200 px-6 py-4">
        <div className="max-w-4xl mx-auto flex items-center gap-4">
          <Link to="/" className="flex items-center gap-2 text-slate-600 hover:text-primary-600">
            <ArrowLeft size={20} />
            <span>Back to Home</span>
          </Link>
        </div>
      </nav>

      <div className="max-w-4xl mx-auto px-6 py-12">
        <h1 className="text-4xl font-bold text-slate-800 mb-6">About Retyrment</h1>
        
        <div className="prose prose-slate max-w-none">
          <p className="text-xl text-slate-600 mb-8">
            Retyrment is a comprehensive personal finance planning application designed to help 
            individuals take control of their financial future.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-12">
            {[
              { icon: <Target className="text-primary-600" />, title: 'Our Mission', desc: 'To democratize financial planning and make it accessible to everyone, regardless of their wealth or financial expertise.' },
              { icon: <Users className="text-primary-600" />, title: 'Our Vision', desc: 'A world where everyone has the tools and knowledge to achieve financial independence and retire with confidence.' },
              { icon: <Shield className="text-primary-600" />, title: 'Our Values', desc: 'Transparency, security, and user-first design guide everything we build.' },
              { icon: <Heart className="text-primary-600" />, title: 'Our Promise', desc: 'Your data stays yours. We never sell user data and use bank-level encryption.' },
            ].map((item, i) => (
              <div key={i} className="p-6 bg-white rounded-xl border border-slate-200">
                <div className="w-12 h-12 rounded-lg bg-primary-100 flex items-center justify-center mb-4">
                  {item.icon}
                </div>
                <h3 className="text-lg font-semibold text-slate-800 mb-2">{item.title}</h3>
                <p className="text-slate-600">{item.desc}</p>
              </div>
            ))}
          </div>

          <h2 className="text-2xl font-bold text-slate-800 mb-4">What We Offer</h2>
          <ul className="space-y-3 mb-8">
            <li className="flex items-start gap-3">
              <span className="text-primary-600 mt-1">✓</span>
              <span><strong>Investment Tracking:</strong> Track mutual funds, PPF, EPF, FDs, stocks, real estate, and more.</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-primary-600 mt-1">✓</span>
              <span><strong>Retirement Planning:</strong> Calculate your retirement corpus and plan withdrawal strategies.</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-primary-600 mt-1">✓</span>
              <span><strong>Insurance Advisor:</strong> Get personalized health and term insurance recommendations.</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-primary-600 mt-1">✓</span>
              <span><strong>Goal Planning:</strong> Set and track financial goals with progress monitoring.</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="text-primary-600 mt-1">✓</span>
              <span><strong>Monte Carlo Simulation:</strong> Understand probability of achieving your goals.</span>
            </li>
          </ul>

          <h2 className="text-2xl font-bold text-slate-800 mb-4">Contact Us</h2>
          <p className="text-slate-600 mb-4">
            Have questions or feedback? We'd love to hear from you.
          </p>
          <p className="text-slate-600">
            Email: <a href="mailto:bansalitadvisory@gmail.com" className="text-primary-600 hover:underline">bansalitadvisory@gmail.com</a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default About;
