import { useLocation } from 'react-router-dom';
import PDLayout from '../../components/layout/PDLayout';
import LocationLayout from '../../components/layout/LocationLayout';
import OpenMap from '../../components/features/location/OpenMap';
import LocationInfo from '../../components/features/location/LocationInfo';
import { useDispatch } from 'react-redux';
import { setEndLocation } from '../../store/slices/mapSlice';
import { useAppSelector } from '../../store/hooks';
import { useEffect, useState } from 'react';
import BaseButton from '../../components/common/BaseButton';
import { useNavigate } from 'react-router-dom';
import { getChatRoomIdByProductId, requestProduct } from '../../services/api/productAPI';
import { toast } from 'react-toastify';

const ProductDetails = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const { constantCategories, constantHobbies } = useAppSelector((state) => state.category);
  const productEmail = location.state?.productData.email || '';
  console.log('productEmail', productEmail);
  const productId = location.state?.productData.id || '';
  console.log('productId', productId);
  const productCode = location.state?.productData.productCode || '';
  console.log('productCode', productCode);
  const currentUserEmail = useAppSelector((state) => state.auth.user?.email);
  console.log('currentUserEmail', currentUserEmail);
  const [isMyProduct, setIsMyProduct] = useState(false);
  
  useEffect(() => {
    if (productEmail === currentUserEmail) {
      setIsMyProduct(true);
    }
    dispatch(
      setEndLocation({
        lat: location.state.productData.latitude,
        lng: location.state.productData.longitude,
        address: location.state.productData.address,
        meetingPlace: location.state.productData.meetingPlace
      })
    );
    console.log('location.state.productData', location.state.productData);
  }, [dispatch, location.state.productData]);

  const productData = location.state?.productData || {
    id: 1,
    images: ['https://picsum.photos/600/400?random=1'],
    productCode: ``,
    title: 'mockProduct.title',
    description: 'mockProduct.description',
    price: 'mockProduct.price',
    email: 'mock@example.com',
    categoryId: 1, // 기본 카테고리 ID
    hobbyId: 1, // 기본 취미 ID
    transactionType: '대면',
    registrationType: '판매',
    maxParticipants: 1,
    currentParticipants: 0,
    days: [],
    startDate: new Date().toISOString(),
    endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
    latitude: null,
    longitude: null,
    meetingPlace: 'mockProduct.location',
    address: 'mockProduct.location',
    createdAt: 'mockProduct.createdAt',
    imagePaths: ['mockProduct.image'],
    thumbnailPath: 'mockProduct.image',
    nickname: 'Mock User',
    dopamine: 1,
    visible: true
  };

  // categoryId를 사용하여 카테고리 이름 찾기
  const mainCategory =
    constantCategories.find((category) => category.categoryId === productData.categoryId)
      ?.categoryName || '카테고리 없음';

  // hobbyId를 사용하여 취미 이름 찾기
  const subCategory =
    constantHobbies.find((hobby) => hobby.hobbyId === productData.hobbyId)?.hobbyName ||
    '취미 없음';

  // 날짜 포맷팅 함수
  const formatDate = (dateString: string | Array<number>) => {
    if (!dateString) return '';
    
    // 배열 형태의 날짜 처리 [년, 월, 일, 시, 분]
    if (Array.isArray(dateString)) {
      const [year, month, day, hour, minute] = dateString;
      // 월은 0부터 시작하므로 +1 하지 않음
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
    }
    
    // 문자열 형태의 날짜 처리
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return '날짜 형식 오류';
      }
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
        date.getDate()
      ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(
        date.getMinutes()
      ).padStart(2, '0')}`;
    } catch (error) {
      console.error('날짜 포맷팅 오류:', error);
      return '날짜 형식 오류';
    }
  };

  // 이미지 URL 처리
  const processedImages = productData.images.map((imagePath: string) => {
    if (imagePath.startsWith('http')) return imagePath;
    
    // imagePath가 이미 /api로 시작하는 경우
    if (imagePath.startsWith('/api')) {
      return `${import.meta.env.VITE_API_URL}${imagePath}`;
    }
    
    // 그 외의 경우 기존 로직 유지
    return `${import.meta.env.VITE_API_URL}/api/core/market/images${imagePath}`;
  });

  // 가격 포맷팅 함수
  const formatPrice = (price: number) => {
    return price.toLocaleString('ko-KR');
  };

  // 거래 유형 포맷팅
  const formatTransactionType = (type: string) => {
    const types = {
      대면: '🤝 대면',
      비대면: '💻 비대면',
      구매: '💰 구매',
      판매: '🏷️ 판매'
    };
    return types[type as keyof typeof types] || type;
  };

  const handleApply = async () => {        
    try {
      const response = await requestProduct(productId);
      if (response.status === 'success') {
        toast.success('상품 신청이 완료되었습니다.');
        try {
          const chatroomId = await getChatRoomIdByProductId(productId);
          console.log('chatroomId', chatroomId);
          
          navigate(`/chat/${chatroomId}/${productData.title}`);
        } catch (error: any) {
          if (error.response?.status === 404) {
            toast.error('해당 상품이 존재하지 않습니다.');
          } else {
            toast.error('채팅방 생성 중 오류가 발생했습니다.');
          }
          console.error('채팅방 생성 오류:', error);
        }
      } else {
        toast.error(response.message || '상품 신청에 실패했습니다.');
      }
    } catch (error: any) {
      if (error.response?.status === 404) {
        toast.error('해당 상품이 존재하지 않습니다.');
      } else if (error.response?.data?.message?.includes('Duplicate')) {
        toast.warning('이미 신청한 상품입니다.');
      } else {
        toast.error(error.response?.data?.message || '상품 신청 중 오류가 발생했습니다.');
        console.error('신청 오류:', error);
      }
    }
  };

  return (
    <PDLayout
      title={productData.title}
      images={processedImages}
      category={`${formatTransactionType(productData.registrationType)} | ${mainCategory}`}
      hobby={subCategory}
      dopamine={productData.dopamine}
      id={productData.id}
      description={productData.description}
      maxParticipants={productData.maxParticipants}
      currentParticipants={productData.currentParticipants}
      startDate={formatDate(productData.startDate)}
      endDate={formatDate(productData.endDate)}
      meetingPlace={productData.meetingPlace}
      
      btName={
        isMyProduct ? (
        <BaseButton
          onClick={() => navigate('/marketplace/product/edit')}
          variant="primary"
          className="w-full py-4 text-lg font-medium rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300"
        >
          수정하기
        </BaseButton>
      ) : (
        <BaseButton
          onClick={handleApply}
          variant="primary"
          className="w-full py-4 text-lg font-medium rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300"
        >
          신청하기
        </BaseButton>
      )}
      price={formatPrice(productData.price)}
      transactionType={productData.transactionType}
      email={productData.email}
      nickname={productData.nickname || '닉x'}
      day={productData.days || []}
      map={
        <LocationLayout
          childrenCenter={<OpenMap nonClickable={true} />}
          childrenBottom={<LocationInfo showEndLocation={true} />}
        ></LocationLayout>
      }
    />
  );
};

export default ProductDetails;
