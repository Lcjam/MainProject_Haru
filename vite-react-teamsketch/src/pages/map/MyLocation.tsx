import OpenMap from '../../components/features/location/OpenMap';
import SearchLocation from '../../components/features/location/SearchLocation';
import LocationInfo from '../../components/features/location/LocationInfo';
import LocationLayout from '../../components/layout/LocationLayout';
import BaseButton from '../../components/common/BaseButton';
import { useNavigate } from 'react-router-dom';
import { useMemo } from 'react';

import { saveLocationApi } from '../../services/api/authAPI';
import { useSelector } from 'react-redux';
import { toast } from 'react-toastify';
import { useState } from 'react'
import Loading from '../../components/common/Loading';
import { RootState } from '../../store/store';

// map 슬라이스에서 선택된 위치 위도경도를 받아와서 api 호출
// 사용자 위치 등록 로그인 후 이동되는 페이지
const MyLocation = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const myLocation = useSelector((state: RootState) => state.map.myLocation);

  // 첫 로그인 여부 확인 (위치 설정 유무로 판단)
  const isFirstLogin = useMemo(() => {
    const token = localStorage.getItem('token');
    const locationSet = localStorage.getItem('locationSet');
    return token && !locationSet;
  }, []);

  const handleLocationConfirm = async () => {
    setIsLoading(true);
    try {
      console.log('위치 선택 완료');
      if (myLocation.lat !== 0 && myLocation.lng !== 0) {
        const response = await saveLocationApi({
          locationName: myLocation.meetingPlace,
          latitude: myLocation.lat,
          longitude: myLocation.lng
        });

        if (response.status === 'success') {
          // 위치 선택 완료 시 localStorage에 플래그 설정
          localStorage.setItem('locationSet', 'true');
          toast.success('위치 업데이트 완료');
          navigate('/');
        } else {
          toast.error('위치 업데이트 실패');
        }
      } else {
        toast.error('위도와 경도를 선택해주세요.');
      }
    } catch (error: any) {
      toast.error('위치 선택이 잘못되었습니다.');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      className={`w-full bg-white dark:bg-gray-800 flex flex-col h-screen ${!isFirstLogin ? 'pb-12' : ''
        }`}
    >
      <LocationLayout
        childrenTop={<SearchLocation onLocationSelect={() => { }} />}
        childrenCenter={<OpenMap nonClickable={false} mode="myLocation" />}
        childrenBottom={
          <LocationInfo showEndLocation={false} showMyLocation={true} showYourLocation={false}/>
        }
        childrenButton={
          <BaseButton
            variant="primary"
            className="w-full rounded-none pb-10"
            onClick={handleLocationConfirm}
            disabled={isLoading}
          >
            {isLoading ? (
              <div className="flex justify-center items-center mb-16">
                <Loading />
              </div>
            ) : (
              <span>나의 위치 설정하기</span>
            )}
          </BaseButton>
        }
      ></LocationLayout>
    </div>
  );
};

export default MyLocation;
