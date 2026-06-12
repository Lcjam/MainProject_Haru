import Grid from '../common/Grid';
import GridItem from '../common/GridItem';
import DaySelect from '../forms/radiobutton/DaySelect';
import BaseLabelBox from '../common/BaseLabelBox';
import { CgArrowLongRightC } from 'react-icons/cg';
import { useState } from 'react';
import ProductImage from '../features/image/ProductImage';
import { FaMapMarkerAlt, FaUsers, FaCalendarAlt } from 'react-icons/fa';

interface PDLayoutProps {
  title?: string;
  images?: string[];
  category?: string;
  hobby?: string;
  dopamine?: number;
  id?: number;
  description?: string;
  maxParticipants?: number;
  currentParticipants?: number;
  startDate?: string | Array<number>;
  endDate?: string | Array<number>;
  meetingPlace?: string;
  btName?: React.ReactNode;
  price?: string;
  transactionType?: string;
  email?: string;
  nickname?: string;
  map?: React.ReactNode;
  day?: string[];
}

const PDLayout: React.FC<PDLayoutProps> = ({
  title = 'dfsfgsafd',
  images,
  category,
  hobby,
  dopamine,
  description,
  maxParticipants,
  currentParticipants,
  startDate,
  endDate,
  meetingPlace,
  btName,
  nickname,
  map,
  day,
  transactionType
}) => {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  const handlePrevImage = () => {
    if (!images) return;
    setCurrentImageIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };

  const handleNextImage = () => {
    if (!images) return;
    setCurrentImageIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
  };

  const isOffline = transactionType === '대면';

  return (
    <div className="container mx-auto px-4 py-8">
      <Grid cols={1} className="max-w-4xl mx-auto gap-8">
        {/* 이미지 리스트 */}
        <GridItem>
          <div className="relative h-[500px] bg-gray-100 rounded-2xl overflow-hidden shadow-lg">
            {images && images.length > 0 ? (
              <>
                <ProductImage imagePath={images[currentImageIndex]} />
                {images.length > 1 && (
                  <>
                    {/* 이전 버튼 */}
                    <button
                      onClick={handlePrevImage}
                      className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/30 text-white p-3 rounded-full hover:bg-black/50 transition-all duration-300 backdrop-blur-sm"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-6 w-6"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M15 19l-7-7 7-7"
                        />
                      </svg>
                    </button>
                    {/* 다음 버튼 */}
                    <button
                      onClick={handleNextImage}
                      className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/30 text-white p-3 rounded-full hover:bg-black/50 transition-all duration-300 backdrop-blur-sm"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-6 w-6"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M9 5l7 7-7 7"
                        />
                      </svg>
                    </button>
                    {/* 이미지 인디케이터 */}
                    <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex gap-3">
                      {images.map((_, index) => (
                        <button
                          key={index}
                          onClick={() => setCurrentImageIndex(index)}
                          className={`w-3 h-3 rounded-full transition-all duration-300 ${
                            index === currentImageIndex
                              ? 'bg-white scale-125'
                              : 'bg-white/50 hover:bg-white/70'
                          }`}
                        />
                      ))}
                    </div>
                  </>
                )}
              </>
            ) : (
              <div className="w-full h-full flex items-center justify-center text-gray-400">
                이미지가 없습니다
              </div>
            )}
          </div>
        </GridItem>

        {/* 카테고리 및 정보 */}
        <GridItem>
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between p-6 bg-white rounded-2xl shadow-sm border border-gray-100">
            <div className="mb-4 md:mb-0">
              <h1 className="text-2xl font-bold">{title}</h1>
            </div>
            <div className="flex items-center gap-4 mb-4 md:mb-0">
              <span className="px-4 py-2 bg-primary-50 text-primary-600 rounded-full text-sm font-medium">
                {category}
              </span>
              <span className="text-gray-300">{<CgArrowLongRightC />}</span>
              <span className="px-4 py-2 bg-primary-50 text-primary-600 rounded-full text-sm font-medium">
                {hobby}
              </span>
            </div>
            <div className="flex items-center gap-6">
              <div className="flex items-center gap-2">
                <span className="text-yellow-500 text-lg">⭐</span>
                <span className="text-gray-700 font-medium">도파민 {dopamine}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-gray-400">by</span>
                <span className="text-gray-700 font-medium">{nickname}</span>
              </div>
            </div>
          </div>
        </GridItem>

        {/* 소개글 */}
        <GridItem>
          <BaseLabelBox label="상품소개">
            <div className="p-6 bg-white rounded-2xl shadow-sm border border-gray-100">
              <p className="text-gray-600 whitespace-pre-line leading-relaxed">{description}</p>
            </div>
          </BaseLabelBox>
        </GridItem>

        {/* 모집 인원 */}
        <GridItem>
          <BaseLabelBox label="모집 인원">
            <div className="p-6 bg-white rounded-2xl shadow-sm border border-gray-100">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <FaUsers className="text-primary-500 text-xl" />
                  <span className="text-gray-600">참여 인원</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-primary-600 font-bold">{currentParticipants}</span>
                  <span className="text-gray-400">/</span>
                  <span className="text-gray-600">{maxParticipants}명</span>
                </div>
              </div>
            </div>
          </BaseLabelBox>
        </GridItem>

        {/* 기간 */}
        <GridItem>
          <BaseLabelBox label="기간">
            <div className="p-6 bg-white rounded-2xl shadow-sm border border-gray-100">
              <div className="flex flex-col sm:flex-row gap-4">
                {/* 시작일 */}
                <div className="flex-1 flex items-center gap-3">
                  <FaCalendarAlt className="text-primary-500 text-lg sm:text-sm" />
                  <div className="flex flex-col">
                    <span className="text-sm text-gray-500">시작일</span>
                    <span className="text-gray-600">{startDate}</span>
                  </div>
                </div>
                
                {/* 구분선 */}
                <div className="hidden sm:flex items-center">
                  <CgArrowLongRightC className="text-gray-400 text-xl" />
                </div>
                
                {/* 종료일 */}
                <div className="flex-1 flex items-center gap-3">
                  <FaCalendarAlt className="text-primary-500 text-lg sm:text-sm" />
                  <div className="flex flex-col">
                    <span className="text-sm text-gray-500">종료일</span>
                    <span className="text-gray-600">{endDate}</span>
                  </div>
                </div>
              </div>
            </div>
          </BaseLabelBox>
        </GridItem>

        {/* 요일 선택 */}
        <GridItem>
          <BaseLabelBox label="요일">
            <div className="p-3 bg-white rounded-2xl shadow-sm border border-gray-100">
              <DaySelect onDaySelect={() => {}} selectedDays={day || []} disabled={true} />
            </div>
          </BaseLabelBox>
        </GridItem>

        {/* 위치 정보 - 대면 거래일 때만 표시 */}
        {isOffline && (
          <>
            <GridItem>
              <BaseLabelBox label="장소">
                <div className="p-6 bg-white rounded-2xl shadow-sm border border-gray-100">
                  <div className="flex items-center gap-3">
                    <FaMapMarkerAlt className="text-primary-500 text-xl" />
                    <p className="text-gray-600">{meetingPlace}</p>
                  </div>
                </div>
              </BaseLabelBox>
            </GridItem>

            {/* 지도 - 대면 거래일 때만 표시 */}
            <GridItem>
              <div className="h-[400px] lg:h-[600px] bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                {map}
              </div>
            </GridItem>
          </>
        )}

        {/* 신청 or 등록하기 버튼 */}
        <GridItem>          
            {btName}          
        </GridItem>
      </Grid>
    </div>
  );
};

export default PDLayout;
