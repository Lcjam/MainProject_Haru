import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useDeleteAccount } from '../../services/api/authAPI';
import { toast } from 'react-toastify';
import Loading from '../../components/common/Loading';
import BaseButton from '../../components/common/BaseButton';

const DeleteAccount = () => {
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const deleteAccount = useDeleteAccount();
  // 순서만 알려드릴게요
  // 회원 탈퇴 API 호출 함수
  const [isLoading, setIsLoading] = useState(false);
  // 탈퇴하기 버튼이 클릭되면 회원탈퇴가 이루어져야 하니까
  // 리액트쿼리 굳이 안써도 되는 api 요청도 있어요
  // 예를 들면 이런 단일 요청
  // 반대로 저희 마켓플레이스는 이미지랑 여러데이터를 리스트형식으로 가져오니까 시간이 좀 걸리잖아요 그런것들은 리액트 쿼리 사용하면 좋아요
  // 그리고 기본적으로 authAPI.ts 여기서 리액트쿼리 사용해요
  // 이 함수에 api 요청 넣으면 돼요
  const handleDeleteAccount = async () => {
    try {
      setIsLoading(true);
      if (password.trim() !== '') {
        const response = await deleteAccount.mutateAsync(password);
        if (response.status === 'success') {
          localStorage.removeItem('accessToken');
          // 리덕스에 유저정보도 초기화 시켜야함
          navigate('/'); // 탈퇴 후 홈 화면으로 이동
          toast.success('회원탈퇴가 완료되었습니다.');
        }
      } else {
        toast.error('비밀번호를 입력해주세요.');
        setIsLoading(false);
        return;
      }
      setIsLoading(false);
    } catch (error: any) {
      setIsLoading(false);
      toast.error(error);
    }
  };

  return (
    <div className="flex flex-col items-center h-screen overflow-y-auto pt-20 pb-20">
      <h2 className="text-2xl font-bold text-center mb-4">회원 탈퇴 안내</h2>
      <p className="text-center text-gray-600 mb-5">
        회원 탈퇴를 진행하시기 전에 아래 내용을 반드시 확인해주세요:
      </p>

      {/* 내용 박스 */}
      <div className="bg-gray-100 p-6 rounded-lg shadow-md w-[90%] max-w-2xl mb-6">
        <ul className="space-y-4 text-gray-700 m-3">
          <li>
            <strong>1. 재가입 제한</strong> <br />
            회원 탈퇴 후,{' '}
            <span className="text-red-500 font-semibold">30일 이내에는 재가입이 불가능</span>합니다.
          </li>
          <li>
            <strong>2. 데이터 삭제</strong> <br />
            탈퇴 후 모든 개인정보 및 관련 데이터가{' '}
            <span className="text-red-500 font-semibold">30일 보관 후 삭제</span>됩니다. 삭제된
            데이터는 복구할 수 없습니다.
          </li>
          <li>
            <strong>3. 서비스 이용 불가</strong> <br />
            탈퇴 후, 해당 계정으로 상품 등록, 구매, 게시판 작성 등{' '}
            <span className="text-red-500 font-semibold">서비스 이용이 불가능</span>합니다.
          </li>
          <li>
            <strong>4. 환불 및 결제</strong> <br />
            탈퇴 전, 진행 중인 거래나 결제 내역이 있을 경우 취소 및 환불이 필요한 경우 미리 처리해
            주세요.
          </li>
        </ul>
      </div>

      <p className="m-8 text-red-500 font-semibold text-center">
        <span>탈퇴 후에는 재가입이 불가능하고, 모든 데이터가 삭제됩니다.</span>
        <span className="text-black">
          {' '}
          이 점을 충분히 이해하신 후 탈퇴 절차를 진행해 주세요. 탈퇴를 원하시면, 아래 "계정 삭제"
          버튼을 클릭해 주세요.
        </span>
      </p>

      {/* 비밀번호 입력 */}
      <input
        type="password"
        placeholder="비밀번호 입력"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        className="mb-4 p-2 border rounded w-[90%] max-w-md text-center"
      />

      {/* 계정 삭제 버튼 */}
      <BaseButton
        onClick={handleDeleteAccount}
        className="bg-primary-500 w-[90%] max-w-md flex justify-center items-center relative mb-8"
        disabled={isLoading}
      >
        {isLoading ? (
          <div className="flex justify-center items-center">
            <Loading />
          </div>
        ) : (
          <span>회원탈퇴</span>
        )}
      </BaseButton>
    </div>
  );
};

export default DeleteAccount;
