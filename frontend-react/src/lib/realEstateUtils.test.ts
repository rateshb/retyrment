import { describe, it, expect } from 'vitest';
import { 
  calculateTotalRentalIncome, 
  calculateNetRentalIncome, 
  getRealEstateByType,
  getRealEstateValueByType,
  generateRealEstateRecommendations 
} from './realEstateUtils';
import { Investment } from './types';

describe('realEstateUtils', () => {
  const mockRentalProperty: Investment = {
    id: '1',
    name: 'Rental Apartment',
    type: 'REAL_ESTATE',
    realEstateType: 'RENTAL',
    currentValue: 5000000,
    monthlyRentalIncome: 25000,
    vacancyRate: 10,
    maintenanceCost: 24000,
    propertyTax: 12000,
    rentalYield: 5,
  };

  const mockSelfOccupied: Investment = {
    id: '2',
    name: 'Primary Home',
    type: 'REAL_ESTATE',
    realEstateType: 'SELF_OCCUPIED',
    currentValue: 10000000,
    isPrimaryResidence: true,
  };

  const mockInvestmentProperty: Investment = {
    id: '3',
    name: 'Plot',
    type: 'REAL_ESTATE',
    realEstateType: 'INVESTMENT',
    currentValue: 3000000,
    expectedAppreciation: 8,
  };

  const mockInheritedProperty: Investment = {
    id: '4',
    name: 'Family Home',
    type: 'REAL_ESTATE',
    realEstateType: 'INHERITED',
    currentValue: 8000000,
  };

  const mockMutualFund: Investment = {
    id: '5',
    name: 'HDFC Equity',
    type: 'MUTUAL_FUND',
    currentValue: 500000,
  };

  describe('calculateTotalRentalIncome', () => {
    it('calculates rental income with vacancy rate', () => {
      const investments = [mockRentalProperty, mockSelfOccupied];
      const result = calculateTotalRentalIncome(investments);
      // 25000 * (1 - 0.10) = 22500
      expect(result).toBe(22500);
    });

    it('returns 0 when no rental properties exist', () => {
      const investments = [mockSelfOccupied, mockMutualFund];
      const result = calculateTotalRentalIncome(investments);
      expect(result).toBe(0);
    });

    it('handles empty investment list', () => {
      const result = calculateTotalRentalIncome([]);
      expect(result).toBe(0);
    });
  });

  describe('calculateNetRentalIncome', () => {
    it('calculates net rental income after expenses', () => {
      const result = calculateNetRentalIncome(mockRentalProperty);
      // 25000 * (1 - 0.10) - 2000 - 1000 = 22500 - 2000 - 1000 = 19500
      expect(result).toBe(19500);
    });

    it('returns 0 for non-rental property', () => {
      const result = calculateNetRentalIncome(mockSelfOccupied);
      expect(result).toBe(0);
    });

    it('returns 0 for non-real-estate investment', () => {
      const result = calculateNetRentalIncome(mockMutualFund);
      expect(result).toBe(0);
    });
  });

  describe('getRealEstateByType', () => {
    it('returns properties of specified type', () => {
      const investments = [mockRentalProperty, mockSelfOccupied, mockInvestmentProperty, mockMutualFund];
      const result = getRealEstateByType(investments, 'RENTAL');
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe('1');
    });

    it('returns empty array when no matches', () => {
      const investments = [mockSelfOccupied, mockMutualFund];
      const result = getRealEstateByType(investments, 'RENTAL');
      expect(result).toHaveLength(0);
    });
  });

  describe('getRealEstateValueByType', () => {
    it('calculates total value of specified type', () => {
      const investments = [mockRentalProperty, mockSelfOccupied];
      const result = getRealEstateValueByType(investments, 'SELF_OCCUPIED');
      expect(result).toBe(10000000);
    });

    it('returns 0 when no matches', () => {
      const investments = [mockSelfOccupied];
      const result = getRealEstateValueByType(investments, 'RENTAL');
      expect(result).toBe(0);
    });
  });

  describe('generateRealEstateRecommendations', () => {
    it('generates recommendations for self-occupied property', () => {
      const investments = [mockSelfOccupied];
      const result = generateRealEstateRecommendations(investments);
      expect(result).toHaveLength(1);
      expect(result[0].recommendation).toBe('Keep');
      expect(result[0].priority).toBe('High');
    });

    it('generates recommendations for high-yield rental property', () => {
      const investments = [mockRentalProperty];
      const result = generateRealEstateRecommendations(investments);
      expect(result).toHaveLength(1);
      expect(result[0].recommendation).toBe('Keep');
    });

    it('generates recommendations for low-yield rental property', () => {
      const lowYieldRental = { ...mockRentalProperty, rentalYield: 2 };
      const result = generateRealEstateRecommendations([lowYieldRental]);
      expect(result[0].recommendation).toBe('Evaluate');
    });

    it('generates recommendations for investment property', () => {
      const investments = [mockInvestmentProperty];
      const result = generateRealEstateRecommendations(investments);
      expect(result).toHaveLength(1);
      expect(result[0].recommendation).toBe('Keep');
    });

    it('generates recommendations for low-appreciation investment', () => {
      const lowAppreciation = { ...mockInvestmentProperty, expectedAppreciation: 4 };
      const result = generateRealEstateRecommendations([lowAppreciation]);
      expect(result[0].recommendation).toBe('Consider Selling');
    });

    it('generates recommendations for inherited property', () => {
      const investments = [mockInheritedProperty];
      const result = generateRealEstateRecommendations(investments);
      expect(result).toHaveLength(1);
      expect(result[0].recommendation).toBe('Evaluate');
    });

    it('filters out non-real-estate investments', () => {
      const investments = [mockMutualFund];
      const result = generateRealEstateRecommendations(investments);
      expect(result).toHaveLength(0);
    });
  });
});
