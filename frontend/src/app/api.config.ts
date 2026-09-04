const LOCAL_API_URL = 'http://localhost:8080';

const PRODUCTION_API_URL =
  'https://resume-analyzer-api-bbep.onrender.com';

const configuredApiUrl =
  globalThis.document?.querySelector('meta[name="resume-api-url"]')?.getAttribute('content');

const hostname = globalThis.location?.hostname ?? '';

export const API_BASE_URL =
  configuredApiUrl && !configuredApiUrl.startsWith('__')
    ? configuredApiUrl.replace(/\/$/, '')
    : hostname === 'localhost' || hostname === '127.0.0.1'
      ? LOCAL_API_URL
      : PRODUCTION_API_URL;