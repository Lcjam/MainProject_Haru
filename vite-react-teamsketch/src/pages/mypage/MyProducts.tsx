import { useNavigate } from "react-router-dom";




const MyProducts = () => {
    const navigate = useNavigate();

    const handleRegistersClick = () => {
        navigate('/registers-list');
    };

    const handleRequestsClick = () => {
        navigate('/requests-list');
    };
    return (
        <div className="flex flex-col">
            {/* 내가 등록한 상품 목록  */}
            <section className="mt-3 flex justify-between items-center font-bold text-lg ml-4 mr-4 p-2">
                내가 등록한 상품 목록
                <button
                    onClick={handleRegistersClick}
                    className="bg-secondary text-white rounded-full hover:bg-secondary-dark m-2 px-4 py-2"
                >
                    View All
                </button>
            </section>
            <hr className="my-3 border-gray-300" />

            {/* 내가 요청한 상품 목록 */}
            <section className="mt-3 flex justify-between items-center font-bold text-lg ml-4 mr-4 p-2">
                내가 요청한 상품 목록
                <button
                    onClick={handleRequestsClick}
                    className="bg-secondary text-white rounded-full hover:bg-secondary-dark m-2 px-4 py-2"
                >
                    View All
                </button>
            </section>
            <hr className="my-3 border-gray-300" />

        </div>
    );
};

export default MyProducts;