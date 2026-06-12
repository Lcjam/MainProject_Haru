import Select from '../../common/Select';
import { useAppSelector } from '../../../store/hooks';

interface HobbySelectProps {
  onHobbySelect: (categoryId: number, hobbyId: number) => void;
  selectedHobbies?: Array<{
    hobbyId: number;
    categoryId: number;
  }>;
  categoryId?: number;
}

const HobbySelect: React.FC<HobbySelectProps> = ({
  onHobbySelect,
  selectedHobbies = [],
  categoryId
}) => {
  const { constantHobbies } = useAppSelector((state) => state.category);

  // constantHobbies에서 현재 선택된 카테고리에 해당하는 취미들만 필터링
  const filteredHobbies = constantHobbies.filter((hobby) =>
    hobby.categories.some((cat) => cat.categoryId === categoryId)
  );

  const handleHobbySelect = (value: string) => {
    if (categoryId) {
      onHobbySelect(categoryId, Number(value));
    }
  };

  return (
    <Select
      options={filteredHobbies.map((hobby) => ({
        value: hobby.hobbyId.toString(),
        label: hobby.hobbyName
      }))}
      onChange={handleHobbySelect}
      className="w-full bg-primary-300 text-text-light dark:bg-gray-800"
      placeholder="취미를 선택해주세요"
      value={selectedHobbies.find((h) => h.categoryId === categoryId)?.hobbyId.toString() || ''}
      disabled={!categoryId}
    />
  );
};

export default HobbySelect;
