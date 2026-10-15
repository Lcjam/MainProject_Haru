export type ErrorCode =
  | 'BAD_REQUEST'
  | 'INVALID_PATH'
  | 'AUTH_REQUIRED'
  | 'AUTH_EXPIRED'
  | 'AUTH_INVALID'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'METHOD_NOT_ALLOWED'
  | 'CONFLICT'
  | 'DUPLICATE_EMAIL'
  | 'DUPLICATE_NICKNAME'
  | 'DUPLICATE_PHONE'
  | 'PAYLOAD_TOO_LARGE'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'TOO_MANY_REQUESTS'
  | 'INTERNAL_ERROR'
  | 'UPSTREAM_UNAVAILABLE'
  | 'UPSTREAM_TIMEOUT';

export interface ApiErrorBody {
  status: 'error';
  code?: ErrorCode | string;
  message?: string;
  data?: any;
}

export interface ApiResponse<T = any> {
  status: 'success' | 'error';
  data?: T;
  message?: string;
  code?: ErrorCode | string;
}

export class ApiError extends Error {
  readonly status?: number;
  readonly code?: string;
  readonly responseBody?: any;

  constructor(message: string, code?: string, status?: number, responseBody?: any) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.responseBody = responseBody;
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}
