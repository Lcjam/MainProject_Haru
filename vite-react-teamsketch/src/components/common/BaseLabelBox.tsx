// 1. 기본 props 인터페이스 정의
interface BaseLabelBoxProps {
  label: string;
  children: React.ReactNode;
  className?: string;
}

// 2. 기본 컴포넌트 구현
const BaseLabelBox = ({ label, children, className = '' }: BaseLabelBoxProps) => {
  return (
    <div className={`flex flex-col gap-2 ${className}`}>
      <label className="text-sm font-medium text-gray-700">{label}</label>
      {children}
    </div>
  );
};

export default BaseLabelBox;
