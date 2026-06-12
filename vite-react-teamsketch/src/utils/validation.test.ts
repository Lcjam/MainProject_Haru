import { describe, it, expect } from 'vitest';
import {
  validateName,
  validateId,
  validatePassword,
  validateEmail,
  validatePhone,
  validateNickname
} from './validation';

describe('validateName', () => {
  it('빈 값은 거부', () => {
    expect(validateName('').isValid).toBe(false);
  });
  it('2자 미만은 거부', () => {
    expect(validateName('가').isValid).toBe(false);
  });
  it('숫자/특수문자 포함 시 거부', () => {
    expect(validateName('홍길동1').isValid).toBe(false);
    expect(validateName('hong dong').isValid).toBe(false);
  });
  it('한글/영문 2자 이상은 통과', () => {
    expect(validateName('홍길동')).toEqual({ isValid: true, message: '' });
    expect(validateName('John').isValid).toBe(true);
  });
});

describe('validateId', () => {
  it('빈 값은 거부', () => {
    expect(validateId('').isValid).toBe(false);
  });
  it('4자 미만 또는 20자 초과는 거부', () => {
    expect(validateId('abc').isValid).toBe(false);
    expect(validateId('a'.repeat(21)).isValid).toBe(false);
  });
  it('특수문자/공백 포함 시 거부', () => {
    expect(validateId('user_id').isValid).toBe(false);
    expect(validateId('user id').isValid).toBe(false);
  });
  it('4~20자 영문/숫자는 통과', () => {
    expect(validateId('user1234').isValid).toBe(true);
  });
});

describe('validatePassword', () => {
  it('빈 값은 거부', () => {
    expect(validatePassword('').isValid).toBe(false);
  });
  it('8자 미만은 거부', () => {
    expect(validatePassword('Ab1!').isValid).toBe(false);
  });
  it('영문/숫자/특수문자 중 하나라도 빠지면 거부', () => {
    expect(validatePassword('abcdefgh').isValid).toBe(false); // 숫자/특수 없음
    expect(validatePassword('abcd1234').isValid).toBe(false); // 특수 없음
    expect(validatePassword('abcd!@#$').isValid).toBe(false); // 숫자 없음
  });
  it('영문+숫자+특수문자 8자 이상은 통과', () => {
    expect(validatePassword('abcd1234!').isValid).toBe(true);
  });
});

describe('validateEmail', () => {
  it('빈 값은 거부', () => {
    expect(validateEmail('').isValid).toBe(false);
  });
  it('형식이 틀리면 거부', () => {
    expect(validateEmail('not-an-email').isValid).toBe(false);
    expect(validateEmail('a@b').isValid).toBe(false);
    expect(validateEmail('a b@c.com').isValid).toBe(false);
  });
  it('올바른 형식은 통과', () => {
    expect(validateEmail('user@haru.com').isValid).toBe(true);
  });
});

describe('validatePhone', () => {
  it('빈 값은 거부', () => {
    expect(validatePhone('').isValid).toBe(false);
  });
  it('형식이 틀리면 거부', () => {
    expect(validatePhone('123-456-7890').isValid).toBe(false);
    expect(validatePhone('010123456').isValid).toBe(false);
  });
  it('하이픈 유무 모두 통과', () => {
    expect(validatePhone('010-1234-5678').isValid).toBe(true);
    expect(validatePhone('01012345678').isValid).toBe(true);
  });
});

describe('validateNickname', () => {
  it('빈 값은 거부', () => {
    expect(validateNickname('').isValid).toBe(false);
  });
  it('2자 미만 또는 10자 초과는 거부', () => {
    expect(validateNickname('가').isValid).toBe(false);
    expect(validateNickname('a'.repeat(11)).isValid).toBe(false);
  });
  it('2~10자는 통과', () => {
    expect(validateNickname('하루').isValid).toBe(true);
  });
});
