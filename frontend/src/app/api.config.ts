const LOCAL_API_URL = 'http://localhost:8080';

const configuredApiUrl =
  globalThis.document?.querySelector('meta[name="resume-api-url"]')?.getAttribute('content');

export const API_BASE_URL =
  configuredApiUrl && !configuredApiUrl.startsWith('__')
    ? configuredApiUrl.replace(/\/$/, '')
    : LOCAL_API_URL;
