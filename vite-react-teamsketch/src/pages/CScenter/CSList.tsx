import { useNavigate } from "react-router-dom";


const CSList = () => {
    const navigate = useNavigate();


    const handleChatClick = () => {
        navigate('/servicechat');
    }

    const hadleInquiryHistory = () => {
        navigate('/inquiry-history');
    }

    return (
        <div className="flex flex-col">

            <div className="flex flex-col gap-5 mt-8 p-4">
                <button onClick={handleChatClick} className="w-full bg-gradient-to-r from-violet-500 to-purple-500 text-white py-4 text-lg font-semibold rounded-lg shadow-md active:from-violet-700 active:to-purple-700 hover:from-violet-700 hover:to-purple-700 transition-all">챗봇 상담</button>

                <button onClick={hadleInquiryHistory} className="w-full bg-gradient-to-r from-violet-500 to-purple-500 text-white py-4 text-lg font-semibold rounded-lg shadow-md active:from-violet-700 active:to-purple-700 hover:from-violet-700 hover:to-purple-700 transition-all">문의 내역</button>

            </div>
        </div>

    );
}

export default CSList;