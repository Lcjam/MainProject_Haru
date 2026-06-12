type CardItemProps = {
    type: string;
    onDelete: () => void;
};

const CardItem = ({ type, onDelete }: CardItemProps) => (
    <div className="space-y-4">
        <div className="group relative flex items-center gap-4 bg-white p-4 rounded-xl border border-gray-200 hover:border-primary-500 hover:shadow-lg transition-all duration-300">
            <div className="w-20 h-12 bg-gradient-to-r from-primary-400 to-primary-600 rounded-lg flex items-center justify-center text-white font-medium">
                {type}
            </div>
            <span className="text-lg font-semibold text-gray-800">{type}</span>
            <button 
                onClick={onDelete} 
                className="ml-auto p-2 text-gray-400 hover:text-red-500 rounded-full hover:bg-red-50 transition-colors duration-200"
                aria-label="카드 삭제"
            >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
            </button>
        </div>
    </div>
);

export default CardItem;