import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';
import { isAxiosError } from 'axios';
import {
  getErrorMessage,
  getErrorCode,
  getErrorStatus,
  ERROR_CODES,
  LEGACY_NUMERIC_CODES,
  normalizeErrorCode,
} from './errorMessage';
import { ApiError } from '../types/api';

// contracts/api-error-fixtures.json 읽기 (런타임 번들에는 포함되지 않고 테스트에서만 fs로 로드)
const fixturesPath = path.resolve(__dirname, '../../../contracts/api-error-fixtures.json');
const fixtures = JSON.parse(fs.readFileSync(fixturesPath, 'utf-8'));

describe('오류 계약 상수 일치 검증', () => {
  it('ERROR_CODES 상수가 api-error-fixtures.json의 codes와 키 및 내용이 완전히 일치한다', () => {
    const fixtureCodes = fixtures.codes;
    const fixtureKeys = Object.keys(fixtureCodes).sort();
    const tsKeys = Object.keys(ERROR_CODES).sort();

    expect(tsKeys).toEqual(fixtureKeys);

    for (const key of fixtureKeys) {
      const expected = fixtureCodes[key];
      const actual = ERROR_CODES[key as keyof typeof ERROR_CODES];

      expect(actual.http).toBe(expected.http);
      expect(actual.message).toBe(expected.message);
      expect(actual.fixedMessage).toBe(expected.fixedMessage);
      if (expected.clientAction) {
        expect(actual.clientAction).toBe(expected.clientAction);
      }
    }
  });

  it('LEGACY_NUMERIC_CODES 상수가 api-error-fixtures.json의 legacyNumericCodes와 완전히 일치한다', () => {
    const fixtureLegacy = fixtures.legacyNumericCodes;
    expect(LEGACY_NUMERIC_CODES).toEqual(fixtureLegacy);
  });
});

