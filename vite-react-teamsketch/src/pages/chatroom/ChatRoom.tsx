import MessageInput from '../../components/features/chat/MessageInput';
import { useState, useEffect, useMemo } from 'react';
import { useRef } from 'react';
import { useChat } from '../../services/real-time/useChat';
import { Navigate, useParams, useNavigate } from 'react-router-dom';
import { MessageType, IChatMessage } from '../../services/real-time/types';
import { ChatRoom as ChatRoomType, getChatRoomDetail } from '../../services/api/userChatAPI';
import { axiosInstance } from '../../services/api/axiosInstance';
import { apiConfig } from '../../services/api/apiConfig';
import { toast } from 'react-toastify';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { useProductByProductId, getApprovalStatus, getProductByProductId } from '../../services/api/productAPI';
import { IconMap } from '../../components/common/Icons';
import ProductImage from '../../components/features/image/ProductImage';
import ProfileImage from '../../components/features/image/ProfileImage';
import { markChatMessagesAsRead } from '../../store/slices/notiSlice';


const ChatRoom: React.FC = () => {
  const { chatroomId } = useParams();
  const userStr = localStorage.getItem('user');
  const user = userStr ? JSON.parse(userStr) : null;
  const userEmail = user?.email || '';

  const chatContainerRef = useRef<HTMLDivElement>(null);
  const messageInputRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [isDisabled, setIsDisabled] = useState(false);
  const [chatInfo, setChatInfo] = useState<ChatRoomType | null>(null);
  const [previousMessages, setPreviousMessages] = useState<IChatMessage[]>([]);
  const [allMessages, setAllMessages] = useState<IChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const token = localStorage.getItem('token');
  const [isConnecting, setIsConnecting] = useState(false);
  const [connectionAttempts, setConnectionAttempts] = useState(0);
  const maxRetries = 3;
  const { constantCategories, constantHobbies } = useAppSelector((state) => state.category);
  const navigate = useNavigate();
  const [isApproved, setIsApproved] = useState<boolean>(false);
  const [transactionType, setTransactionType] = useState<string>('');
  const [isKeyboardVisible, setIsKeyboardVisible] = useState(false);
  const dispatch = useAppDispatch();
  
  // useProductById 훅 사용
  const { data: productData } = useProductByProductId(chatInfo?.productId || 0);

  // useChat 훅 사용
  const { messages, sendMessage, sendImage, isConnected, connect } = useChat({
    chatroomId: Number(chatroomId),
    userEmail,
    productId: Number(chatInfo?.productId) || 0,
    useGlobalConnection: false,
    token: token || ''
  });  

  // productData가 변경될 때마다 chatInfo 업데이트
  useEffect(() => {
    if (productData && chatInfo) {
      setChatInfo(prev => ({
        ...prev!,
        productInfo: {
          categoryId: productData.categoryId,
          hobbyId: productData.hobbyId
        }
      }));
    }
  }, [productData]);

  // 카테고리와 취미 이름 찾기
  const mainCategory = useMemo(() => {
    const categoryId = chatInfo?.productInfo?.categoryId;
    if (!categoryId) return '';
    return constantCategories.find(
      (category) => category.categoryId === categoryId
    )?.categoryName || '';
  }, [constantCategories, chatInfo?.productInfo?.categoryId]);

  const subCategory = useMemo(() => {
    const hobbyId = chatInfo?.productInfo?.hobbyId;
    if (!hobbyId) return '';
    return constantHobbies.find(
      (hobby) => hobby.hobbyId === hobbyId
    )?.hobbyName || '';
  }, [constantHobbies, chatInfo?.productInfo?.hobbyId]);


  // 컴포넌트 마운트 시 연결 시도 (한 번만)
  useEffect(() => {
    if (!token) {
      setError('인증 토큰이 없습니다. 다시 로그인해주세요.');
      return;
    }

    if (!isConnected && !isConnecting && chatroomId && connectionAttempts < maxRetries) {
      setIsConnecting(true);
      try {
        connect();
      } catch (error) {
        console.error('채팅 서버 연결 실패:', error);
        setConnectionAttempts(prev => prev + 1);
      } finally {
        setIsConnecting(false);
      }
    }
  }, []); // 빈 의존성 배열로 마운트 시 한 번만 실행

  // 연결 상태 변경 감지
  useEffect(() => {
    if (isConnected) {
      setConnectionAttempts(0);
    }
  }, [isConnected]);

  // 채팅방 정보 로드
  useEffect(() => {
    const loadChatRoomInfo = async () => {
      if (!chatroomId || !userEmail) {
        setError('채팅방 정보가 올바르지 않습니다.');
        return;
      }

      try {
        const response = await getChatRoomDetail(Number(chatroomId));
        const chatRoomData = response.data;

        if (chatRoomData.success) {
          // API 응답 데이터를 ChatRoom 타입에 맞게 변환
          setChatInfo({
            chatroomId: Number(chatroomId),
            chatname: chatRoomData.chatname,
            productId: chatRoomData.productId,
            productName: chatRoomData.productName,
            productImageUrl: chatRoomData.productImageUrl,
            registrantEmail: chatRoomData.registrantEmail,
            sellerEmail: chatRoomData.sellerEmail,
            requestEmail: chatRoomData.requestEmail,
            otherUserEmail: chatRoomData.otherUserEmail,
            otherUserName: chatRoomData.otherUserName,
            lastMessage: chatRoomData.lastMessage || '',
            lastMessageTime: Array.isArray(chatRoomData.lastMessageTime) 
              ? new Date(
                  chatRoomData.lastMessageTime[0], 
                  chatRoomData.lastMessageTime[1] - 1, 
                  chatRoomData.lastMessageTime[2], 
                  chatRoomData.lastMessageTime[3], 
                  chatRoomData.lastMessageTime[4], 
                  chatRoomData.lastMessageTime[5]
                ).toISOString()
              : new Date().toISOString(),
            status: 'ACTIVE',
            createdAt: Array.isArray(chatRoomData.createdAt)
              ? new Date(
                  chatRoomData.createdAt[0],
                  chatRoomData.createdAt[1] - 1,
                  chatRoomData.createdAt[2],
                  chatRoomData.createdAt[3],
                  chatRoomData.createdAt[4],
                  chatRoomData.createdAt[5]
                ).toISOString()
              : new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            unreadCount: 0
          });

          // 상품 등록자가 아닌 경우 함께하기 버튼 숨기기
          if (userEmail !== chatRoomData.registrantEmail) {
            setIsDisabled(true);
          }
        } else {
          setError('채팅방 정보를 불러오는데 실패했습니다.');
        }

      } catch (error) {
        console.error('채팅방 정보 로딩 오류:', error);
        setError('채팅방 정보를 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    loadChatRoomInfo();
  }, [chatroomId, userEmail]);

  // 이전 메시지 로드
  useEffect(() => {
    const loadPreviousMessages = async () => {
      if (!chatroomId || !token) return;
      
      try {
        setIsLoadingMessages(true);
        const response = await axiosInstance.get(
          `${apiConfig.endpoints.core.base}/chat/rooms/${chatroomId}/messages`
        );
        
        if (response.data?.status === 'success' && response.data?.data?.messages) {
          const messages = response.data.data.messages.map((msg: any) => ({
            messageId: msg.messageId,
            chatroomId: msg.chatroomId,
            content: msg.content,
            senderEmail: msg.senderEmail,
            messageType: msg.messageType || MessageType.TEXT,
            sentAt: msg.sentAt || msg.createdAt,
            isRead: msg.isRead || false
          }));
          setPreviousMessages(messages);
          console.log('이전 메시지 로드 완료:', messages);
        }
      } catch (error) {
        console.error('이전 메시지 로드 실패:', error);
        toast.error('이전 메시지를 불러오는데 실패했습니다.');
      } finally {
        setIsLoadingMessages(false);
      }
    };

    loadPreviousMessages();
  }, [chatroomId, token]);

  // 메시지 읽음 처리
  useEffect(() => {
    const markMessagesAsRead = async () => {
      if (!chatroomId || !token) return;

      try {
        await axiosInstance.put(
          `${apiConfig.endpoints.core.base}/chat/rooms/${chatroomId}/messages/read`,
          null,
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        );
        console.log('메시지 읽음 처리 완료');
      } catch (error) {
        console.error('메시지 읽음 처리 실패:', error);
      }
    };

    if (isConnected) {
      markMessagesAsRead();
    }
  }, [chatroomId, token, isConnected]);

  // 키보드 표시 상태 감지
  useEffect(() => {
    const handleResize = () => {
      const isKeyboard = window.innerHeight < window.outerHeight;
      setIsKeyboardVisible(isKeyboard);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);
  
  // ChatRoom.tsx에 useEffect 추가
useEffect(() => {
  // 컴포넌트 언마운트 시 실행될 cleanup 함수
  return () => {
    // 채팅방을 나갈 때 메시지 읽음 처리
    if (chatroomId) {
      // Redux store에서 읽음 처리
      dispatch(markChatMessagesAsRead(Number(chatroomId)));
      
      // API 호출로도 읽음 처리
      axiosInstance.put(
        `${apiConfig.endpoints.core.base}/chat/rooms/${chatroomId}/messages/read`,
        null,
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      ).catch(error => {
        console.error('메시지 읽음 처리 실패:', error);
      });
    }
  };
}, [chatroomId, token, dispatch]);

  // 메시지 영역 클릭 시 키보드 내리기
  const handleChatAreaClick = () => {
    if (isKeyboardVisible) {
      const activeElement = document.activeElement as HTMLElement;
      if (activeElement) {
        activeElement.blur();
      }
    }
  };

  // 스크롤을 항상 최신 메시지로 이동 (키보드 상태 고려)
  useEffect(() => {
    if (chatContainerRef.current) {
      const scrollToBottom = () => {
        // setTimeout을 사용하여 DOM 업데이트 후 스크롤 실행
        setTimeout(() => {
          chatContainerRef.current?.scrollTo({
            top: chatContainerRef.current.scrollHeight,
            behavior: 'smooth'
          });
        }, 100);
      };
      
      // 키보드가 나타나거나 사라질 때 스크롤 조정
      scrollToBottom();
    }
  }, [messages, previousMessages, isKeyboardVisible]);

  // 메시지 시간 포맷팅 함수 수정
  const formatMessageTime = (dateStr: string | number[]) => {
    try {
      let date: Date;
      
      if (Array.isArray(dateStr)) {
        // 배열 형식의 날짜 처리 [년, 월, 일, 시, 분, 초]
        const [year, month, day, hour, minute, second] = dateStr;
        // 월은 0부터 시작하므로 1을 빼줌
        date = new Date(year, month - 1, day, hour, minute, second || 0);
      } else {
        // 문자열 형식의 날짜 처리
        date = new Date(dateStr);
      }

      // 날짜가 유효한지 확인
      if (isNaN(date.getTime())) {
        console.error('유효하지 않은 날짜:', dateStr);
        return '시간 정보 없음';
      }

      // 한국 시간으로 표시
      return new Intl.DateTimeFormat('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
      }).format(date);
    } catch (error) {
      console.error('날짜 변환 오류:', error);
      return '시간 정보 없음';
    }
  };

  useEffect(() => {
    const fetchProductData = async () => {
      try {
        if (chatInfo?.productId) {
          const response = await getProductByProductId(chatInfo.productId);
          console.log("상품 정보:", response);
          setTransactionType(response.transactionType);
        }
      } catch (error) {
        console.error("상품 정보 조회 중 오류 발생:", error);
      }
    };

    fetchProductData();
  }, [chatInfo?.productId]); // chatInfo.productId가 변경될 때마다 실행

  // 승인 상태 확인
  const checkApprovalStatus = async () => {
    if (!chatInfo?.productId) {
      console.log('상품 ID가 없어 승인 상태를 확인할 수 없습니다.');
      setIsApproved(false);
      return;
    }

    try {
      console.log('승인 상태 확인 시도: productId=', chatInfo.productId);
      const response = await getApprovalStatus(chatInfo.productId, chatInfo.requestEmail);
      console.log('승인 상태 확인 응답:', response);
      
      if (response.status === 'success' && response.data) {
        const isApprovedStatus = response.data.status === '승인';
        console.log('승인 상태 확인 결과:', isApprovedStatus);
        setIsApproved(isApprovedStatus);
      } else {
        console.log('승인되지 않은 상태:', response);
        setIsApproved(false);
      }
    } catch (error) {
      console.error('승인 상태 확인 중 오류 발생:', error);
      setIsApproved(false);
    }
  };

  // chatInfo가 변경될 때마다 승인 상태 확인
  useEffect(() => {
    if (chatInfo?.productId) {
      checkApprovalStatus();
    }
  }, [chatInfo]);

  // 위치보기 버튼 클릭 핸들러
  const handleLocationClick = () => {
    if (!isApproved) {
      toast.warning('승인된 사용자만 위치 정보를 볼 수 있습니다.');
      return;
    }
    navigate(`/sharelocation/${chatroomId}/${chatInfo?.chatname}`, { 
      state: { 
        email: userEmail,
        otherUserEmail: chatInfo?.otherUserEmail,
        nickname: chatInfo?.otherUserName,
        productId: chatInfo?.productId,
      } 
    });
  };

  // 시스템 메시지 처리
  useEffect(() => {
    if (messages.length > 0) {
      const lastMessage = messages[messages.length - 1];
      
      // 시스템 메시지 처리
      if (lastMessage.messageType === MessageType.SYSTEM) {
        try {
          const systemData = JSON.parse(lastMessage.content);
          
          if (systemData.type === 'APPROVAL_STATUS') {
            // 승인 상태 변경 메시지 처리
            if (systemData.status === '승인') {
              setIsDisabled(true);
              toast.success('함께하기 요청이 승인되었습니다.');
            }
          }
        } catch (error) {
          console.error('시스템 메시지 파싱 오류:', error);
        }
      }
      
      // 메시지 목록 스크롤
      if (messagesEndRef.current) {
        messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }, [messages]);

  // 메시지 정렬 및 상태 업데이트를 위한 useEffect
  useEffect(() => {
    const sortedMessages = [...previousMessages, ...messages].sort((a, b) => {
      const getTime = (date: string | number[] | undefined) => {
        if (!date) return 0;
        let kstDate: Date;
        
        if (Array.isArray(date)) {
          // 배열 형식의 날짜를 KST로 변환
          kstDate = new Date(date[0], date[1] - 1, date[2], date[3], date[4], date[5]);
        } else {
          // 문자열 형식의 날짜를 KST로 변환
          const utcDate = new Date(date);
          kstDate = new Date(utcDate.getTime() + (9 * 60 * 60 * 1000));
        }
        
        return kstDate.getTime();
      };
      
      return getTime(a.sentAt) - getTime(b.sentAt);
    });

    // 메시지 시간을 KST로 변환하여 저장
    const messagesWithKST = sortedMessages.map(message => ({
      ...message,
      sentAt: Array.isArray(message.sentAt) 
        ? message.sentAt 
        : new Date(new Date(message.sentAt || '').getTime() + (9 * 60 * 60 * 1000)).toISOString()
    }));

    setAllMessages(messagesWithKST);
  }, [previousMessages, messages]);

  // 로딩 상태 표시 부분 수정
  if (isLoading || isConnecting || isLoadingMessages ) {
    return (
      <div className="flex flex-col h-full">
        {/* 채팅방 헤더는 항상 표시 */}
        <div className="fixed bg-gradient-to-r from-primary-500 to-primary-600 dark:from-gray-800 dark:to-gray-700 
          p-3 flex items-center justify-between shadow-md z-10">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-full overflow-hidden ring-2 ring-white/30 shrink-0">
              <ProductImage 
                imagePath={chatInfo?.productImageUrl || ''} 
              />
            </div>
            <div className="flex flex-col">
              <span className="text-base font-medium text-white">
                {chatInfo?.otherUserName || '상대방'}
              </span>
              <span className="text-sm text-white/70">
                {mainCategory}
                {subCategory}
              </span>
            </div>
          </div>
        </div>

        {/* 로딩 상태에 따른 메시지 표시 */}
        <div className="flex-1 flex justify-center items-center">
          <div className="text-center">
            {isConnecting ? (
              <>
                <p className="text-gray-500 mb-2">채팅 서버에 연결 중...</p>
                <p className="text-xs text-gray-400">잠시만 기다려주세요</p>
              </>
            ) : isLoadingMessages ? (
              <>
                <p className="text-gray-500 mb-2">이전 메시지를 불러오는 중...</p>
                <p className="text-xs text-gray-400">잠시만 기다려주세요</p>
              </>
            ) : (
              <>
                <p className="text-gray-500 mb-2">채팅방 정보를 불러오는 중...</p>
                <p className="text-xs text-gray-400">잠시만 기다려주세요</p>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  // 에러가 있거나 필수 정보가 없을 때
  if (!chatroomId || !userEmail) {
    console.log('채팅방 정보 로드 실패:', error);
    return <Navigate to="/chat-list" replace />;
  }

  // 함께하기 버튼 클릭 핸들러
  const handleJoinClick = async () => {
    if (!chatroomId) return;

    try {
      const response = await axiosInstance.post(
        `${apiConfig.endpoints.core.base}/chat/rooms/${chatroomId}/approve`
      );
      
      if (response.data?.status === 'success') {
        // 승인 상태 즉시 갱신
        setIsDisabled(true);
        
        // 승인 상태 변경을 웹소켓으로 알림
        sendMessage(chatInfo?.productId || 0, JSON.stringify({
          type: 'PRODUCT_APPROVAL',
          status: '승인',
          chatroomId: chatroomId,
          requestEmail: chatInfo?.requestEmail
        }), MessageType.SYSTEM);
        
        // 승인 상태 체크하여 UI 갱신
        await checkApprovalStatus();        
      }
    } catch (error) {
      console.error('함께하기 요청 실패:', error);
      toast.error('함께하기 요청에 실패했습니다.');
    }
  };

  // 메시지 전송 핸들러
  const handleSendMessage = async (
    message: string,
    file?: { type: string; url: string; name?: string }
  ) => {
    if (!message.trim() && !file) return;

    try {
      const now = new Date();
      const kstDate = new Date(now.getTime() + (9 * 60 * 60 * 1000));
      
      if (file) {
        if (file.type === 'image') {
          // 이미지 메시지 처리
          const formData = new FormData();
          formData.append('file', file.url);
          formData.append('message', message);
          formData.append('sentAt', kstDate.toISOString());
          await sendImage(formData.get('file') as string);
        }
      } else {
        // 텍스트 메시지 처리
        await sendMessage(chatInfo?.productId || 0, message.trim(), MessageType.TEXT);
      }

      // 메시지 전송 후 스크롤 처리
      setTimeout(() => {
        if (chatContainerRef.current) {
          chatContainerRef.current.scrollTo({
            top: chatContainerRef.current.scrollHeight,
            behavior: 'smooth'
          });
        }
      }, 100);
    } catch (error) {
      console.error('메시지 전송 중 오류:', error);
      toast.error('메시지 전송에 실패했습니다.');
    }
  };

  console.log("userEmail",userEmail);
  return (
    <div className="relative flex flex-col h-full bg-gray-50 dark:bg-gray-900">
      {/* 채팅 상대방 정보 */}
      <div className="bg-gradient-to-r from-primary-500 to-primary-600 dark:from-gray-800 dark:to-gray-700 
        p-3 flex items-center justify-between shadow-md z-10 flex-shrink-0">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-full overflow-hidden ring-2 ring-white/30 shrink-0">
            <ProductImage 
              imagePath={chatInfo?.productImageUrl || ''} 
            />
          </div>
          <div className="flex flex-col">
            <span className="text-base font-medium text-white">
              {chatInfo?.otherUserName || '상대방'}
            </span>
            <span className="text-sm text-white/70">
              {mainCategory}
              {subCategory}
            </span>
          </div>
        </div>

        {/* 함께하기 버튼 또는 위치보기 버튼 */}
        { chatInfo?.registrantEmail === userEmail && !isApproved ? (
          <button
            onClick={handleJoinClick}
            disabled={isDisabled}
            className="
              bg-white text-primary-600 dark:bg-gray-700 dark:text-white
              px-4 py-2 rounded-full shadow-md hover:shadow-lg
              transition-all duration-300 hover:bg-primary-100 dark:hover:bg-gray-600
              disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium
            "
          >
            함께하기
          </button>
        ) : isApproved && transactionType === '대면' && (
          <button
            onClick={handleLocationClick}
            className="
              bg-white text-primary-600 dark:bg-gray-700 dark:text-white
              px-4 py-2 rounded-full shadow-md hover:shadow-lg
              transition-all duration-300 hover:bg-primary-100 dark:hover:bg-gray-600
              text-sm font-medium
            "
          >
            <span className="flex items-center">              
                <IconMap className="w-4 h-4 mr-2" onClick={handleLocationClick}/>
                위치보기              
            </span>
          </button>
        )}
      </div>

        {/* 채팅 메시지 영역 */}
        <div
          ref={chatContainerRef}
          onClick={handleChatAreaClick}
          className="flex-1 p-4 overflow-y-auto space-y-4 bg-gray-50 dark:bg-gray-900 pb-20"
        >
          {allMessages.map((msg, index) => (
            <div key={msg.messageId || index} className="group relative">
              <div className={`flex flex-col ${msg.senderEmail === userEmail ? 'items-end' : 'items-start'}`}>
                {/* 상대방 메시지일 때만 프로필 이미지와 닉네임 표시 */}
                {msg.senderEmail !== userEmail && (
                  <div className="flex items-start space-x-2 mb-1">
                    <div className="w-8 h-8 rounded-full overflow-hidden ring-2 ring-primary-100 dark:ring-primary-900 flex-shrink-0">
                      <ProfileImage 
                        nickname={chatInfo?.otherUserName || ''}                     
                      />
                    </div>
                    <span className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                      {chatInfo?.otherUserName || '알 수 없음'}
                    </span>
                  </div>
                )}
                
                {/* 메시지와 시간을 감싸는 컨테이너 */}
                <div className={`flex items-end gap-2 ${msg.senderEmail !== userEmail ? 'ml-10' : ''}`}>
                  {/* 시간을 왼쪽에 표시 (내가 보낸 메시지일 경우) */}
                  {msg.senderEmail === userEmail && (
                    <span className="text-xs text-gray-500 flex-shrink-0">
                      {formatMessageTime(msg.sentAt || '')}
                    </span>
                  )}
                  {/* 메시지 내용 */}
                  <div className={`max-w-[80%] rounded-2xl p-3 shadow-md ${
                    msg.senderEmail === userEmail 
                      ? 'bg-primary-500 text-white rounded-tr-sm' 
                      : 'bg-white dark:bg-gray-800 rounded-tl-sm'
                  }`}>
                    {msg.messageType === MessageType.IMAGE ? (
                      <img
                        src={msg.content}
                        alt="전송된 이미지"
                        className="w-full max-w-xs rounded-xl"
                      />
                    ) : msg.messageType === MessageType.FILE ? (
                      <a
                        href={msg.content}
                        download
                        className="block p-3 bg-white/10 rounded-xl text-white hover:bg-white/20 transition-colors"
                      >
                        📄 첨부파일
                      </a>
                    ) : (
                      <p className={msg.senderEmail === userEmail ? 'text-white' : 'text-gray-800'}>
                        {msg.content}
                      </p>
                    )}
                  </div>
                  {/* 시간을 오른쪽에 표시 (상대방이 보낸 메시지일 경우) */}
                  {msg.senderEmail !== userEmail && (
                    <span className="text-xs text-gray-500 flex-shrink-0">
                      {formatMessageTime(msg.sentAt || '')}
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* 하단 입력 영역 */}
        <div 
          ref={messageInputRef}
          className="absolute bottom-0 left-0 right-0 bg-white dark:bg-gray-800 shadow-lg border-t border-gray-200 dark:border-gray-700"
        >
          <div className="mx-auto max-w-screen-md">
            <MessageInput 
              onSendMessage={handleSendMessage}
              onFocus={() => setIsKeyboardVisible(true)}
              onBlur={() => setIsKeyboardVisible(false)}
            />
          </div>
        </div>
      </div>
   
  );
};

export default ChatRoom;
