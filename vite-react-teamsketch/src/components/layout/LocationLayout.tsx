import Grid from '../common/Grid';
import GridItem from '../common/GridItem';

interface LocationLayoutProps {
  childrenTop?: React.ReactNode;
  childrenCenter?: React.ReactNode;
  childrenBottom?: React.ReactNode;
  childrenButton?: React.ReactNode;
  className?: string;
}

const TestLocationLayout: React.FC<LocationLayoutProps> = ({
  childrenTop,
  childrenCenter,
  childrenBottom,
  childrenButton,
  className = 'grid-rows-[60px_1fr_auto]'
}) => {
  return (
    <div className={`h-full w-full bg-white dark:bg-gray-800 ${className}`}>
      <Grid cols={1} gap="none" className={`h-full grid ${className}`}>
        {/* 상단 검색창 영역 */}
        <GridItem className="flex items-center justify-center">
          <div className="w-full">{childrenTop}</div>
        </GridItem>

        {/* 중앙 지도 영역 */}
        <GridItem className="relative w-full">{childrenCenter}</GridItem>

        {/* 하단 위치 정보 영역 */}
        <GridItem className="w-full">
          <div className=" bg-white dark:bg-gray-800 rounded-t-3xl shadow-lg">
            <div>{childrenBottom}</div>
            <div>{childrenButton}</div>
          </div>
        </GridItem>
      </Grid>
    </div>
  );
};

export default TestLocationLayout;
