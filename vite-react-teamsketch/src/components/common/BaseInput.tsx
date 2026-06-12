import { INPUT_STYLES } from '../../constants/styles';

export interface BaseInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  variant?: 'default' | 'error' | 'success' | 'warning';
  inputSize?: 'sm' | 'md' | 'lg';
  className?: string;
  baseClassName?: string;
  wrapperClassName?: string;
  label?: React.ReactNode;
  rightElement?: React.ReactNode;
  error?: string;
  type?: string;
}

const BaseInput: React.FC<BaseInputProps> = ({
  variant = 'default',
  inputSize = 'md',
  className = '',
  baseClassName,
  wrapperClassName = '',
  label,
  rightElement,
  error,
  children,
  ...props
}) => {
  const inputClassName =
    baseClassName ||
    `
    ${INPUT_STYLES.variants[variant]}
    ${INPUT_STYLES.sizes[inputSize]}
    ${INPUT_STYLES.base}
    ${rightElement ? 'pr-10' : ''}
  `;

  return (
    <div className={`space-y-1 ${wrapperClassName}`}>
      {label}
      <div className="relative">
        <input className={`${inputClassName} ${className}`} {...props} />
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

export default BaseInput;
