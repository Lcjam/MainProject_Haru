import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { setUser } from '../../store/slices/userSlice';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { RootState } from '../../store/store';
import MyPageLayout from '../../components/layout/MyPageLayout';
import { getProfileImage } from '../../services/api/profileImageAPI';
import { FileResponse } from '../../types/fileResponse';
import { useEffect } from 'react';
import { FaUserCog, FaBoxOpen, FaCreditCard, FaHistory, FaHeadset } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const MyPage = () => {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state: RootState) => state.user.user);
  const queryClient = useQueryClient();

  const navigate = useNavigate();

  const {
    data: profileImage,
    isLoading,
    isError,
    error
  } = useQuery<FileResponse | null>({
    queryKey: ['profileImage'],
    queryFn: getProfileImage,
    staleTime: 0,
    retry: 1,
    refetchOnMount: true,
    refetchOnWindowFocus: true
  });

  useEffect(() => {
    console.log(user);
    // 쿼리 상태 로깅
    console.log('Query Status:', { isLoading, isError, error, profileImage });
  }, [user]);

  useEffect(() => {
    if (profileImage) {
      dispatch(
        setUser({
          ...user,
          profileImagePath: profileImage.data.response
        })
      );
    }
  }, [profileImage]);

  const handleProfileUpdate = () => {
    queryClient.invalidateQueries({ queryKey: ['profileImage'] });
  };

  const menuItems = [
    {
      icon: <FaUserCog className="w-6 h-6" />,
      label: '프로필 관리',
      color: 'text-primary-500',
      onClick: () => navigate('/profile-manage')
    },
    {

      icon: <FaBoxOpen className="w-6 h-6" />,
      label: '상품 관리',
      color: 'text-blue-500',
      onClick: () => navigate('/my-products')
    },
    {
      icon: <FaBoxOpen className="w-6 h-6" />,
      label: '거래 내역',
      color: 'text-blue-500',
      onClick: () => navigate('/transaction-list')
    },
    {
      icon: <FaCreditCard className="w-6 h-6" />,
      label: '결제 관리',
      color: 'text-green-500',
      onClick: () => navigate('/registered-card')
    },
    {
      icon: <FaHistory className="w-6 h-6" />,
      label: '활동 내역',
      color: 'text-purple-500',
      onClick: () => navigate('/activity-list')
    },
    {
      icon: <FaHeadset className="w-6 h-6" />,
      label: 'AI 고객센터',
      color: 'text-indigo-500',
      onClick: () => navigate('/cs-list')
    }
  ];

  return (
    <div className="h-full w-full bg-white dark:bg-gray-800 flex flex-col mb-16">
      <MyPageLayout
        email={user.email || ''}
        name={user.name || ''}
        nickname={user.nickname || ''}
        profileImagePath={user.profileImagePath as File | string}
        points={user.points}
        dopamine={user.dopamine}
        onProfileUpdate={handleProfileUpdate}
        menuItems={menuItems}
      />
    </div>
  );
};

export default MyPage;
