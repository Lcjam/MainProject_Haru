package com.example.demo.service;

import com.example.demo.dto.board.PostCreateRequest;
import com.example.demo.dto.board.PostResponse;
import com.example.demo.dto.board.PostUpdateRequest;
import com.example.demo.dto.board.PagedPostResponse;
import com.example.demo.dto.board.PostSearchRequest;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import com.example.demo.model.board.Post;
import com.example.demo.model.board.PostImage;
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
public class PostService {

    private final PostMapper postMapper;
    private final BoardMapper boardMapper;
    private final BoardMemberMapper boardMemberMapper;
    private final FileStorageService fileStorageService;

    /**
     * 게시글 생성
     */
    @Transactional
    public PostResponse createPost(String email, PostCreateRequest request) {
        // 게시판 존재 여부 확인
        Board board = boardMapper.findBoardById(request.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        if (!board.getHostEmail().equals(email)) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, request.getBoardId());
            if (member == null || !"ACTIVE".equals(member.getStatus())) {
                throw new IllegalArgumentException("게시판의 멤버만 게시글을 작성할 수 있습니다.");
            }
        }
        
        // 게시글 생성
        Post post = Post.builder()
                .boardId(request.getBoardId())
                .authorEmail(email)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        
        postMapper.createPost(post);
        
