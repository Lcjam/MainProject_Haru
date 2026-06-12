export interface ValidationResult {
  isValid: boolean;
  message: string;
}

export const validateName = (name: string): ValidationResult => {
  if (!name) {
    return { isValid: false, message: '이름을 입력해주세요.' };
  }
  if (name.length < 2) {
    return { isValid: false, message: '이름은 2자 이상이어야 합니다.' };
  }
  if (!/^[가-힣a-zA-Z]+$/.test(name)) {
    return { isValid: false, message: '이름은 한글 또는 영문만 가능합니다.' };
  }
  return { isValid: true, message: '' };
};

export const validateId = (id: string): ValidationResult => {
  if (!id) {
    return { isValid: false, message: '아이디를 입력해주세요.' };
  }
  if (!/^[a-zA-Z0-9]{4,20}$/.test(id)) {
    return { isValid: false, message: '아이디는 4~20자의 영문자 또는 숫자여야 합니다.' };
  }
  return { isValid: true, message: '' };
};

export const validatePassword = (password: string): ValidationResult => {
  if (!password) {
    return { isValid: false, message: '비밀번호를 입력해주세요.' };
  }
  if (password.length < 8) {
    return { isValid: false, message: '비밀번호는 8자 이상이어야 합니다.' };
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(password)) {
    return { isValid: false, message: '비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.' };
  }
  return { isValid: true, message: '' };
};

export const validateEmail = (email: string): ValidationResult => {
  if (!email) {
    return { isValid: false, message: '이메일을 입력해주세요.' };
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return { isValid: false, message: '올바른 이메일 형식이 아닙니다.' };
  }
  return { isValid: true, message: '' };
};

export const validatePhone = (phoneNumber: string): ValidationResult => {
  if (!phoneNumber) {
    return { isValid: false, message: '전화번호를 입력해주세요.' };
  }
  if (!/^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$/.test(phoneNumber)) {
    return { isValid: false, message: '올바른 전화번호 형식이 아닙니다.' };
  }
  return { isValid: true, message: '' };
};

export const validateNickname = (nickname: string): ValidationResult => {
  if (!nickname) {
    return { isValid: false, message: '닉네임을 입력해주세요.' };
  }
  if (nickname.length < 2 || nickname.length > 10) {
    return { isValid: false, message: '닉네임은 2~10자 사이여야 합니다.' };
  }
  return { isValid: true, message: '' };
};
