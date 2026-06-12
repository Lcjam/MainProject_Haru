import BaseInput, { BaseInputProps } from '../../common/BaseInput';

interface LoginPasswordInputProps extends Omit<BaseInputProps, 'type'> {
  label?: string;
}

const LoginPasswordInput = ({ label, ...props }: LoginPasswordInputProps) => {
  return (
    <BaseInput
      {...props}
      type="password"
      autoComplete="current-password"
      placeholder={props.placeholder || '비밀번호를 입력하세요'}
      className="border-primary-500"
      label={
        label && (
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-2">
            {label}
          </label>
        )
      }
    />
  );
};

export default LoginPasswordInput;
