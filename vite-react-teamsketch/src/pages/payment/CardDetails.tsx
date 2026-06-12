import { useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAppDispatch } from '../../store/hooks';
import { resetCardInfo } from '../../store/slices/cardSlice';
import creditCardImg from './cardchip.png';
import { toast } from "react-toastify";

interface OCRResult {
  status: string;
  data: {
    message: string;
    response: string;
  };
  code: string;
}

interface CardDetailsProps {
  ocrResult?: OCRResult | null;
}

const CardDetails: React.FC<CardDetailsProps> = ({ ocrResult }) => {
  const { cardId } = useParams();
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(resetCardInfo());
  }, [dispatch]);

  const [cardInfo, setCardInfo] = useState({
    name: '',
    number: '',
    expiryDate: '',
    owner: ''
  });

  useEffect(() => {
    if (ocrResult && ocrResult.data.response) {
      const responseParts = ocrResult.data.response.split(" ");
      const number = responseParts.slice(0, 4).join("");
      const name = responseParts[4] || '';
      const expiryDate = responseParts[5] || '';
      const owner = responseParts.slice(6).join(" ") || '';

      setCardInfo({
        number: number || '',
        name: name || '',
        expiryDate: expiryDate || '',
        owner: owner || ''
      });
    }
  }, [ocrResult]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setCardInfo((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = () => {
    console.log('저장된 카드 정보:', cardInfo);
    toast.success('카드 정보가 저장되었습니다!');
  };

  return (
    <div className="max-w-md mx-auto bg-white rounded-2xl shadow-lg p-6 space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">카드 상세 정보</h2>
      <p className="text-sm text-gray-500">카드 ID: {cardId}</p>

      {/* 카드 미리보기 */}
      <div className="relative w-full h-56 bg-gradient-to-br from-primary-500 to-primary-600 rounded-2xl p-6 text-white shadow-xl">
        <div className="text-lg font-semibold mb-4">{cardInfo.name || '카드 이름'}</div>
        <img src={creditCardImg} alt="Card Chip" className="w-12 h-10" />
        <div className="text-2xl font-bold tracking-widest mb-6">
          {cardInfo.number ? cardInfo.number.replace(/(\d{4})/g, '$1 ') : '**** **** **** ****'}
        </div>
        <div className="flex justify-between items-end">
          <div>
            <div className="text-xs text-white/70">소유자</div>
            <div className="font-medium">{cardInfo.owner || '소유자 이름'}</div>
          </div>
          <div className="text-right">
            <div className="text-xs text-white/70">유효기간</div>
            <div className="font-medium">{cardInfo.expiryDate || 'MM/YY'}</div>
          </div>
        </div>
      </div>

      {/* 입력 폼 */}
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">카드 이름</label>
          <input
            type="text"
            name="name"
            placeholder="카드 이름"
            value={cardInfo.name}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">카드 번호</label>
          <input
            type="text"
            name="number"
            placeholder="카드 번호 (16자리)"
            value={cardInfo.number}
            maxLength={19}
            onChange={(e) => {
              const value = e.target.value.replace(/\D/g, '').slice(0, 16);
              setCardInfo((prev) => ({ ...prev, number: value.replace(/(\d{4})/g, '$1 ').trim() }));
            }}
            className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">만료일</label>
            <input
              type="text"
              name="expiryDate"
              placeholder="MM/YY"
              value={cardInfo.expiryDate}
              onChange={handleChange}
              className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">소유자</label>
            <input
              type="text"
              name="owner"
              placeholder="카드 소유자 이름"
              value={cardInfo.owner}
              onChange={handleChange}
              className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
            />
          </div>
        </div>

        <button
          onClick={handleSave}
          className="w-full py-3 px-4 bg-primary-500 text-white rounded-xl hover:bg-primary-600 focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transition-all duration-200 font-medium"
        >
          저장하기
        </button>
      </div>
    </div>
  );
};

export default CardDetails;
