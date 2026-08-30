const configuredApiUrl =
  document.querySelector('meta[name="resume-api-url"]')?.getAttribute('content');

const isLocal =
  globalThis.location?.hostname === 'localhost' ||
  globalThis.location?.hostname === '127.0.0.1';

export const API_BASE_URL =
  configuredApiUrl && !configuredApiUrl.startsWith('__')
    ? configuredApiUrl.replace(/\/$/, '')
    : isLocal
      ? 'http://localhost:8080'
      : 'https://resume-analyzer-api-bbep.onrender.com';