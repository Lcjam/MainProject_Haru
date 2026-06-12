import React from 'react';

interface SelectOption {
  value: string | number;
  label: string;
}

interface SelectProps {
  options: SelectOption[];
  value?: string | number;
  onChange?: (value: string) => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
}

const Select: React.FC<SelectProps> = ({
  options,
  value,
  onChange,
  className = '',
  placeholder,
  disabled = false
}) => {
  return (
    <select
      value={value}
      onChange={(e) => onChange?.(e.target.value)}
      className={`
        px-3 py-1 text-sm 
        rounded-full 
        bg-primary-light text-white
        hover:bg-primary
        transition-colors
        cursor-pointer
        focus:outline-none focus:ring-2 focus:ring-primary-light
        ${className}
      `}
      disabled={disabled}
    >
      {placeholder && (
        <option value="" disabled>
          {placeholder}
        </option>
      )}
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
};

export default Select;
