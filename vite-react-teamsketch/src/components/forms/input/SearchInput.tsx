import BaseInput, { BaseInputProps } from '../../common/BaseInput';

interface SearchInputProps extends Omit<BaseInputProps, 'type' | 'rightElement'> {
  onSearch?: (value: string) => void;
}

const SearchInput = ({ onSearch, ...props }: SearchInputProps) => {
  const searchIcon = (
    <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
      />
    </svg>
  );

  return (
    <BaseInput
      {...props}
      type="search"
      variant={props.variant || 'default'}
      placeholder={props.placeholder || '검색어를 입력하세요...'}
      className={`pl-10 ${props.className || 'boder-primary-500 '}`}
      onChange={(e) => {
        props.onChange?.(e);
        onSearch?.(e.target.value);
      }}
      rightElement={searchIcon}
    />
  );
};

export default SearchInput;
