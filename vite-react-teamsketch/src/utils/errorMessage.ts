import { isAxiosError } from 'axios';
import { ApiError, ErrorCode } from '../types/api';

export const ERROR_CODES: Record<ErrorCode, { http: number; message: string; fixedMessage: boolean; clientAction?: string }> = {
  BAD_REQUEST: { http: 400, message: '잘못된 요청입니다.', fixedMessage: false },
  INVALID_PATH: { http: 400, message: '잘못된 요청 경로입니다.', fixedMessage: true },
  AUTH_REQUIRED: { http: 401, message: '로그인이 필요합니다.', fixedMessage: false, clientAction: 'login' },
  AUTH_EXPIRED: { http: 401, message: '인증이 만료되었습니다.', fixedMessage: true, clientAction: 'relogin' },
  AUTH_INVALID: { http: 401, message: '유효하지 않은 인증입니다.', fixedMessage: false, clientAction: 'relogin' },
  FORBIDDEN: { http: 403, message: '접근 권한이 없습니다.', fixedMessage: false },
  NOT_FOUND: { http: 404, message: '요청한 자원을 찾을 수 없습니다.', fixedMessage: false },
  METHOD_NOT_ALLOWED: { http: 405, message: '허용되지 않은 요청 방식입니다.', fixedMessage: true },
  CONFLICT: { http: 409, message: '요청이 현재 상태와 충돌합니다.', fixedMessage: false },
  DUPLICATE_EMAIL: { http: 409, message: '이미 사용 중인 이메일입니다.', fixedMessage: false },
  DUPLICATE_NICKNAME: { http: 409, message: '이미 사용 중인 닉네임입니다.', fixedMessage: false },
  DUPLICATE_PHONE: { http: 409, message: '이미 등록된 전화번호입니다.', fixedMessage: false },
  PAYLOAD_TOO_LARGE: { http: 413, message: '요청 크기가 너무 큽니다.', fixedMessage: true },
  UNSUPPORTED_MEDIA_TYPE: { http: 415, message: '지원하지 않는 요청 형식입니다.', fixedMessage: true },
  TOO_MANY_REQUESTS: { http: 429, message: '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.', fixedMessage: true },
  INTERNAL_ERROR: { http: 500, message: '서버 오류가 발생했습니다.', fixedMessage: true },
  UPSTREAM_UNAVAILABLE: { http: 503, message: '서비스에 일시적으로 연결할 수 없습니다.', fixedMessage: true },
  UPSTREAM_TIMEOUT: { http: 504, message: '서비스 응답이 지연되고 있습니다.', fixedMessage: true },
} as const;

export const LEGACY_NUMERIC_CODES: Record<string, ErrorCode> = {
  '400': 'BAD_REQUEST',
  '401': 'AUTH_REQUIRED',
  '403': 'FORBIDDEN',
  '404': 'NOT_FOUND',
  '405': 'METHOD_NOT_ALLOWED',
  '409': 'CONFLICT',
  '413': 'PAYLOAD_TOO_LARGE',
  '415': 'UNSUPPORTED_MEDIA_TYPE',
  '429': 'TOO_MANY_REQUESTS',
  '500': 'INTERNAL_ERROR',
  '502': 'UPSTREAM_UNAVAILABLE',
  '503': 'UPSTREAM_UNAVAILABLE',
  '504': 'UPSTREAM_TIMEOUT',
} as const;

export const normalizeErrorCode = (code?: unknown): string | undefined => {
  if (code === undefined || code === null) return undefined;
  const codeStr = String(code).trim();
  if (!codeStr) return undefined;
  return LEGACY_NUMERIC_CODES[codeStr] || codeStr;
};

interface ExtractedErrorInfo {
  message?: unknown;
  code?: unknown;
  data?: unknown;
  errorMessage?: string;
}

const isAxiosTechMessage = (msg: unknown): boolean =>
  typeof msg === 'string' && msg.startsWith('Request failed with status code');

