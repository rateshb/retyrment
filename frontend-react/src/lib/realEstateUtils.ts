import { Investment } from './types';
import { formatCurrency } from './utils';

// Calculate total monthly rental income from all rental properties
export const calculateTotalRentalIncome = (investments: Investment[]): number => {
  return investments
    .filter(inv => inv.type === 'REAL_ESTATE' && inv.realEstateType === 'RENTAL' && inv.monthlyRentalIncome)
    .reduce((total, inv) => {
      const rentalIncome = inv.monthlyRentalIncome || 0;
      const vacancyRate = (inv.vacancyRate || 10) / 100; // Default 10% vacancy
      return total + (rentalIncome * (1 - vacancyRate));
    }, 0);
};

// Calculate net rental income after expenses
export const calculateNetRentalIncome = (investment: Investment): number => {
  if (investment.type !== 'REAL_ESTATE' || investment.realEstateType !== 'RENTAL') {
    return 0;
  }
  
  const grossIncome = investment.monthlyRentalIncome || 0;
  const vacancyRate = (investment.vacancyRate || 10) / 100;
  const monthlyMaintenance = (investment.maintenanceCost || 0) / 12;
  const monthlyPropertyTax = (investment.propertyTax || 0) / 12;
  
  const effectiveIncome = grossIncome * (1 - vacancyRate);
  const netIncome = effectiveIncome - monthlyMaintenance - monthlyPropertyTax;
  
  return Math.max(0, netIncome);
};

// Get real estate properties by usage type
export const getRealEstateByType = (investments: Investment[], type: string): Investment[] => {
  return investments.filter(inv => inv.type === 'REAL_ESTATE' && inv.realEstateType === type);
};

// Calculate total value of real estate by usage type
export const getRealEstateValueByType = (investments: Investment[], type: string): number => {
  return getRealEstateByType(investments, type)
    .reduce((total, inv) => total + (inv.currentValue || 0), 0);
};

// Generate real estate recommendations for retirement
export const generateRealEstateRecommendations = (investments: Investment[]): Array<{
  property: Investment;
  recommendation: string;
  reason: string;
  priority: 'High' | 'Medium' | 'Low';
}> => {
  const recommendations: Array<{
    property: Investment;
    recommendation: string;
    reason: string;
    priority: 'High' | 'Medium' | 'Low';
  }> = [];

  investments
    .filter(inv => inv.type === 'REAL_ESTATE')
    .forEach(property => {
      const netIncome = calculateNetRentalIncome(property);
      const currentValue = property.currentValue || 0;
      const rentalYield = property.rentalYield || 0;

      switch (property.realEstateType) {
        case 'SELF_OCCUPIED':
          recommendations.push({
            property,
            recommendation: 'Keep',
            reason: property.isPrimaryResidence 
              ? 'Primary residence provides housing security and saves rent costs'
              : 'Self-occupied property provides emotional value and housing flexibility',
            priority: 'High'
          });
          break;

        case 'RENTAL':
          if (rentalYield >= 3) {
            recommendations.push({
              property,
              recommendation: 'Keep',
              reason: `Good rental yield (${rentalYield.toFixed(1)}%) generates ${formatCurrency(netIncome)}/month passive income`,
              priority: 'High'
            });
          } else {
            recommendations.push({
              property,
              recommendation: 'Evaluate',
              reason: `Low rental yield (${rentalYield.toFixed(1)}%). Consider selling and reinvesting in higher-yield assets`,
              priority: 'Medium'
            });
          }
          break;

        case 'INVESTMENT':
          const appreciation = property.expectedAppreciation || 5;
          if (appreciation >= 7) {
            recommendations.push({
              property,
              recommendation: 'Keep',
              reason: `Strong appreciation potential (${appreciation}%) makes this a good long-term investment`,
              priority: 'Medium'
            });
          } else {
            recommendations.push({
              property,
              recommendation: 'Consider Selling',
              reason: `Low appreciation (${appreciation}%). Consider partial selling for diversification`,
              priority: 'Low'
            });
          }
          break;

        case 'INHERITED':
          recommendations.push({
            property,
            recommendation: 'Evaluate',
            reason: 'Inherited property has emotional value. Consider tax implications before selling',
            priority: 'Medium'
          });
          break;
      }
    });

  return recommendations;
};
