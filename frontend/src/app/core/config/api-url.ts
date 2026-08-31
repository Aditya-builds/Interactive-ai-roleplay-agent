import { environment } from '../../../environments/environment';

export function apiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const base = environment.apiUrl.replace(/\/$/, '');
  return `${base}${normalizedPath}`;
}

/** Resolve actor portrait URLs — API-served images need the backend base in dev/prod. */
export function resolveActorImageUrl(url?: string | null): string {
  if (!url?.trim()) {
    return '';
  }
  if (url.startsWith('/api/')) {
    return apiUrl(url);
  }
  return url;
}
