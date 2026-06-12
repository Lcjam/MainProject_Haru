import OpenMap from '../../components/features/location/OpenMap';
import SearchLocation from '../../components/features/location/SearchLocation';
import LocationInfo from '../../components/features/location/LocationInfo';
import LocationLayout from '../../components/layout/LocationLayout';
import { useLocation, useParams } from 'react-router-dom';
import { useEffect } from 'react';
import { getProductByProductId } from '../../services/api/productAPI';
import { ILocation } from '../../types/map';
import { setEndLocation, setYourLocation } from '../../store/slices/mapSlice';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { useLocation as useLocationHook } from '../../services/real-time/useLocation';
import { ILocation as IWebSocketLocation } from '../../services/real-time/types';
import { toast } from 'react-toastify';
import { getAddressFromCoord } from '../../services/api/productAPI';

interface LocationState {
  productId: number;
  endLocation?: ILocation;
  chatroomId?: number;
}

const ShareLocationMap = () => {
  const { chatroomId } = useParams();
  const location = useLocation();
  const state = location.state as LocationState;
  const dispatch = useAppDispatch();
  const { user, token } = useAppSelector((state) => state.auth);
  const { myLocation, yourLocation, endLocation } = useAppSelector((state) => state.map);

  const { sendLocation, isConnected, locations } = useLocationHook({
    chatroomId: chatroomId ? parseInt(chatroomId) : 0,
    userEmail: user?.email || '',
    token: token || ''
  });

  // 다른 사용자의 위치 정보 업데이트
  useEffect(() => {
    if (!locations || !user?.email) return;

    const updateOtherUserLocation = async () => {
      try {
        // 현재 사용자를 제외한 다른 사용자의 가장 최근 위치 찾기
        const otherUserLocation = Array.from(locations.values()).find(loc => loc.email !== user.email);
        
        if (!otherUserLocation) {
          console.log('다른 사용자의 위치 정보가 없습니다.');
          return;
        }

        // 위도와 경도가 유효한지 확인
        if (typeof otherUserLocation.latitude !== 'number' || typeof otherUserLocation.longitude !== 'number') {
          console.error('유효하지 않은 위치 정보:', otherUserLocation);
          return;
        }

        // 주소 정보 가져오기
        const response = await getAddressFromCoord(otherUserLocation.latitude, otherUserLocation.longitude);
        const address = response.address || '주소 정보 없음';
        const meetingPlace = address.split(' ')[0] || '장소 정보 없음';

        // Redux 상태 업데이트
        dispatch(setYourLocation({
          lat: otherUserLocation.latitude,
          lng: otherUserLocation.longitude,
          address: address,
          meetingPlace: meetingPlace
        }));

        console.log('상대방 위치 정보 업데이트 완료:', {
          lat: otherUserLocation.latitude,
          lng: otherUserLocation.longitude,
          address: address,
          meetingPlace: meetingPlace
        });
      } catch (error) {
        console.error('위치 정보 업데이트 중 오류 발생:', error);
        toast.error('위치 정보를 가져오는데 실패했습니다.');
      }
    };

    updateOtherUserLocation();
  }, [locations, user?.email, dispatch]);

  useEffect(() => {
    const fetchProductLocation = async () => {
      try {
        if (!state?.productId) return;

        const response = await getProductByProductId(state.productId);
        if (response && response.latitude && response.longitude) {
          const location: ILocation = {
            lat: response.latitude,
            lng: response.longitude,
            address: response.address || '',
            meetingPlace: response.meetingPlace || ''
          };
          dispatch(setEndLocation(location));
        }
      } catch (error) {
        console.error('상품 위치 정보 조회 실패:', error);
      }
    };

    fetchProductLocation();
  }, [state?.productId]);

  // 현재 위치 정보를 실시간으로 공유하는 함수
  const handleShareLocation = () => {
    console.log('위치 공유 시도:', {
      isConnected,
      chatroomId: state?.chatroomId || chatroomId,
      myLocation
    });

    const activeChatroomId = state?.chatroomId || (chatroomId ? parseInt(chatroomId) : null);

    if (!isConnected) {
      toast.error('웹소켓 연결이 끊어졌습니다. 페이지를 새로고침해주세요.');
      return;
    }

    if (!activeChatroomId) {
      toast.error('채팅방 정보를 찾을 수 없습니다.');
      return;
    }

    if (!myLocation.lat || !myLocation.lng) {
      toast.error('현재 위치를 찾을 수 없습니다.');
      return;
    }

    try {
      const wsLocation: Omit<IWebSocketLocation, 'email'> = {
        chatroomId: activeChatroomId,
        latitude: myLocation.lat,
        longitude: myLocation.lng,
        timestamp: new Date().toISOString()
      };
      
      console.log('위치 정보 전송:', wsLocation);
      sendLocation(wsLocation);
      toast.success('위치를 성공적으로 공유했습니다.');
    } catch (error) {
      console.error('위치 공유 실패:', error);
      toast.error('위치 공유에 실패했습니다.');
    }
  };

  return (
    <div className="h-full w-full">
      <LocationLayout
        childrenTop={<SearchLocation onLocationSelect={() => {}} />}
        childrenCenter={<OpenMap nonClickable={true} mode="all" />}
        childrenBottom={
          <LocationInfo            
            showEndLocation={true}
            showMyLocation={true}
            showYourLocation={true}
            onShareLocation={handleShareLocation}
            isConnected={isConnected}
            myLocation={myLocation}
            yourLocation={yourLocation}
            endLocation={endLocation}
          />
        }
      ></LocationLayout>
    </div>
  );
};

export default ShareLocationMap;
