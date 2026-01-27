import { config } from '../config/env';

declare global {
  interface Window {
    dataLayer?: Array<Record<string, unknown> | unknown[]>;
    gtag?: (...args: unknown[]) => void;
  }
}

const isAnalyticsEnabled = Boolean(
  config.isProduction && config.ga4MeasurementId && config.gtmContainerId
);

let isInitialized = false;

const ensureDataLayer = () => {
  if (!window.dataLayer) {
    window.dataLayer = [];
  }
};

const ensureGtag = () => {
  if (!window.gtag) {
    window.gtag = (...args: unknown[]) => {
      window.dataLayer?.push(args);
    };
  }
};

export const initAnalytics = () => {
  if (isInitialized || !isAnalyticsEnabled || typeof window === 'undefined') {
    return;
  }

  ensureDataLayer();
  ensureGtag();

  const gtmScriptId = 'gtm-script';
  if (!document.getElementById(gtmScriptId)) {
    const gtmScript = document.createElement('script');
    gtmScript.id = gtmScriptId;
    gtmScript.async = true;
    gtmScript.src = `https://www.googletagmanager.com/gtm.js?id=${config.gtmContainerId}`;
    document.head.appendChild(gtmScript);
  }

  const gaScriptId = 'ga4-script';
  if (!document.getElementById(gaScriptId)) {
    const gaScript = document.createElement('script');
    gaScript.id = gaScriptId;
    gaScript.async = true;
    gaScript.src = `https://www.googletagmanager.com/gtag/js?id=${config.ga4MeasurementId}`;
    document.head.appendChild(gaScript);
  }

  window.gtag?.('js', new Date());
  window.gtag?.('config', config.ga4MeasurementId, {
    send_page_view: false,
  });

  const gtmNoScriptId = 'gtm-noscript';
  if (!document.getElementById(gtmNoScriptId)) {
    const noScript = document.createElement('noscript');
    noScript.id = gtmNoScriptId;
    noScript.innerHTML = `<iframe src=\"https://www.googletagmanager.com/ns.html?id=${config.gtmContainerId}\" height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe>`;
    document.body.appendChild(noScript);
  }

  isInitialized = true;
};

export const trackPageView = (pagePath: string) => {
  if (!isAnalyticsEnabled || typeof window === 'undefined') {
    return;
  }

  const pageLocation = window.location.href;
  const pageTitle = document.title;

  window.gtag?.('event', 'page_view', {
    page_path: pagePath,
    page_location: pageLocation,
    page_title: pageTitle,
  });

  window.dataLayer?.push({
    event: 'page_view',
    page_path: pagePath,
    page_location: pageLocation,
    page_title: pageTitle,
  });
};

export const trackEvent = (eventName: string, params?: Record<string, unknown>) => {
  if (!isAnalyticsEnabled || typeof window === 'undefined') {
    return;
  }

  window.gtag?.('event', eventName, params || {});
  window.dataLayer?.push({
    event: eventName,
    ...(params || {}),
  });
};
