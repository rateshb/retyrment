import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

// Mock config BEFORE imports
vi.mock('../config/env', () => ({
  config: {
    isProduction: true,
    ga4MeasurementId: 'G-TEST123',
    gtmContainerId: 'GTM-TEST123',
  },
}));

import { initAnalytics, trackPageView, trackEvent } from './analytics';

describe('analytics', () => {
  let mockDataLayer: any[];
  let mockGtag: ReturnType<typeof vi.fn>;
  let mockCreateElement: ReturnType<typeof vi.fn>;
  let mockGetElementById: ReturnType<typeof vi.fn>;
  let mockAppendChild: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockDataLayer = [];
    mockGtag = vi.fn();
    mockCreateElement = vi.fn(() => ({
      id: '',
      async: false,
      src: '',
      innerHTML: '',
    }));
    mockGetElementById = vi.fn(() => null);
    mockAppendChild = vi.fn();

    // Setup global mocks
    (global as any).window = {
      dataLayer: mockDataLayer,
      gtag: mockGtag,
      location: {
        href: 'http://localhost:3000/test',
      },
    };

    (global as any).document = {
      title: 'Test Page',
      head: {
        appendChild: mockAppendChild,
      },
      body: {
        appendChild: mockAppendChild,
      },
      createElement: mockCreateElement,
      getElementById: mockGetElementById,
    };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('initAnalytics', () => {
    it('should not throw errors when called', () => {
      expect(() => initAnalytics()).not.toThrow();
    });

    it('should handle repeated calls without errors', () => {
      expect(() => {
        initAnalytics();
        initAnalytics();
        initAnalytics();
      }).not.toThrow();
    });
  });

  describe('trackPageView', () => {
    it('should track page view when analytics is available', () => {
      trackPageView('/test-page');

      expect(mockGtag).toHaveBeenCalledWith(
        'event',
        'page_view',
        expect.objectContaining({
          page_path: '/test-page',
        })
      );
    });

    it('should push event to dataLayer', () => {
      trackPageView('/dashboard');

      expect(mockDataLayer.length).toBeGreaterThan(0);
      expect(mockDataLayer.some(item => 
        item && item.event === 'page_view' && item.page_path === '/dashboard'
      )).toBe(true);
    });

    it('should handle different page paths', () => {
      trackPageView('/settings');
      trackPageView('/profile');
      trackPageView('/home');

      expect(mockGtag).toHaveBeenCalledTimes(3);
    });

    it('should not error if gtag is undefined', () => {
      (global.window as any).gtag = undefined;

      expect(() => trackPageView('/test')).not.toThrow();
    });

    it('should not error if dataLayer is undefined', () => {
      (global.window as any).dataLayer = undefined;

      expect(() => trackPageView('/test')).not.toThrow();
    });

    it('should not error if window is undefined (SSR)', () => {
      (global as any).window = undefined;

      expect(() => trackPageView('/test')).not.toThrow();
    });

    it('should include page location and title', () => {
      trackPageView('/custom-page');

      expect(mockGtag).toHaveBeenCalledWith(
        'event',
        'page_view',
        expect.objectContaining({
          page_location: expect.any(String),
          page_title: expect.any(String),
        })
      );
    });
  });

  describe('trackEvent', () => {
    it('should track custom events', () => {
      trackEvent('button_click', { button_name: 'submit' });

      expect(mockGtag).toHaveBeenCalledWith('event', 'button_click', {
        button_name: 'submit',
      });
    });

    it('should push event to dataLayer with parameters', () => {
      trackEvent('form_submit', { form_id: 'contact' });

      expect(mockDataLayer.length).toBeGreaterThan(0);
      expect(mockDataLayer.some(item =>
        item && item.event === 'form_submit' && item.form_id === 'contact'
      )).toBe(true);
    });

    it('should track event without parameters', () => {
      trackEvent('page_load');

      expect(mockGtag).toHaveBeenCalledWith('event', 'page_load', {});
    });

    it('should handle undefined parameters', () => {
      expect(() => trackEvent('test_event', undefined)).not.toThrow();

      expect(mockGtag).toHaveBeenCalledWith('event', 'test_event', {});
    });

    it('should handle empty parameters object', () => {
      trackEvent('test_event', {});

      expect(mockGtag).toHaveBeenCalledWith('event', 'test_event', {});
    });

    it('should track multiple events', () => {
      trackEvent('event1');
      trackEvent('event2');
      trackEvent('event3');

      expect(mockGtag).toHaveBeenCalledTimes(3);
    });

    it('should handle complex parameters', () => {
      const complexParams = {
        category: 'user_action',
        label: 'sign_up',
        value: 100,
        user_id: 'user123',
      };

      trackEvent('conversion', complexParams);

      expect(mockGtag).toHaveBeenCalledWith('event', 'conversion', complexParams);
    });

    it('should not error if gtag is undefined', () => {
      (global.window as any).gtag = undefined;

      expect(() => trackEvent('test', { param: 'value' })).not.toThrow();
    });

    it('should not error if dataLayer is undefined', () => {
      (global.window as any).dataLayer = undefined;

      expect(() => trackEvent('test')).not.toThrow();
    });

    it('should not error if window is undefined (SSR)', () => {
      (global as any).window = undefined;

      expect(() => trackEvent('test')).not.toThrow();
    });

    it('should handle special characters in event names', () => {
      expect(() => trackEvent('user-action_123', { 'param-key': 'value_123' })).not.toThrow();
    });

    it('should handle numeric values in parameters', () => {
      trackEvent('purchase', { price: 99.99, quantity: 2 });

      expect(mockGtag).toHaveBeenCalledWith('event', 'purchase', {
        price: 99.99,
        quantity: 2,
      });
    });

    it('should handle boolean values in parameters', () => {
      trackEvent('feature_toggle', { enabled: true });

      expect(mockGtag).toHaveBeenCalledWith('event', 'feature_toggle', {
        enabled: true,
      });
    });

    it('should handle array values in parameters', () => {
      trackEvent('select_items', { items: ['item1', 'item2'] });

      expect(mockGtag).toHaveBeenCalledWith('event', 'select_items', {
        items: ['item1', 'item2'],
      });
    });
  });

  describe('Edge Cases and Safety', () => {
    it('should handle missing window.gtag gracefully', () => {
      delete (global.window as any).gtag;

      expect(() => {
        trackEvent('test');
        trackPageView('/test');
      }).not.toThrow();
    });

    it('should handle missing window.dataLayer gracefully', () => {
      delete (global.window as any).dataLayer;

      expect(() => {
        trackEvent('test');
        trackPageView('/test');
      }).not.toThrow();
    });

    it('should handle missing window object gracefully (SSR)', () => {
      const original = global.window;
      (global as any).window = undefined;

      expect(() => {
        initAnalytics();
        trackEvent('test');
        trackPageView('/test');
      }).not.toThrow();

      (global as any).window = original;
    });

    it('should handle null parameters', () => {
      expect(() => trackEvent('test', null as any)).not.toThrow();
    });

    it('should handle very long event names', () => {
      const longEventName = 'a'.repeat(1000);
      expect(() => trackEvent(longEventName)).not.toThrow();
    });

    it('should handle special characters in page paths', () => {
      expect(() => trackPageView('/page?query=test&id=123#section')).not.toThrow();
    });

    it('should handle empty string event name', () => {
      expect(() => trackEvent('')).not.toThrow();
    });

    it('should handle empty string page path', () => {
      expect(() => trackPageView('')).not.toThrow();
    });
  });
});
