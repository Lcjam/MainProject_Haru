export interface RadioButtonProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'onChange' | 'size'> {
  label: string;
  value: string;
  checked: boolean;
  onChange: (value: string) => void;
  variant?: 'default' | 'circle' | 'square' | 'pill';
  size?: 'sm' | 'md' | 'lg';
  textSize?: 'xs' | 'sm' | 'base' | 'lg';
  borderColor?: string;
  backgroundColor?: string;
  textColor?: string;
  hoverBorderColor?: string;
  hoverTextColor?: string;
  checkedBorderColor?: string;
  checkedBackgroundColor?: string;
  checkedTextColor?: string;
  className?: string;
}

const RadioButton: React.FC<RadioButtonProps> = ({
  label,
  value,
  checked,
  onChange,
  variant = 'default',
  size = 'md',
  textSize = 'sm',
  borderColor = 'border-gray-300',
  backgroundColor = 'bg-white',
  textColor = 'text-gray-600',
  hoverBorderColor = 'hover:border-primary-light',
  hoverTextColor = 'hover:text-primary-light',
  checkedBorderColor = 'border-primary-light',
  checkedBackgroundColor = 'bg-primary-light',
  checkedTextColor = 'text-white',
  className = '',
  ...props
}) => {
  const sizeStyles = {
    sm: 'w-12 h-12',
    md: 'w-14 h-14',
    lg: 'w-16 h-16'
  };

  const textStyles = {
    xs: 'text-xs',
    sm: 'text-sm',
    base: 'text-base',
    lg: 'text-lg'
  };

  const variantStyles = {
    default: 'rounded-full',
    circle: 'rounded-full',
    square: 'rounded-lg',
    pill: 'rounded-full px-4'
  };

  return (
    <label className="relative inline-flex items-center">
      <input
        type="radio"
        value={value}
        checked={checked}
        onChange={() => onChange(value)}
        className="sr-only peer"
        {...props}
      />
      <div
        className={`
          cursor-pointer
          flex items-center justify-center
          font-medium
          transition-all duration-200 ease-in-out
          ${sizeStyles[size]}
          ${textStyles[textSize]}
          ${variantStyles[variant]}
          border-2
          ${
            checked
              ? `${checkedBackgroundColor} ${checkedTextColor} ${checkedBorderColor} transform scale-110 font-bold`
              : `${backgroundColor} ${textColor} ${borderColor} ${hoverBorderColor} ${hoverTextColor}`
          }
          peer-focus:ring-2 peer-focus:ring-primary-light peer-focus:ring-opacity-50
          ${className}
        `}
      >
        {label}
      </div>
    </label>
  );
};

export default RadioButton;
