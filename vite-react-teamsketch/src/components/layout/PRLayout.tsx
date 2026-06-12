import Grid from '../common/Grid';
import GridItem from '../common/GridItem';

interface PRLayoutProps {
  title?: React.ReactNode;
  productTitle?: React.ReactNode;
  price?: React.ReactNode;
  transactionType?: React.ReactNode;
  registrationType?: React.ReactNode;
  category?: React.ReactNode;
  participants?: React.ReactNode;
  schedule?: React.ReactNode;
  images?: React.ReactNode;
  description?: React.ReactNode;
  submitButton?: React.ReactNode;
  meetingPlace?: React.ReactNode;
}

const PRLayout = ({
  title = '상품 등록',
  productTitle = '제목',
  price = 10000,
  transactionType = '비대면',
  registrationType = '판매',
  category,
  participants = 2,
  schedule = '2025-01-01 ~ 2025-01-01',
  images,
  description = '설명',
  submitButton,
  meetingPlace = '테스트 장소'
}: PRLayoutProps) => {
  return (
    <div className="h-full w-full bg-white dark:bg-gray-800 overflow-y-auto">
      <Grid cols={1} gap="sm" className="h-full p-4 sm:p-6">
        {/* 타이틀 */}
        <GridItem className="flex items-center justify-center mb-4">
          <div className="text-xl font-semibold">{title}</div>
        </GridItem>

        {/* 메인 폼 영역 */}
        <GridItem className="space-y-6">
          {/* 제목 입력 */}
          <div className="space-y-2">{productTitle}</div>

          {/* 가격 입력 */}
          <div className="space-y-2">{price}</div>

          {/* 거래 방식 & 등록 유형 */}
          <Grid cols={12} gap="sm">
            <GridItem className="col-span-8 space-y-2">{transactionType}</GridItem>
            <GridItem className="col-span-4 space-y-2 flex justify-end">
              {registrationType}
            </GridItem>
          </Grid>

          {/* 대면 거래 장소 */}
          <div className="space-y-2">{meetingPlace}</div>

          {/* 카테고리 */}
          <div className="space-y-2">{category}</div>

          {/* 모집 인원 */}
          <div className="space-y-2">{participants}</div>

          {/* 일정 기간 */}
          <div className="space-y-2">{schedule}</div>

          {/* 이미지 업로드 */}
          <div className="space-y-2">{images}</div>

          {/* 설명 */}
          <div className="space-y-2">{description}</div>
        </GridItem>

        {/* 등록 버튼 - 하단 고정 */}
        <GridItem className="sticky bottom-0 bg-white dark:bg-gray-800 py-4 border-t border-gray-200 dark:border-gray-700 mt-6">
          <div className="max-w-2xl mx-auto">{submitButton}</div>
        </GridItem>
      </Grid>
    </div>
  );
};

export default PRLayout;
