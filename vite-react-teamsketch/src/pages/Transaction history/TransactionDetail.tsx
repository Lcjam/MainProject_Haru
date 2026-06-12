import { useEffect, useState } from "react";
import { transactionsListApi } from "../../services/api/authAPI";
import { toast } from "react-toastify";
import TransactionCard from "../../components/features/card/TransactionCard";

interface Transactions {
  id: number;
  productId: number;
  buyerEmail: string;
  sellerEmail: string;
  transactionStatus: '진행중' | '완료';
  paymentStatus: '미완료' | '완료';
  price: number;
  description: string;
  createdAt: number[];
}

const TransactionDetail = () => {
  const [transactions, setTransactions] = useState<Transactions[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTransaction, setSelectedTransaction] = useState<Transactions | null>(null);

  useEffect(() => {
    const fetchTransactions = async () => {
      try {
        const response = await transactionsListApi();
        console.log(response.data);
        if (response.status === "success") {

          setTransactions(response.data || []);
        } else {
          toast.error(response.message);
        }
      } catch (error) {
        toast.error("결제 내역을 불러오는데 실패했습니다.");
        console.log(error);
      }
    };

    fetchTransactions();
  }, []);

  const handleTransactionClick = (transaction: Transactions) => {
    setSelectedTransaction(transaction);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedTransaction(null);
  };

  return (
    <div className="p-4">
      {/* 거래 세부 사항 */}
      <div className="mt-6">
        {transactions.length > 0 ? (
          transactions.map((transaction) => (
            <TransactionCard
              key={transaction.id}
              transaction={transaction}
              onClick={() => handleTransactionClick(transaction)}
            />
          ))
        ) : (
          <div className="flex justify-center items-center h-full flex-col">
            <p className="text-gray-500">거래 내역이 없습니다.</p></div>
        )}
      </div>
      {/* 상세보기 모달 */}
      {isModalOpen && selectedTransaction && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex justify-center items-center z-50">
          <div className="bg-white p-6 rounded-lg w-[90%] max-w-3xl relative shadow-md">
            <button
              onClick={closeModal}
              className="absolute top-2 right-2 text-lg flex justify-center items-center"
            >
              x
            </button>
            <h2 className="text-lg font-bold mb-2">{selectedTransaction.productId}</h2>
            <hr className="my-3 border-gray-300" />
            <p>구매자 이메일: {selectedTransaction.buyerEmail}</p>
            <p>판매자 이메일: {selectedTransaction.sellerEmail}</p>
            <p>거래 상태: {selectedTransaction.transactionStatus}</p>
            <p>결제 상태: {selectedTransaction.paymentStatus}</p>
            <p>가격: {selectedTransaction.price} 원</p>
            <p>설명: {selectedTransaction.description}</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionDetail;
