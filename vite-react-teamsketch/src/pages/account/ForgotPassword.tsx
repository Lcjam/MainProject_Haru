import { useNavigate } from 'react-router-dom';
import LoginLayout from '../../components/layout/LoginLayout';
import Button from '../../components/common/BaseButton';
import EmailInput from '../../components/forms/input/EmailInput';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { updatePasswordInfo } from '../../store/slices/passwordChangeSlice';
const ForgotPassword = () => {
  const email = useAppSelector((state) => state.passwordChange.email);
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    dispatch(updatePasswordInfo({ email: e.target.value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('입력된 이메일:', email);
    // api 호출 응답 성공 시

    navigate('/verify-method');
  };

  return (
    <LoginLayout title={<h1 className="text-2xl font-bold text-center">비밀번호 찾기</h1>}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-gray-500 text-center">
          비밀번호를 찾고자하는 이메일을 입력해주세요.
        </p>
        <EmailInput
          name="email"
          value={email}
          onChange={handleChange}
          placeholder="이메일을 입력하세요"
        />
        <Button type="submit" className="w-full bg-primary-500">
          다음
        </Button>
      </form>
    </LoginLayout>
  );
};

export default ForgotPassword;
