import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export function Privacy() {
  useEffect(() => {
    document.title = 'Privacy | Retyrment';
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
        <h1 className="text-4xl font-bold text-slate-800 mb-2">Privacy Policy</h1>
        <p className="text-slate-500 mb-8">Last updated: January 2026</p>
        
        <div className="prose prose-slate max-w-none">
          <h2>1. Information We Collect</h2>
          <p>
            We collect information you provide directly to us, including:
          </p>
          <ul>
            <li>Account information (name, email address)</li>
            <li>Financial data you choose to enter (income, investments, expenses, goals)</li>
            <li>Usage data and preferences</li>
          </ul>

          <h2>2. How We Use Your Information</h2>
          <p>
            We use the information we collect to:
          </p>
          <ul>
            <li>Provide, maintain, and improve our services</li>
            <li>Generate personalized financial insights and recommendations</li>
            <li>Send you technical notices and support messages</li>
            <li>Respond to your comments and questions</li>
          </ul>

          <h2>3. Data Security</h2>
          <p>
            We take reasonable measures to help protect your personal information from loss, theft, 
            misuse, unauthorized access, disclosure, alteration, and destruction. All data is encrypted 
            in transit and at rest using industry-standard encryption.
          </p>

          <h2>4. Data Sharing</h2>
          <p>
            We do not sell, trade, or rent your personal information to third parties. We may share 
            your information only in the following circumstances:
          </p>
          <ul>
            <li>With your consent</li>
            <li>To comply with legal obligations</li>
            <li>To protect our rights and prevent fraud</li>
          </ul>

          <h2>5. Data Retention</h2>
          <p>
            We retain your information for as long as your account is active or as needed to provide 
            you services. You can delete your data at any time through the Account settings.
          </p>

          <h2>6. Your Rights</h2>
          <p>
            You have the right to:
          </p>
          <ul>
            <li>Access your personal data</li>
            <li>Correct inaccurate data</li>
            <li>Delete your data</li>
            <li>Export your data</li>
            <li>Withdraw consent</li>
          </ul>

          <h2>7. Cookies</h2>
          <p>
            We use cookies and similar technologies to remember your preferences, understand how you 
            use our service, and improve your experience.
          </p>

          <h2>8. Changes to This Policy</h2>
          <p>
            We may update this privacy policy from time to time. We will notify you of any changes 
            by posting the new policy on this page and updating the "Last updated" date.
          </p>

          <h2>9. Contact Us</h2>
          <p>
            If you have any questions about this Privacy Policy, please contact us at:{' '}
            <a href="mailto:privacy@retyrment.com" className="text-primary-600 hover:underline">
              privacy@retyrment.com
            </a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Privacy;
