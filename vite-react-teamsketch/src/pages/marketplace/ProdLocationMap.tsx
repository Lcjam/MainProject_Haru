import OpenMap from '../../components/features/location/OpenMap';
import SearchLocation from '../../components/features/location/SearchLocation';
import LocationInfo from '../../components/features/location/LocationInfo';
import LocationLayout from '../../components/layout/LocationLayout';
import { useNavigate } from 'react-router-dom';
import BaseButton from '../../components/common/BaseButton';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { updateProductForm } from '../../store/slices/productSlice';
import { setEndLocation } from '../../store/slices/mapSlice';

const ProdLocationMap = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const endLocation = useAppSelector((state) => state.map.endLocation);


  // 검색을 통한 위치 선택 처리
  const handleEndLocationSelect = (location: {
    lat: number;
    lng: number;
    address: string;
    meetingPlace: string;
  }) => {
    // 검색으로 선택한 위치 정보 로깅
    console.log('검색으로 선택한 위치 정보:', {
      위도: location.lat,
      경도: location.lng,
      주소: location.address,
      장소명: location.meetingPlace
    });

    // mapSlice의 endLocation 상태 업데이트
    dispatch(
      setEndLocation({
        meetingPlace: location.meetingPlace,
        lat: location.lat,
        lng: location.lng,
        address: location.address
      })
    );

    // productSlice의 위치 정보 업데이트
    dispatch(
      updateProductForm({
        latitude: location.lat,
        longitude: location.lng,
        address: location.address,
        meetingPlace: location.meetingPlace
      })
    );
    console.log('productSlice 위치 정보:', {
      위도: location.lat,
      경도: location.lng,
      주소: location.address,
      장소명: location.meetingPlace
    });
  };

  // 위치 선택 완료 처리
  const handleLocationConfirm = () => {
    if (endLocation) {
      dispatch(
        updateProductForm({
          latitude: endLocation.lat,
          longitude: endLocation.lng,
          address: endLocation.address,
          meetingPlace: endLocation.meetingPlace
        })
      );
      navigate(-1);
    }
  };
  return (
    <div className="h-full w-full">
      <LocationLayout
        childrenTop={<SearchLocation onLocationSelect={handleEndLocationSelect} />}
        childrenCenter={<OpenMap mode="myAndEnd" />}
        childrenBottom={<LocationInfo showEndLocation={true} showMyLocation={true}/>}
        childrenButton={
          <BaseButton
            variant="primary"
            className="w-full rounded-none pb-8"
            onClick={handleLocationConfirm}
          >
            위치 선택 완료
          </BaseButton>
        }
      />
    </div>
  );
};

export default ProdLocationMap;