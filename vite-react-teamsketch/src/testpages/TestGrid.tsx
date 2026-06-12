import Grid from '../components/common/Grid';
import GridItem from '../components/common/GridItem';

const TestGrid = () => {
  return (
    <div className="p-8 space-y-8">
      <h1 className="text-3xl font-bold mb-8 text-center">그리드 시스템 테스트</h1>

      <div className="space-y-12">
        <section>
          <h2 className="text-xl font-semibold mb-4">기본 2x2 그리드</h2>
          <Grid
            cols={2}
            rows={2}
            gap="lg"
            className="min-h-[300px] bg-gray-100 dark:bg-gray-800 p-4 rounded-lg"
          >
            <GridItem>
              <div className="h-full bg-blue-200 dark:bg-blue-800 p-4 rounded-lg flex items-center justify-center">
                아이템 1
              </div>
            </GridItem>
            <GridItem>
              <div className="h-full bg-green-200 dark:bg-green-800 p-4 rounded-lg flex items-center justify-center">
                아이템 2
              </div>
            </GridItem>
            <GridItem>
              <div className="h-full bg-yellow-200 dark:bg-yellow-800 p-4 rounded-lg flex items-center justify-center">
                아이템 3
              </div>
            </GridItem>
            <GridItem>
              <div className="h-full bg-red-200 dark:bg-red-800 p-4 rounded-lg flex items-center justify-center">
                아이템 4
              </div>
            </GridItem>
          </Grid>
        </section>

        <section>
          <h2 className="text-xl font-semibold mb-4">복잡한 레이아웃 예시</h2>
          <Grid
            cols={3}
            rows={3}
            gap="md"
            className="min-h-[500px] bg-gray-100 dark:bg-gray-800 p-4 rounded-lg"
          >
            <GridItem colSpan={2} rowSpan={2}>
              <div className="h-full bg-purple-200 dark:bg-purple-800 p-4 rounded-lg flex items-center justify-center">
                큰 메인 영역
              </div>
            </GridItem>
            <GridItem rowSpan={3}>
              <div className="h-full bg-pink-200 dark:bg-pink-800 p-4 rounded-lg flex items-center justify-center">
                사이드바
              </div>
            </GridItem>
            <GridItem>
              <div className="h-full bg-indigo-200 dark:bg-indigo-800 p-4 rounded-lg flex items-center justify-center">
                작은 영역 1
              </div>
            </GridItem>
            <GridItem>
              <div className="h-full bg-teal-200 dark:bg-teal-800 p-4 rounded-lg flex items-center justify-center">
                작은 영역 2
              </div>
            </GridItem>
          </Grid>
        </section>

        <section>
          <h2 className="text-xl font-semibold mb-4">비대칭 레이아웃</h2>
          <Grid
            cols={4}
            rows={2}
            gap="md"
            className="min-h-[400px] bg-gray-100 dark:bg-gray-800 p-4 rounded-lg"
          >
            <GridItem colSpan={3}>
              <div className="h-full bg-orange-200 dark:bg-orange-800 p-4 rounded-lg flex items-center justify-center">
                헤더 영역
              </div>
            </GridItem>
            <GridItem rowSpan={2}>
              <div className="h-full bg-cyan-200 dark:bg-cyan-800 p-4 rounded-lg flex items-center justify-center">
                사이드 메뉴
              </div>
            </GridItem>
            <GridItem>
              <div className="h-full bg-lime-200 dark:bg-lime-800 p-4 rounded-lg flex items-center justify-center">
                콘텐츠 1
              </div>
            </GridItem>
            <GridItem colSpan={2}>
              <div className="h-full bg-violet-200 dark:bg-violet-800 p-4 rounded-lg flex items-center justify-center">
                콘텐츠 2
              </div>
            </GridItem>
          </Grid>
        </section>
      </div>
    </div>
  );
};

export default TestGrid;
