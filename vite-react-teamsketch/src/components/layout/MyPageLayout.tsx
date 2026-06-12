import Grid from '../common/Grid';
import GridItem from '../common/GridItem';
import BaseButton from '../common/BaseButton';
import ImageUpload from '../features/upload/ImageUpload';
import { FaArrowRight } from 'react-icons/fa';
interface MyPageLayoutProps {
  email?: string;
  name?: string;
  nickname?: string;
  profileImagePath?: File | string;
  followerCount?: number;
  points?: number;
  dopamine?: number;
  onProfileUpdate?: () => void;
  menuItems?: {
    icon: React.ReactNode;
    label: string;
    color: string;
    onClick?: () => void;
  }[];
}

const MyPageLayout = ({
  email,
  name,
  nickname,
  profileImagePath,
  followerCount = 126,
  points = 1200,
  dopamine,
  onProfileUpdate = () => {},
  menuItems = []
}: MyPageLayoutProps) => {


  return (
    <div className="h-full w-full bg-white dark:bg-gray-800 overflow-y-auto">
      <Grid cols={1} className="p-4 gap-6">
        {/* 프로필 이미지 */}
        <GridItem>
          <div className="flex justify-center px-4">
            <ImageUpload
              onFileSelect={onProfileUpdate}
              currentImage={profileImagePath as File | string}
              type="profile"
              isEdit={false}
            />
          </div>
        </GridItem>

        {/* 사용자 기본 정보 */}
        <GridItem>
          <div className="text-center space-y-2 border-b border-primary-200 dark:border-gray-700 pb-4">
            <p className="text-xl font-semibold">{email ? email : '홍길동@example.com'}</p>
            <p className="text-gray-600 dark:text-gray-300">{name ? name : '홍길동'}</p>
            <p className="text-gray-500 dark:text-gray-200">{nickname ? nickname : 'OooO'}</p>
          </div>
        </GridItem>

        {/* 통계 정보 */}
        <GridItem>
          <div className="grid grid-cols-3 gap-4 border-b border-primary-200 bg-primary-50 dark:bg-gray-700 dark:border-gray-700 rounded-xl p-4">
            <div className="text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">팔로워</p>
              <p className="text-lg font-bold text-primary">{followerCount}</p>
            </div>
            <div className="text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">포인트</p>
              <p className="text-lg font-bold text-primary">{points}</p>
            </div>
            <div className="text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">도파민</p>
              <p className="text-lg font-bold text-primary">{dopamine}</p>
            </div>
          </div>
        </GridItem>

        {/* 메뉴 버튼들 */}
        <GridItem>
          <div className="space-y-2">
            {menuItems.map((item, index) => (
              <BaseButton
                key={index}
                variant="outline"
                onClick={item.onClick}
                className={`
                  w-full py-4 px-6
                  flex items-center justify-between
                  rounded-xl
                  transition-all duration-300
                  border border-primary-200 dark:border-gray-700
                  bg-primary-50 dark:bg-gray-700
                  ${item.color}
                  group
                `}
              >
                <div className="flex items-center gap-4">
                  <div
                    className="text-black-400 dark:text-gray-500 
                    group-hover:text-primary dark:group-hover:text-primary-light 
                    transition-colors duration-300"
                  >
                    {item.icon}
                  </div>
                  <span
                    className="font-medium text-black dark:text-gray-300
                    group-hover:text-primary dark:group-hover:text-primary-light"
                  >
                    {item.label}
                  </span>
                </div>
                <div
                  className="text-black-400 dark:text-gray-500 
                  group-hover:text-primary dark:group-hover:text-primary-light 
                  group-hover:transform group-hover:translate-x-1 
                  transition-all duration-300"
                >
                  <FaArrowRight />
                </div>
              </BaseButton>
            ))}
          </div>
        </GridItem>
      </Grid>
    </div>
  );
};

export default MyPageLayout;
