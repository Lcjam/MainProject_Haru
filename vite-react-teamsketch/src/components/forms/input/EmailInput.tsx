import BaseInput, { BaseInputProps } from '../../common/BaseInput';

interface EmailInputProps extends Omit<BaseInputProps, 'type'> {
  label?: string;
  isNewEmail?: boolean; // 새 이메일 입력인지 여부
}

const EmailInput = ({ label, isNewEmail = false, ...props }: EmailInputProps) => {
  return (
    <BaseInput
      {...props}
      type="email"
      autoComplete={isNewEmail ? 'new-email' : 'username'} // 로그인용은 username, 새 이메일은 new-email
      variant={props.error ? 'error' : props.variant}
      placeholder={props.placeholder || '이메일을 입력하세요'}
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

export default EmailInput;
