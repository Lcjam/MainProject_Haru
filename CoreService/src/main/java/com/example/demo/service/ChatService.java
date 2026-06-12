package com.example.demo.service;

import com.example.demo.dto.chat.*;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.model.Market.Product;
import com.example.demo.model.Market.ProductImage;
import com.example.demo.model.Market.ProductRequest;
import com.example.demo.model.User;
import com.example.demo.model.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductRequestMapper productRequestMapper;

    /**
     * 채팅방 생성 또는 조회
     */
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(String userEmail, ChatRoomRequest request) {
        Product product = productMapper.findById(request.getProductId(), userEmail);
        if (product == null) {
            return ChatRoomResponse.builder()
                    .success(false)
                    .message("존재하지 않는 상품입니다.")
                    .build();
        }
        
        String sellerEmail = product.getEmail();
        
        String requestEmail = userEmail;
        
        // 모집 완료된 상품의 경우 승인된 사용자만 채팅방 접근 허용
        if (!product.isVisible() && !sellerEmail.equals(userEmail)) {
            // 상품 요청 정보 확인
            ProductRequest productRequest = productRequestMapper.findByProductIdAndRequesterEmail(
                request.getProductId(), requestEmail);
            
            // 모집 완료된 상품인데 승인되지 않은 사용자인 경우
            if (productRequest == null || !"승인".equals(productRequest.getApprovalStatus())) {
                return ChatRoomResponse.builder()
                        .success(false)
                        .message("모집이 완료된 상품은 승인된 사용자만 채팅이 가능합니다.")
                        .build();
            }
        }
        
        ChatRoom existingChatRoom = chatRoomMapper.findChatRoomByProductAndBuyer(
                request.getProductId(), requestEmail);
        
        ChatRoom chatRoom;
        boolean isNewChatRoom = false;
        
        if (existingChatRoom == null) {
            // 새로운 채팅방 생성
            chatRoom = ChatRoom.builder()
                    .chatname(request.getChatname() != null ? request.getChatname() : product.getTitle())
                    .productId(request.getProductId())
                    .requestEmail(requestEmail)
                    .lastMessage("채팅이 시작되었습니다.")
                    .lastMessageTime(LocalDateTime.now())
                    .status("ACTIVE")
                    .build();
            
            chatRoomMapper.createChatRoom(chatRoom);
            isNewChatRoom = true;
        } else {
            chatRoom = existingChatRoom;
        }
        
        User seller = userMapper.findByEmail(sellerEmail);
        User buyer = userMapper.findByEmail(requestEmail);
        
        String thumbnailPath = getProductThumbnailImage(product.getId());
        
        String otherUserEmail = sellerEmail.equals(userEmail) ? requestEmail : sellerEmail;
        String otherUserName = sellerEmail.equals(userEmail) ? buyer.getNickname() : seller.getNickname();
        
        return ChatRoomResponse.builder()
                .success(true)
                .message(isNewChatRoom ? "새 채팅방이 생성되었습니다." : "기존 채팅방을 조회했습니다.")
                .chatroomId(chatRoom.getChatroomId())
                .chatname(chatRoom.getChatname())
                .productId(chatRoom.getProductId())
                .productName(product.getTitle())
                .productImageUrl(thumbnailPath)
                .sellerEmail(sellerEmail)
                .requestEmail(chatRoom.getRequestEmail())
                .otherUserEmail(otherUserEmail)
                .otherUserName(otherUserName)
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageTime(chatRoom.getLastMessageTime())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    /**
     * 사용자의 채팅방 목록 조회
     * - productrequests 테이블에서 대기 또는 승인 상태인 요청과 연결된 채팅방만 조회
     * - 사용자가 구매자이거나 상품 등록자인 경우에만 조회됨
     */
    public ChatRoomResponse getChatRoomsByUser(String userEmail) {
        List<ChatRoom> chatRooms = chatRoomMapper.findChatRoomsByUser(userEmail);
        List<ChatRoom> enhancedChatRooms = new ArrayList<>();
        
        for (ChatRoom room : chatRooms) {
            Product product = productMapper.findById(room.getProductId(), userEmail);
            if (product != null) {
                room.setProductName(product.getTitle());
                
                // 대화 상대 정보 설정
                String otherEmail = room.getSellerEmail().equals(userEmail) ? 
                        room.getRequestEmail() : room.getSellerEmail();
                User otherUser = userMapper.findByEmail(otherEmail);
                if (otherUser != null) {
                    room.setOtherUserName(otherUser.getNickname());
                }
                
                // 상품 이미지 설정
                String thumbnailPath = getProductThumbnailImage(room.getProductId());
                room.setProductImageUrl(thumbnailPath);
                
                // 읽지 않은 메시지 수 설정
                int unreadCount = chatMessageMapper.countUnreadMessages(room.getChatroomId(), userEmail);
                room.setUnreadCount(unreadCount);
                
                enhancedChatRooms.add(room);
            }
        }
        
        return ChatRoomResponse.builder()
                .success(true)
                .message("대기 또는 승인된 요청의 채팅방 목록 조회 성공")
                .chatRooms(enhancedChatRooms)
                .build();
    }

    /**
     * 채팅방 상세 정보 조회
     */
    public ChatRoomResponse getChatRoomDetail(String userEmail, Integer chatroomId) {
        ChatRoom chatRoom = chatRoomMapper.findChatRoomById(chatroomId, userEmail);
        if (chatRoom == null) {
            return ChatRoomResponse.builder()
                    .success(false)
                    .message("존재하지 않는 채팅방입니다.")
                    .build();
        }
        
        Product product = productMapper.findById(chatRoom.getProductId(), userEmail);
        if (product == null) {
            return ChatRoomResponse.builder()
                    .success(false)
                    .message("상품 정보를 찾을 수 없습니다.")
                    .build();
        }
        
        String sellerEmail = product.getEmail();
        String requestEmail = chatRoom.getRequestEmail();
        
        if (!sellerEmail.equals(userEmail) && !requestEmail.equals(userEmail)) {
            return ChatRoomResponse.builder()
                    .success(false)
                    .message("해당 채팅방에 접근 권한이 없습니다.")
                    .build();
        }
        
        // 모집 완료된 상품의 경우 승인된 사용자만 채팅방 접근 허용
        if (!product.isVisible() && requestEmail.equals(userEmail)) {
            // 상품 요청 정보 확인
            ProductRequest productRequest = productRequestMapper.findByProductIdAndRequesterEmail(
                    product.getId(), requestEmail);
            
            // 모집 완료된 상품인데 승인되지 않은 사용자인 경우
            if (productRequest == null || !"승인".equals(productRequest.getApprovalStatus())) {
                return ChatRoomResponse.builder()
                        .success(false)
                        .message("모집이 완료된 상품은 승인된 사용자만 채팅이 가능합니다.")
                        .build();
            }
        }

        String registrantEmail = product.getEmail();
        
        String thumbnailPath = getProductThumbnailImage(product.getId());
        
        String otherUserEmail = sellerEmail.equals(userEmail) ? 
                requestEmail : sellerEmail;
        
        User otherUser = userMapper.findByEmail(otherUserEmail);
        if (otherUser == null) {
            return ChatRoomResponse.builder()
                    .success(false)
                    .message("상대방 정보를 찾을 수 없습니다.")
                    .build();
        }
        
        // 메시지 읽음 상태 업데이트
        chatMessageMapper.updateMessageReadStatus(chatroomId, userEmail);
        
        return ChatRoomResponse.builder()
                .success(true)
                .message("채팅방 조회 성공")
                .chatroomId(chatRoom.getChatroomId())
                .chatname(chatRoom.getChatname())
                .productId(chatRoom.getProductId())
                .registrantEmail(registrantEmail)
                .productName(product.getTitle())
                .productImageUrl(thumbnailPath)
                .sellerEmail(sellerEmail)
                .requestEmail(requestEmail)
                .otherUserEmail(otherUserEmail)
                .otherUserName(otherUser.getNickname())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageTime(chatRoom.getLastMessageTime())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
    
    /**
     * 상품 썸네일 이미지 가져오기
     */
    private String getProductThumbnailImage(Long productId) {
        List<ProductImage> images = productImageMapper.findByProductId(productId);
        if (images != null && !images.isEmpty()) {
            return "/api/core/market/products/images/" + images.get(0).getId();
        }
        return null;
    }
    
    /**
     * 채팅방 상태 업데이트
     */
    @Transactional
    public ChatRoomResponse updateChatRoomStatus(String userEmail, Integer chatroomId, String status) {
        ChatRoom chatRoom = chatRoomMapper.findChatRoomById(chatroomId, userEmail);
        if (chatRoom == null) {
            return ChatRoomResponse.builder()
                    .success(false)
                    .message("존재하지 않는 채팅방입니다.")
                    .build();
        }
        
        chatRoomMapper.updateChatRoomStatus(chatroomId, status);
        
        return ChatRoomResponse.builder()
                .success(true)
                .message("채팅방 상태가 업데이트되었습니다.")
                .build();
    }

    /**
     * 모집 중이거나 승인된 채팅방 목록 조회
     * 상품 등록자는 모든 채팅방을, 신청자는 모집중이거나 승인된 채팅방만 볼 수 있음
     */
    public ChatRoomResponse getActiveChatRoomsByUser(String userEmail) {
        List<ChatRoom> chatRooms = chatRoomMapper.findActiveChatRoomsByUser(userEmail);
        List<ChatRoom> enhancedChatRooms = new ArrayList<>();
        
        for (ChatRoom room : chatRooms) {
            Product product = productMapper.findById(room.getProductId(), userEmail);
            if (product != null) {
                room.setProductName(product.getTitle());
                
                // 대화 상대 정보 설정
                String otherEmail;
                
                if (product.getEmail().equals(userEmail)) {
                    // 상품 등록자인 경우, 상대방은 구매자
                    otherEmail = room.getRequestEmail();
                } else {
                    // 구매자/요청자인 경우, 상대방은 판매자
                    otherEmail = product.getEmail();
                }
                
                User otherUser = userMapper.findByEmail(otherEmail);
                if (otherUser != null) {
                    room.setOtherUserName(otherUser.getNickname());
                }
                
                // 상품 이미지 설정
                String thumbnailPath = getProductThumbnailImage(room.getProductId());
                room.setProductImageUrl(thumbnailPath);
                
                // 읽지 않은 메시지 수 설정
                int unreadCount = chatMessageMapper.countUnreadMessages(room.getChatroomId(), userEmail);
                room.setUnreadCount(unreadCount);
                
                enhancedChatRooms.add(room);
            }
        }
        
        return ChatRoomResponse.builder()
                .success(true)
                .message("채팅방 목록 조회 성공")
                .chatRooms(enhancedChatRooms)
                .build();
    }
}