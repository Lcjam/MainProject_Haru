import { ReactNode } from 'react';
import Grid from '../common/Grid';
import GridItem from '../common/GridItem';

interface SignupLayoutProps {
  title?: ReactNode;
  children?: ReactNode;
  signupButton?: ReactNode;
  divider?: ReactNode;
  loginSection?: ReactNode;
}

const SignupLayout = ({
  title,
  children,
  signupButton,
  divider,
  loginSection
}: SignupLayoutProps) => {
  return (
    <div className="h-screen w-full bg-white dark:bg-gray-800">
      <Grid cols={1} gap="sm" className="h-full grid-rows-[1fr_3fr_1fr] items-center">
        {/* 로고 및 타이틀 */}
        <GridItem className="flex items-center justify-center">
          <div className="text-2xl font-bold text-primary-500 dark:text-primary-400">{title}</div>
        </GridItem>

        {/* 메인 폼 */}
        <GridItem className="flex items-center justify-center">
          <div className="w-[70%] sm:w-[60%] md:w-[50%] lg:w-[40%] space-y-6">
            {/* 회원가입 폼 */}
            <div className="space-y-4">{children}</div>

            {/* 회원가입 버튼 */}
            <div className="pt-4">{signupButton}</div>

            {/* 구분선 */}
            <div className="relative">{divider}</div>
          </div>
        </GridItem>

        {/* 로그인 섹션 */}
        <GridItem className="flex items-center justify-center">
          <div className="text-center text-sm text-gray-600 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 transition-colors">
            {loginSection}
          </div>
        </GridItem>
      </Grid>
    </div>
  );
};

export default SignupLayout;
