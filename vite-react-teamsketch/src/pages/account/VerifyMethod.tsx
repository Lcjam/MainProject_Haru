import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginLayout from '../../components/layout/LoginLayout';
import Button from '../../components/common/BaseButton';
import TextInput from '../../components/forms/input/TextInput';
import EmailInput from '../../components/forms/input/EmailInput';
import { useSendSms } from '../../services/api/authAPI';
import { useAppDispatch } from '../../store/hooks';
import { updatePasswordInfo } from '../../store/slices/passwordChangeSlice';

const VerifyMethod = () => {
  const [method, setMethod] = useState<'email' | 'phone' | null>(null);
  const [inputValue, setInputValue] = useState('');
  const navigate = useNavigate();
  const { mutate: sendSms } = useSendSms();
  const dispatch = useAppDispatch();

  console.log(inputValue);
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!method) {
      alert('인증 방법을 선택해주세요!');
      return;
    }
    if (!inputValue) {
      alert(`${method === 'email' ? '이메일' : '전화번호'}를 입력해주세요!`);
      return;
    }

    if (method === 'email') {
      dispatch(updatePasswordInfo({ email: inputValue }));
    } else {
      sendSms(inputValue, {
        onSuccess: (data) => {
          console.log('전송 성공:', data.message);
        }
      });
    }
    dispatch(updatePasswordInfo({ phoneNumber: inputValue })); // Redux 상태 업데이트

    navigate('/verfication-code', { state: { method, inputValue } });
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    console.log(inputValue);
    setInputValue(e.target.value); // 입력값 상태 업데이트
  };

  return (
    <LoginLayout title={<h1 className="text-2xl font-bold text-center">비밀번호 찾기</h1>}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-gray-500 text-center">
          비밀번호를 재설정하려면 아래 방법 중 하나를 선택하세요.
        </p>

        {/* 이메일 인증 선택 */}
        <div className="flex items-center space-x-2">
          <input
            type="radio"
            id="email"
            name="verificationMethod"
            value="email"
            onChange={() => setMethod('email')}
            className="h-4 w-4"
          />
          <label htmlFor="email" className="text-sm text-gray-700">
            이메일 인증하기
          </label>
        </div>

        {/* 전화번호 인증 선택 */}
        <div className="flex items-center space-x-2">
          <input
            type="radio"
            id="phone"
            name="verificationMethod"
            value="phone"
            onChange={() => setMethod('phone')}
            className="h-4 w-4"
          />
          <label htmlFor="phone" className="text-sm text-gray-700">
            전화번호 인증하기
          </label>
        </div>

        {/* 이메일 또는 전화번호 입력란 */}
        {method === 'email' && (
          <EmailInput
            name="email"
            label="이메일"
            value={inputValue}
            onChange={handleChange}
            placeholder="이메일을 입력하세요"
          />
        )}
        {method === 'phone' && (
          <TextInput
            name="phone"
            type="tel"
            label="전화번호"
            value={inputValue}
            onChange={handleChange}
            placeholder="전화번호를 입력하세요"
          />
        )}

        {/* 인증 방법 선택 후 비밀번호 재설정 페이지로 이동 */}
        <Button type="submit" className="w-full bg-primary-500">
          비밀번호 재설정
        </Button>
      </form>
    </LoginLayout>
  );
};

export default VerifyMethod;
