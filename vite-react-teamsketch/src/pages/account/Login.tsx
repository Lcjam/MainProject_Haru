import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '../../store/hooks';
import { login } from '../../store/slices/authSlice';
import { setUser } from '../../store/slices/userSlice';
import type { LoginRequest } from '../../types/auth';
import { useLogin, useInfoApi } from '../../services/api/authAPI';
import LoginLayout from '../../components/layout/LoginLayout';
import Button from '../../components/common/BaseButton';
import Loading from '../../components/common/Loading';
import { google, kakao, naver } from '../../assets/images/login';
import EmailInput from '../../components/forms/input/EmailInput';
import LoginPasswordInput from '../../components/forms/input/LoginPasswordInput';
import { validateEmail, validatePassword } from '../../utils/validation';
import { toast } from 'react-toastify';
import { isIOS } from 'react-device-detect';

const Login = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [isLoading, setIsLoading] = useState(false);
  const loginMutation = useLogin();
  const userData = useInfoApi();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [validationErrors, setValidationErrors] = useState({
    email: '',
    password: ''
  });

  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showInstallButton, setShowInstallButton] = useState(false);

  useEffect(() => {
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstallButton(true); // 설치 버튼 표시
    });
  }, []);

  const handleInstallClick = () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      deferredPrompt.userChoice.then((choiceResult: any) => {
        if (choiceResult.outcome === 'accepted') {
          console.log('✅ 앱 설치 완료!');
        } else {
          console.log('❌ 앱 설치 취소됨');
        }
        setDeferredPrompt(null);
        setShowInstallButton(false);
      });
    }
  };

  const handleLogin = async (loginData: LoginRequest) => {
    setIsLoading(true);
    try {
      const response = await loginMutation.mutateAsync(loginData);

      if (response.status === 'success' && response.data) {
        // 1. 로그인 성공 시 토큰과 기본 정보 저장
        dispatch(
          login({
            email: response.data.email || '',
            nickname: response.data.nickname || '',
            userId: response.data.id || 0,
            token: response.data.token || ''
          })
        );

        const userinfo = await userData.mutateAsync();
        console.log('사용자 정보:', userinfo.data);

        // 2. 사용자 정보를 userSlice에 저장
        dispatch(
          setUser({
            id: userinfo.data.id || 0,
            email: userinfo.data.email || '',
            name: userinfo.data.name || '',
            nickname: userinfo.data.nickname || '',
            phoneNumber: userinfo.data.phoneNumber || null,
            bio: userinfo.data.bio || null,
            loginMethod: userinfo.data.loginMethod || 'EMAIL',
            socialProvider: userinfo.data.socialProvider || 'NONE',
            accountStatus: userinfo.data.accountStatus || 'Active',
            authority: userinfo.data.authority || 'USER',
            profileImagePath: null,
            signupDate: userinfo.data.signupDate || '',
            lastUpdateDate: userinfo.data.lastUpdateDate || '',
            lastLoginTime: userinfo.data.lastLoginTime || null,
            loginFailedAttempts: 0,
            loginIsLocked: false,
            dopamine: userinfo.data.dopamine || 0,
            points: userinfo.data.points || 0,
            interest: userinfo.data.hobbies?.map((hobby: any) => hobby.categoryName) || [],
            hobby:
              userinfo.data.hobbies?.map((hobby: any) => ({
                hobbyId: hobby.hobbyId,
                hobbyName: hobby.hobbyName,
                categoryId: hobby.categoryId,
                categoryName: hobby.categoryName
              })) || []
          })
        );

        navigate('/my-location');
      } else {
        throw new Error(response || '로그인에 실패했습니다.');
      }
    } catch (err: any) {
      const errorMessage = err?.response?.data?.data?.message || '알 수 없는 에러가 발생했습니다.';
      console.error('로그인 에러:', errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value
    }));

    // 실시간 유효성 검사
    let validationResult = { isValid: true, message: '' };
    if (name === 'email') {
      validationResult = validateEmail(value);
    } else if (name === 'password') {
      validationResult = validatePassword(value);
    }

    setValidationErrors((prev) => ({
      ...prev,
      [name]: validationResult.message
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();


    // 입력값 유효성 검사
    const emailValidation = validateEmail(formData.email);
    const passwordValidation = validatePassword(formData.password);

    setValidationErrors({
      email: emailValidation.message,
      password: passwordValidation.message
    });

    if (!emailValidation.isValid || !passwordValidation.isValid) {
      return;
    }

    await handleLogin(formData);
  };

  return (
    <>
      {isIOS && (
      <div className="fixed bottom-4 left-1/2 -translate-x-1/2 bg-purple-100 text-purple-800 px-4 py-2 rounded shadow">
        iPhone 사용자는 Safari 공유 버튼을 눌러 <b>“홈 화면에 추가”</b> 해주세요
      </div>
      )}

      {showInstallButton && (
        <button
          className="fixed top-4 right-4 bg-purple-600 text-white px-4 py-2 rounded shadow-lg z-50"
          onClick={handleInstallClick}
        >
          앱 설치하기
        </button>
      )}

    
      {isLoading && <Loading />}
      <form
        className="h-full w-full bg-white dark:bg-gray-800 flex flex-col"
        onSubmit={handleSubmit}
      >
        <LoginLayout
          title={<h1 className="text-2xl font-bold">Haru, 함께 하는 즐거움!</h1>}
          forgotPassword={
            <a
              href="/forgotpassword"
              className="text-sm text-primary-light hover:text-primary-dark"
            >
              비밀번호 찾기
            </a>
          }
          loginButton={
            <Button
              type="submit"
              className="w-full bg-primary-500 text-white"
              data-testid="login-button"
              disabled={isLoading}
            >
              {isLoading ? <Loading /> : '로그인'}
            </Button>
          }
          divider={
            <div className="relative my-6 h-px bg-gray-300 dark:bg-gray-700">
              <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-white dark:bg-gray-800 px-4 text-sm text-gray-500">
                OR
              </span>
            </div>
          }
          socialLogins={
            <div className="flex justify-center items-center gap-6">
              <button type="button" className="w-20 h-20 hover:opacity-80 transition-opacity">
                <img src={google} alt="구글 로그인" className="w-full h-full object-contain" />
              </button>
              <button type="button" className="w-20 h-20 hover:opacity-80 transition-opacity">
                <img src={kakao} alt="카카오 로그인" className="w-full h-full object-contain" />
              </button>
              <button type="button" className="w-20 h-20 hover:opacity-80 transition-opacity">
                <img src={naver} alt="네이버 로그인" className="w-full h-full object-contain" />
              </button>
            </div>
          }
          signupLink={
            <div className="text-sm">
              아직 회원이 아니신가요?{' '}
              <button
                type="button"
                onClick={() => navigate('/signup')}
                className="text-primary-500 shadow-none hover:text-primary-dark"
              >
                회원가입
              </button>
            </div>
          }
        >
          <EmailInput
            name="email"
            value={formData.email}
            onChange={handleChange}
            placeholder="이메일"
            data-testid="email-input"
            disabled={isLoading}
            error={validationErrors.email}
          />
          <LoginPasswordInput
            name="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="비밀번호"
            data-testid="password-input"
            disabled={isLoading}
            error={validationErrors.password}
          />
        </LoginLayout>
      </form>
    </>
  );
};

export default Login;
