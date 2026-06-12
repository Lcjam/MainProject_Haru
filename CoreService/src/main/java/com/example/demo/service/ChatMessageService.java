package com.example.demo.service;

import com.example.demo.dto.chat.ChatMessageRequest;
import com.example.demo.dto.chat.ChatMessagesResponse;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Market.Product;
import com.example.demo.model.Market.ProductRequest;
import com.example.demo.model.User;
import com.example.demo.model.chat.ChatMessage;
import com.example.demo.model.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {
    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomMapper chatRoomMapper;
    private final ProductMapper productMapper;
    private final ProductRequestMapper productRequestMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 📌 `@Qualifier`를 필드에 직접 적용하여 명확하게 지정
    private final @Qualifier("chatChannelTopic") ChannelTopic chatChannelTopic;

    @Value("${chat.default.page-size:20}")
    private int defaultPageSize;

    // 실행 중 생성 가능한 채팅 이미지 저장 경로
    private final Path chatImageRootDir = Paths.get(System.getProperty("user.dir"), "uploads", "chat-images");

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatMessage sendMessage(String senderEmail, ChatMessageRequest request) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findChatRoomById(request.getChatroomId(), senderEmail);
        validateChatRoomAccess(chatRoom, senderEmail);
        
        // 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatroomId(request.getChatroomId())
                .senderEmail(senderEmail)
                .productId(chatRoom.getProductId())
                .content(request.getContent())
                .messageType(request.getMessageType())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
        
        // 메시지 저장
        chatMessageMapper.saveChatMessage(message);
        
        // 채팅방 정보 업데이트 (마지막 메시지, 시간)
        String lastMessage = "";
        
        // 메시지 타입에 따라 lastMessage 다르게 설정
        switch (request.getMessageType()) {
            case "TEXT":
                lastMessage = request.getContent();
                break;
            case "IMAGE":
                lastMessage = "이미지를 보냈습니다.";
                break;
            case "FILE":
                lastMessage = "파일을 보냈습니다.";
                break;
            case "OFFER":
                lastMessage = "제안을 보냈습니다.";
                break;
            default:
                lastMessage = request.getContent();
        }
        
        // updateChatRoom 대신 updateChatRoomLastMessage 사용
        chatRoomMapper.updateChatRoomLastMessage(
                request.getChatroomId(),
                lastMessage,
                message.getSentAt()
        );
        
        // 발신자 정보 추가 (for 실시간 메시지)
        User sender = userMapper.findByEmail(senderEmail);
        if (sender != null) {
            message.setSenderName(sender.getNickname());
        }
        
        try {
            // Redis를 통해 메시지 발행
            redisTemplate.convertAndSend(chatChannelTopic.getTopic(), message);
        } catch (Exception e) {
            log.error("Redis 메시지 발행 실패: {}", e.getMessage());
        }
        
        return message;
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    public ChatMessagesResponse getChatMessages(Integer chatroomId, String userEmail, Integer page, Integer size) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findChatRoomById(chatroomId, userEmail);
        try {
            validateChatRoomAccess(chatRoom, userEmail);
        } catch (IllegalArgumentException e) {
            return ChatMessagesResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
        
        // 페이징 처리
        int pageSize = (size != null && size > 0) ? size : defaultPageSize;
        int pageNum = (page != null && page >= 0) ? page : 0;
        int offset = pageNum * pageSize;
        
        // 메시지 목록 조회
        List<ChatMessage> messages = chatMessageMapper.findMessagesByChatRoomId(chatroomId, offset, pageSize);
        
        // 총 메시지 수
        int totalCount = chatMessageMapper.countMessagesByChatRoomId(chatroomId);
        int totalPages = (totalCount + pageSize - 1) / pageSize;
        
        // 메시지 읽음 상태 업데이트
        chatMessageMapper.updateMessageReadStatus(chatroomId, userEmail);
        
        return ChatMessagesResponse.builder()
                .success(true)
                .message("메시지 목록 조회 성공")
                .messages(messages)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(pageNum)
                .build();
    }

    /**
     * 메시지 읽음 상태 업데이트
     */
    public boolean markMessagesAsRead(Integer chatroomId, String receiverEmail) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findChatRoomById(chatroomId, receiverEmail);
        validateChatRoomAccess(chatRoom, receiverEmail);
        
        // 메시지 읽음 상태 업데이트
        chatMessageMapper.updateMessageReadStatus(chatroomId, receiverEmail);
        
        // 업데이트 된 메시지가 있는지 확인
        int unreadCount = chatMessageMapper.countUnreadMessages(chatroomId, receiverEmail);
        
        // 채팅방의 읽지 않은 메시지 카운트 업데이트
        // unreadCount가 0이 되었으므로 메시지를 모두 읽었다는 의미
        return unreadCount == 0;
    }

    /**
     * 이미지 메시지 전송
     */
    @Transactional
    public ChatMessage sendImageMessage(String userEmail, Integer chatroomId, MultipartFile image) {
        try {
            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = chatRoomMapper.findChatRoomById(chatroomId, userEmail);
            validateChatRoomAccess(chatRoom, userEmail);
            
            // 이미지 저장 디렉토리 확인 및 생성
            Path chatImageDir = chatImageRootDir.resolve("chatroom_" + chatroomId);
            if (!Files.exists(chatImageDir)) {
                Files.createDirectories(chatImageDir);
                log.info("채팅 이미지 저장 디렉토리 생성: {}", chatImageDir);
            }
            
            // 이미지 파일 저장
            String originalFilename = image.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String newFilename = UUID.randomUUID().toString() + extension;
            Path imagePath = chatImageDir.resolve(newFilename);
            Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("채팅 이미지 저장 완료: {}", imagePath);
            
            // 이미지 경로 설정 (웹에서 접근 가능한 경로)
            String imageUrl = "/chat-images/chatroom_" + chatroomId + "/" + newFilename;
            
            // 이미지 메시지 생성 및 저장
            ChatMessageRequest request = new ChatMessageRequest();
            request.setChatroomId(chatroomId);
            request.setProductId(chatRoom.getProductId());
            request.setMessageType("IMAGE");
            request.setContent(imageUrl); // 이미지 URL을 메시지 내용으로 저장
            
            return sendMessage(userEmail, request);
            
        } catch (Exception e) {
            log.error("이미지 메시지 전송 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private boolean isChatParticipant(ChatRoom chatRoom, String userEmail) {
        return userEmail != null && (
                userEmail.equals(chatRoom.getSellerEmail()) ||
                userEmail.equals(chatRoom.getRequestEmail())
        );
    }

    private void validateChatRoomAccess(ChatRoom chatRoom, String userEmail) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }

        if (!isChatParticipant(chatRoom, userEmail)) {
            throw new IllegalArgumentException("해당 채팅방에 접근 권한이 없습니다.");
        }

        Product product = productMapper.findById(chatRoom.getProductId(), chatRoom.getSellerEmail());
        if (product == null) {
            throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
        }

        if (!product.isVisible() && userEmail.equals(chatRoom.getRequestEmail())) {
            ProductRequest productRequest = productRequestMapper.findByProductIdAndRequesterEmail(
                    chatRoom.getProductId(), userEmail);

            if (productRequest == null || !"승인".equals(productRequest.getApprovalStatus())) {
                throw new IllegalArgumentException("모집이 완료된 상품은 승인된 사용자만 채팅이 가능합니다.");
            }
        }
    }
}
