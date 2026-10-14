package com.example.demo.service;

import com.example.demo.controller.ChatMessageController;
import com.example.demo.controller.ProfileController;
import com.example.demo.controller.Market.ProductController;
import com.example.demo.controller.board.BoardImageViewerController;
import com.example.demo.controller.board.PostImageController;
import com.example.demo.controller.board.PostImageViewerController;
import com.example.demo.mapper.*;
import com.example.demo.mapper.Market.*;
import com.example.demo.mapper.board.*;
import com.example.demo.model.Market.Product;
import com.example.demo.model.Market.ProductImage;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.Market.ImageUploadService;
import com.example.demo.service.Market.ProductService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LegacyImageCompatibilityTest {
    private static final String ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    @TempDir Path temporary;
    private Path current;
    private Path legacy;
    private Path legacyChat;
    private FileStorageService files;
    private final TokenUtils tokens = mock(TokenUtils.class);
    private final BoardMapper boards = mock(BoardMapper.class);
    private final BoardMemberMapper members = mock(BoardMemberMapper.class);
    private final BoardTempImageMapper images = mock(BoardTempImageMapper.class);
    private BoardImageAccess access;

    @BeforeEach
    void setup() throws Exception {
        current = temporary.resolve("current");
        legacy = temporary.resolve("static");
        legacyChat = temporary.resolve("uploads");
        files = storage(current, legacy, legacyChat);
        when(tokens.getEmailFromAuthHeader("Bearer owner")).thenReturn("owner@haru.test");
        when(tokens.getEmailFromAuthHeader("Bearer member")).thenReturn("member@haru.test");
        when(tokens.getEmailFromAuthHeader("Bearer outsider")).thenReturn("outsider@haru.test");
        when(tokens.getEmailFromAuthHeader("Bearer admin")).thenReturn("admin@haru.test");
        when(boards.findBoardById(30L)).thenReturn(Board.builder().id(30L).hostEmail("owner@haru.test")
                .status("ACTIVE").imagePath("/board-images/30/" + ID + ".png").build());
        when(members.findMemberByBoardIdAndUserEmail(30L, "member@haru.test"))
                .thenReturn(BoardMember.builder().status("ACTIVE").role("MEMBER").build());
        when(members.findMemberByBoardIdAndUserEmail(30L, "admin@haru.test"))
                .thenReturn(BoardMember.builder().status("ACTIVE").role("ADMIN").build());
        access = new BoardImageAccess(boards, members);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/profile-images/owner_123e4567-e89b-12d3-a456-426614174000.png",
            "/board-images/30/123e4567-e89b-12d3-a456-426614174000.png",
            "/board-files/board_30/image/123e4567-e89b-12d3-a456-426614174000_휴가 사진.PNG",
            "/uploads/product_owner@haru.test/product_42/123e4567-e89b-12d3-a456-426614174000_product photo.png",
            "/chat-images/chatroom_20/123e4567-e89b-12d3-a456-426614174000.PNG"
    })
    void legacyFormatsReadCurrentFirstAndDeleteBothCopies(String relative) throws Exception {
        Path old = write(legacy, relative);
        assertEquals(old, files.resolveStoredPath(relative));
        Path replacement = write(current, relative);
        assertEquals(replacement, files.resolveStoredPath(relative));
        assertTrue(files.deleteStoredFile(relative));
        assertFalse(Files.exists(old));
        assertFalse(Files.exists(replacement));
    }

    @Test
    void profileAndDefaultRemainPublicOnlyUnderExistingProfilePolicy() throws Exception {
        String name = "owner_" + ID + ".png";
        write(legacy, "/profile-images/" + name);
        UserMapper users = mock(UserMapper.class);
        when(users.countActiveProfilesByImage(name)).thenReturn(1);
        ProfileController controller = new ProfileController(mock(UserService.class), files, users);
        assertEquals(200, controller.getProfileImage(name).getStatusCode().value());
        when(users.countActiveProfilesByImage(name)).thenReturn(0);
        assertEquals(404, controller.getProfileImage(name).getStatusCode().value());
        Path oldDefault = write(legacy, "/profile-images/default-profile.png");
        Files.delete(current.resolve("profile-images/default-profile.png"));
        assertEquals(oldDefault, files.getProfileImagePath(null));
        assertEquals(200, controller.getDefaultProfileImage().getStatusCode().value());
        files.deleteProfileImage("default-profile.png");
        assertTrue(Files.exists(oldDefault));
        files.deleteProfileImage(name);
        assertFalse(Files.exists(legacy.resolve("profile-images/" + name)));
    }

    @Test
    void legacyBoardBannerAndPostRequireMembershipWithoutInventingUploaderRows() throws Exception {
        write(legacy, "/board-images/30/" + ID + ".png");
        BoardImageViewerController banner = new BoardImageViewerController(tokens, access, boards, files);
        assertEquals(200, banner.getImage("Bearer member", 30L, ID + ".png").getStatusCode().value());
        assertEquals(404, banner.getImage("Bearer outsider", 30L, ID + ".png").getStatusCode().value());
        assertEquals(401, banner.getImage(null, 30L, ID + ".png").getStatusCode().value());
        String name = ID + "_photo.png";
        write(legacy, "/board-files/board_30/image/" + name);
        PostImageViewerController viewer = new PostImageViewerController(files, tokens, access, images);
        assertEquals(200, viewer.getImage("Bearer member", 30L, "image", name).getStatusCode().value());
        assertEquals(404, viewer.getImage("Bearer outsider", 30L, "image", name).getStatusCode().value());
        assertEquals(401, viewer.getImage(null, 30L, "image", name).getStatusCode().value());
        write(current, "/board-files/board_30/image/" + ID + ".png");
        assertEquals(404, viewer.getImage("Bearer member", 30L, "image", ID + ".png").getStatusCode().value());
        assertEquals(404, viewer.getImage("Bearer member", 30L, "other", name).getStatusCode().value());
        verify(images, never()).insert(anyLong(), anyString(), anyString());
    }

    @Test
    void legacyPostDeleteWithoutUploaderIsLimitedToHostOrAdmin() throws Exception {
        String name = ID + "_photo.png";
        Path old = write(legacy, "/board-files/board_30/image/" + name);
        PostImageController controller = new PostImageController(files, tokens, access, images);
        assertEquals(404, controller.deletePostImage("Bearer member", 30L, name).getStatusCode().value());
        assertEquals(404, controller.deletePostImage("Bearer outsider", 30L, name).getStatusCode().value());
        assertTrue(Files.exists(old));
        assertEquals(200, controller.deletePostImage("Bearer owner", 30L, name).getStatusCode().value());
        assertFalse(Files.exists(old));
        assertEquals(404, controller.deletePostImage("Bearer owner", 30L, name).getStatusCode().value());
        write(legacy, "/board-files/board_30/image/" + name);
        assertEquals(200, controller.deletePostImage("Bearer admin", 30L, name).getStatusCode().value());
        verify(images, never()).delete(anyLong(), anyString());
    }

    @Test
    void legacyProductStillRequiresVisibleProduct() throws Exception {
        String relative = "/uploads/product_owner@haru.test/product_42/" + ID + "_photo.png";
        write(legacy, relative);
        ProductMapper products = mock(ProductMapper.class);
        ProductImageMapper productImages = mock(ProductImageMapper.class);
        when(productImages.findById(7L)).thenReturn(ProductImage.builder().id(7L).productId(42L).imagePath(relative).build());
        when(products.findById(42L, null)).thenReturn(Product.builder().id(42L).isVisible(true).build());
        ProductService service = new ProductService(products, mock(ProductRequestMapper.class), mock(ChatRoomMapper.class),
                productImages, mock(NotificationService.class), new ImageUploadService(files), mock(ChatService.class),
                mock(TransactionsMapper.class), mock(UserLocationMapper.class));
        ProductController controller = new ProductController(service, mock(JwtTokenProvider.class));
        assertEquals(200, controller.getProductImage(7L).getStatusCode().value());
        when(products.findById(42L, null)).thenReturn(Product.builder().id(42L).isVisible(false).build());
        assertEquals(404, controller.getProductImage(7L).getStatusCode().value());
    }

    @Test
    void legacyChatOutsideStaticRootStillRequiresChatParticipation() throws Exception {
        String name = ID + ".png";
        String relative = "/chat-images/chatroom_20/" + name;
        Path old = write(legacyChat, relative);
        ChatRoomMapper rooms = mock(ChatRoomMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        when(rooms.findChatRoomById(20, "member@haru.test")).thenReturn(ChatRoom.builder().chatroomId(20)
                .productId(42L).sellerEmail("owner@haru.test").requestEmail("member@haru.test").build());
        when(products.findById(42L, "owner@haru.test")).thenReturn(Product.builder().id(42L).isVisible(true).build());
        ChatMessageService service = new ChatMessageService(mock(ChatMessageMapper.class), rooms, products,
                mock(ProductRequestMapper.class), mock(UserMapper.class), null, files, null);
        ChatMessageController controller = new ChatMessageController(service, tokens, null, null);
        assertEquals(old, service.getChatImagePath(20, "member@haru.test", name));
        assertEquals(200, controller.getChatImage("Bearer member", 20, name).getStatusCode().value());
        assertEquals(404, controller.getChatImage("Bearer outsider", 20, name).getStatusCode().value());
        assertEquals(401, controller.getChatImage(null, 20, name).getStatusCode().value());
        assertTrue(files.deleteStoredFile(relative));
        assertFalse(Files.exists(old));
    }

    @Test
    void newUploadsUseOnlyCurrentRootAndGeneratedNames() throws Exception {
        Files.createDirectories(legacy);
        Files.createDirectories(legacyChat);
        var image = new MockMultipartFile("file", "old name.png", "image/png", PNG);
        String profile = files.storeProfileImage(image, "owner@haru.test");
        List<String> paths = List.of("/profile-images/" + profile,
                files.storeBoardFile(image, 30L, "image"),
                files.storeFile(image, "board-images/30", "old name.png"),
                files.storeFile(image, "product-images/42", "old name.png"),
                files.storeFile(image, "chat-images/chatroom_20", "old name.png"));
        for (String path : paths) {
            assertTrue(files.resolveStoredPath(path).startsWith(current));
            assertTrue(path.substring(path.lastIndexOf('/') + 1).matches("[0-9a-f-]{36}\\.png"));
            assertArrayEquals(PNG, Files.readAllBytes(files.resolveStoredPath(path)));
        }
        try (var oldFiles = Files.walk(legacy); var oldChats = Files.walk(legacyChat)) {
            assertEquals(0, oldFiles.filter(Files::isRegularFile).count());
            assertEquals(0, oldChats.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void absentLegacyRootsAreSkippedWithoutCreatingDirectories() {
        String path = "/board-images/30/" + ID + ".png";
        assertEquals(current.resolve(path.substring(1)), files.resolveStoredPath(path));
        assertFalse(Files.exists(legacy));
        assertFalse(Files.exists(legacyChat));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/profile-images/../owner_123e4567-e89b-12d3-a456-426614174000.png",
            "/profile-images/owner.._123e4567-e89b-12d3-a456-426614174000.png",
            "/profile-images/owner\n_123e4567-e89b-12d3-a456-426614174000.png",
            "/profile-images/owner\\_123e4567-e89b-12d3-a456-426614174000.png",
            "/board-files/board_30/image/123e4567-e89b-12d3-a456-426614174000_../../secret.png",
            "/uploads/product_../product_42/123e4567-e89b-12d3-a456-426614174000_photo.png",
            "/uploads/product_owner/product_42/123e4567-e89b-12d3-a456-426614174000_photo.svg",
            "/board-files/board_30/image/random_photo.png"
    })
    void invalidLegacyPathsCannotReadOrDelete(String path) {
        assertThrows(IllegalArgumentException.class, () -> files.resolveStoredPath(path));
        assertFalse(files.deleteStoredFile(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file", "directory", "root"})
    void legacySymlinksNeverEscapeOrDeleteTarget(String placement) throws Exception {
        String relative = "/profile-images/owner_" + ID + ".png";
        Path outside = temporary.resolve("outside");
        Path target = write(outside, relative);
        switch (placement) {
            case "file" -> {
                Files.createDirectories(legacy.resolve("profile-images"));
                Files.createSymbolicLink(legacy.resolve(relative.substring(1)), target);
            }
            case "directory" -> {
                Files.createDirectories(legacy);
                Files.createSymbolicLink(legacy.resolve("profile-images"), outside.resolve("profile-images"));
            }
            case "root" -> Files.createSymbolicLink(legacy, outside);
            default -> throw new AssertionError(placement);
        }
        assertThrows(IllegalArgumentException.class, () -> files.resolveStoredPath(relative));
        assertFalse(files.deleteStoredFile(relative));
        assertArrayEquals(PNG, Files.readAllBytes(target));
    }

    private FileStorageService storage(Path current, Path legacy, Path chat) {
        var storage = new FileStorageService();
        ReflectionTestUtils.setField(storage, "uploadRoot", current.toString());
        ReflectionTestUtils.setField(storage, "uploadLegacyRoot", legacy.toString());
        ReflectionTestUtils.setField(storage, "uploadLegacyChatRoot", chat.toString());
        ReflectionTestUtils.setField(storage, "defaultProfileImage", "default-profile.png");
        storage.init();
        return storage;
    }

    private Path write(Path root, String relative) throws Exception {
        Path path = root.resolve(relative.substring(1));
        Files.createDirectories(path.getParent());
        Files.write(path, PNG);
        return path;
    }
}
