import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent } from '../components/ui';
import { insuranceRecommendationsApi, insuranceApi } from '../lib/api';
import { formatCurrency } from '../lib/utils';
import { Shield, Heart, Umbrella, CheckCircle, AlertTriangle, Users } from 'lucide-react';

export function InsuranceRecommendations() {
  const { data: recommendations, isLoading, error } = useQuery({
    queryKey: ['insurance-recommendations'],
    queryFn: insuranceRecommendationsApi.getOverall,
  });
  const { data: insurances = [] } = useQuery({
    queryKey: ['insurance'],
    queryFn: insuranceApi.getAll,
  });

  const health = recommendations?.healthRecommendation || {};
  const term = recommendations?.termRecommendation || {};
  const summary = recommendations?.summary || {};
  const hasHealthPolicy = insurances.some((policy: any) => policy.type === 'HEALTH');
  const hasNonGroupHealth = insurances.some((policy: any) => policy.type === 'HEALTH' && policy.healthType !== 'GROUP');
  const isGroupOnlyHealth = hasHealthPolicy && !hasNonGroupHealth;

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-success-600';
    if (score >= 50) return 'text-warning-600';
    return 'text-danger-600';
  };

  const getScoreBg = (score: number) => {
    if (score >= 80) return 'bg-success-50 border-success-200';
    if (score >= 50) return 'bg-warning-50 border-warning-200';
    return 'bg-danger-50 border-danger-200';
  };

  if (isLoading) {
    return (
      <MainLayout title="Insurance Advisor" subtitle="Personalized insurance recommendations">
        <div className="text-center py-12 text-slate-400">Loading recommendations...</div>
      </MainLayout>
    );
  }

  if (error) {
    return (
      <MainLayout title="Insurance Advisor" subtitle="Personalized insurance recommendations">
        <Card className="bg-danger-50 border-danger-200">
          <CardContent className="text-center py-8">
            <AlertTriangle className="mx-auto text-danger-500 mb-4" size={48} />
            <p className="text-danger-700">Failed to load recommendations. Please add your family members first.</p>
          </CardContent>
        </Card>
      </MainLayout>
    );
  }

  return (
    <MainLayout
      title="Insurance Advisor"
      subtitle="Personalized health and term insurance recommendations"
    >
      {/* Overall Score */}
      <Card className={`mb-6 ${getScoreBg(summary.overallScore || 0)}`}>
        <CardContent>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-full bg-white shadow-lg flex items-center justify-center">
                <span className={`text-3xl font-bold ${getScoreColor(summary.overallScore || 0)}`}>
                  {summary.overallScore || 0}
                </span>
              </div>
              <div>
                <h2 className="text-xl font-semibold text-slate-800">Insurance Score</h2>
                <p className="text-slate-600">{summary.status || 'Add family members for recommendations'}</p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-sm text-slate-500">Total Recommended Cover</p>
              <p className="text-2xl font-bold text-slate-800">
                {formatCurrency((health.totalRecommendedCover || 0) + (term.totalRecommendedCover || 0))}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Health Insurance */}
        <Card>
          <CardContent>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-3 rounded-xl bg-success-100">
                <Heart className="text-success-600" size={24} />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-800">Health Insurance</h3>
                <p className="text-sm text-slate-500">Recommended coverage for your family</p>
              </div>
            </div>

            {/* Summary */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div className="p-3 bg-slate-50 rounded-lg">
                <p className="text-xs text-slate-500">Recommended</p>
                <p className="text-lg font-bold text-success-600">{formatCurrency(health.totalRecommendedCover || 0)}</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg">
                <p className="text-xs text-slate-500">Existing</p>
                <p className="text-lg font-bold text-slate-600">{formatCurrency(health.existingCover || 0)}</p>
              </div>
            </div>

            {/* Gap Indicator */}
            {health.gap > 0 && (
              <div className="p-3 bg-warning-50 border border-warning-200 rounded-lg mb-6">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="text-warning-600" size={18} />
                  <span className="text-warning-700 font-medium">Gap: {formatCurrency(health.gap)}</span>
                </div>
              </div>
            )}
            {isGroupOnlyHealth && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg mb-6">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="text-amber-600" size={18} />
                  <span className="text-amber-700 font-medium">
                    Group health cover ends at retirement. Add a personal/family policy.
                  </span>
                </div>
              </div>
            )}

            {/* Member Breakdown */}
            <div className="space-y-3">
              <h4 className="font-medium text-slate-700">Recommended Policies</h4>
              {health.memberBreakdown?.map((member: any, index: number) => (
                <div 
                  key={index} 
                  className={`p-4 rounded-lg border ${member.isSupplementary ? 'bg-purple-50 border-purple-200' : 'bg-slate-50 border-slate-200'}`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium text-slate-800">{member.memberName || member.memberType}</p>
                      <p className="text-sm text-slate-500">{member.recommendedPolicyType}</p>
                    </div>
                    <div className="text-right">
                      <p className={`text-lg font-bold ${member.isSupplementary ? 'text-purple-600' : 'text-success-600'}`}>
                        {formatCurrency(member.recommendedCover || 0)}
                      </p>
                      {member.estimatedPremium && (
                        <p className="text-xs text-slate-500">~{formatCurrency(member.estimatedPremium)}/yr</p>
                      )}
                    </div>
                  </div>
                  {member.reasoning && (
                    <p className="text-sm text-slate-600 mt-2">{member.reasoning}</p>
                  )}
                  {member.isSupplementary && (
                    <p className="text-xs text-purple-700 mt-2">Not included in total - this is layered coverage</p>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Term Insurance */}
        <Card>
          <CardContent>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-3 rounded-xl bg-primary-100">
                <Umbrella className="text-primary-600" size={24} />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-800">Term Insurance</h3>
                <p className="text-sm text-slate-500">Life cover for earning members</p>
              </div>
            </div>

            {/* Summary */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div className="p-3 bg-slate-50 rounded-lg">
                <p className="text-xs text-slate-500">Recommended</p>
                <p className="text-lg font-bold text-primary-600">{formatCurrency(term.totalRecommendedCover || 0)}</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg">
                <p className="text-xs text-slate-500">Existing</p>
                <p className="text-lg font-bold text-slate-600">{formatCurrency(term.existingCover || 0)}</p>
              </div>
            </div>

            {/* Gap Indicator */}
            {term.gap > 0 && (
              <div className="p-3 bg-warning-50 border border-warning-200 rounded-lg mb-6">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="text-warning-600" size={18} />
                  <span className="text-warning-700 font-medium">Gap: {formatCurrency(term.gap)}</span>
                </div>
              </div>
            )}

            {/* Member Breakdown */}
            <div className="space-y-3">
              <h4 className="font-medium text-slate-700">Coverage by Member</h4>
              {term.memberBreakdown?.map((member: any, index: number) => (
                <div key={index} className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium text-slate-800">{member.memberName}</p>
                      <p className="text-sm text-slate-500">{member.methodology}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-bold text-primary-600">{formatCurrency(member.recommendedCover || 0)}</p>
                      {member.estimatedPremium && (
                        <p className="text-xs text-slate-500">~{formatCurrency(member.estimatedPremium)}/yr</p>
                      )}
                    </div>
                  </div>
                  {member.reasoning && (
                    <p className="text-sm text-slate-600 mt-2">{member.reasoning}</p>
                  )}
                </div>
              ))}

              {(!term.memberBreakdown || term.memberBreakdown.length === 0) && (
                <p className="text-slate-400 text-center py-4">No earning members to insure</p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Action Items */}
      {(health.policySuggestions?.length > 0 || term.policySuggestions?.length > 0) && (
        <Card className="mt-6">
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">💡 Recommended Actions</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {health.policySuggestions?.map((suggestion: string, index: number) => (
                <div key={`health-${index}`} className="flex items-start gap-3 p-3 bg-success-50 rounded-lg">
                  <CheckCircle className="text-success-600 flex-shrink-0 mt-0.5" size={18} />
                  <span className="text-sm text-slate-700">{suggestion}</span>
                </div>
              ))}
              {term.policySuggestions?.map((suggestion: string, index: number) => (
                <div key={`term-${index}`} className="flex items-start gap-3 p-3 bg-primary-50 rounded-lg">
                  <CheckCircle className="text-primary-600 flex-shrink-0 mt-0.5" size={18} />
                  <span className="text-sm text-slate-700">{suggestion}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </MainLayout>
  );
}

export default InsuranceRecommendations;
