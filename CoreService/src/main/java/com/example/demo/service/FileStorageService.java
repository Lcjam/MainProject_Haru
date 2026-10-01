package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FileStorageService {
    private static final long DEFAULT_MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Pattern UUID_IMAGE = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif|bmp|webp)");
    private static final Pattern BOARD_IMAGE_DIRECTORY = Pattern.compile("board-images/[1-9][0-9]*");
    private static final Pattern PRODUCT_IMAGE_DIRECTORY = Pattern.compile("product-images/[1-9][0-9]*");
    private static final Pattern CHAT_IMAGE_DIRECTORY = Pattern.compile("chat-images/chatroom_[1-9][0-9]*");
    private static final Pattern BOARD_FILE_DIRECTORY = Pattern.compile("board-files/board_[1-9][0-9]*/image");
    private static final Pattern STORED_PATH = Pattern.compile(
            "/profile-images/[0-9a-f-]+\\.(?:jpg|jpeg|png|gif|bmp|webp)"
                    + "|/board-images/[1-9][0-9]*/[0-9a-f-]+\\.(?:jpg|jpeg|png|gif|bmp|webp)"
                    + "|/product-images/[1-9][0-9]*/[0-9a-f-]+\\.(?:jpg|jpeg|png|gif|bmp|webp)"
                    + "|/chat-images/chatroom_[1-9][0-9]*/[0-9a-f-]+\\.(?:jpg|jpeg|png|gif|bmp|webp)"
                    + "|/board-files/board_[1-9][0-9]*/image/[0-9a-f-]+\\.(?:jpg|jpeg|png|gif|bmp|webp)");

    @Value("${UPLOAD_ROOT:${user.home}/haru/uploads}")
    private String uploadRoot;
    @Value("${file.default-profile-image:default-profile.png}")
    private String defaultProfileImage;
    @Value("${file.max-size:10485760}")
    private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

    private Path storageRoot;

    @PostConstruct
    public void init() {
        storageRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
        ensureDirectory(storageRoot);
        directory("profile-images");
        createDefaultProfileImageIfNotExists();
    }

    public String storeProfileImage(MultipartFile file, String userEmail) {
        return storeImage(file, "profile-images", file.getOriginalFilename());
    }

    public String storeFile(MultipartFile file, String directory, String originalFilename) {
        String safeDirectory = safeDirectory(directory);
        return "/" + safeDirectory + "/" + storeImage(file, safeDirectory, originalFilename);
    }

    public String storeBoardFile(MultipartFile file, Long boardId, String fileType) {
        if (boardId == null || boardId < 1 || !"image".equals(fileType)) {
            throw new IllegalArgumentException("허용되지 않은 게시판 파일 경로입니다.");
        }
        String directory = "board-files/board_" + boardId + "/image";
        return "/" + directory + "/" + storeImage(file, directory, file.getOriginalFilename());
    }

    public boolean deleteBoardFile(String filePath) {
        return deleteStoredFile(filePath);
    }

    public void deleteProfileImage(String filename) {
        if (filename != null && !filename.equals(defaultProfileImage)) deleteStoredFile("/profile-images/" + filename);
    }

    /** Resolves only a generated relative path and rejects traversal and symbolic links. */
    public Path resolveStoredPath(String relativePath) {
        if (!isStoredPath(relativePath)) throw new IllegalArgumentException("허용되지 않은 파일 경로입니다.");
        return resolveChecked(relativePath.substring(1));
    }

    public boolean deleteStoredFile(String relativePath) {
        try {
            Path file = resolveStoredPath(relativePath);
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(file)) {
                throw new IllegalArgumentException("심볼릭 링크 파일은 삭제할 수 없습니다.");
            }
            return Files.deleteIfExists(file);
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("파일 삭제 거부: {}", relativePath, ex);
            return false;
        }
    }

    public Path getProfileImagePath(String filename) {
        String image = filename == null || filename.isBlank() ? defaultProfileImage : filename;
        if (!isProfileImageName(image)) throw new IllegalArgumentException("허용되지 않은 프로필 이미지입니다.");
        return resolveChecked("profile-images/" + image);
    }

    public void createDefaultProfileImageIfNotExists() {
        Path defaultImage = getProfileImagePath(defaultProfileImage);
        try {
            if (Files.exists(defaultImage, LinkOption.NOFOLLOW_LINKS) && Files.size(defaultImage) > 0) return;
            try (InputStream source = getClass().getResourceAsStream("/static/images/default-profile.png")) {
                if (source == null) throw new IllegalStateException("기본 프로필 이미지를 찾을 수 없습니다.");
                Files.copy(source, defaultImage, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(defaultImage) == 0) throw new IllegalStateException("기본 프로필 이미지가 비어 있습니다.");
        } catch (IOException ex) {
            throw new IllegalStateException("기본 프로필 이미지를 만들 수 없습니다.", ex);
        }
    }

    public String getDefaultProfileImageName() { return defaultProfileImage; }

    private String storeImage(MultipartFile file, String relativeDirectory, String originalFilename) {
        if (file == null) throw new IllegalArgumentException("저장할 파일이 비어 있습니다.");
        String extension = extension(originalFilename);
        Path directory = directory(relativeDirectory);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, ".upload-", ".tmp");
            byte[] header = copyWithinLimit(file.getInputStream(), temporary);
            ImageType type = ImageType.from(header);
            if (type == null || !type.matchesExtension(extension) || !type.mime.equals(normalizeMime(file.getContentType()))) {
                throw new IllegalArgumentException("파일 내용, 확장자, MIME 형식이 일치하지 않습니다.");
            }
            String filename = UUID.randomUUID() + "." + type.extension;
            Files.move(temporary, resolveChecked(relativeDirectory + "/" + filename));
            temporary = null;
            return filename;
        } catch (IOException ex) {
            throw new IllegalStateException("파일 저장 중 오류가 발생했습니다.", ex);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { log.warn("임시 업로드 파일을 지우지 못했습니다: {}", temporary); }
        }
    }

    private byte[] copyWithinLimit(InputStream input, Path target) throws IOException {
        long limit = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        byte[] header = new byte[12];
        int headerLength = 0;
        byte[] buffer = new byte[8192];
        long total = 0;
        try (input; var output = Files.newOutputStream(target)) {
            for (int read; (read = input.read(buffer)) != -1; ) {
                total += read;
                if (total > limit) throw new IllegalArgumentException("파일 크기가 제한을 초과합니다.");
                int copied = Math.min(read, header.length - headerLength);
                if (copied > 0) { System.arraycopy(buffer, 0, header, headerLength, copied); headerLength += copied; }
                output.write(buffer, 0, read);
            }
        }
        if (total == 0) throw new IllegalArgumentException("저장할 파일이 비어 있습니다.");
        byte[] actualHeader = new byte[headerLength];
        System.arraycopy(header, 0, actualHeader, 0, headerLength);
        return actualHeader;
    }

    private String safeDirectory(String directory) {
        if ("profile-images".equals(directory) || BOARD_IMAGE_DIRECTORY.matcher(directory).matches()
                || PRODUCT_IMAGE_DIRECTORY.matcher(directory).matches() || CHAT_IMAGE_DIRECTORY.matcher(directory).matches()
                || BOARD_FILE_DIRECTORY.matcher(directory).matches()) return directory;
        throw new IllegalArgumentException("허용되지 않은 저장 디렉토리입니다.");
    }

    private Path directory(String relativeDirectory) {
        String safeDirectory = safeDirectory(relativeDirectory);
        Path current = storageRoot;
        for (String part : safeDirectory.split("/")) { current = current.resolve(part); ensureDirectory(current); }
        return current;
    }

    private Path resolveChecked(String relativePath) {
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(storageRoot)) {
            throw new IllegalArgumentException("심볼릭 링크 저장 루트는 사용할 수 없습니다.");
        }
        Path candidate = storageRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(storageRoot)) throw new IllegalArgumentException("저장 루트를 벗어난 경로입니다.");
        Path parent = candidate.getParent();
        if (parent == null) throw new IllegalArgumentException("허용되지 않은 파일 경로입니다.");
        Path current = storageRoot;
        for (Path part : storageRoot.relativize(parent)) {
            current = current.resolve(part);
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(current)) throw new IllegalArgumentException("심볼릭 링크 경로는 사용할 수 없습니다.");
        }
        if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(candidate)) throw new IllegalArgumentException("심볼릭 링크 파일은 사용할 수 없습니다.");
        return candidate;
    }

    private void ensureDirectory(Path directory) {
        try {
            Path current = directory.getRoot();
            for (Path part : directory) {
                current = current.resolve(part);
                if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                    if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(current)) throw new IllegalStateException("심볼릭 링크 또는 파일을 저장 경로로 사용할 수 없습니다.");
                } else {
                    Files.createDirectory(current);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("저장 디렉토리를 만들 수 없습니다.", ex);
        }
    }

    private String extension(String filename) {
        if (filename == null) throw new IllegalArgumentException("유효한 파일 이름이 아닙니다.");
        int index = filename.lastIndexOf('.');
        if (index < 1 || index == filename.length() - 1) throw new IllegalArgumentException("유효한 파일 이름이 아닙니다.");
        String extension = filename.substring(index + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("jpg|jpeg|png|gif|bmp|webp")) throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다.");
        return extension;
    }

    private String normalizeMime(String mime) { return mime == null ? "" : mime.toLowerCase(Locale.ROOT).trim(); }

    private boolean isStoredPath(String path) {
        if (path == null) return false;
        if (("/profile-images/" + defaultProfileImage).equals(path)) return true;
        if (!STORED_PATH.matcher(path).matches()) return false;
        return UUID_IMAGE.matcher(path.substring(path.lastIndexOf('/') + 1)).matches();
    }

    private boolean isProfileImageName(String filename) { return defaultProfileImage.equals(filename) || UUID_IMAGE.matcher(filename).matches(); }

    private enum ImageType {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), GIF("gif", "image/gif"), BMP("bmp", "image/bmp"), WEBP("webp", "image/webp");
        private final String extension;
        private final String mime;
        ImageType(String extension, String mime) { this.extension = extension; this.mime = mime; }
        private boolean matchesExtension(String candidate) { return extension.equals(candidate) || this == JPEG && "jpeg".equals(candidate); }
        private static ImageType from(byte[] header) {
            if (startsWith(header, 0xFF, 0xD8, 0xFF)) return JPEG;
            if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
            if (startsWith(header, 'G', 'I', 'F', '8') && header.length > 5 && (header[4] == '7' || header[4] == '9') && header[5] == 'a') return GIF;
            if (startsWith(header, 'B', 'M')) return BMP;
            if (startsWith(header, 'R', 'I', 'F', 'F') && header.length >= 12 && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return WEBP;
            return null;
        }
        private static boolean startsWith(byte[] bytes, int... values) {
            if (bytes.length < values.length) return false;
            for (int i = 0; i < values.length; i++) if ((bytes[i] & 0xFF) != values[i]) return false;
            return true;
        }
    }
}
