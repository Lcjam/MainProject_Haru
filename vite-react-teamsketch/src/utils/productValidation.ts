import { IProductRegisterForm } from '../store/slices/productSlice';

/**
 * 상품 등록 폼 검증.
 * 첫 번째로 실패한 규칙의 사용자 메시지를 반환하고, 모두 통과하면 null을 반환한다.
 * (ProductRegister.handleSubmit에서 분리 — 순수 함수라 단위 테스트 가능)
 */
export const validateProductForm = (
  form: IProductRegisterForm,
  userEmail?: string
): string | null => {
  if (!userEmail) return '로그인이 필요합니다.';

  if (!form.title) return '제목을 입력해주세요.';
  if (!form.price) return '가격을 입력해주세요.';
  if (!form.transactionType) return '거래 유형을 선택해주세요.';
  if (!form.registrationType) return '등록 유형을 선택해주세요.';
  if (form.transactionType === '대면' && !form.meetingPlace) {
    return '대면 거래는 장소입력이 필수입니다.';
  }
  if (!form.categoryId) return '카테고리를 선택해주세요.';
  if (!form.hobbyId) return '취미를 선택해주세요.';
  if (!form.maxParticipants) return '모집 인원을 입력해주세요.';
  if (!form.startDate) return '시작 일시를 입력해주세요.';
  if (!form.endDate) return '종료 일시를 입력해주세요.';
  if (!form.days) return '진행 요일을 선택해주세요.';
  if (!form.description) return '상품 설명을 입력해주세요.';

  // 대면 거래인 경우 위치 정보 검증
  if (
    form.transactionType === '대면' &&
    (!form.meetingPlace || !form.latitude || !form.longitude || !form.address)
  ) {
    return '대면 거래의 경우 위치 정보가 필요합니다.';
  }

  return null;
};
