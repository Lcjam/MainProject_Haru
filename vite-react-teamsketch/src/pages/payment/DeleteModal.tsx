interface DeleteModalProp {
  cardId: number;
  onClose: () => void;
  onConfirmDelete: (cardId: number) => void;
}

const DeleteModal = ({ cardId, onClose, onConfirmDelete }: DeleteModalProp) => {
  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex justify-center items-center z-50">
      <div className="w-[320px] bg-white rounded-2xl shadow-xl overflow-hidden">
        <div className="p-6">
          <div className="text-center space-y-3">
            <h3 className="text-xl font-bold text-gray-900">
              카드 삭제
            </h3>
            <p className="text-gray-600 text-sm">
              선택한 카드를 정말 삭제하시겠습니까?
            </p>
          </div>
          
          <div className="mt-6 flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 py-2.5 px-4 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors duration-200 font-medium text-sm"
            >
              취소
            </button>
            <button
              onClick={() => onConfirmDelete(cardId)}
              className="flex-1 py-2.5 px-4 bg-red-500 text-white rounded-xl hover:bg-red-600 transition-colors duration-200 font-medium text-sm"
            >
              삭제
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DeleteModal;
