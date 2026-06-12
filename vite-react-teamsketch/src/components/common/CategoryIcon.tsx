import { useState } from 'react';
import { useAppSelector } from '../../store/hooks';

interface ICategoryIconProps {
  categorySize?: 'sm' | 'md' | 'lg';
  onCategorySelect?: (categoryId: number, categoryName: string) => void;
}

const CategoryIcon: React.FC<ICategoryIconProps> = ({ onCategorySelect, categorySize = 'md' }) => {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const { categories } = useAppSelector((state) => state.category);

  const handleCategoryClick = (categoryId: number, categoryName: string) => {
    setSelectedCategory(categoryName);
    onCategorySelect?.(categoryId, categoryName);
  };

  // 전체 버튼 클릭 핸들러 추가
  const handleAllClick = () => {
    setSelectedCategory(null);
    onCategorySelect?.(0, '전체');
  };

  const getButtonSize = () => {
    switch (categorySize) {
      case 'sm':
        return 'w-10 h-10';
      case 'lg':
        return 'w-16 h-16';
      default:
        return 'w-14 h-14';
    }
  };

  const getTextSize = () => {
    switch (categorySize) {
      case 'sm':
        return 'text-xs';
      case 'lg':
        return 'text-base';
      default:
        return 'text-sm';
    }
  };

  const getCategoryEmoji = (categoryName: string): string => {
    const emojiMap: { [key: string]: string } = {
      전체: '🔍',
      '건강과 웰빙': '💪',
      게임: '🎮',
      '공연 예술': '🎭',
      '기술과 공학': '🔧',
      '독서와 문학': '📚',
      반려동물: '🐶',
      '사회 활동': '👥',
      수공예: '🔨',
      수집: '🎁',
      스포츠: '🏃',
      '시각 예술': '🎨',
      '실내 운동': '🏃',
      '야외 활동': '🏃',
      '언어 학습': '🎓',
      여행: '🌍',
      '요리와 음식': '🍽️',
      음악: '🎵',
      자기계발: '📚',
      '자연과 환경': '🌳',
      '창작 활동': '🎨'
    };
    return emojiMap[categoryName] || '🔥';
  };

  return (
    <div className="w-full dark:bg-gray-800 pt-2">
      <div className="flex gap-3 overflow-x-auto pb-4 px-2 no-scrollbar">
        {/* 전체 버튼 */}
        <div className="flex-shrink-0 flex flex-col items-center gap-2 p-1">
          <button
            onClick={() => handleAllClick()}
            className={`
              ${getButtonSize()}
              flex items-center justify-center
              bg-gradient-to-br from-primary-50 to-primary-100
              dark:from-gray-700 dark:to-gray-800
              border-2 rounded-full
              transition-all duration-300 ease-in-out
              shadow-sm hover:shadow-md
              ${
                selectedCategory === null
                  ? 'border-primary-400 dark:border-primary-500 bg-gradient-to-br from-primary-100 to-primary-200 dark:from-primary-900 dark:to-primary-800 transform hover:scale-105'
                  : 'border-primary-200 dark:border-primary-700 hover:border-primary-300 dark:hover:border-primary-600 hover:scale-105'
              }
            `}
          >
            <span className="text-xl">{getCategoryEmoji('전체')}</span>
          </button>
          <span
            className={`${getTextSize()} font-medium text-center text-primary-700 dark:text-primary-300`}
          >
            전체
          </span>
        </div>

        {categories.map((category) => (
          <div
            key={category.categoryId}
            className="flex-shrink-0 flex flex-col items-center gap-2 p-1"
          >
            <button
              onClick={() => handleCategoryClick(category.categoryId, category.categoryName)}
              className={`
                ${getButtonSize()}
                flex items-center justify-center
                bg-gradient-to-br from-primary-50 to-primary-100
                dark:from-gray-700 dark:to-gray-800
                border-2 rounded-full
                transition-all duration-300 ease-in-out
                shadow-sm hover:shadow-md
                ${
                  selectedCategory === category.categoryName
                    ? 'border-primary-400 dark:border-primary-500 bg-gradient-to-br from-primary-100 to-primary-200 dark:from-primary-900 dark:to-primary-800 transform hover:scale-105'
                    : 'border-primary-200 dark:border-primary-700 hover:border-primary-300 dark:hover:border-primary-600 hover:scale-105'
                }
              `}
            >
              <span className="text-xl">{getCategoryEmoji(category.categoryName)}</span>
            </button>
            <span
              className={`${getTextSize()} font-medium text-center text-primary-700 dark:text-primary-300`}
            >
              {category.categoryName}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CategoryIcon;
