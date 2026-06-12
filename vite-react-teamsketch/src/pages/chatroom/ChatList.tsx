import React from "react";
import { useState, useEffect } from "react";
import ChatListItem from "./ChatListItem"; 
import { useChatRooms, ChatRoom } from "../../services/api/userChatAPI";
import Loading from "../../components/common/Loading";
import ProductImage from "../../components/features/image/ProductImage";
import { MessageType } from "../../services/real-time/types";
import { useWebSocket } from "../../contexts/WebSocketContext";
import { useAppSelector, useAppDispatch } from "../../store/hooks";
import { useChat } from "../../services/real-time/useChat";
import { useNavigate } from "react-router-dom";
import { markChatMessagesAsRead } from "../../store/slices/notiSlice";

const ChatList: React.FC = () => {
  const { data: chatRooms, isLoading, isError, error, refetch } = useChatRooms();
  const [mockChats, setMockChats] = useState<ChatRoom[]>([]);
  const { token, user } = useAppSelector((state) => state.auth);
  const { isConnected } = useWebSocket();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { notifications } = useAppSelector((state) => state.noti);
  const lastNotification = notifications[0]; // 가장 최근 알림
  
  // 새로운 채팅 메시지 알림이 오면 채팅방 목록 업데이트
  useEffect(() => {
    if (lastNotification && lastNotification.type === 'CHAT_MESSAGE') {
      refetch(); // 채팅방 목록 새로고침
    }    
    
  }, [lastNotification, refetch]);

  // 웹소켓 연결 설정
  const { connect } = useChat({
    userEmail: user?.email || undefined,
    token: token || undefined,
    useGlobalConnection: true
  });

  // 웹소켓 연결 및 메시지 구독
  useEffect(() => {
    if (user?.email && token && !isConnected) {
      connect();
    }
  }, [user?.email, token, isConnected, connect]);

  // // 채팅방 데이터 주기적 갱신
  // useEffect(() => {
  //   const intervalId = setInterval(() => {
  //     refetch();
  //   }, 1000); // 1초마다 갱신

  //   return () => clearInterval(intervalId);
  // }, [refetch]);

  // 채팅방 데이터 유효성 검사
  useEffect(() => {
    if (chatRooms) {
      // 유효하지 않은 productId를 가진 채팅방 필터링
      const validChatRooms = chatRooms.filter(chat => {
        if (!chat.productId) {
          console.warn(`유효하지 않은 productId를 가진 채팅방 발견: ${chat.chatroomId}`);
          return false;
        }
        return true;
      });

      if (validChatRooms.length !== chatRooms.length) {
        console.warn(`${chatRooms.length - validChatRooms.length}개의 유효하지 않은 채팅방이 필터링됨`);
      }

      // 필터링된 채팅방 목록 사용
      setMockChats(validChatRooms);
    }
  }, [chatRooms]);

  // 날짜를 "3시간 전", "방금 전" 등의 형식으로 변환하는 함수
  const formatTime = (dateValue: string | number[]) => {
    try {
      let date: Date;
      
      if (Array.isArray(dateValue)) {
        // 배열 형식의 날짜 처리 [년, 월, 일, 시, 분]
        const [year, month, day, hour, minute] = dateValue;
        date = new Date(year, month - 1, day, hour, minute); // 월은 0부터 시작하므로 -1
      } else {
        // UTC 시간을 KST로 변환
        date = new Date(dateValue);
        date = new Date(date.getTime() + 9 * 60 * 60 * 1000); // UTC+9 (한국 시간)
      }

      if (isNaN(date.getTime())) {
        throw new Error('Invalid date');
      }

      const now = new Date();
      // 현재 시간도 KST로 맞추기
      now.setTime(now.getTime() + 9 * 60 * 60 * 1000);
      
      const diffInMs = now.getTime() - date.getTime();
      
      // 시간 차이 계산
      const diffInSec = Math.floor(diffInMs / 1000);
      const diffInMin = Math.floor(diffInSec / 60);
      const diffInHours = Math.floor(diffInMin / 60);
      const diffInDays = Math.floor(diffInHours / 24);

      // 1분 미만
      if (diffInSec < 60) return '방금 전';
      // 1시간 미만
      if (diffInMin < 60) return `${diffInMin}분 전`;
      // 24시간 미만
      if (diffInHours < 24) return `${diffInHours}시간 전`;
      // 7일 미만
      if (diffInDays < 7) return `${diffInDays}일 전`;
      
      // 1년 미만인 경우 월/일 표시
      if (date.getFullYear() === now.getFullYear()) {
        return `${date.getMonth() + 1}월 ${date.getDate()}일`;
      }
      
      // 그 외의 경우 년/월/일 표시
      return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
    } catch (error) {
      console.error('날짜 변환 오류:', error);
      return '날짜 오류';
    }
  };

  // API 호출이 실패한 경우 대체할 목업 데이터
  useEffect(() => {
    if (isError) {
      console.error('채팅방 목록 조회 실패:', error);
      setMockChats([
        {
          chatroomId: 1,
          chatname: "상품명 거래 채팅",
          productId: 1,
          registrantEmail: "seller@example.com",
          productName: "전자제품 거래",
          productImageUrl: "https://picsum.photos/600/400",
          sellerEmail: "seller@example.com",
          requestEmail: "buyer@example.com",
          otherUserEmail: "seller@example.com",
          otherUserName: "곰탱이",
          lastMessage: "안녕하세요",
          lastMessageTime: "2023-11-01T12:05:00",
          status: "ACTIVE",
          createdAt: "2023-11-01T12:00:00",
          updatedAt: "2023-11-01T12:05:00",
          unreadCount: 2,
          messageType: MessageType.TEXT
        },
        {
          chatroomId: 2,
          chatname: "옷 거래 채팅",
          productId: 2,
          registrantEmail: "seller2@example.com",
          productName: "의류 거래",
          productImageUrl: "https://picsum.photos/600/400",
          sellerEmail: "seller2@example.com",
          requestEmail: "buyer@example.com",
          otherUserEmail: "seller2@example.com",
          otherUserName: "집순이",
          lastMessage: "반가워요",
          lastMessageTime: "2023-11-01T10:30:00",
          status: "ACTIVE",
          createdAt: "2023-11-01T10:00:00",
          updatedAt: "2023-11-01T10:30:00",
          unreadCount: 0,
          messageType: MessageType.TEXT
        },
        {
          chatroomId: 3,
          chatname: "도서 거래 채팅",
          productId: 3,
          registrantEmail: "seller3@example.com",
          productName: "책 거래",
          productImageUrl: "https://picsum.photos/600/400",
          sellerEmail: "seller3@example.com",
          requestEmail: "buyer@example.com",
          otherUserEmail: "seller3@example.com",
          otherUserName: "알라딘딘",
          lastMessage: "어디세요?",
          lastMessageTime: "2023-11-01T09:00:00",
          status: "ACTIVE",
          createdAt: "2023-11-01T08:00:00",
          updatedAt: "2023-11-01T09:00:00",
          unreadCount: 3,
          messageType: MessageType.TEXT
        }
      ]);
    }
  }, [isError, error]);

  // 채팅방 클릭 처리
  const handleChatRoomClick = (chatroomId: number) => {
    // 채팅방 읽음 상태로 표시
    dispatch(markChatMessagesAsRead(chatroomId));
    
    // 클릭한 채팅방 찾기
    const chatRoom = (chatRooms || mockChats).find(chat => chat.chatroomId === chatroomId);
    if (!chatRoom) return;
    
    // 채팅방으로 이동
    navigate(`/chat/${chatroomId}/${chatRoom.chatname}`, {
      state: {
        chatroomId,
        otherUserEmail: chatRoom.otherUserEmail,
        nickname: chatRoom.otherUserName,
        chatname: chatRoom.chatname,
        productId: chatRoom.productId,
        imageUrl: chatRoom.productImageUrl
      }
    });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-full">
        <Loading />
      </div>
    );
  }

  // 채팅방이 없는 경우
  if ((!chatRooms || chatRooms.length === 0) && (!mockChats || mockChats.length === 0)) {
    return (
      <div className="flex justify-center items-center h-full flex-col">
        <div className="text-gray-500 text-xl mb-3">
          <i className="fas fa-comments text-3xl"></i>
        </div>
        <p className="text-gray-500">진행 중인 채팅이 없습니다.</p>
      </div>
    );
  }

  // API에서 가져온 데이터나 목업 데이터를 사용하여 채팅방 목록 표시
  const displayChats = chatRooms || mockChats;

  return (
    <div className="flex justify-center">
      <div className="grid grid-cols-1 w-full">
        {displayChats.map((chat) => (
          <ChatListItem          
            key={chat.chatroomId}
            nickname={chat.otherUserName}
            lastMessage={chat.lastMessage}
            time={formatTime(chat.lastMessageTime ?? new Date().toISOString())}
            unreadCount={chat.unreadCount}    
            productImage={<ProductImage imagePath={chat.productImageUrl} />}
            chatname={chat.chatname}
            messageType={chat.messageType}
            onClick={() => handleChatRoomClick(chat.chatroomId)}
          />
        ))}
      </div>
    </div>
  );
};

export default ChatList;