        // 이미지 URL이 있으면 저장
        List<String> savedImageUrls = new ArrayList<>();
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String imageUrl : request.getImageUrls()) {
                PostImage postImage = PostImage.builder()
                        .postId(post.getId())
                        .imageUrl(imageUrl)
                        .build();
                postMapper.addPostImage(postImage);
                savedImageUrls.add(imageUrl);
            }
        }
        
        // 게시글 정보 조회
        Post savedPost = postMapper.getPostById(post.getId());
        
        // 응답 데이터 생성
        return PostResponse.builder()
                .id(savedPost.getId())
                .boardId(savedPost.getBoardId())
                .boardName(savedPost.getBoardName())
                .authorEmail(savedPost.getAuthorEmail())
                .authorName(savedPost.getAuthorName())
                .authorNickname(savedPost.getAuthorNickname())
                .authorProfileImage(savedPost.getAuthorProfileImage())
                .title(savedPost.getTitle())
                .content(savedPost.getContent())
                .createdAt(savedPost.getCreatedAt())
                .updatedAt(savedPost.getUpdatedAt())
                .viewCount(savedPost.getViewCount())
                .likeCount(savedPost.getLikeCount())
                .commentCount(savedPost.getCommentCount())
                .imageUrls(savedImageUrls)
                .isAuthor(true) // 작성자 본인이므로 true
                .build();
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse updatePost(String email, Long postId, PostUpdateRequest request) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 작성자 확인
        if (!post.getAuthorEmail().equals(email)) {
            throw new IllegalArgumentException("게시글 작성자만 수정할 수 있습니다.");
        }
        
        // 게시글 수정
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        
        postMapper.updatePost(post);
        
        // 이미지 처리 - 기존 이미지 삭제 후 새로 추가
        postMapper.deletePostImages(postId);
        
        List<String> savedImageUrls = new ArrayList<>();
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String imageUrl : request.getImageUrls()) {
                PostImage postImage = PostImage.builder()
                        .postId(postId)
                        .imageUrl(imageUrl)
                        .build();
                postMapper.addPostImage(postImage);
                savedImageUrls.add(imageUrl);
            }
        }
        
        // 업데이트된 게시글 정보 조회
        Post updatedPost = postMapper.getPostById(postId);
        
        // 응답 데이터 생성
        return PostResponse.builder()
                .id(updatedPost.getId())
                .boardId(updatedPost.getBoardId())
                .boardName(updatedPost.getBoardName())
                .authorEmail(updatedPost.getAuthorEmail())
                .authorName(updatedPost.getAuthorName())
                .authorNickname(updatedPost.getAuthorNickname())
                .authorProfileImage(updatedPost.getAuthorProfileImage())
                .title(updatedPost.getTitle())
                .content(updatedPost.getContent())
                .createdAt(updatedPost.getCreatedAt())
                .updatedAt(updatedPost.getUpdatedAt())
                .viewCount(updatedPost.getViewCount())
                .likeCount(updatedPost.getLikeCount())
                .commentCount(updatedPost.getCommentCount())
                .imageUrls(savedImageUrls)
                .isAuthor(true) // 작성자 본인이므로 true
                .build();
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(String email, Long postId) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 작성자 또는 게시판 관리자/호스트 여부 확인
        boolean isAuthor = post.getAuthorEmail().equals(email);
        boolean isHostOrAdmin = false;
        
        // 게시판 정보 조회
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board != null) {
            isHostOrAdmin = board.getHostEmail().equals(email);
            
            if (!isHostOrAdmin) {
                // 관리자 권한 확인
                BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, post.getBoardId());
                if (member != null && "ADMIN".equals(member.getRole())) {
                    isHostOrAdmin = true;
                }
            }
        }
        
        if (!isAuthor && !isHostOrAdmin) {
            throw new IllegalArgumentException("게시글 작성자 또는 게시판 관리자만 삭제할 수 있습니다.");
        }
        
        // 게시글 삭제 (소프트 삭제)
        postMapper.deletePost(postId, email);
        
        // 연결된 이미지 파일 처리
        // 실제 파일 삭제는 하지 않고 DB 레코드만 삭제
        postMapper.deletePostImages(postId);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostResponse getPostById(String email, Long postId) {
        // 게시글 조회
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = false;
        Board board = boardMapper.findBoardById(post.getBoardId());
        
        if (board != null) {
            isHost = board.getHostEmail().equals(email);
            
            if (!isHost) {
                BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, post.getBoardId());
                if (member != null && "ACTIVE".equals(member.getStatus())) {
                    isMember = true;
                }
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 게시글을 볼 수 있습니다.");
        }
        
        // 게시글 이미지 조회
        List<PostImage> postImages = postMapper.getPostImagesByPostId(postId);
        List<String> imageUrls = postImages.stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toList());
        
        // 조회수 증가
        postMapper.increaseViewCount(postId);
        
        // 응답 데이터 생성
        return PostResponse.builder()
                .id(post.getId())
                .boardId(post.getBoardId())
                .boardName(post.getBoardName())
                .authorEmail(post.getAuthorEmail())
                .authorName(post.getAuthorName())
                .authorNickname(post.getAuthorNickname())
                .authorProfileImage(post.getAuthorProfileImage())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .viewCount(post.getViewCount() + 1) // 증가된 조회수 반영
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .imageUrls(imageUrls)
                .isAuthor(post.getAuthorEmail().equals(email))
                .build();
    }

    /**
     * 게시판 내 게시글 목록 조회
     */
    public List<PostResponse> getPostsByBoardId(String email, Long boardId) {
        // 게시판 존재 여부 확인
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(email);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, boardId);
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 게시글 목록을 볼 수 있습니다.");
        }
        
        // 게시글 목록 조회
        List<Post> posts = postMapper.getPostsByBoardId(boardId);
        
        // 응답 데이터 생성
        return posts.stream().map(post -> {
            // 게시글 이미지 조회
            List<PostImage> postImages = postMapper.getPostImagesByPostId(post.getId());
            List<String> imageUrls = postImages.stream()
                    .map(PostImage::getImageUrl)
                    .collect(Collectors.toList());
            
            return PostResponse.builder()
                    .id(post.getId())
                    .boardId(post.getBoardId())
                    .boardName(post.getBoardName())
                    .authorEmail(post.getAuthorEmail())
                    .authorName(post.getAuthorName())
                    .authorNickname(post.getAuthorNickname())
                    .authorProfileImage(post.getAuthorProfileImage())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .imageUrls(imageUrls)
                    .isAuthor(post.getAuthorEmail().equals(email))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 게시글 검색
     */
    public List<PostResponse> searchPosts(String email, Long boardId, String keyword) {
        // 게시판 존재 여부 확인
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(email);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, boardId);
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 게시글을 검색할 수 있습니다.");
        }
        
        // 게시글 검색
        List<Post> posts = postMapper.searchPosts(boardId, keyword);
        
        // 응답 데이터 생성
        return posts.stream().map(post -> {
            // 게시글 이미지 조회
            List<PostImage> postImages = postMapper.getPostImagesByPostId(post.getId());
            List<String> imageUrls = postImages.stream()
                    .map(PostImage::getImageUrl)
                    .collect(Collectors.toList());
            
            return PostResponse.builder()
                    .id(post.getId())
                    .boardId(post.getBoardId())
                    .boardName(post.getBoardName())
                    .authorEmail(post.getAuthorEmail())
                    .authorName(post.getAuthorName())
                    .authorNickname(post.getAuthorNickname())
                    .authorProfileImage(post.getAuthorProfileImage())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .imageUrls(imageUrls)
                    .isAuthor(post.getAuthorEmail().equals(email))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 검색 및 필터링 기능이 적용된 게시글 목록 조회
     */
    public PagedPostResponse searchPostsWithFilters(String email, PostSearchRequest request) {
        // 게시판 존재 여부 확인
        Board board = boardMapper.findBoardById(request.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(email);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(email, request.getBoardId());
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 게시글 목록을 볼 수 있습니다.");
        }
        
        // 기본값 설정
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        String sortDirection = request.getSortDirection() != null ? request.getSortDirection() : "DESC";
        
        // 페이징 처리를 위한 offset 계산
        int offset = page * size;
        
        // 필터링된 게시글 목록 조회
        List<Post> posts = postMapper.findPostsWithFilters(
                request.getBoardId(),
                request.getKeyword(),
                request.getSearchField(),
                request.getStartDate(),
                request.getEndDate(),
                sortBy,
                sortDirection,
                offset,
                size
        );
        
        // 게시글 총 개수 조회
        int totalCount = postMapper.countPostsWithFilters(
                request.getBoardId(),
                request.getKeyword(),
                request.getSearchField(),
                request.getStartDate(),
                request.getEndDate()
        );
        
        // 총 페이지 수 계산
        int totalPages = (totalCount + size - 1) / size;
        
        // 응답 데이터 구성
        List<PostResponse> postResponses = posts.stream()
                .map(post -> {
                    // 게시글 이미지 조회
                    List<PostImage> postImages = postMapper.getPostImagesByPostId(post.getId());
                    List<String> imageUrls = postImages.stream()
                            .map(PostImage::getImageUrl)
                            .collect(Collectors.toList());
                    
                    return PostResponse.builder()
                            .id(post.getId())
                            .boardId(post.getBoardId())
                            .boardName(post.getBoardName())
                            .authorEmail(post.getAuthorEmail())
                            .authorName(post.getAuthorName())
                            .authorNickname(post.getAuthorNickname())
                            .authorProfileImage(post.getAuthorProfileImage())
                            .title(post.getTitle())
                            .content(post.getContent())
                            .createdAt(post.getCreatedAt())
                            .updatedAt(post.getUpdatedAt())
                            .viewCount(post.getViewCount())
                            .likeCount(post.getLikeCount())
                            .commentCount(post.getCommentCount())
                            .imageUrls(imageUrls)
                            .isAuthor(post.getAuthorEmail().equals(email))
                            .build();
                })
                .collect(Collectors.toList());
        
        return PagedPostResponse.builder()
                .content(postResponses)
                .pageNumber(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalElements(totalCount)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
