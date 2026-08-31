const LOCAL_API_URL = 'http://localhost:8080';

const PRODUCTION_API_URL =
  'https://resume-analyzer-api-bbep.onrender.com';

const hostname = globalThis.location?.hostname ?? '';

export const API_BASE_URL =
  hostname === 'localhost' ||
  hostname === '127.0.0.1'
    ? LOCAL_API_URL
    : PRODUCTION_API_URL;