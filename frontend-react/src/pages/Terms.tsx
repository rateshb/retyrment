import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export function Terms() {
  useEffect(() => {
    document.title = 'Terms | Retyrment';
  }, []);

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
        <h1 className="text-4xl font-bold text-slate-800 mb-2">Terms of Service</h1>
        <p className="text-slate-500 mb-8">Last updated: January 2026</p>
        
        <div className="prose prose-slate max-w-none">
          <h2>1. Acceptance of Terms</h2>
          <p>
            By accessing or using Retyrment, you agree to be bound by these Terms of Service. 
            If you do not agree to these terms, please do not use our service.
          </p>

          <h2>2. Description of Service</h2>
          <p>
            Retyrment is a personal finance planning application that helps you track investments, 
            plan for retirement, and manage your financial goals. The service is provided "as is" 
            for informational purposes only.
          </p>

          <h2>3. User Accounts</h2>
          <p>
            You are responsible for:
          </p>
          <ul>
            <li>Maintaining the confidentiality of your account</li>
            <li>All activities that occur under your account</li>
            <li>Providing accurate and complete information</li>
            <li>Notifying us immediately of any unauthorized use</li>
          </ul>

          <h2>4. Acceptable Use</h2>
          <p>
            You agree not to:
          </p>
          <ul>
            <li>Use the service for any illegal purpose</li>
            <li>Attempt to gain unauthorized access to any part of the service</li>
            <li>Interfere with or disrupt the service</li>
            <li>Upload malicious code or content</li>
            <li>Impersonate any person or entity</li>
          </ul>

          <h2>5. Financial Disclaimer</h2>
          <p>
            <strong>Important:</strong> Retyrment is not a financial advisor, tax advisor, or 
            investment advisor. The information provided by the service is for informational 
            purposes only and should not be construed as financial advice.
          </p>
          <p>
            You should consult with qualified professionals before making any financial decisions. 
            We are not responsible for any financial losses resulting from decisions made based 
            on information provided by our service.
          </p>

          <h2>6. Intellectual Property</h2>
          <p>
            The service and its original content, features, and functionality are owned by 
            Retyrment and are protected by international copyright, trademark, and other 
            intellectual property laws.
          </p>

          <h2>7. Limitation of Liability</h2>
          <p>
            To the maximum extent permitted by law, Retyrment shall not be liable for any 
            indirect, incidental, special, consequential, or punitive damages resulting from 
            your use of or inability to use the service.
          </p>

          <h2>8. Termination</h2>
          <p>
            We may terminate or suspend your account at any time, without prior notice, for 
            conduct that we believe violates these Terms or is harmful to other users, us, 
            or third parties, or for any other reason.
          </p>

          <h2>9. Changes to Terms</h2>
          <p>
            We reserve the right to modify these terms at any time. We will notify users of 
            any material changes by posting the new terms on this page.
          </p>

          <h2>10. Governing Law</h2>
          <p>
            These Terms shall be governed by and construed in accordance with the laws of India, 
            without regard to its conflict of law provisions.
          </p>

          <h2>11. Contact</h2>
          <p>
            For questions about these Terms, please contact us at:{' '}
            <a href="mailto:legal@retyrment.com" className="text-primary-600 hover:underline">
              legal@retyrment.com
            </a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Terms;
