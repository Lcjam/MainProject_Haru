import BaseLabelBox from '../../common/BaseLabelBox';

// 1. 확장된 props 인터페이스 정의
interface InfoBoxProps {
  label: string;
  content: string;
  description?: string;
  highlight?: boolean;
}

// 2. BaseLabelBox를 활용한 새로운 컴포넌트
const InfoBox = ({ label, content, description, highlight = false }: InfoBoxProps) => {
  return (
    <BaseLabelBox label={label} className={highlight ? 'bg-yellow-50 p-4 rounded-lg' : ''}>
      <div className="space-y-1">
        <div className="text-gray-900">{content}</div>
        {description && <div className="text-sm text-gray-500">{description}</div>}
      </div>
    </BaseLabelBox>
  );
};

export default InfoBox;