/** 에러 객체에서 { message, code, data, errorMessage }를 안전하게 추출한다. */
const extractErrorInfo = (error: unknown): ExtractedErrorInfo => {
  if (!error || typeof error !== 'object') {
    return {};
  }

  if (error instanceof ApiError) {
    const body = error.responseBody;
    const bodyObj = typeof body === 'object' && body !== null ? (body as Record<string, unknown>) : undefined;
    return {
      message: bodyObj?.message,
      code: error.code ?? bodyObj?.code,
      data: bodyObj?.data,
      errorMessage: error.message,
    };
  }

  if (isAxiosError(error)) {
    const responseData = error.response?.data;
    const dataObj = typeof responseData === 'object' && responseData !== null
      ? (responseData as Record<string, unknown>)
      : undefined;
    return {
      message: dataObj?.message,
      code: dataObj?.code,
      data: dataObj?.data ?? (typeof responseData === 'string' ? responseData : undefined),
      errorMessage: error.message,
    };
  }

  if (error instanceof Error) {
    const record = error as unknown as Record<string, unknown>;
    const resp = typeof record.response === 'object' && record.response !== null
      ? (record.response as Record<string, unknown>)
      : undefined;
    const respData = typeof resp?.data === 'object' && resp?.data !== null
      ? (resp.data as Record<string, unknown>)
      : undefined;

    return {
      message: respData?.message ?? (typeof record.message === 'string' && !isAxiosTechMessage(record.message) ? record.message : undefined),
      code: respData?.code ?? record.code,
      data: respData?.data ?? record.data,
      errorMessage: error.message,
    };
  }

  const record = error as Record<string, unknown>;
  const resp = typeof record.response === 'object' && record.response !== null
    ? (record.response as Record<string, unknown>)
    : undefined;
  const respData = typeof resp?.data === 'object' && resp?.data !== null
    ? (resp.data as Record<string, unknown>)
    : undefined;

  return {
    message: respData?.message ?? record.message,
    code: respData?.code ?? record.code,
    data: respData?.data ?? record.data,
    errorMessage: typeof record.message === 'string' ? record.message : undefined,
  };
};

/** 추출된 에러 정보로부터 계약 우선순위에 따라 사람이 읽을 메시지를 결정한다. */
const resolveErrorMessage = (
  info: ExtractedErrorInfo,
  fallback: string
): string => {
  // 1. 비어 있지 않은 최상위 message
  if (
    typeof info.message === 'string' &&
    info.message.trim() &&
    !isAxiosTechMessage(info.message)
  ) {
    return info.message.trim();
  }

  // 2. 인식한 code의 계약 기본 문구
  const normalizedCode = normalizeErrorCode(info.code);
  if (normalizedCode && normalizedCode in ERROR_CODES) {
    return ERROR_CODES[normalizedCode as ErrorCode].message;
  }

  // 3. code가 없을 때만 레거시 data 문자열 또는 data.message
  if (!normalizedCode) {
    if (typeof info.data === 'string' && info.data.trim()) {
      return info.data.trim();
    }
    if (typeof info.data === 'object' && info.data !== null && 'message' in info.data) {
      const dataMsg = (info.data as Record<string, unknown>).message;
      if (typeof dataMsg === 'string' && dataMsg.trim()) {
        return dataMsg.trim();
      }
    }
  }

  // 4. Axios 기술 문구를 거른 Error.message
  if (
    typeof info.errorMessage === 'string' &&
    info.errorMessage.trim() &&
    !isAxiosTechMessage(info.errorMessage)
  ) {
    return info.errorMessage.trim();
  }

  // 5. fallback
  return fallback;
};

/**
 * axios 에러 또는 ApiError 또는 일반 Error 에서 사람이 읽을 메시지를 안전하게 추출한다.
 * 1. 비어 있지 않은 최상위 message
 * 2. 인식한 code의 계약 기본 문구
 * 3. code가 없을 때만 레거시 data 문자열 또는 data.message
 * 4. Axios 기술 문구를 거른 Error.message
 * 5. fallback 순서로 해석한다.
 */
export const getErrorMessage = (
  error: unknown,
  fallback = '알 수 없는 에러가 발생했습니다.'
): string => {
  const info = extractErrorInfo(error);
  return resolveErrorMessage(info, fallback);
};

/** axios 에러 또는 ApiError의 HTTP status 코드를 안전하게 추출한다. 없으면 undefined. */
export const getErrorStatus = (error: unknown): number | undefined => {
  if (error instanceof ApiError) {
    return error.status;
  }
  if (isAxiosError(error)) {
    return error.response?.status;
  }
  if (typeof error === 'object' && error !== null) {
    const record = error as Record<string, unknown>;
    if (typeof record.response === 'object' && record.response !== null) {
      const resp = record.response as Record<string, unknown>;
      if (typeof resp.status === 'number') {
        return resp.status;
      }
    }
    if (typeof record.status === 'number') {
      return record.status;
    }
  }
  return undefined;
};

/** 에러 응답의 `code` 필드를 추출하고 레거시 숫자 코드를 정규화한다. */
export const getErrorCode = (error: unknown): string | undefined => {
  if (error instanceof ApiError) {
    return normalizeErrorCode(error.code);
  }
  if (isAxiosError(error)) {
    const data = error.response?.data;
    if (typeof data === 'object' && data !== null && 'code' in data) {
      return normalizeErrorCode((data as Record<string, unknown>).code);
    }
  }
  if (typeof error === 'object' && error !== null) {
    const record = error as Record<string, unknown>;
    if (typeof record.response === 'object' && record.response !== null) {
      const resp = record.response as Record<string, unknown>;
      if (typeof resp.data === 'object' && resp.data !== null && 'code' in resp.data) {
        return normalizeErrorCode((resp.data as Record<string, unknown>).code);
      }
    }
    if ('code' in record) {
      return normalizeErrorCode(record.code);
    }
  }
  return undefined;
};
