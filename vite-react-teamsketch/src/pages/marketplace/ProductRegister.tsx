import TextInput from '../../components/forms/input/TextInput';

import RadioButton from '../../components/common/RadioButton';
import InterestSelect from '../../components/forms/select/InterestSelect';
import ImageUpload from '../../components/features/upload/ImageUpload';
import TextAreaInput from '../../components/forms/textarea/TextAreaInput';
import PRLayout from '../../components/layout/PRLayout';
import { useNavigate } from 'react-router-dom';
import BaseLabelBox from '../../components/common/BaseLabelBox';
import HobbySelect from '../../components/forms/select/HobbySelect';
import BaseButton from '../../components/common/BaseButton';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import DaySelect from '../../components/forms/radiobutton/DaySelect';
import { toast } from 'react-toastify';
import {
  updateProductForm,
  addProductImage,
  removeProductImage,
  resetProductForm,
  setError
} from '../../store/slices/productSlice';
import { resetLocations } from '../../store/slices/mapSlice';
import { registerProduct } from '../../services/api/productAPI';

const ProductRegister = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { registerForm, isLoading } = useAppSelector((state) => state.product);
  const { user } = useAppSelector((state) => state.user);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    dispatch(updateProductForm({ [name]: value }));
  };

  const handleRadioButtonChange = (field: string, value: string) => {
    dispatch(updateProductForm({ [field]: value }));
  };

  const handleInterestSelect = (categoryId: number) => {
    dispatch(updateProductForm({ categoryId }));
  };

  const handleHobbySelect = (categoryId: number, hobbyId: number) => {
    dispatch(updateProductForm({ hobbyId: hobbyId, categoryId: categoryId }));
  };

  const handleFileUpload = async (formData: FormData): Promise<void> => {
    const file = formData.get('file') as File;
    if (file) {
      // 이미지 중복 체크
      const isDuplicate = registerForm.images?.some(
        (img) => img.name === file.name && img.size === file.size
      );

      if (!isDuplicate) {
        dispatch(addProductImage(file));
      }
    }
  };

  const handleRemoveImage = (index: number) => {
    const removedImage = registerForm.images[index];
    dispatch(removeProductImage(index));
    if (removedImage) {
      toast.info(`이미지 "${removedImage.name}" 제거됨`);
    }
  };

  const handleSubmit = async () => {
    try {
      if (!user?.email) {
        throw new Error('로그인이 필요합니다.');
      }

      // 필수 필드 검증
      if (!registerForm.title) {
        throw new Error('제목을 입력해주세요.');
      }
      if (!registerForm.price) {
        throw new Error('가격을 입력해주세요.');
      }
      if (!registerForm.transactionType) {
        throw new Error('거래 유형을 선택해주세요.');
      }
      if (!registerForm.registrationType) {
        throw new Error('등록 유형을 선택해주세요.');
      }
      if (registerForm.transactionType === '대면') {
        if (!registerForm.meetingPlace) {
          throw new Error('대면 거래는 장소입력이 필수입니다.');
        }
      }
      if (!registerForm.categoryId) {
        throw new Error('카테고리를 선택해주세요.');
      }
      if (!registerForm.hobbyId) {
        throw new Error('취미를 선택해주세요.');
      }
      if (!registerForm.maxParticipants) {
        throw new Error('모집 인원을 입력해주세요.');
      }
      if (!registerForm.startDate) {
        throw new Error('시작 일시를 입력해주세요.');
      }
      if (!registerForm.endDate) {
        throw new Error('종료 일시를 입력해주세요.');
      }
      if (!registerForm.days) {
        throw new Error('진행 요일을 선택해주세요.');
      }

      if (!registerForm.description) {
        throw new Error('상품 설명을 입력해주세요.');
      }

      // 대면 거래인 경우 위치 정보 검증
      if (
        registerForm.transactionType === '대면' &&
        (!registerForm.meetingPlace ||
          !registerForm.latitude ||
          !registerForm.longitude ||
          !registerForm.address)
      ) {
        throw new Error('대면 거래의 경우 위치 정보가 필요합니다.');
      }

      // 이미지 중복 제거
      const uniqueImages = registerForm.images.filter(
        (image, index, self) =>
          index === self.findIndex((img) => img.name === image.name && img.size === image.size)
      );

      const productData = {
        title: registerForm.title,
        description: registerForm.description,
        price: registerForm.price,        
        hobbyId: registerForm.hobbyId,
        categoryId: registerForm.categoryId,
        transactionType: registerForm.transactionType,
        registrationType: registerForm.registrationType,
        meetingPlace: registerForm.meetingPlace,
        latitude: registerForm.latitude || undefined,
        longitude: registerForm.longitude || undefined,
        address: registerForm.address,
        maxParticipants: Number(registerForm.maxParticipants) || 1,
        days: registerForm.days,
        startDate: registerForm.startDate,
        endDate: registerForm.endDate
      };

      const response = await registerProduct(productData, uniqueImages);

      if (response.status === 'success') {
        toast.success('상품이 성공적으로 등록되었습니다.');
        dispatch(resetProductForm());
        dispatch(resetLocations());
        navigate('/');
      } else {
        toast.error(response.message || '상품 등록에 실패했습니다.');
        throw new Error(response.message || '상품 등록에 실패했습니다.');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '상품 등록에 실패했습니다.';
      toast.error(errorMessage);
      dispatch(setError(errorMessage));
    }
  };
  console.log('productData', registerForm);
  return (
    <PRLayout
      title={<h1>상품 등록</h1>}
      productTitle={
        <BaseLabelBox label="제목">
          <TextInput
            name="title"
            value={registerForm.title}
            onChange={handleChange}
            placeholder="상품 제목을 입력하세요"
            error={''}
          />
        </BaseLabelBox>
      }
      price={
        <BaseLabelBox label="가격">
          <TextInput
            name="price"
            value={registerForm.price || ''}
            onChange={handleChange}
            type="number"
            placeholder="가격을 입력하세요"
            error={''}
          />
        </BaseLabelBox>
      }
      transactionType={
        <div className="flex flex-col gap-2">
          <div className="flex gap-3">
            <RadioButton
              label="대면"
              value="대면"
              checkedTextColor="text-white"
              checkedBackgroundColor="bg-primary-500"
              checked={registerForm.transactionType === '대면'}
              onChange={(value) => handleRadioButtonChange('transactionType', value)}
              variant="circle"
              size="sm"
              className="shadow-sm border-primary-300 border-2 rounded-lg"
            />
            <RadioButton
              label="비대면"
              value="비대면"
              checkedTextColor="text-white"
              checkedBackgroundColor="bg-primary-500"
              checked={registerForm.transactionType === '비대면'}
              onChange={(value) => handleRadioButtonChange('transactionType', value)}
              variant="circle"
              size="sm"
              className="shadow-sm border-primary-300 border-2 rounded-lg"
            />
            {registerForm.transactionType === '대면' && (
              <BaseButton variant="primary" onClick={() => navigate('/product/location')}>
                {registerForm.address ? '위치 변경' : '위치 선택'}
              </BaseButton>
            )}
          </div>
        </div>
      }
      registrationType={
        <div className="flex gap-3 justify-end">
          <RadioButton
            label="판매"
            value="판매"
            checkedTextColor="text-white"
            checkedBackgroundColor="bg-primary-500"
            checked={registerForm.registrationType === '판매'}
            onChange={(value) => handleRadioButtonChange('registrationType', value)}
            variant="circle"
            size="sm"
            className="shadow-sm border-primary-300 border-2 rounded-lg"
          />
          <RadioButton
            label="구매"
            value="구매"
            checked={registerForm.registrationType === '구매'}
            checkedTextColor="text-white"
            checkedBackgroundColor="bg-primary-500"
            onChange={(value) => handleRadioButtonChange('registrationType', value)}
            variant="circle"
            size="sm"
            className="shadow-sm border-primary-300 border-2 rounded-lg"
          />
        </div>
      }
      category={
        <div className="flex flex-col gap-2">
          <BaseLabelBox label="카테고리">
            <InterestSelect
              onInterestSelect={handleInterestSelect}
              selectedCategory={registerForm.categoryId || undefined}
            />
          </BaseLabelBox>
          <BaseLabelBox label="취미">
            <HobbySelect
              onHobbySelect={handleHobbySelect}
              selectedHobbies={
                registerForm.hobbyId
                  ? [{ hobbyId: registerForm.hobbyId, categoryId: registerForm.categoryId || 0 }]
                  : []
              }
              categoryId={registerForm.categoryId || 0}
            />
          </BaseLabelBox>
        </div>
      }
      meetingPlace={
        registerForm.transactionType === '대면' && (
          <BaseLabelBox label="장소">
            {registerForm.meetingPlace ? registerForm.meetingPlace : '장소를 선택해주세요'}
          </BaseLabelBox>
        )
      }
      participants={
        <div className="flex items-center gap-4">
          모집인원
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                if ((registerForm.maxParticipants ?? 1) > 1) {
                  dispatch(
                    updateProductForm({
                      maxParticipants: registerForm.maxParticipants
                        ? registerForm.maxParticipants - 1
                        : 1
                    })
                  );
                }
              }}
              className="border p-2 border-primary-500 rounded-md hover:bg-primary-light w-7 h-7 flex items-center justify-center text-black text-lg focus:text-white focus:bg-primary-500 transition-colors"
            >
              -
            </button>
            <span>{registerForm.maxParticipants}</span>
            <button
              type="button"
              onClick={() =>
                dispatch(
                  updateProductForm({
                    maxParticipants: registerForm.maxParticipants
                      ? registerForm.maxParticipants + 1
                      : 0
                  })
                )
              }
              className="border p-2 border-primary-500 rounded-md hover:bg-primary-light w-7 h-7 flex items-center justify-center text-black text-lg focus:text-white focus:bg-primary-500 transition-colors"
            >
              +
            </button>
          </div>
        </div>
      }
      schedule={
        <BaseLabelBox label="일정">
          <div className="flex flex-col gap-4">
            {/* 날짜 선택 */}
            <div className="flex flex-col gap-2">
              <div>
                <label className="text-sm text-gray-600">시작 일시</label>
                <TextInput
                  type="datetime-local"
                  name="startDate"
                  value={registerForm.startDate}
                  onChange={handleChange}
                />
              </div>
              <div>
                <label className="text-sm text-gray-600">종료 일시</label>
                <TextInput
                  type="datetime-local"
                  name="endDate"
                  value={registerForm.endDate}
                  onChange={handleChange}
                />
              </div>
            </div>

            {/* 요일 선택 */}
            <div>
              <label className="text-sm text-gray-600 mb-2 block">진행 요일</label>
              <DaySelect
                onDaySelect={(days: string[]) => {
                  dispatch(updateProductForm({ days: days }));
                }}
                selectedDays={registerForm.days || []}
              />
            </div>
          </div>
        </BaseLabelBox>
      }
      images={
        <ImageUpload
          type="prod"
          multiple={true}
          images={registerForm.images}
          borderStyle="border-2 border-dashed border-primary-500 rounded-lg dark:border-primary-500"
          onFileSelect={(file) => {
            const formData = new FormData();
            formData.append('file', file);
            handleFileUpload(formData);
          }}
          onRemove={handleRemoveImage}
          maxImages={10}
          className="w-full"
        />
      }
      description={
        <BaseLabelBox label="상품 설명">
          <TextAreaInput
            name="description"
            className="border-primary-300 border-2"
            value={registerForm.description}
            onChange={handleChange}
            placeholder="상품에 대한 상세한 설명을 입력하세요"
          />
        </BaseLabelBox>
      }
      submitButton={
        <BaseButton
          variant="primary"
          className="w-full "
          onClick={handleSubmit}
          disabled={isLoading}
        >
          {isLoading ? '등록 중...' : '상품 등록'}
        </BaseButton>
      }
    />
  );
};

export default ProductRegister;
