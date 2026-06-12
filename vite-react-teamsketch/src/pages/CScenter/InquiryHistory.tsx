import { useNavigate } from "react-router-dom";

const InquiryHistory = () => {
    const navigate = useNavigate();


    const inquiries = [
        { id: 1, title: "거래 문의", date: "2025-03-14", status: "답변 대기" },
        { id: 2, title: "등록 문의", date: "2025-03-09", status: "답변 완료" },

    ];

    const handleViewDetails = (id: number) => {
        navigate(`/inquiry/${id}`);
    };

    return (
        <div className="flex flex-col">

            <div className="flex flex-col gap-4 mt-8 mr-4 ml-4">
                {inquiries.map((inquiry) => (
                    <div key={inquiry.id} className="bg-white p-4 rounded-lg flex flex-col gap-2 border border-primary-100">
                        <h3 className="font-semibold text-lg">{inquiry.title}</h3>
                        <div className="flex justify-between items-center">
                            <p className="text-sm text-gray-600">문의 날짜: {inquiry.date}</p>
                            <button
                                onClick={() => handleViewDetails(inquiry.id)}
                                className="bg-gradient-to-r from-primary-500 to-primary-600
                                    dark:from-primary-600 dark:to-primary-700
                                    text-white 
                                    px-5 py-2
                                    rounded-full
                                    text-sm 
                                    font-medium
                                    hover:from-primary-600 hover:to-primary-700
                                    dark:hover:from-primary-500 dark:hover:to-primary-600
                                    transition-all 
                                    duration-300
                                    shadow-sm 
                                    hover:shadow"
                            >
                                상세보기
                            </button>
                        </div>
                        <p className={`text-sm ${inquiry.status === "답변 대기" ? 'text-secondary-600' : 'text-accent-500'}`}>
                            {inquiry.status}
                        </p>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default InquiryHistory;
