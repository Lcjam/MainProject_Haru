import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import {
  updateField,
  setValidationError,
  addHobby,
  removeHobby,
  resetSignupInfo
} from '../../store/slices/signupSlice';
//import { Category } from '../../types/auth';
import SignupLayout from '../../components/layout/SignupLayout';
import Button from '../../components/common/BaseButton';
import {
  validateName,
  validatePassword,
  validateEmail,
  validatePhone,
  validateNickname
} from '../../utils/validation';

import TextInput from '../../components/forms/input/TextInput';
import PasswordInput from '../../components/forms/input/PasswordInput';
import EmailInput from '../../components/forms/input/EmailInput';
import InterestSelect from '../../components/forms/select/InterestSelect';
import { useSignup } from '../../services/api/authAPI';
import { SignupForm, HobbiesRequest } from '../../types/auth';
import HobbySelect from '../../components/forms/select/HobbySelect';
import { useState } from 'react';
import { toast } from 'react-toastify';

const Signup = () => {
  const signupMutation = useSignup();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { formData, validationErrors } = useAppSelector((state) => state.signup);

  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [selectedHobbies, setSelectedHobbies] = useState<HobbiesRequest[]>([]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    dispatch(updateField({ name: name as keyof SignupForm, value }));

    let validationResult = { isValid: true, message: '' };
    switch (name) {
      case 'name':
        validationResult = validateName(value);
        break;
      case 'password':
        validationResult = validatePassword(value);
        break;
      case 'email':
        validationResult = validateEmail(value);
        break;
      case 'phoneNumber':
        validationResult = validatePhone(value);
        break;
      case 'nickname':
        validationResult = validateNickname(value);
        break;
    }

    dispatch(setValidationError({ field: name, message: validationResult.message }));
  };

  const handleInterestSelect = (categoryId: number) => {
    setSelectedCategoryId(categoryId);
  };

  const handleHobbySelect = (categoryId: number, hobbyId: number) => {
    setSelectedHobbies((prev) => {
      const exists = prev.some(
        (hobby) => hobby.categoryId === categoryId && hobby.hobbyId === hobbyId
      );

      if (exists) {
        const newHobbies = prev.filter(
          (hobby) => !(hobby.categoryId === categoryId && hobby.hobbyId === hobbyId)
        );
        dispatch(removeHobby({ categoryId, hobbyId }));
        return newHobbies;
      } else {
        const newHobby = { categoryId, hobbyId };
        dispatch(addHobby(newHobby));
        return [...prev, newHobby];
      }
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      // 모든 필수 필드 검증
      const requiredFields = ['name', 'password', 'email', 'phoneNumber', 'nickname'];
      const missingFields = requiredFields.filter((field) => !formData[field as keyof SignupForm]);

      if (missingFields.length > 0) {
        toast.error('모든 필수 항목을 입력해주세요.');
        return;
      }

      // formData.hobbies 대신 selectedHobbies 사용
      const signupData = {
        name: formData.name,
        password: formData.password,
        email: formData.email,
        phoneNumber: formData.phoneNumber,
        nickname: formData.nickname,
        bio: formData.bio || '',
        hobbies: selectedHobbies.map((hobby) => ({
          hobbyId: hobby.hobbyId,
          categoryId: hobby.categoryId
        })),
        loginMethod: 'EMAIL' as const,
        socialProvider: 'NONE' as const
      };

      console.log('회원가입 요청 데이터:', signupData); // 요청 데이터 로깅
      await signupMutation.mutateAsync(signupData);
      dispatch(resetSignupInfo());
      
      // 회원가입 성공 처리
      toast.success('회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.');
      navigate('/login');
    } catch (error: any) {
      // AxiosError에서 서버 응답 메시지 추출
      const errorMessage = error.response?.data?.data?.message || 
                          (error instanceof Error ? error.message : '회원가입 중 오류가 발생했습니다.');
      
      toast.error(errorMessage);
      console.error('회원가입 에러:', error); // 에러 로깅
    }
  };

  return (
    <form className="h-full w-full bg-white dark:bg-gray-800 flex flex-col" onSubmit={handleSubmit}>
      <SignupLayout
        title={<h1 className="text-xl font-bold">Haru에 오신것을 환영합니다!</h1>}
        signupButton={
          <Button type="submit" className="w-full py-2.5 bg-primary-500 text-sm sm:text-base">
            회원가입
          </Button>
        }
        divider={<div className="relative h-px bg-gray-300 dark:bg-gray-700 w-full" />}
        loginSection={
          <div className="text-sm">
            이미 계정이 있으신가요?{' '}
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="text-primary-500 shadow-none hover:text-primary-dark font-medium"
            >
              로그인
            </button>
          </div>
        }
      >
        <div className="flex flex-col gap-2">
          <EmailInput
            label="이메일"
            name="email"
            value={formData.email}
            onChange={handleChange}
            error={validationErrors.email}
          />

          <PasswordInput
            label="비밀번호"
            name="password"
            value={formData.password}
            onChange={handleChange}
            error={validationErrors.password}
          />

          <TextInput
            label="이름"
            name="name"
            value={formData.name}
            onChange={handleChange}
            error={validationErrors.name}
          />

          <TextInput
            label="전화번호"
            name="phoneNumber"
            value={formData.phoneNumber}
            onChange={handleChange}
            error={validationErrors.phoneNumber}
          />

          <TextInput
            label="닉네임"
            name="nickname"
            value={formData.nickname}
            onChange={handleChange}
            error={validationErrors.nickname}
          />

          <div className="flex flex-row gap-3">
            <div className="flex flex-col gap-1 w-1/2">
              <label className="text-sm font-medium text-gray-700">관심사</label>
              <InterestSelect
                onInterestSelect={handleInterestSelect}
                selectedCategory={selectedCategoryId || undefined}
              />
            </div>

            <div className="flex flex-col gap-1 w-1/2">
              <label className="text-sm font-medium text-gray-700">취미</label>
              <HobbySelect
                onHobbySelect={handleHobbySelect}
                selectedHobbies={selectedHobbies}
                categoryId={selectedCategoryId || undefined}
              />
            </div>
          </div>
        </div>      
      </SignupLayout>
    </form>
  );
};

export default Signup;
