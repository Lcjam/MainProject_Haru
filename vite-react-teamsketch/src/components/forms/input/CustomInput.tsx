import { useState } from 'react';
import BaseInput, { BaseInputProps } from '../../common/BaseInput';

interface CustomInputProps extends Omit<BaseInputProps, 'type'> {
  label?: string;
  helperText?: string;
  prefix?: string;
  suffix?: string;
}

const CustomInput = ({ label, helperText, prefix, suffix, ...props }: CustomInputProps) => {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-2">
          {label}
        </label>
      )}

      <div className="relative flex items-center w-full">
        {prefix && <span className="flex-shrink-0 text-gray-500 pl-3">{prefix}</span>}

        <BaseInput
          {...props}
          type="text"
          variant={props.error ? 'error' : props.variant}
          className={`
            flex-grow
            ${prefix ? 'pl-2' : 'pl-3'}
            ${suffix ? 'pr-2' : 'pr-3'}
            ${props.className || ''}
          `}
          onFocus={(e) => {
            setIsFocused(true);
            props.onFocus?.(e);
          }}
          onBlur={(e) => {
            setIsFocused(false);
            props.onBlur?.(e);
          }}
        />

        {suffix && <span className="flex-shrink-0 text-gray-500 pr-3">{suffix}</span>}
      </div>

      {helperText && !props.error && (
        <p className={`text-xs mt-1 ${isFocused ? 'text-primary-light' : 'text-gray-500'}`}>
          {helperText}
        </p>
      )}
    </div>
  );
};

export default CustomInput;
