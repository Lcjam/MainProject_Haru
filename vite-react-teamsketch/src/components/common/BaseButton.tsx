import { BUTTON_STYLES } from '../../constants/styles';

export interface BaseButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline';
  buttonSize?: 'sm' | 'md' | 'lg';
  className?: string;
  baseClassName?: string;
}

const BaseButton: React.FC<BaseButtonProps> = ({
  variant = 'primary',
  buttonSize = 'md',
  className = '',
  baseClassName,
  children,
  ...props
}) => {
  const buttonClassName =
    baseClassName ||
    `
    ${BUTTON_STYLES.variants[variant]}
    ${BUTTON_STYLES.sizes[buttonSize]}
    ${BUTTON_STYLES.base}
  `;

  return (
    <button className={`${buttonClassName} ${className}`} {...props}>
      {children}
    </button>
  );
};

export default BaseButton;
