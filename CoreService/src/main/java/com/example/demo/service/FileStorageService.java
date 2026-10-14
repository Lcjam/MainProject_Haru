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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FileStorageService {
    private static final long DEFAULT_MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Pattern UUID_IMAGE = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif|bmp|webp)");
    private static final String UUID_PART = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String IMAGE_EXTENSION = "(?i:jpg|jpeg|png|gif|bmp|webp)";
    private static final String LEGACY_COMPONENT = "[^/\\\\\\p{Cntrl}]+";
    private static final Pattern LEGACY_PROFILE_IMAGE = Pattern.compile(LEGACY_COMPONENT + "_" + UUID_PART + "\\." + IMAGE_EXTENSION);
    private static final Pattern LEGACY_NAMED_IMAGE = Pattern.compile(UUID_PART + "_" + LEGACY_COMPONENT + "\\." + IMAGE_EXTENSION);
    private static final Pattern LEGACY_STORED_PATH = Pattern.compile(
            "/profile-images/" + LEGACY_PROFILE_IMAGE.pattern()
                    + "|/board-files/board_[1-9][0-9]*/image/" + LEGACY_NAMED_IMAGE.pattern()
                    + "|/uploads/product_" + LEGACY_COMPONENT + "/product_[1-9][0-9]*/" + LEGACY_NAMED_IMAGE.pattern()
                    + "|/chat-images/chatroom_[1-9][0-9]*/" + UUID_PART + "\\." + IMAGE_EXTENSION);
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
    @Value("${UPLOAD_LEGACY_ROOT:${user.dir}/src/main/resources/static}")
    private String uploadLegacyRoot;
    @Value("${UPLOAD_LEGACY_CHAT_ROOT:${user.dir}/uploads}")
    private String uploadLegacyChatRoot;
    @Value("${file.default-profile-image:default-profile.png}")
    private String defaultProfileImage;
    @Value("${file.max-size:10485760}")
    private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

    private Path storageRoot;
    private Path legacyStorageRoot;
    private Path legacyChatStorageRoot;

    @PostConstruct
    public void init() {
        storageRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
        legacyStorageRoot = optionalRoot(uploadLegacyRoot);
        legacyChatStorageRoot = optionalRoot(uploadLegacyChatRoot);
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

    /** Reads current storage first, then the allowlisted legacy locations, without migrating DB paths. */
    public Path resolveStoredPath(String relativePath) {
        if (!isStoredPath(relativePath)) throw new IllegalArgumentException("허용되지 않은 파일 경로입니다.");
        String relative = relativePath.substring(1);
        for (Path root : storageRoots(relative)) {
            Path candidate = resolveChecked(root, relative);
            if (Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) return candidate;
        }
        return resolveChecked(storageRoot, relative);
    }

    public boolean deleteStoredFile(String relativePath) {
        try {
            if (!isStoredPath(relativePath)) throw new IllegalArgumentException("허용되지 않은 파일 경로입니다.");
            String relative = relativePath.substring(1);
            List<Path> candidates = new ArrayList<>();
            for (Path root : storageRoots(relative)) candidates.add(resolveChecked(root, relative));
            boolean deleted = false;
            // Delete duplicates too, so an old copy cannot reappear after replacement/deletion.
            for (Path file : candidates) {
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) deleted |= Files.deleteIfExists(file);
            }
            return deleted;
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("파일 삭제 거부: {}", relativePath, ex);
            return false;
        }
    }

    public Path getProfileImagePath(String filename) {
        String image = filename == null || filename.isBlank() ? defaultProfileImage : filename;
        if (!isProfileImageName(image)) throw new IllegalArgumentException("허용되지 않은 프로필 이미지입니다.");
        return resolveStoredPath("/profile-images/" + image);
    }

    public void createDefaultProfileImageIfNotExists() {
        Path defaultImage = resolveChecked("profile-images/" + defaultProfileImage);
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

    private Path optionalRoot(String root) {
        return root == null || root.isBlank() ? null : Paths.get(root).toAbsolutePath().normalize();
    }

    private List<Path> storageRoots(String relativePath) {
        List<Path> roots = new ArrayList<>();
        roots.add(storageRoot);
        if (legacyStorageRoot != null && !roots.contains(legacyStorageRoot)) roots.add(legacyStorageRoot);
        // main stored chat images under user.dir/uploads, outside its static resource directory.
        if (relativePath.startsWith("chat-images/") && legacyChatStorageRoot != null
                && !roots.contains(legacyChatStorageRoot)) roots.add(legacyChatStorageRoot);
        return roots;
    }

    private Path resolveChecked(String relativePath) {
        return resolveChecked(storageRoot, relativePath);
    }

    private Path resolveChecked(Path root, String relativePath) {
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) throw new IllegalArgumentException("저장 루트를 벗어난 경로입니다.");
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
                throw new IllegalArgumentException("심볼릭 링크 저장 루트는 사용할 수 없습니다.");
            }
        }
        Path parent = candidate.getParent();
        if (parent == null) throw new IllegalArgumentException("허용되지 않은 파일 경로입니다.");
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(current)) {
                    throw new IllegalArgumentException("심볼릭 링크 경로는 사용할 수 없습니다.");
                }
            }
        }
        if (Files.isSymbolicLink(candidate)) throw new IllegalArgumentException("심볼릭 링크 파일은 사용할 수 없습니다.");
        return candidate;
    }

    private void ensureDirectory(Path directory) {
        try {
            if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) {
                    throw new IllegalStateException("심볼릭 링크 또는 파일을 저장 경로로 사용할 수 없습니다.");
                }
            } else {
                Files.createDirectories(directory);
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
        if (!isSafePath(path)) return false;
        if (("/profile-images/" + defaultProfileImage).equals(path)) return true;
        if (LEGACY_STORED_PATH.matcher(path).matches()) return true;
        if (!STORED_PATH.matcher(path).matches()) return false;
        return UUID_IMAGE.matcher(path.substring(path.lastIndexOf('/') + 1)).matches();
    }

    private boolean isSafePath(String path) {
        return path != null && !path.contains("..") && path.indexOf('\\') < 0
                && path.chars().noneMatch(Character::isISOControl);
    }

    public boolean isLegacyBoardFileName(String filename) {
        return isSafePath(filename) && LEGACY_NAMED_IMAGE.matcher(filename).matches();
    }

    private boolean isProfileImageName(String filename) {
        return isSafePath(filename) && (defaultProfileImage.equals(filename)
                || UUID_IMAGE.matcher(filename).matches() || LEGACY_PROFILE_IMAGE.matcher(filename).matches());
    }

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
