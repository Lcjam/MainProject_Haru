import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAppDispatch } from '../../store/hooks';
import { login } from '../../store/slices/authSlice';
import { setUser } from '../../store/slices/userSlice';
import { exchangeOAuth2Code, getUserInfoApi } from '../../services/api/authAPI';
import { getErrorMessage } from '../../utils/errorMessage';

interface ServerHobby {
  hobbyId: number;
  hobbyName: string;
  categoryId: number;
  categoryName: string;
}

const OAuth2Redirect = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [message, setMessage] = useState('소셜 로그인 정보를 확인하고 있습니다.');
  const started = useRef(false);

  useEffect(() => {
    const error = params.get('error');
    const code = params.get('code');
    if (error || !code) {
      setMessage('소셜 로그인에 실패했습니다. 다시 시도해주세요.');
      return;
    }
    if (started.current) return;
    started.current = true;
    window.history.replaceState(window.history.state, '', '/oauth2/redirect');

    const completeLogin = async () => {
      try {
        const response = await exchangeOAuth2Code(code);
        if (response.status !== 'success' || !response.data?.token) {
          throw new Error('소셜 로그인에 실패했습니다.');
        }
        dispatch(login({
          email: response.data.email || '',
          nickname: response.data.nickname || '',
          userId: response.data.id || 0,
          token: response.data.token
        }));
        const userinfo = await getUserInfoApi();
        dispatch(setUser({
          id: userinfo.data.id || 0,
          email: userinfo.data.email || '',
          name: userinfo.data.name || '',
          nickname: userinfo.data.nickname || '',
          phoneNumber: userinfo.data.phoneNumber || null,
          bio: userinfo.data.bio || null,
          loginMethod: userinfo.data.loginMethod || 'SOCIAL',
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
          interest: userinfo.data.hobbies?.map((hobby: ServerHobby) => hobby.categoryName) || [],
          hobby: userinfo.data.hobbies?.map((hobby: ServerHobby) => ({
            hobbyId: hobby.hobbyId,
            hobbyName: hobby.hobbyName,
            categoryId: hobby.categoryId,
            categoryName: hobby.categoryName
          })) || []
        }));
        navigate('/my-location', { replace: true });
      } catch (err: unknown) {
        setMessage(getErrorMessage(err, '소셜 로그인에 실패했습니다. 다시 시도해주세요.'));
      }
    };
    void completeLogin();
  }, [dispatch, navigate, params]);

  return <main role="status" className="p-6 text-center">{message}</main>;
};

export default OAuth2Redirect;
