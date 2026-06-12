import { useNavigate } from "react-router-dom";



const TransactionList = () => {
  const navigate = useNavigate();



  const handleTransactionClick = () => {
    navigate("/transaction-detail");
  };

  return (
    <div className="flex flex-col">
      <div className="mt-3 flex justify-between items-center font-bold text-lg ml-4 mr-4 p-2">
        <span>거래 내역</span>
        <button
          onClick={handleTransactionClick}
          className="bg-secondary text-white rounded-full hover:bg-secondary-dark m-2 px-4 py-2"
        >
          View All
        </button>
      </div>
      <div className="border-t border-gray-300 my-2" />

    </div>



  );
};

export default TransactionList;
