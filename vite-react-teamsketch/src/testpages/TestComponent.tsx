import Button from '../components/common/Button';
import Card from '../components/features/card/Card';
import Select from '../components/common/Select';
import Grid from '../components/common/Grid';
import GridItem from '../components/common/GridItem';
import ImageUpload from '../components/features/upload/ImageUpload';
import { fileUpload } from '../services/api/cloudAPI';
import { increment, decrement } from '../store/slices/testSlice';
import { useAppSelector, useAppDispatch } from '../store/hooks';
import SearchInput from '../components/forms/input/SearchInput';
import { useState } from 'react';
import InfoBox from '../components/forms/box/InfoBox';
import CustomInput from '../components/forms/input/CustomInput';
import DaySection from '../components/forms/radiobutton/DaySelect';
import ProfileSelector from '../components/features/upload/ProfileSelector';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import axios from 'axios';

interface OCRResult {
  status: string;
  data: {
    message: string;
    response: any; // OCR 응답 데이터의 실제 타입에 맞게 수정할 수 있습니다
  };
  code: string;
}

const TestComponent = () => {
  const dispatch = useAppDispatch();
  const value = useAppSelector((state) => state.test.value);
  const count = useAppSelector((state) => state.test.count);

  const [selectedDay, setSelectedDay] = useState<string[]>([]);
  const options = [
    { value: '1', label: '오름차순' },
    { value: '2', label: '내림차순' }
  ];

  const [search, setSearch] = useState('');
  const [ocrResult, setOcrResult] = useState<OCRResult | null>(null);
  const [recipientEmail, setRecipientEmail] = useState('');
  const [notificationMessage, setNotificationMessage] = useState('테스트 알림 메시지입니다!');

  const handleOCRUpload = async (formData: FormData) => {
    try {
      const result = await fileUpload.CloudOCR(formData);
      console.log('OCR 결과:', result);
      setOcrResult(result);
    } catch (error) {
      console.error('OCR 처리 중 오류:', error);
      setOcrResult(null);
    }
  };
  const notificationService = async () => {
    try {
      const token = localStorage.getItem('token');
      const myEmail = localStorage.getItem('userEmail') || 'test@test.com';

      // 직접 테스트 토스트 메시지 제거 (중복 알림 방지)
      // toast.info('직접 테스트 토스트 메시지입니다!', { ... });

      // 수신자 이메일 확인
      const targetEmail = recipientEmail.trim() || myEmail;
      console.log('알림 테스트: 보내는 사람 -', myEmail, '받는 사람 -', targetEmail);

      // GET 요청으로 변경하고 파라미터를 올바르게 전달
      await axios.get('http://sunbee.world:8080/api/core/test/v2/notify', {
        params: {
          email: targetEmail, // 입력한 이메일 또는 기본값
          message: notificationMessage
        },
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      console.log('알림 전송 요청 완료');

      // 메시지 전송 성공 알림은 유지
      toast.success(`${targetEmail}님에게 알림을 전송했습니다!`, {
        position: "top-right", // 위치 변경하여 구분
        autoClose: 2000
      });
    } catch (error) {
      console.error('알림 전송 요청 실패:', error);
      toast.error('알림 전송 실패: ' + (error as any).message);
    }
  }

  return (
    <div className="p-8 space-y-8 flex flex-col items-center justify-center">
      <h1 className="text-2xl font-bold mb-6">컴포넌트 테스트 페이지</h1>

      <div className="w-full max-w-md p-4 bg-white rounded-lg shadow mb-4">
        <h2 className="text-lg font-semibold mb-4">알림 테스트</h2>
        <div className="space-y-3">
          <div>
            <label className="block text-sm font-medium mb-1">수신자 이메일</label>
            <input
              type="email"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              placeholder="test@test.test"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <p className="text-xs text-gray-500 mt-1">비워두면 본인에게 알림이 발송됩니다</p>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">알림 메시지</label>
            <input
              type="text"
              value={notificationMessage}
              onChange={(e) => setNotificationMessage(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <Button onClick={notificationService} variant="primary" className="w-full">알림 전송</Button>
        </div>
      </div>

      <section className="space-y-6 w-full max-w-3xl p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md">
        <div>
          <h2 className="text-xl font-semibold mb-4">버튼 테스트</h2>
          <div className="flex gap-4 flex-wrap">
            <Button variant="primary">Primary 버튼</Button>
            <Button variant="secondary">Secondary 버튼</Button>
            <Button variant="outline">Outline 버튼</Button>
            <Button variant="primary" disabled>
              비활성화 버튼
            </Button>
          </div>
        </div>

        <div>
          <h2 className="text-xl font-semibold mb-4">카드 테스트</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <Card
              title="테스트 카드 1"
              description="카드 컴포넌트 테스트입니다."
              image="https://picsum.photos/200/300"
            />
            <Card
              title="테스트 카드 2"
              description="이미지가 있는 카드입니다."
              image="https://picsum.photos/200/300"
            />
          </div>
        </div>
        <div>
          <h2 className="text-xl font-semibold mb-4">선택 테스트</h2>
          <Select options={options} />
        </div>
        <div>
          <h2 className="text-xl font-semibold mb-4">그리드 테스트</h2>
          <Grid>
            <GridItem>
              <h3>그리드 아이템 1</h3>
            </GridItem>
          </Grid>
        </div>
        <div>
          <h2 className="text-xl font-semibold mb-4">OCR 이미지 업로드 테스트</h2>
          <ImageUpload onUpload={handleOCRUpload} className="max-w-md mx-auto" type="ocr" />

          {/* OCR 결과 표시 */}
          {ocrResult && (
            <div className="mt-4 p-4 bg-gray-50 rounded-lg">
              <h3 className="text-lg font-semibold mb-2">OCR 결과:</h3>
              <div className="whitespace-pre-wrap break-words">
                <p>상태: {ocrResult.status}</p>
                <p>메시지: {ocrResult.data.message}</p>
                <p>응답 데이터:</p>
                <pre className="bg-white p-2 rounded mt-2 overflow-x-auto">
                  {JSON.stringify(ocrResult.data.response, null, 2)}
                </pre>
              </div>
            </div>
          )}
        </div>
        <div>
          <h2 className="text-xl font-semibold mb-4">프로필 이미지 업로드 테스트</h2>
          <ImageUpload
            onUpload={fileUpload.assistProfile}
            className="max-w-md mx-auto"
            type="image"
          />
        </div>

        <div>
          <h2 className="text-xl font-semibold mb-4">카운터 테스트</h2>
          <p>현재 Value 값: {value}</p>
          <p>현재 Count 값: {count}</p>
          <Button onClick={() => dispatch(increment(2))}>증가</Button>
          <Button onClick={() => dispatch(decrement(2))}>감소</Button>
        </div>
        <div>
          <h2 className="text-xl font-semibold mb-4"></h2>
          <SearchInput value={search} onChange={(e) => setSearch(e.target.value)} name="search" />
        </div>
        <div>
          <h2 className="text-xl font-semibold mb-4">InfoBox 테스트</h2>
          <InfoBox label="테스트 라벨" content="테스트 컨텐츠" description="테스트 설명" />
        </div>
        <div>
          <CustomInput
            label="금액"
            prefix="₩"
            suffix="원"
            helperText="1,000원 단위로 입력해주세요"
          />
        </div>
        <div>
          <CustomInput
            label="웹사이트"
            prefix="https://"
            suffix=".com"
            helperText="도메인 이름만 입력해주세요"
          />
        </div>
        <div>
          <CustomInput
            label="사용자 이름"
            prefix="이름->"
            suffix="입니다."
            helperText="영문, 숫자, 밑줄(_)만 사용 가능합니다"
          />
        </div>
        <div>
          <button className="bg-border-light">스타일 테스트</button>
        </div>
        <div className="bg-purple-level_1">
          <label className="text-purple_color-level_10">ddddd</label>
        </div>
        <div>
          <DaySection onDaySelect={setSelectedDay} selectedDays={selectedDay} />
          <br />
          {selectedDay.join(', ')}
        </div>
        <div>
          <ProfileSelector isEditable={false} size="lg" />
        </div>
        <div>
          <ProfileSelector
            imageUrl="https://picsum.photos/600/400?random=37"
            isEditable={true}
            size="md"
          />
        </div>
        <div>
          <ImageUpload
            type="profile"
            imageUrl="https://picsum.photos/600/400?random=35"
            className="max-w-md mx-auto"
            isEdit={false}
          />
        </div>

        <div>
          <h2 className="text-xl font-semibold mb-4">상품 이미지 업로드</h2>
          <ImageUpload onUpload={fileUpload.CloudOCR} type="prod" />
        </div>
        <br />
        <br />
      </section>
    </div>
  );
};

export default TestComponent;
