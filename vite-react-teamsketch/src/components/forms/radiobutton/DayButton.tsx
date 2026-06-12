interface DayButtonProps {
  onDaySelect: (day: string) => void;
  selectedDays: string[]; // 기존 selectedDay(string) → selectedDays(string[])로 변경
}

const daysOfWeek = ['월', '화', '수', '목', '금', '토', '일'];

const DayButton: React.FC<DayButtonProps> = ({ onDaySelect, selectedDays }) => {
  return (
    <div className="flex flex-wrap justify-center gap-2">
      {daysOfWeek.map((day) => (
        <button
          key={day}
          onClick={() => onDaySelect(day)}
          className={`px-4 py-2 rounded-full text-white font-bold transition-all sm:text-sm md:text-mb
            ${selectedDays.includes(day) ? 'bg-pink-300' : 'bg-gray-400'}
            hover:bg-blue-500`}
        >
          {day}
        </button>
      ))}
    </div>
  );
};

export default DayButton;
