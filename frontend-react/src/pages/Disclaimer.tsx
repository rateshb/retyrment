import { Link } from 'react-router-dom';
import { ArrowLeft, AlertTriangle } from 'lucide-react';

export function Disclaimer() {
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
        <div className="flex items-center gap-3 mb-6">
          <AlertTriangle className="text-warning-500" size={32} />
          <h1 className="text-4xl font-bold text-slate-800">Disclaimer</h1>
        </div>
        
        <div className="prose prose-slate max-w-none">
          <div className="bg-warning-50 border border-warning-200 rounded-xl p-6 mb-8">
            <h2 className="text-warning-800 mt-0">Important Notice</h2>
            <p className="text-warning-700 mb-0">
              Retyrment is a financial planning tool for informational purposes only. It is not 
              a substitute for professional financial advice. Always consult with qualified 
              financial advisors before making investment decisions.
            </p>
          </div>

          <h2>1. Not Financial Advice</h2>
          <p>
            The information, calculations, projections, and recommendations provided by Retyrment 
            are for general informational and educational purposes only. They do not constitute 
            financial advice, investment advice, tax advice, or any other kind of professional advice.
          </p>

          <h2>2. No Guarantees</h2>
          <p>
            While we strive to provide accurate calculations and projections, we make no guarantees 
            about the accuracy, completeness, or reliability of any information provided. Past 
            performance does not guarantee future results.
          </p>
          <ul>
            <li>Investment returns can vary significantly from projections</li>
            <li>Inflation rates may differ from assumptions</li>
            <li>Tax laws and rates may change</li>
            <li>Market conditions are unpredictable</li>
          </ul>

          <h2>3. User Responsibility</h2>
          <p>
            You are solely responsible for:
          </p>
          <ul>
            <li>Verifying the accuracy of data you enter into the system</li>
            <li>Making your own financial decisions</li>
            <li>Seeking professional advice when needed</li>
            <li>Understanding the risks involved in any financial decision</li>
          </ul>

          <h2>4. Investment Risks</h2>
          <p>
            All investments carry risk. The value of investments can go down as well as up, and 
            you may get back less than you invested. Different types of investments carry different 
            levels of risk:
          </p>
          <ul>
            <li>Equity investments are subject to market volatility</li>
            <li>Fixed income investments are subject to interest rate risk</li>
            <li>Real estate values can fluctuate</li>
            <li>Past performance is not indicative of future results</li>
          </ul>

          <h2>5. Assumptions and Limitations</h2>
          <p>
            Our projections are based on various assumptions including:
          </p>
          <ul>
            <li>Expected returns on different asset classes</li>
            <li>Inflation rates</li>
            <li>Tax rates and rules</li>
            <li>Life expectancy</li>
          </ul>
          <p>
            These assumptions may not reflect actual future conditions and should be reviewed 
            and adjusted based on your specific circumstances.
          </p>

          <h2>6. Third-Party Information</h2>
          <p>
            We may display information from third-party sources. We do not verify or guarantee 
            the accuracy of such information and are not responsible for any errors or omissions 
            in third-party content.
          </p>

          <h2>7. Limitation of Liability</h2>
          <p>
            To the fullest extent permitted by law, Retyrment and its creators shall not be 
            liable for any direct, indirect, incidental, consequential, or punitive damages 
            arising from your use of this service or any decisions made based on information 
            provided by this service.
          </p>

          <h2>8. Consult Professionals</h2>
          <p>
            We strongly recommend consulting with qualified professionals including:
          </p>
          <ul>
            <li>Certified Financial Planners (CFP)</li>
            <li>Chartered Accountants (CA) for tax matters</li>
            <li>Insurance advisors for insurance needs</li>
            <li>Legal professionals for estate planning</li>
          </ul>

          <h2>9. Changes to Disclaimer</h2>
          <p>
            We may update this disclaimer from time to time. Continued use of the service 
            constitutes acceptance of the current disclaimer.
          </p>

          <div className="bg-slate-100 rounded-xl p-6 mt-8">
            <p className="text-slate-600 mb-0">
              By using Retyrment, you acknowledge that you have read, understood, and agree 
              to this disclaimer. If you do not agree, please do not use our service.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Disclaimer;
