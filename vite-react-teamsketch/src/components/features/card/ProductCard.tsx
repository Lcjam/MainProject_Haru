
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

interface ProductCardProps {
    product: Product;
    onClick: () => void;
}

const ProductCard = ({ product, onClick }: ProductCardProps) => {
    console.log('ProductCard 렌더링 데이터:', product);



    return (

        <div className="flex justify-center items-center p-4 mr-3">
            <div className="w-full max-w-[600px] h-[150px] bg-white rounded-2xl border-2 flex items-center p-5 gap-3 ml-4">
                <div className="w-[100px] h-[100px] bg-gray-200 rounded-lg">
                    <img src={product.imagePaths[0]} className="item-image" />
                </div>

                <div className="flex flex-col flex-1 w-full">
                    <span className="text-gray-400 text-xs">{product.nickname}</span>
                    <h3 className="text-lg font-semibold">{product.title}</h3>
                    <p className="text-gray-500 text-sm">{product.transactionType}, {product.registrationType}</p>

                    <div className="flex flex-col items-start">
                        <span>{product.dopamine} point</span>
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

export default ProductCard;
