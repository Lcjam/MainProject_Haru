import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Button from "../../components/common/BaseButton";
import LoginLayout from "../../components/layout/LoginLayout";
import PasswordInput from "../../components/forms/input/PasswordInput";
import { validatePassword } from '../../utils/validation';
import { useAppSelector } from '../../store/hooks';
import { toast } from 'react-toastify'
import { usePasswordChangeNT } from '../../services/api/authAPI';
import { resetPasswordInfo } from '../../store/slices/passwordChangeSlice';
import { useAppDispatch } from '../../store/hooks';
// , passwordChangeNoneToken

// import axios from 'axios';
const ResetPassword = () => {
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const passwordChangeInfo = useAppSelector((state) => state.passwordChange);
    const passwordChange = usePasswordChangeNT();
    const dispatch = useAppDispatch();

   
    
    console.log("현재 비밀번호 변경 정보:", passwordChangeInfo);
    
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (password !== confirmPassword) {
            toast.error('비밀번호가 일치하지 않습니다.');
            return;
        }

        if (password.length < 8) {
            toast.error('비밀번호는 8자 이상이어야 합니다.');
            return;
        }
        
        // 이메일이나 전화번호가 없는 경우 처리
        if (!passwordChangeInfo.email || !passwordChangeInfo.phoneNumber) {
            toast.error('이메일과 전화번호 정보가 필요합니다.');
            return;
        }
        
        const passwordCaNoToData = {
            isToken: 'false', // 명시적으로 문자열 'false' 설정
            email: passwordChangeInfo.email,
            phoneNumber: passwordChangeInfo.phoneNumber,
            currentPassword: '',
            newPassword: password,
            confirmPassword: confirmPassword
        };
        console.log("passwordCaNoToData",passwordCaNoToData);
        try {
            const response = await passwordChange.mutateAsync(passwordCaNoToData);

            // 이건 react-query 사용 안하고 api 호출 함수 자체를 가져와서 쓰는 방법법
            // const responseNoneReactQuery = passwordChangeNoneToken(passwordCaNoToData);
            // console.log(responseNoneReactQuery)
        
            // 이렇게 axios 사용해서 api요청하는데
            // 저희 앱에서는 api 호출시 설정(config), 인스턴스 만들어서
            // 호출하고 있어요
            // 이렇게만 해도 되는데, react query까지 쓰면 위의 어려운 버전
            // 으로 호출하게 돼요요
            // try {
            //     const url = 'http://localhost:8080/api/core/auth/me/password/notoken';
        
            //     const passwordCaNoToData = {
            //       isToken: passwordChangeInfo.isToken,
            //       email: passwordChangeInfo.email,
            //       phoneNumber: passwordChangeInfo.phoneNumber,
            //       newPassword: passwordChangeInfo.newPassword,
            //       confirmPassword: passwordChangeInfo.confirmPassword,
            //     };
        
            //     const response = await axios.put(url, passwordCaNoToData, {
            //       headers: {
            //         'Content-Type': 'application/json',
            //       },
            //     });
        
            //     console.log('비밀번호 변경 성공:', response.data);
            //     return response.data;
            //   } catch (error) {
            //     console.error('비밀번호 변경 실패:', error);
            //     throw error;
            // }
        
            console.log("response:", response.success);
            if (response.success) {
                dispatch(resetPasswordInfo());
                toast.success('비밀번호가 성공적으로 변경되었습니다.');
                navigate('/login');
            } else {
                // 서버에서 success: false로 응답한 경우
                toast.error(response.message || '비밀번호 변경에 실패했습니다.');
            }
        } catch (error: any) {
            console.error('비밀번호 변경 오류:', error);
            
            // 서버 응답에서 에러 메시지 추출
            const errorMessage = error.response?.data?.message || 
                               '비밀번호 변경 중 오류가 발생했습니다.';
            
            toast.error(errorMessage);
        }
    }

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { value } = e.target;
        let validationResult = { isValid: true, message: '' };
        validationResult = validatePassword(value); 
        setPassword(e.target.value);
        setError(validationResult.message);
    }

    return (
        <LoginLayout
            title={<h1 className="text-2xl font-bold text-center">비밀번호 재설정</h1>}           
        >   
            <form onSubmit={handleSubmit} className="space-y-4">
                <p className="text-sm text-gray-500 text-center">
                    새로운 비밀번호를 입력하세요.
                </p>
                {/* 새 비밀번호 입력 */}
                <PasswordInput
                    name="password"
                    label="새 비밀번호"
                    placeholder="새 비밀번호를 입력하세요"
                    value={password}
                    isNewPassword={true}
                    onChange={handleChange}
                />

                {/* 비밀번호 확인 입력 */}
                <PasswordInput
                    name="confirmPassword"
                    label="비밀번호 확인"
                    placeholder="비밀번호를 다시 입력하세요"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                />
                {error && <p className="text-primary-500 text-xs">{error}</p>}
                <Button type="submit" className="w-full bg-primary-500">
                    비밀번호 변경
                </Button>
            </form>
        </LoginLayout>
    );
};

export default ResetPassword;