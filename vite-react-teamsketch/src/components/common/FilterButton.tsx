import { useState, useEffect, useRef } from 'react';
import { HiOutlineAdjustmentsHorizontal } from 'react-icons/hi2';
import { useDispatch, useSelector } from 'react-redux';
import { setDistance } from '../../store/slices/productSlice';
import { RootState } from '../../store/store';

interface FilterButtonProps {
    onDistanceChange: (distance: number) => void;
    className?: string;
}

const FilterButton = ({ onDistanceChange = () => {}, className = '' }: FilterButtonProps) => {
    const dispatch = useDispatch();
    const distance = useSelector((state: RootState) => state.product.distance);
    const [open, setOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    // 드롭다운 외부 클릭 시 닫기
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleDistanceChange = (newDistance: number) => {
        onDistanceChange(newDistance);
        dispatch(setDistance(newDistance));
    };


    return (
        <div className={`relative ${className}`} ref={dropdownRef}>
            {/* 필터 버튼 */}
            <button
                onClick={() => setOpen(!open)}
                className={`
                    flex items-center gap-2 px-4 py-2 rounded-full
                    bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300
                    border border-gray-200 dark:border-gray-700
                    shadow-sm hover:shadow-md transition-all duration-200
                    ${open ? 'ring-2 ring-primary-500 dark:ring-primary-400' : ''}
                `}
            >
                <HiOutlineAdjustmentsHorizontal className="w-5 h-5" />
                <span className="text-sm font-medium">반경 {distance}km</span>
            </button>

            {/* 드롭다운 메뉴 */}
            {open && (
                <div className="absolute mt-2 right-0 w-72 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 p-4 z-50">
                    <div className="space-y-4">
                        <div className="flex items-center justify-between">
                            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                                검색 반경 설정
                            </h3>
                            <span className="text-sm font-medium text-primary-500 dark:text-primary-400">
                                {distance}km
                            </span>
                        </div>

                        {/* 슬라이더 */}
                        <div className="px-2">
                            <input
                                type="range"
                                min="1"
                                max="500"
                                value={distance}
                                onChange={(e) => handleDistanceChange(Number(e.target.value))}
                                className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer
                                    [&::-webkit-slider-thumb]:appearance-none
                                    [&::-webkit-slider-thumb]:w-4
                                    [&::-webkit-slider-thumb]:h-4
                                    [&::-webkit-slider-thumb]:rounded-full
                                    [&::-webkit-slider-thumb]:bg-primary-500
                                    [&::-webkit-slider-thumb]:cursor-pointer
                                    [&::-webkit-slider-thumb]:transition-all
                                    [&::-webkit-slider-thumb]:duration-200
                                    [&::-webkit-slider-thumb]:hover:scale-110
                                    [&::-moz-range-thumb]:w-4
                                    [&::-moz-range-thumb]:h-4
                                    [&::-moz-range-thumb]:rounded-full
                                    [&::-moz-range-thumb]:bg-primary-500
                                    [&::-moz-range-thumb]:cursor-pointer
                                    [&::-moz-range-thumb]:border-0
                                    [&::-moz-range-thumb]:transition-all
                                    [&::-moz-range-thumb]:duration-200
                                    [&::-moz-range-thumb]:hover:scale-110"
                            />
                        </div>

                        {/* 빠른 선택 버튼들 */}
                        <div className="flex flex-wrap gap-2">
                            {[0, 5, 10, 500].map((value) => (
                                <button
                                    key={value}
                                    onClick={() => handleDistanceChange(value)}
                                    className={`
                                        px-3 py-1.5 rounded-full text-sm font-medium
                                        transition-all duration-200
                                        ${distance === value
                                            ? 'bg-primary-500 text-white'
                                            : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                                        }
                                    `}
                                >
                                    {value === 0 ? '전체' : `${value}km`}
                                </button>
                            ))}
                        </div>

                    </div>
                </div>
            )}
        </div>
    );
};

export default FilterButton;
