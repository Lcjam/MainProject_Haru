
interface FloatingButtonProps {
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  color?: 'primary' | 'secondary';
}

const FloatingButton: React.FC<FloatingButtonProps> = ({
  onClick,
  icon,
  label,
  position = 'bottom-right',
  color = 'primary'
}) => {
  const positionClasses = {
    //버튼 위치
    'bottom-right': 'bottom-24 right-5',
    'bottom-left': 'bottom-15 left-6',
    'top-right': 'top-6 right-6',
    'top-left': 'top-6 left-6'
  };

  const colorClasses = {
    primary: 'bg-#FF3399 hover:bg-#660033 text-white',
    secondary: 'bg-blue-500 hover:bg-blue-900 text-white',
  };

  return (
    <button
      onClick={onClick}
      className={`fixed ${positionClasses[position]} ${colorClasses[color]} 
        w-14 h-14 p-5 rounded-full shadow-lg 
        flex items-center justify-center 
        transition-colors duration-300 ease-in-out 
        focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-light 
        dark:focus:bg-background-white z-50 
        dark:hover:bg-[#ffffff]/10`}
      aria-label={label}
    >
      <span className="flex items-center justify-center w-full h-full">{icon}</span>
      <span className="sr-only">{label}</span>
    </button>
  );
};

export default FloatingButton;
