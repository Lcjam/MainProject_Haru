import { INPUT_STYLES } from '../../constants/styles';

export interface BaseTextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  variant?: 'default' | 'error' | 'success' | 'warning';
  inputSize?: 'sm' | 'md' | 'lg';
  className?: string;
  baseClassName?: string;
  wrapperClassName?: string;
  label?: React.ReactNode;
  rightElement?: React.ReactNode;
  error?: string;
  rows?: number;
  resize?: boolean;
}

const BaseTextArea: React.FC<BaseTextAreaProps> = ({
  variant = 'default',
  inputSize = 'md',
  className = '',
  baseClassName,
  wrapperClassName = '',
  label,
  rightElement,
  error,
  children,
  rows = 3,
  resize = false,
  ...props
}) => {
  const textareaClassName =
    baseClassName ||
    `
    ${INPUT_STYLES.variants[variant]}
    ${INPUT_STYLES.sizes[inputSize]}
    ${INPUT_STYLES.base}
    ${!resize && 'resize-none'}
    ${rightElement ? 'pr-10' : ''}
  `;

  return (
    <div className={`space-y-1 ${wrapperClassName}`}>
      {label}
      <div className="relative">
        <textarea rows={rows} className={`${textareaClassName} ${className}`} {...props} />
        {rightElement && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">{rightElement}</div>
        )}
      </div>
      {children}
      {error && (
        <p className="text-red-500 text-xs mt-1" role="alert">
          {error}
        </p>
      )}
    </div>
  );
};

export default BaseTextArea;
