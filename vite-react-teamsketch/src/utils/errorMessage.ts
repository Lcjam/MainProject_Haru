import { isAxiosError } from 'axios';

/**
 * axios 에러 또는 일반 Error 에서 사람이 읽을 메시지를 안전하게 추출한다.
 * 서버 응답이 `{ data: { message } }` 또는 `{ message }` 형태인 경우를 모두 처리한다.
 * `any` 캐스팅 없이 타입 가드(isAxiosError)로 좁혀 쓰기 위한 공용 헬퍼.
 */
export const getErrorMessage = (
  error: unknown,
  fallback = '알 수 없는 에러가 발생했습니다.'
): string => {
  if (isAxiosError(error)) {
    const body = error.response?.data as
      | { message?: string; data?: { message?: string } }
      | undefined;
    return body?.data?.message || body?.message || error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message || fallback;
  }
  return fallback;
};

/** axios 에러의 HTTP status 코드를 안전하게 추출한다. axios 에러가 아니면 undefined. */
export const getErrorStatus = (error: unknown): number | undefined =>
  isAxiosError(error) ? error.response?.status : undefined;

/** axios 에러 응답 바디의 `code` 필드(예: DUPLICATE_EMAIL)를 안전하게 추출한다. */
export const getErrorCode = (error: unknown): string | undefined => {
  if (isAxiosError(error)) {
    const body = error.response?.data as { code?: string } | undefined;
    return body?.code;
  }
  return undefined;
};
