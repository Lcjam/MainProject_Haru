import Select from '../../common/Select';
import { useAppSelector } from '../../../store/hooks';

interface InterestSelectProps {
  onInterestSelect: (categoryId: number) => void;
  selectedCategory?: number;
  categories?: Array<{
    categoryId: number;
    categoryName: string;
  }>;
}

const InterestSelect: React.FC<InterestSelectProps> = ({ onInterestSelect, selectedCategory }) => {
  const { categories } = useAppSelector((state) => state.category);
  const handleCategorySelect = (value: string) => {
    onInterestSelect(Number(value));
  };

  return (
    <Select
      options={categories.map((category) => ({
        value: category.categoryId.toString(),
        label: category.categoryName
      }))}
      onChange={handleCategorySelect}
      className="w-full bg-primary-300 text-text-light dark:bg-gray-800"
      placeholder="관심사를 선택해주세요"
      value={selectedCategory ? selectedCategory.toString() : ''}
    />
  );
};

export default InterestSelect;
