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

interface TransactionCardProps {
  transaction: Transactions;
  onClick: () => void;
}

const TransactionCard = ({ transaction, onClick }: TransactionCardProps) => {
  return (
    <div className="flex justify-center items-center p-4 mr-3">
      <div className="w-full max-w-[600px] h-[150px] bg-white rounded-2xl border-2 flex items-center p-5 gap-3 ml-4">
        <div className="flex flex-col flex-1 w-full">
          <span className="text-gray-400 text-xs">{transaction.productId}</span>
          <h3 className="text-lg font-semibold">{transaction.buyerEmail}</h3>
          <p className="text-gray-500 text-sm">{transaction.sellerEmail}</p>

          <div className="flex flex-col items-start">
            <span>{transaction.price} point</span>
            <span className={`text-sm ${transaction.transactionStatus === '완료' ? 'text-green-500' : 'text-yellow-500'}`}>
              {transaction.transactionStatus}
            </span>
            <span className={`text-sm ${transaction.paymentStatus === '완료' ? 'text-green-500' : 'text-red-500'}`}>
              {transaction.paymentStatus}
            </span>
          </div>
        </div>

        <button
          className="bg-primary-500 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-600"
          onClick={onClick}
        >
          상세보기
        </button>
      </div>
    </div>
  );
};

export default TransactionCard;
