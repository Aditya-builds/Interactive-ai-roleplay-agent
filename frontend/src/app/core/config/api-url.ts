import { environment } from '../../../environments/environment';

export function apiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const base = environment.apiUrl.replace(/\/$/, '');
  return `${base}${normalizedPath}`;
}
