import { useNavigate } from 'react-router-dom';
import TransactionDetail from '../pages/Transaction history/TransactionDetail';



const TestPages = () => {
  const navigate = useNavigate();

  return (
    <div>
      <h1>TestPages</h1>
      <button onClick={() => navigate('/test/component')}>테스트 컴포넌트</button>
      <button onClick={() => navigate('/test/grid')}>테스트 그리드</button>
      <button onClick={() => navigate('/test/func')}>테스트 함수</button>
      <button onClick={() => navigate('/test/api')}>테스트 API</button>
      <button onClick={() => navigate('/test/chat')}>테스트 채팅</button>
      <div>
        <TransactionDetail />


      </div>
    </div>
  );
};

export default TestPages;
