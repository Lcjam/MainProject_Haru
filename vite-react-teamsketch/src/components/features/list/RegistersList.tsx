import { useEffect, useState } from 'react';
import { myProdListApi } from '../../../services/api/authAPI';
import ProductCard from '../card/ProductCard';

interface Product {
    id: number;
    productCode: string;
    title: string;
    description: string;
    price: number;
    email: string;
    categoryId: number;
    hobbyId: number;
    transactionType: string;
    registrationType: string;
    maxParticipants: number;
    currentParticipants: number;
    days: string[];
    startDate: string;
    endDate: string;
    latitude: number | null;
    longitude: number | null;
    meetingPlace: string | null;
    address: string | null;
    createdAt: string;
    imagePaths: string[];
    thumbnailPath: string;
    nickname: string;
    bio: string;
    dopamine: number;
    visible: boolean;
}

const RegistersList = () => {
    const [buyProducts, setBuyProducts] = useState<Product[]>([]);
    const [sellProducts, setSellProducts] = useState<Product[]>([]);
    const [selectedProduct, setSelectedProduct] = useState<Product | null>(null); // 상세보기 상태 추가
    const [isModalOpen, setIsModalOpen] = useState(false);

    useEffect(() => {
        const fetchBuyProducts = async () => {
            try {
                const response = await myProdListApi('registers/buy');
                setBuyProducts(response.data); // 구매 상품 목록 상태 업데이트
            } catch (error) {
                console.error('구매 목록을 불러오는 데 실패했습니다:', error);
            }
        };

        const fetchSellProducts = async () => {
            try {
                const response = await myProdListApi('registers/sell');
                setSellProducts(response.data); // 판매 상품 목록 상태 업데이트
            } catch (error) {
                console.error('판매 목록을 불러오는 데 실패했습니다:', error);
            }
        };

        fetchBuyProducts();  // 구매 목록 불러오기
        fetchSellProducts(); // 판매 목록 불러오기
    }, []);

    const handleProductClick = (product: Product) => {
        setIsModalOpen(true);
        setSelectedProduct(product);

    };

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedProduct(null);
    };



    return (
        <div className="flex flex-col">
            <h2 className="text-lg font-semibold mb-2 ml-4 mt-4">
                내가 등록한 상품 목록 (구매)
            </h2>

            <div className="border-t border-gray-300 my-2" />

            <div>
                {buyProducts.map((product) => (
                    <ProductCard key={product.id} product={product} onClick={() => handleProductClick(product)} />
                ))}
            </div>

            <div className="border-t border-gray-300 my-2" />

            <h2 className="text-lg font-semibold mb-2 ml-4 mt-2">
                내가 등록한 상품 목록 (판매)
            </h2>

            <div className="border-t border-gray-300 my-2" />

            <div>
                {sellProducts.map((product) => (
                    <ProductCard key={product.id} product={product} onClick={() => handleProductClick(product)} />
                ))}
            </div>

            {/* 상세보기 모달 */}
            {isModalOpen && selectedProduct && (
                <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex justify-center items-center z-50">
                    <div className="bg-white p-6 rounded-lg w-[90%] max-w-3xl relative shadow-md">
                        <button onClick={closeModal} className="absolute top-2 right-2 text-lg  flex justify-center items-center">x</button>
                        {selectedProduct.thumbnailPath && (
                            <img
                                src={selectedProduct.thumbnailPath}
                                alt="대표 이미지"
                                className="w-full h-64 object-cover rounded-md mb-4"
                            />
                        )}
                        <h2 className="text-lg font-bold mb-2">{selectedProduct.title}</h2>
                        <hr className="my-3 border-gray-300" />
                        <p>거래 유형: {selectedProduct.transactionType}</p>
                        <p>등록 유형: {selectedProduct.registrationType}</p>
                        <p>가격: {selectedProduct.price}원</p>
                        <p>참여자: {selectedProduct.currentParticipants}/{selectedProduct.maxParticipants}</p>
                        <p>시작일: {selectedProduct.startDate}</p>
                        <p>종료일: {selectedProduct.endDate}</p>
                        <p>주소: {selectedProduct.address || '주소 없음'}</p>
                        <p>모임 장소: {selectedProduct.meetingPlace || '없음'}</p>
                        <p>설명: {selectedProduct.description}</p>

                    </div>
                </div>
            )}
        </div>
    );
};

export default RegistersList;
