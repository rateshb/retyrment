import { config } from '../config/env';

declare global {
  interface Window {
    dataLayer?: Array<Record<string, unknown> | unknown[]>;
    gtag?: (...args: unknown[]) => void;
    __retyrmentAnalyticsInitialized?: boolean;
    __retyrmentAnalyticsLastPageView?: { path: string; ts: number; eventId?: string };
  }
}

const isAnalyticsEnabled = Boolean(config.ga4MeasurementId);
const isDev = Boolean(config.isDevelopment);

let isInitialized = Boolean(window?.__retyrmentAnalyticsInitialized);
let lastPageViewPath: string | null = window?.__retyrmentAnalyticsLastPageView?.path ?? null;
let lastPageViewAt = window?.__retyrmentAnalyticsLastPageView?.ts ?? 0;
let lastPageViewEventId: string | null = window?.__retyrmentAnalyticsLastPageView?.eventId ?? null;

const createEventId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const ensureDataLayer = () => {
  if (!window.dataLayer) {
    window.dataLayer = [];
  }
};

const ensureGtag = () => {
  if (!window.gtag) {
    window.gtag = function () {
      window.dataLayer?.push(arguments as unknown as Record<string, unknown>[]);
    };
  }
};

export const initAnalytics = () => {
  if (isInitialized || !isAnalyticsEnabled || typeof window === 'undefined') {
    return;
  }

  ensureDataLayer();
  ensureGtag();

  const configureGa4 = () => {
    if (isDev) {
      window.gtag?.('set', 'debug_mode', true);
    }
    if (isDev) {
      window.gtag?.('consent', 'default', {
        analytics_storage: 'granted',
        ad_storage: 'denied',
      });
    }
    window.gtag?.('js', new Date());
    window.gtag?.('config', config.ga4MeasurementId, {
      send_page_view: false,
    });
  };

  const gaScriptId = 'ga4-script';
  if (!document.getElementById(gaScriptId)) {
    const gaScript = document.createElement('script');
    gaScript.id = gaScriptId;
    gaScript.async = true;
    gaScript.src = `https://www.googletagmanager.com/gtag/js?id=${config.ga4MeasurementId}`;
    gaScript.addEventListener('load', configureGa4);
    gaScript.addEventListener('error', () => {
      // Ignore load failures silently in production.
    });
    document.head.appendChild(gaScript);
  }

  // Queue config immediately in case script loads later.
  configureGa4();

  isInitialized = true;
  window.__retyrmentAnalyticsInitialized = true;

};

export const trackPageView = (pagePath: string) => {
  if (!isAnalyticsEnabled || typeof window === 'undefined') {
    return;
  }

  const now = Date.now();
  if (lastPageViewPath === pagePath && now - lastPageViewAt < 5000) {
    return;
  }
  const eventId = lastPageViewPath === pagePath && lastPageViewEventId
    ? lastPageViewEventId
    : createEventId();
  lastPageViewPath = pagePath;
  lastPageViewAt = now;
  lastPageViewEventId = eventId;
  window.__retyrmentAnalyticsLastPageView = { path: pagePath, ts: now, eventId };

  if (!isInitialized) {
    initAnalytics();
  }

  ensureDataLayer();
  ensureGtag();

  const pageLocation = window.location.href;
  const pageTitle = document.title;

  window.gtag?.('event', 'page_view', {
    send_to: config.ga4MeasurementId,
    event_id: eventId,
    page_path: pagePath,
    page_location: pageLocation,
    page_title: pageTitle,
  });


  // Avoid double-counting: GTM may auto-fire page views.
};

export const trackEvent = (eventName: string, params?: Record<string, unknown>) => {
  if (!isAnalyticsEnabled || typeof window === 'undefined') {
    return;
  }

  if (!isInitialized) {
    initAnalytics();
  }

  ensureDataLayer();
  ensureGtag();

  window.gtag?.('event', eventName, {
    send_to: config.ga4MeasurementId,
    ...(params || {}),
  });

  window.dataLayer?.push({
    event: eventName,
    ...(params || {}),
  });
};
