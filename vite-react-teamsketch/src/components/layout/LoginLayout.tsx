import { ReactNode } from 'react';
import Grid from '../common/Grid';
import GridItem from '../common/GridItem';

interface LoginLayoutProps {
  title?: ReactNode;
  children?: ReactNode;
  forgotPassword?: ReactNode;
  loginButton?: ReactNode;
  divider?: ReactNode;
  socialLogins?: ReactNode;
  signupLink?: ReactNode;
}

const LoginLayout = ({
  title,
  children,
  forgotPassword,
  loginButton,
  divider,
  socialLogins,
  signupLink
}: LoginLayoutProps) => {
  return (
    <div className="h-screen w-full bg-white dark:bg-gray-800">
      <Grid cols={1} gap="sm" className="h-full grid-rows-[1fr_2fr_1fr] items-center">
        {/* 로고 및 타이틀 */}
        <GridItem className="flex items-center justify-center">
          <div className="text-2xl font-bold text-primary-500 dark:text-primary-400">{title}</div>
        </GridItem>

        {/* 메인 폼 */}
        <GridItem className="flex items-center justify-center">
          <div className="w-[70%] sm:w-[60%] md:w-[50%] lg:w-[40%] space-y-6">
            <div className="space-y-4">{children}</div>

            {/* 비밀번호 찾기 */}
            <div className="flex justify-end text-sm text-gray-600 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 transition-colors">
              {forgotPassword}
            </div>

            {/* 로그인 버튼 */}
            <div className="pt-2">{loginButton}</div>

            {/* 구분선 */}
            <div className="relative">{divider}</div>

            {/* 소셜 로그인 버튼들 */}
            <div className="space-y-3">{socialLogins}</div>
          </div>
        </GridItem>

        {/* 회원가입 링크 */}
        <GridItem className="flex items-center justify-center">
          <div className="text-center text-sm text-gray-600 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 transition-colors">
            {signupLink}
          </div>
        </GridItem>
      </Grid>
    </div>
  );
};

export default LoginLayout;
