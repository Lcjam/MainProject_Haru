import BaseInput, { BaseInputProps } from '../../common/BaseInput';

interface TextInputProps extends Omit<BaseInputProps, 'type'> {
  label?: string;
  error?: string;
  inputType?: string;
  type?: string;
}

const TextInput = ({ label, type = 'input', ...props }: TextInputProps) => {
  // 입력 필드 타입에 따른 autoComplete 값 설정

  return (
    <BaseInput
      {...props}
      type={type}
      variant={props.error ? 'error' : props.variant}
      className={`${props.className || 'border-primary-500'}`}
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

export default TextInput;
