import { ReactNode } from 'react';

interface CardProps {
  title: string;
  description?: string;
  image?: ReactNode;
  onClick?: () => void;
  className?: string;
  price?: string;
  dopamine?: number;
  currentParticipants?: number;
  maxParticipants?: number;
  location?: string;
}

const Card = ({
  title,
  description,
  image,
  onClick,
  className = '',
  price,
  dopamine,
  currentParticipants,
  maxParticipants,
  location
}: CardProps) => {
  return (
    <div
      onClick={onClick}
      className={`
        bg-white dark:bg-gray-800 
        rounded-lg shadow-md 
        hover:shadow-lg 
        transition-all duration-300 
        flex flex-row 
        overflow-hidden
        h-32
        ${className}
      `}
    >
      {/* 좌측: 이미지 + 도파민 */}
      <div className="relative w-32 h-full flex-shrink-0 overflow-hidden">
        {image || (
          <img 
            src="/placeholder.png" 
            alt={title} 
            className="w-full h-full object-cover"
          />
        )}
        {dopamine && (
          <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-primary-500/80 to-transparent p-2">
            <span className="text-white text-sm font-semibold">도파민 {dopamine}</span>
          </div>
        )}
      </div>

      {/* 우측: 정보 */}
      <div className="flex-1 p-3 flex flex-col justify-between min-w-0">
        {/* 상단: 제목 + 모집인원 */}
        <div className="flex justify-between items-start gap-2 mb-1">
          <h3 className="text-gray-900 dark:text-white font-medium text-sm truncate flex-1">
            {title}
          </h3>
          {currentParticipants !== undefined && maxParticipants !== undefined && (
            <span className="text-xs text-primary-600 dark:text-primary-400 whitespace-nowrap">
              {currentParticipants}/{maxParticipants}명
            </span>
          )}
        </div>

        {/* 중단: 설명 */}
        {description && (
          <p className="text-gray-600 dark:text-gray-300 text-xs line-clamp-2 mb-2">
            {description}
          </p>
        )}

        {/* 하단: 가격 + 위치 */}
        <div className="flex justify-between items-center mt-auto">
          {price !== undefined && (
            <span className="text-primary-600 dark:text-primary-400 font-semibold text-sm">
              {price}원
            </span>
          )}
        </div>
        <div>
          {location && (
            <div className="flex items-center text-xs text-gray-500 dark:text-gray-400">
              <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
              <span className="truncate">{location}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Card;
