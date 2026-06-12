interface DaySelectProps {
  onDaySelect: (days: string[]) => void;
  selectedDays: string[];
  disabled?: boolean;
}

const DaySelect = ({ onDaySelect, selectedDays, disabled = false }: DaySelectProps) => {
  const daysOfWeek = ['월', '화', '수', '목', '금', '토', '일'];

  const handleDayClick = (day: string) => {
    if (disabled) return;
    const updatedDays = selectedDays.includes(day)
      ? selectedDays.filter((d) => d !== day)
      : [...selectedDays, day];
    onDaySelect(updatedDays);
  };

  return (
    <div className="flex flex-wrap justify-center gap-1 sm:gap-1.5 md:gap-2">
      {daysOfWeek.map((day) => (
        <button
          key={day}
          onClick={() => handleDayClick(day)}
          disabled={disabled}
          className={`
            relative
            w-[32px] sm:w-[40px] md:w-[50px]
            h-[32px] sm:h-[40px] md:h-[50px]
            rounded-full
            font-semibold
            text-xs sm:text-sm md:text-base
            transition-all duration-300 ease-in-out
            ${!disabled && 'transform hover:scale-105'}
            flex items-center justify-center           
            ${
              selectedDays.includes(day)
                ? 'bg-gradient-to-r from-purple-400 to-pink-400 text-white shadow-lg'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
            }
            ${
              selectedDays.includes(day)
                ? 'before:content-[""] before:absolute before:inset-0 before:rounded-full before:bg-white before:opacity-0 before:animate-ping'
                : ''
            }
            focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500
            [&:disabled]:opacity-100
            [&:disabled]:cursor-default
          `}
        >
          {day}
        </button>
      ))}
    </div>
  );
};

export default DaySelect;