describe('오류 해석 단일화 및 fixture 전 시나리오 검증', () => {
  const makeAxiosError = (status: number, data: unknown) =>
    Object.assign(new Error('Request failed with status code ' + status), {
      isAxiosError: true,
      response: {
        status,
        data,
      },
    });

  it('fixture의 모든 시나리오(gateway, core, assist)에 대해 getErrorCode, getErrorMessage, getErrorStatus를 검증한다', () => {
    const scenarios = fixtures.scenarios as Array<{
      id: string;
      origin: string;
      http: number;
      code: string;
      request: string;
    }>;

    expect(scenarios.length).toBeGreaterThan(0);

    for (const scenario of scenarios) {
      // 1. 표준 봉투 형태의 Axios 응답 에러 생성
      const errorWithMsg = makeAxiosError(scenario.http, {
        status: 'error',
        code: scenario.code,
        message: '사용자 지정 메시지',
      });

      expect(getErrorCode(errorWithMsg)).toBe(scenario.code);
      expect(getErrorStatus(errorWithMsg)).toBe(scenario.http);
      // body.message가 우선이어야 함
      expect(getErrorMessage(errorWithMsg)).toBe('사용자 지정 메시지');

      // 2. body.message가 누락된 경우 -> 계약 기본 문구로 fallback
      const errorWithoutMsg = makeAxiosError(scenario.http, {
        status: 'error',
        code: scenario.code,
      });

      const contractDefaultMessage = fixtures.codes[scenario.code].message;
      expect(getErrorMessage(errorWithoutMsg)).toBe(contractDefaultMessage);
    }
  });

  it('레거시 숫자 code는 대응되는 대문자 기계 코드로 정규화된다', () => {
    for (const [numericCode, expectedCode] of Object.entries(fixtures.legacyNumericCodes)) {
      const error = makeAxiosError(Number(numericCode), {
        status: 'error',
        code: numericCode,
      });

      expect(getErrorCode(error)).toBe(expectedCode);
      expect(normalizeErrorCode(numericCode)).toBe(expectedCode);
    }
  });

  it('HTTP 200 + status=error 응답(ApiError)에서 code, status, message가 온전히 추출된다', () => {
    const apiError = new ApiError('이미 사용 중인 이메일입니다.', 'DUPLICATE_EMAIL', 200, {
      status: 'error',
      code: 'DUPLICATE_EMAIL',
      message: '이미 사용 중인 이메일입니다.',
    });

    expect(getErrorCode(apiError)).toBe('DUPLICATE_EMAIL');
    expect(getErrorStatus(apiError)).toBe(200);
    expect(getErrorMessage(apiError)).toBe('이미 사용 중인 이메일입니다.');
  });

  it('ApiError에 message가 없으면 code의 계약 기본 문구를 반환한다', () => {
    const apiError = new ApiError('', 'AUTH_REQUIRED', 200, {
      status: 'error',
      code: 'AUTH_REQUIRED',
    });

    expect(getErrorCode(apiError)).toBe('AUTH_REQUIRED');
    expect(getErrorMessage(apiError)).toBe('로그인이 필요합니다.');
  });

  it('code와 legacy data가 공존할 때 code의 계약 기본 문구가 legacy data보다 우선한다 (AxiosError 및 ApiError)', () => {
    // AxiosError 형태
    const axiosErrorWithData = makeAxiosError(500, {
      status: 'error',
      code: '500',
      data: '레거시 data 문구',
    });
    expect(getErrorMessage(axiosErrorWithData)).toBe('서버 오류가 발생했습니다.');

    // ApiError 형태
    const apiErrorWithData = new ApiError('서버 오류가 발생했습니다.', '500', 200, {
      status: 'error',
      code: '500',
      data: '레거시 data 문구',
    });
    expect(getErrorMessage(apiErrorWithData)).toBe('서버 오류가 발생했습니다.');

    // 표준 기계 코드(INTERNAL_ERROR)인 경우도 동일
    const apiErrorWithInternalCode = new ApiError('', 'INTERNAL_ERROR', 500, {
      status: 'error',
      code: 'INTERNAL_ERROR',
      data: '레거시 data 문구',
    });
    expect(getErrorMessage(apiErrorWithInternalCode)).toBe('서버 오류가 발생했습니다.');
  });

  it('code가 없는 레거시 응답 { data: { message } } 또는 { data: string }은 data의 메시지를 반환한다', () => {
    const errorNestedData = makeAxiosError(400, { data: { message: '비즈니스 실패 메시지' } });
    expect(getErrorMessage(errorNestedData)).toBe('비즈니스 실패 메시지');

    const errorStringData = makeAxiosError(400, { data: '옛날 문자열 데이터' });
    expect(getErrorMessage(errorStringData)).toBe('옛날 문자열 데이터');

    // ApiError 형태에서도 code가 없으면 data.message 반환
    const apiErrorNoCode = new ApiError('', undefined, 400, {
      data: { message: 'ApiError 레거시 메시지' },
    });
    expect(getErrorMessage(apiErrorNoCode)).toBe('ApiError 레거시 메시지');
  });

  it('일반 Error 인스턴스 및 Axios 상태 기술 문구 필터링을 검증한다', () => {
    // 1. 일반 Error 인스턴스
    const plainError = new Error('네트워크 연결이 끊어졌습니다.');
    expect(getErrorMessage(plainError)).toBe('네트워크 연결이 끊어졌습니다.');

    // 2. Axios 기술 문구는 사용자에게 노출하지 않고 fallback 반환
    const axiosTechError = new Error('Request failed with status code 500');
    expect(getErrorMessage(axiosTechError, '기본 안내 문구')).toBe('기본 안내 문구');

    // 3. 알 수 없는 객체는 fallback 반환
    expect(getErrorMessage(null, '대체 문구')).toBe('대체 문구');
    expect(getErrorMessage(undefined)).toBe('알 수 없는 에러가 발생했습니다.');
  });

  it('409 가입 중복 응답(DUPLICATE_EMAIL, DUPLICATE_NICKNAME, DUPLICATE_PHONE)에서 서버 message 우선 및 계약 기본 문구 fallback을 검증한다', () => {
    // 서버 message가 존재하는 경우: 서버 message 우선
    const emailDupWithServerMsg = makeAxiosError(409, {
      status: 'error',
      code: 'DUPLICATE_EMAIL',
      message: '해당 이메일로 이미 가입된 계정이 존재합니다.',
    });
    expect(getErrorMessage(emailDupWithServerMsg)).toBe('해당 이메일로 이미 가입된 계정이 존재합니다.');
    expect(getErrorCode(emailDupWithServerMsg)).toBe('DUPLICATE_EMAIL');

    // 서버 message가 없는 경우: 계약 기본 문구 fallback
    const nicknameDup = makeAxiosError(409, {
      status: 'error',
      code: 'DUPLICATE_NICKNAME',
    });
    expect(getErrorMessage(nicknameDup)).toBe('이미 사용 중인 닉네임입니다.');
    expect(getErrorCode(nicknameDup)).toBe('DUPLICATE_NICKNAME');

    const phoneDup = makeAxiosError(409, {
      status: 'error',
      code: 'DUPLICATE_PHONE',
    });
    expect(getErrorMessage(phoneDup)).toBe('이미 등록된 전화번호입니다.');
    expect(getErrorCode(phoneDup)).toBe('DUPLICATE_PHONE');
  });
});
