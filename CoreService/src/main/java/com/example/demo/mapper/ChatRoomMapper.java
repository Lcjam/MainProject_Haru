package com.example.demo.mapper;

import com.example.demo.model.chat.ChatRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@Repository
public interface ChatRoomMapper {
    
    // 채팅방 생성
    void createChatRoom(ChatRoom chatRoom);
    
    // 채팅방 ID로 채팅방 조회 - 매개변수 순서 수정
    ChatRoom findChatRoomById(@Param("chatroomId") Integer chatroomId, @Param("email") String email);
    
    // 상품 ID와 구매자 이메일로 채팅방 조회
    ChatRoom findChatRoomByProductAndBuyer(
            @Param("productId") Long productId, 
            @Param("requestEmail") String requestEmail);
    
    // 사용자와 관련된 채팅방 목록 조회
    List<ChatRoom> findChatRoomsByUser(@Param("email") String email);
    
    // 채팅방 정보 업데이트
    void updateChatRoom(ChatRoom chatRoom);
    
    // 채팅방 상태 변경
    void updateChatRoomStatus(
            @Param("chatroomId") Integer chatroomId, 
            @Param("status") String status);
    
    // 채팅방 마지막 메시지 및 시간 업데이트 (개별 파라미터)
    void updateChatRoomLastMessage(
        @Param("chatroomId") Integer chatroomId, 
        @Param("lastMessage") String lastMessage, 
        @Param("lastMessageTime") LocalDateTime lastMessageTime
    );

    /**
     * 사용자와 관련된 모집 중이거나 승인된 채팅방 목록 조회
     */
    List<ChatRoom> findActiveChatRoomsByUser(@Param("email") String email);

    /**
     * 상품 ID와 사용자 이메일로 채팅방 조회
     */
    @Select("SELECT * FROM chatrooms " +
            "WHERE product_id = #{productId} " +
            "AND (request_email = #{email} OR EXISTS (" +
            "    SELECT 1 FROM products p " +
            "    WHERE p.id = chatrooms.product_id " +
            "    AND p.email = #{email}" +
            "))")
    ChatRoom findChatRoomByProductIdAndEmail(@Param("productId") Long productId, @Param("email") String email);
}
