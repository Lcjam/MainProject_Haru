package com.example.demo.service;

import com.example.demo.dto.board.CommentCreateRequest;
import com.example.demo.dto.board.CommentResponse;
import com.example.demo.dto.board.CommentUpdateRequest;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.CommentMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import com.example.demo.model.board.Comment;
import com.example.demo.model.board.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final BoardMapper boardMapper;
    private final BoardMemberMapper boardMemberMapper;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponse createComment(String email, CommentCreateRequest request) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(request.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 게시판 및 권한 확인
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(email);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, board.getId());
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 댓글을 작성할 수 있습니다.");
        }
        
        // 부모 댓글이 있는지 확인 (대댓글인 경우)
        if (request.getParentId() != null) {
            Comment parentComment = commentMapper.getCommentById(request.getParentId());
            if (parentComment == null) {
                throw new IllegalArgumentException("부모 댓글을 찾을 수 없습니다.");
            }
            
            // 부모 댓글과 동일한 게시글인지 확인
            if (!parentComment.getPostId().equals(request.getPostId())) {
                throw new IllegalArgumentException("부모 댓글과 게시글이 일치하지 않습니다.");
            }
            
            // 대댓글의 대댓글은 작성할 수 없음 (최대 깊이 1)
            if (parentComment.getParentId() != null) {
                throw new IllegalArgumentException("대댓글에는 댓글을 달 수 없습니다.");
            }
        }
        
        // 댓글 생성
        Comment comment = Comment.builder()
                .postId(request.getPostId())
                .parentId(request.getParentId())
                .authorEmail(email)
                .content(request.getContent())
                .build();
        
        commentMapper.createComment(comment);
        
        // 부모 댓글이 있는 경우 대댓글 수 업데이트
        if (request.getParentId() != null) {
            int replyCount = commentMapper.countRepliesByParentId(request.getParentId());
            commentMapper.updateReplyCount(request.getParentId(), replyCount);
        }
        
        // 게시글의 댓글 수 업데이트
        int commentCount = commentMapper.countCommentsByPostId(request.getPostId());
        commentMapper.updatePostCommentCount(request.getPostId(), commentCount);
        
        // 생성된 댓글 조회
        Comment createdComment = commentMapper.getCommentById(comment.getId());
        
        // 응답 데이터 구성
        return convertToCommentResponse(createdComment, email);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(String email, Long commentId, CommentUpdateRequest request) {
        // 댓글 존재 여부 확인
        Comment comment = commentMapper.getCommentById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("댓글을 찾을 수 없습니다.");
        }
        
        // 작성자 확인
        if (!comment.getAuthorEmail().equals(email)) {
            throw new IllegalArgumentException("댓글 작성자만 수정할 수 있습니다.");
        }
        
        // 댓글 수정
        comment.setContent(request.getContent());
        commentMapper.updateComment(comment);
        
        // 수정된 댓글 조회
        Comment updatedComment = commentMapper.getCommentById(commentId);
        
        // 응답 데이터 구성
        return convertToCommentResponse(updatedComment, email);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(String email, Long commentId) {
        // 댓글 존재 여부 확인
        Comment comment = commentMapper.getCommentById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("댓글을 찾을 수 없습니다.");
        }
        
        // 게시글 정보 조회
        Post post = postMapper.getPostById(comment.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 게시판 및 권한 확인
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 권한 확인 (작성자, 게시판 호스트, 게시글 작성자)
        boolean isCommentAuthor = comment.getAuthorEmail().equals(email);
        boolean isBoardHost = board.getHostEmail().equals(email);
        boolean isPostAuthor = post.getAuthorEmail().equals(email);
        boolean hasBoardAdminRole = false;
        
        // 게시판 관리자 권한 확인
        if (!isCommentAuthor && !isBoardHost && !isPostAuthor) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, board.getId());
            if (member != null && "ADMIN".equals(member.getRole())) {
                hasBoardAdminRole = true;
            }
        }
        
        if (!isCommentAuthor && !isBoardHost && !isPostAuthor && !hasBoardAdminRole) {
            throw new IllegalArgumentException("댓글 작성자, 게시판 관리자, 게시글 작성자만 삭제할 수 있습니다.");
        }
        
        // 댓글 삭제
        commentMapper.deleteComment(commentId, email);
        
        // 부모 댓글이 있는 경우 대댓글 수 업데이트
        if (comment.getParentId() != null) {
            int replyCount = commentMapper.countRepliesByParentId(comment.getParentId());
            commentMapper.updateReplyCount(comment.getParentId(), replyCount);
        }
        
        // 게시글의 댓글 수 업데이트
        int commentCount = commentMapper.countCommentsByPostId(comment.getPostId());
        commentMapper.updatePostCommentCount(comment.getPostId(), commentCount);
    }

    /**
     * 게시글의 댓글 목록 조회
     */
    public List<CommentResponse> getCommentsByPostId(String email, Long postId) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 게시판 및 권한 확인
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(email);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, board.getId());
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 댓글을 볼 수 있습니다.");
        }
        
        // 최상위 댓글 목록 조회
        List<Comment> comments = commentMapper.getCommentsByPostId(postId);
        
        // 각 댓글의 대댓글 조회 및 응답 데이터 구성
        List<CommentResponse> commentResponses = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponse commentResponse = convertToCommentResponse(comment, email);
            
            // 대댓글 조회
            if (comment.getReplyCount() > 0) {
                List<Comment> replies = commentMapper.getRepliesByParentId(comment.getId());
                List<CommentResponse> replyResponses = replies.stream()
                        .map(reply -> convertToCommentResponse(reply, email))
                        .collect(Collectors.toList());
                commentResponse.setReplies(replyResponses);
            } else {
                commentResponse.setReplies(new ArrayList<>());
            }
            
            commentResponses.add(commentResponse);
        }
        
        return commentResponses;
    }

    /**
     * 대댓글 목록 조회
     */
    public List<CommentResponse> getRepliesByParentId(String email, Long parentId) {
        // 부모 댓글 존재 여부 확인
        Comment parentComment = commentMapper.getCommentById(parentId);
        if (parentComment == null) {
            throw new IllegalArgumentException("부모 댓글을 찾을 수 없습니다.");
        }
        
        // 게시글 정보 조회
        Post post = postMapper.getPostById(parentComment.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 게시판 및 권한 확인
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(email);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, board.getId());
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 댓글을 볼 수 있습니다.");
        }
        
        // 대댓글 목록 조회
        List<Comment> replies = commentMapper.getRepliesByParentId(parentId);
        
        // 응답 데이터 구성
        return replies.stream()
                .map(reply -> convertToCommentResponse(reply, email))
                .collect(Collectors.toList());
    }

    /**
     * Comment 모델 객체를 CommentResponse DTO로 변환
     */
    private CommentResponse convertToCommentResponse(Comment comment, String currentUserEmail) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .parentId(comment.getParentId())
                .authorEmail(comment.getAuthorEmail())
                .authorName(comment.getAuthorName())
                .authorNickname(comment.getAuthorNickname())
                .authorProfileImage(comment.getAuthorProfileImage())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isAuthor(comment.getAuthorEmail().equals(currentUserEmail))
                .replyCount(comment.getReplyCount())
                .depth(comment.getDepth())
                .replies(null) // 기본적으로 null, 필요 시 별도로 설정
                .build();
    }
}
