import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { ApiKeyService } from '../services/api-key.service';

export const llmApiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const apiKey = inject(ApiKeyService).getKey();
  if (!apiKey || !req.url.includes('/api/')) {
    return next(req);
  }

  return next(req.clone({
    setHeaders: { 'X-LLM-Api-Key': apiKey }
  }));
};
