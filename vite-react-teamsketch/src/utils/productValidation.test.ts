import { describe, it, expect } from 'vitest';
import { validateProductForm } from './productValidation';
import { IProductRegisterForm } from '../store/slices/productSlice';

/** 모든 필수 항목이 채워진 유효한 비대면 폼 (테스트 베이스) */
const validForm = (): IProductRegisterForm => ({
  title: '기타 레슨',
  description: '주말 기타 모임입니다.',
  price: 10000,
  categoryId: 1,
  hobbyId: 2,
  transactionType: '비대면',
  registrationType: '판매',
  maxParticipants: 4,
  currentParticipants: 0,
  meetingPlace: '',
  address: '',
  latitude: null,
  longitude: null,
  images: [],
  days: ['월', '수'],
  startDate: '2026-07-01T10:00',
  endDate: '2026-07-01T12:00'
});

describe('validateProductForm', () => {
  it('이메일이 없으면 로그인 필요 메시지', () => {
    expect(validateProductForm(validForm(), undefined)).toBe('로그인이 필요합니다.');
  });

  it('유효한 비대면 폼은 null(통과)', () => {
    expect(validateProductForm(validForm(), 'user@haru.com')).toBeNull();
  });

  it('제목이 비면 거부', () => {
    expect(validateProductForm({ ...validForm(), title: '' }, 'user@haru.com')).toBe(
      '제목을 입력해주세요.'
    );
  });

  it('가격이 없거나 0이면 거부', () => {
    expect(validateProductForm({ ...validForm(), price: null }, 'user@haru.com')).toBe(
      '가격을 입력해주세요.'
    );
    expect(validateProductForm({ ...validForm(), price: 0 }, 'user@haru.com')).toBe(
      '가격을 입력해주세요.'
    );
  });

  it('대면 거래인데 장소가 없으면 장소 입력 메시지', () => {
    const form = { ...validForm(), transactionType: '대면' as const, meetingPlace: '' };
    expect(validateProductForm(form, 'user@haru.com')).toBe('대면 거래는 장소입력이 필수입니다.');
  });

  it('대면 거래는 장소가 있어도 위치 좌표/주소가 없으면 거부', () => {
    const form = {
      ...validForm(),
      transactionType: '대면' as const,
      meetingPlace: '강남역',
      latitude: null,
      longitude: null,
      address: ''
    };
    expect(validateProductForm(form, 'user@haru.com')).toBe(
      '대면 거래의 경우 위치 정보가 필요합니다.'
    );
  });

  it('대면 거래에 장소+좌표+주소가 모두 있으면 통과', () => {
    const form = {
      ...validForm(),
      transactionType: '대면' as const,
      meetingPlace: '강남역',
      latitude: 37.49,
      longitude: 127.02,
      address: '서울 강남구'
    };
    expect(validateProductForm(form, 'user@haru.com')).toBeNull();
  });

  it('설명이 비면 거부', () => {
    expect(validateProductForm({ ...validForm(), description: '' }, 'user@haru.com')).toBe(
      '상품 설명을 입력해주세요.'
    );
  });
});
