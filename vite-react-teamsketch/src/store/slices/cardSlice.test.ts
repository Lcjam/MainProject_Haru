import { describe, it, expect } from 'vitest';
import cardReducer, { updateCardInfo, resetCardInfo, type ICardInfo } from './cardSlice';

const initialState: ICardInfo = {
  cardNum_1: 0,
  cardNum_2: 0,
  cardNum_3: 0,
  cardNum_4: 0,
  cardName: '',
  cardExpiry: '',
  lastName: '',
  firstName: ''
};

describe('cardSlice', () => {
  it('updateCardInfo: 부분 업데이트(나머지 필드는 유지)', () => {
    const state = cardReducer(
      initialState,
      updateCardInfo({ cardNum_1: 1234, cardName: 'Haru Card' })
    );
    expect(state.cardNum_1).toBe(1234);
    expect(state.cardName).toBe('Haru Card');
    expect(state.cardNum_2).toBe(0);
  });

  it('resetCardInfo: 초기 상태로 복귀', () => {
    const dirty = { ...initialState, cardNum_1: 9999, firstName: 'Jamie' };
    expect(cardReducer(dirty, resetCardInfo())).toEqual(initialState);
  });
});
