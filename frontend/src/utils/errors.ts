import { isAxiosError } from 'axios';
import type { ApiErrorResponse } from '../types';

/**
 * Pulls the backend's { message } out of an Axios error response (see
 * GlobalExceptionHandler / ErrorResponse on the backend), falling back
 * to a generic message for network errors or anything unexpected.
 */
export function extractApiErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'string') {
    return error;
  }
  if (isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}
