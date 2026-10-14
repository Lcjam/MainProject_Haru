import { expect, it } from 'vitest';
import { getErrorMessage } from './errorMessage';

it('새 최상위 메시지를 우선하고 이전 문자열 data도 읽는다', () => {
  const error = (data: unknown) => Object.assign(new Error('network'), {
    isAxiosError: true,
    response: { data }
  });
  expect(getErrorMessage(error({ message: '표시 문구', data: '옛 문구' }))).toBe('표시 문구');
  expect(getErrorMessage(error({ data: '옛 문구' }))).toBe('옛 문구');
});
