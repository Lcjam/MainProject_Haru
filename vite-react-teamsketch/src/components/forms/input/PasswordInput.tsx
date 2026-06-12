import { useState } from 'react';
import { FaEye, FaEyeSlash } from 'react-icons/fa';
import BaseInput, { BaseInputProps } from '../../common/BaseInput';

interface PasswordInputProps extends Omit<BaseInputProps, 'type' | 'rightElement'> {
  label?: string;
  isNewPassword?: boolean;
}

const PasswordInput = ({ label, isNewPassword = false, ...props }: PasswordInputProps) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <BaseInput
      {...props}
      type={showPassword ? 'text' : 'password'}
      variant={props.error ? 'error' : props.variant}
      placeholder={props.placeholder || '비밀번호를 입력하세요'}
      autoComplete={isNewPassword ? 'new-password' : 'current-password'}
      className="border-primary-500"
      label={
        label && (
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-2">
            {label}
          </label>
        )
      }
      rightElement={
        <button
          type="button"
          onClick={() => setShowPassword(!showPassword)}
          className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
        >
          {showPassword ? <FaEyeSlash size={20} /> : <FaEye size={20} />}
        </button>
      }
    />
  );
};

export default PasswordInput;
