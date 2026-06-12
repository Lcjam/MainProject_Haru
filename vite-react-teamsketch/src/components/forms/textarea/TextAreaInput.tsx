import BaseTextArea, { BaseTextAreaProps } from '../../common/BaseTextArea';

interface TextAreaInputProps extends BaseTextAreaProps {
  inputType?: 'bio' | 'long';
}

const TextAreaInput: React.FC<TextAreaInputProps> = ({
  inputType = 'bio',
  className = '',
  ...props
}) => {
  return (
    <BaseTextArea
      className={`min-h-[100px] ${className}`}
      rows={inputType === 'bio' ? 3 : 5}
      resize={false}
      {...props}
    />
  );
};

export default TextAreaInput;
