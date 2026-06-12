import { useNavigate } from "react-router-dom";
import CardItem from "./CardItem";
import { useState } from "react";
import DeleteModal from "./DeleteModal";

const RegisteredCardList = () => {
  const navigate = useNavigate(); // useNavigate 훅 사용
  const [showModal, setShowModal] = useState(false);
  const [selectedCardId, setSelectedCardId] = useState<number | null>(null);

  const handleAddCard = () => {
    navigate("/ocr-upload"); // useNavigate로 리다이렉션
  };

  const handleDeleteCard = (id: number) => {
    setSelectedCardId(id);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedCardId(null);
  };

  const handleConfirmDelete = (id: number) => {
    setCardList((prevList) =>
      prevList.filter((card) => card.id !== id));
    setShowModal(false);
    setSelectedCardId(null);
  };



  const [cardList, setCardList] = useState([
    { id: 1, type: "체크카드" },
    { id: 2, type: "신용카드" },
    { id: 3, type: "신용카드" },
  ]);

  return (
    <div className="mt-5 space-y-4 p-4">
      <h2 className="text-xl font-bold mb-2 ml-4">등록된 카드</h2>

      {cardList.map((card) => (
        <CardItem
          key={card.id}
          type={card.type}
          onDelete={() => handleDeleteCard(card.id)}
        />
      ))}

      <div
        className="flex items-center justify-center gap-4 bg-white p-3 rounded-lg border-2 border-primary-light hover:bg-secondary-light cursor-pointer mb-24"
        onClick={handleAddCard}
      >
        <span className="text-lg font-semibold ">➕ 카드 등록하기</span>
      </div>

      {/* 모달 */}
      {showModal && selectedCardId !== null && (
        <DeleteModal
          cardId={selectedCardId}
          onClose={handleCloseModal}
          onConfirmDelete={() => handleConfirmDelete(selectedCardId)}
        />
      )}
    </div>
  );
};

export default RegisteredCardList;
