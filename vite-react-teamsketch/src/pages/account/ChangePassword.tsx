import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/common/BaseButton';
import LoginLayout from '../../components/layout/LoginLayout';
import PasswordInput from '../../components/forms/input/PasswordInput';
import { toast } from 'react-toastify';
import Loading from '../../components/common/Loading';
import { useChangePassword } from '../../services/api/authAPI';

const ChangePassword = () => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const changePassword = useChangePassword();
  const isToken = "true";
  const handleSubmitChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    if (currentPassword.length < 8) {
      toast.error('현재 비밀번호는 8자 이상이어야 합니다.');
      setIsLoading(false);
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error('비밀번호가 일치하지 않습니다.');
      setIsLoading(false);
      return;
    }


    if (newPassword.length < 8) {
      toast.error('비밀번호는 8자 이상이어야 합니다.');
      setIsLoading(false);
      return;
    }

    try {

      await changePassword.mutateAsync({ isToken, currentPassword, newPassword, confirmPassword });
      navigate('/mypage');
      toast.success('비밀번호가 성공적으로 변경되었습니다.');

    } catch (error) {
      console.error(error);
      toast.error('비밀번호 변경에 실패했습니다.');

    } finally {
      setIsLoading(false);
    }

  };



  return (
    <LoginLayout title={<h1 className="text-2xl font-bold text-center">비밀번호 변경</h1>}>
      <form onSubmit={handleSubmitChangePassword} className="space-y-4">
        <PasswordInput
          label="현재 비밀번호"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          placeholder="현재 비밀번호를 입력하세요"
        />

        {/* 새 비밀번호 입력 */}
        <PasswordInput
          name="password"
          label="새 비밀번호"
          placeholder="새 비밀번호를 입력하세요"
          value={newPassword}
          isNewPassword={true}
          onChange={(e) => setNewPassword(e.target.value)}
        />

        {/* 비밀번호 확인 입력 */}
        <PasswordInput
          name="confirmPassword"
          label="비밀번호 확인"
          placeholder="비밀번호를 다시 입력하세요"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
        />
        <Button type="submit" variant="primary" className="w-full  bg-purple-600" disabled={isLoading}>

          {isLoading ? (
            <div className="flex justify-center items-center">
              <Loading />
            </div>
          ) : (
            <span>비밀번호 변경</span>
          )}
        </Button>
      </form>
    </LoginLayout>
  );
};

export default ChangePassword;
