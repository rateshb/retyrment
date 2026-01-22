import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, toast } from '../components/ui';
import { api } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { formatCurrency } from '../lib/utils';
import { 
  FileText, FileSpreadsheet, Download, Upload, CheckCircle, 
  AlertTriangle, Calendar, Umbrella, TrendingUp, Shield 
} from 'lucide-react';

export function Reports() {
  const { features, user } = useAuthStore();
  const [isExporting, setIsExporting] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  const { data: networth } = useQuery({
    queryKey: ['networth'],
    queryFn: api.analysis.networth,
  });

  const { data: retirementData } = useQuery({
    queryKey: ['retirement-reports'],
    queryFn: () => api.retirement.calculate({ currentAge: 35, retirementAge: 60, lifeExpectancy: 85 }),
  });

  const canExportPdf = features?.canExportPdf === true;
  const canExportExcel = features?.canExportExcel === true;
  const canExportJson = features?.canExportJson === true;
  const canImportData = features?.canImportData === true;
  const isPro = user?.role === 'PRO' || user?.role === 'ADMIN';

  const handleDownloadPdf = async (type: string) => {
    if (!canExportPdf) {
      toast.error('PDF export requires Pro subscription');
      return;
    }
    
    setIsExporting(true);
    try {
      const url = `${api.export.getPdfUrl()}&type=${type}`;
      const response = await fetch(url);
      
      if (!response.ok) throw new Error('Export failed');
      
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = downloadUrl;
      a.download = `retyrment-${type}-report-${new Date().toISOString().split('T')[0]}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(downloadUrl);
      document.body.removeChild(a);
      
      toast.success('PDF exported successfully');
    } catch (error) {
      toast.error('Failed to export PDF');
    } finally {
      setIsExporting(false);
    }
  };

  const handleDownloadExcel = async () => {
    if (!canExportExcel) {
      toast.error('Excel export requires Pro subscription');
      return;
    }

    setIsExporting(true);
    try {
      const url = api.export.getExcelUrl();
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('retyrment_token')}`,
        },
      });
      
      if (!response.ok) throw new Error('Export failed');
      
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = downloadUrl;
      a.download = `retyrment-export-${new Date().toISOString().split('T')[0]}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(downloadUrl);
      document.body.removeChild(a);
      
      toast.success('Excel exported successfully');
    } catch (error) {
      toast.error('Failed to export Excel');
    } finally {
      setIsExporting(false);
    }
  };

  const handleExportJson = async () => {
    if (!canExportJson) {
      toast.error('JSON export requires Pro subscription');
      return;
    }

    setIsExporting(true);
    try {
      const data = await api.export.json();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `retyrment-backup-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      toast.success('JSON backup exported successfully');
    } catch (error) {
      toast.error('Failed to export JSON');
    } finally {
      setIsExporting(false);
    }
  };

  const handleImportJson = async (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!canImportData) {
      toast.error('Import data requires Pro subscription');
      event.target.value = '';
      return;
    }
    
    const file = event.target.files?.[0];
    if (!file) return;

    setIsImporting(true);
    try {
      const text = await file.text();
      const data = JSON.parse(text);
      await api.export.importJson(data);
      toast.success('Data imported successfully! Refreshing...');
      window.location.reload();
    } catch (error) {
      toast.error('Failed to import data. Please check the file format.');
    } finally {
      setIsImporting(false);
      event.target.value = '';
    }
  };

  const summary = retirementData?.summary || {};
  const gapAnalysis = retirementData?.gapAnalysis || {};

  const reportCards = [
    {
      title: 'Financial Summary PDF',
      description: 'Complete overview of your net worth, assets, and liabilities',
      icon: <FileText className="text-primary-500" size={32} />,
      color: 'bg-primary-50 border-primary-200',
      buttonText: 'Download PDF',
      onClick: () => handleDownloadPdf('summary'),
      disabled: !canExportPdf,
      requiresPro: true,
    },
    {
      title: 'Retirement Report PDF',
      description: 'Detailed retirement projections and recommendations',
      icon: <Umbrella className="text-teal-500" size={32} />,
      color: 'bg-teal-50 border-teal-200',
      buttonText: 'Download PDF',
      onClick: () => handleDownloadPdf('retirement'),
      disabled: !canExportPdf,
      requiresPro: true,
    },
    {
      title: 'Retirement Matrix Excel',
      description: 'Year-by-year projections in spreadsheet format',
      icon: <FileSpreadsheet className="text-success-500" size={32} />,
      color: 'bg-success-50 border-success-200',
      buttonText: 'Download Excel',
      onClick: handleDownloadExcel,
      disabled: !canExportExcel,
      requiresPro: true,
    },
    {
      title: 'Full Data Backup (JSON)',
      description: 'Export all your data for backup or migration',
      icon: <Download className="text-purple-500" size={32} />,
      color: 'bg-purple-50 border-purple-200',
      buttonText: 'Export JSON',
      onClick: handleExportJson,
      disabled: !canExportJson,
      requiresPro: false,
    },
    {
      title: 'Calendar Report PDF',
      description: 'Upcoming financial events and payment schedule',
      icon: <Calendar className="text-blue-500" size={32} />,
      color: 'bg-blue-50 border-blue-200',
      buttonText: 'Download PDF',
      onClick: () => handleDownloadPdf('calendar'),
      disabled: !canExportPdf,
      requiresPro: true,
    },
  ];

  return (
    <MainLayout 
      title="Reports & Export"
      subtitle="Download and backup your financial data"
    >
      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <Card className="bg-primary-50 border-primary-200">
          <CardContent className="text-center">
            <p className="text-sm text-primary-600">Net Worth</p>
            <p className="text-2xl font-bold text-primary-700">{formatCurrency(networth?.netWorth || 0)}</p>
          </CardContent>
        </Card>
        <Card className="bg-success-50 border-success-200">
          <CardContent className="text-center">
            <p className="text-sm text-success-600">Total Assets</p>
            <p className="text-2xl font-bold text-success-700">{formatCurrency(networth?.totalAssets || 0)}</p>
          </CardContent>
        </Card>
        <Card className="bg-teal-50 border-teal-200">
          <CardContent className="text-center">
            <p className="text-sm text-teal-600">Projected Corpus</p>
            <p className="text-2xl font-bold text-teal-700">{formatCurrency(summary.finalCorpus || 0)}</p>
          </CardContent>
        </Card>
        <Card className={`${gapAnalysis.corpusGap > 0 ? 'bg-danger-50 border-danger-200' : 'bg-success-50 border-success-200'}`}>
          <CardContent className="text-center">
            <p className={`text-sm ${gapAnalysis.corpusGap > 0 ? 'text-danger-600' : 'text-success-600'}`}>
              {gapAnalysis.corpusGap > 0 ? 'Corpus Gap' : 'Corpus Surplus'}
            </p>
            <p className={`text-2xl font-bold ${gapAnalysis.corpusGap > 0 ? 'text-danger-700' : 'text-success-700'}`}>
              {formatCurrency(Math.abs(gapAnalysis.corpusGap || 0))}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Pro Feature Notice */}
      {!isPro && (
        <Card className="mb-6 bg-amber-50 border-amber-200">
          <CardContent className="flex items-center gap-4">
            <AlertTriangle className="text-amber-500 flex-shrink-0" size={24} />
            <div>
              <p className="font-medium text-amber-800">Pro Features Required</p>
              <p className="text-sm text-amber-700">
                PDF and Excel exports are available with Pro subscription. JSON backup is available for all users.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Report Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        {reportCards.map((card, index) => (
          <Card key={index} className={`${card.color} relative`}>
            {card.requiresPro && !isPro && (
              <div className="absolute top-2 right-2 bg-amber-500 text-white text-xs px-2 py-0.5 rounded">
                PRO
              </div>
            )}
            <CardContent className="text-center">
              <div className="flex justify-center mb-4">
                {card.icon}
              </div>
              <h3 className="text-lg font-semibold text-slate-800 mb-2">{card.title}</h3>
              <p className="text-sm text-slate-600 mb-4">{card.description}</p>
              <Button 
                onClick={card.onClick}
                disabled={card.disabled || isExporting}
                variant={card.disabled ? 'secondary' : 'primary'}
                className="w-full"
              >
                {isExporting ? 'Processing...' : card.buttonText}
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Import Section */}
      <Card>
        <CardContent>
          <div className="flex items-center gap-4 mb-4">
            <Upload className="text-slate-500" size={24} />
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-slate-800">Import Data</h3>
              <p className="text-sm text-slate-600">Restore data from a JSON backup file</p>
            </div>
            {!canImportData && (
              <span className="px-3 py-1 bg-amber-100 text-amber-700 text-xs font-medium rounded-full">
                Pro Feature
              </span>
            )}
          </div>
          
          {canImportData ? (
            <>
              <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg mb-4">
                <p className="text-sm text-amber-700">
                  ⚠️ <strong>Warning:</strong> Importing data will merge with your existing data. 
                  Consider exporting a backup first.
                </p>
              </div>

              <div className="flex items-center gap-4">
                <label className="flex-1">
                  <input
                    type="file"
                    accept=".json"
                    onChange={handleImportJson}
                    className="hidden"
                    disabled={isImporting}
                  />
                  <div className="w-full py-3 px-4 border-2 border-dashed border-slate-300 rounded-lg text-center cursor-pointer hover:border-primary-400 hover:bg-primary-50/50 transition-colors">
                    <Upload className="mx-auto mb-2 text-slate-400" size={20} />
                    <p className="text-sm text-slate-600">
                      {isImporting ? 'Importing...' : 'Click to select JSON file or drag and drop'}
                    </p>
                  </div>
                </label>
              </div>
            </>
          ) : (
            <div className="p-6 bg-slate-50 border-2 border-dashed border-slate-200 rounded-lg text-center">
              <div className="max-w-md mx-auto">
                <Shield className="mx-auto mb-3 text-slate-400" size={32} />
                <p className="text-slate-600 mb-2">
                  Import data is a <strong>Pro feature</strong>
                </p>
                <p className="text-sm text-slate-500">
                  Upgrade to Pro to restore data from JSON backup files and merge with your existing data.
                </p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Feature Access List */}
      <Card className="mt-6">
        <CardContent>
          <h3 className="text-lg font-semibold text-slate-800 mb-4">Reports & Data Features Status</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className={`p-4 rounded-lg border ${canExportPdf ? 'bg-success-50 border-success-200' : 'bg-slate-50 border-slate-200'}`}>
              <div className="flex items-center gap-2 mb-1">
                {canExportPdf ? <CheckCircle className="text-success-500" size={16} /> : <AlertTriangle className="text-slate-400" size={16} />}
                <span className="font-medium">PDF Export</span>
              </div>
              <p className={`text-sm ${canExportPdf ? 'text-success-600' : 'text-slate-500'}`}>
                {canExportPdf ? 'Available' : 'Requires Pro'}
              </p>
            </div>
            <div className={`p-4 rounded-lg border ${canExportExcel ? 'bg-success-50 border-success-200' : 'bg-slate-50 border-slate-200'}`}>
              <div className="flex items-center gap-2 mb-1">
                {canExportExcel ? <CheckCircle className="text-success-500" size={16} /> : <AlertTriangle className="text-slate-400" size={16} />}
                <span className="font-medium">Excel Export</span>
              </div>
              <p className={`text-sm ${canExportExcel ? 'text-success-600' : 'text-slate-500'}`}>
                {canExportExcel ? 'Available' : 'Requires Pro'}
              </p>
            </div>
            <div className={`p-4 rounded-lg border ${canExportJson ? 'bg-success-50 border-success-200' : 'bg-slate-50 border-slate-200'}`}>
              <div className="flex items-center gap-2 mb-1">
                {canExportJson ? <CheckCircle className="text-success-500" size={16} /> : <AlertTriangle className="text-slate-400" size={16} />}
                <span className="font-medium">JSON Export</span>
              </div>
              <p className={`text-sm ${canExportJson ? 'text-success-600' : 'text-slate-500'}`}>
                {canExportJson ? 'Available' : 'Requires Pro'}
              </p>
            </div>
            <div className={`p-4 rounded-lg border ${canImportData ? 'bg-success-50 border-success-200' : 'bg-slate-50 border-slate-200'}`}>
              <div className="flex items-center gap-2 mb-1">
                {canImportData ? <CheckCircle className="text-success-500" size={16} /> : <AlertTriangle className="text-slate-400" size={16} />}
                <span className="font-medium">Data Import</span>
              </div>
              <p className={`text-sm ${canImportData ? 'text-success-600' : 'text-slate-500'}`}>
                {canImportData ? 'Available' : 'Requires Pro'}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </MainLayout>
  );
}

export default Reports;
