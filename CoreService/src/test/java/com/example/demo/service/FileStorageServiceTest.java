package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    @TempDir
    Path root;

    @Test
    void generatedFilesSurviveRestartAndDefaultImageIsNotEmpty() throws IOException {
        FileStorageService first = storage(root, 1024);
        String id = first.storeProfileImage(image("avatar.png", "image/png", PNG), "owner@haru.test");
        String product = first.storeFile(image("product.png", "image/png", PNG), "product-images/42", "product.png");
        String chat = first.storeFile(image("chat.png", "image/png", PNG), "chat-images/chatroom_7", "chat.png");

        FileStorageService restarted = storage(root, 1024);
        Path stored = restarted.resolveStoredPath("/profile-images/" + id);

        assertArrayEquals(PNG, Files.readAllBytes(stored));
        assertFalse(id.contains("owner"));
        assertTrue(restarted.resolveStoredPath(product).startsWith(root));
        assertTrue(restarted.resolveStoredPath(chat).startsWith(root));
        assertTrue(Files.size(restarted.getProfileImagePath(null)) > 0);
    }

    @Test
    void rejectsTraversalDisguisedContentReportedSizeBypassAndSymlink() throws IOException {
        FileStorageService storage = storage(root, PNG.length);

        assertThrows(IllegalArgumentException.class, () -> storage.resolveStoredPath("/profile-images/../../outside.png"));
        assertThrows(IllegalArgumentException.class, () -> storage.storeFile(image("avatar.png", "image/png", PNG), "../profile-images", "avatar.png"));
        assertThrows(IllegalArgumentException.class, () -> storage.storeProfileImage(image("avatar.png", "image/png", "GIF89a".getBytes()), "owner@haru.test"));
        assertThrows(IllegalArgumentException.class, () -> storage.storeProfileImage(image("avatar.png", "image/jpeg", PNG), "owner@haru.test"));
        assertThrows(IllegalArgumentException.class, () -> storage.storeProfileImage(sizeLyingImage(), "owner@haru.test"));

        Path linked = root.resolve("profile-images/123e4567-e89b-12d3-a456-426614174000.png");
        Files.createSymbolicLink(linked, Files.createTempFile(root, "outside", ".png"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveStoredPath("/profile-images/123e4567-e89b-12d3-a456-426614174000.png"));
        assertFalse(storage.deleteStoredFile("/profile-images/123e4567-e89b-12d3-a456-426614174000.png"));
    }

    @Test
    void ancestorSymlinkAllowsUploadAndResolutionButRootOrChildSymlinkRejected() throws IOException {
        Path real = root.resolve("real");
        Path realUploads = real.resolve("uploads");
        Files.createDirectories(realUploads);
        Path link = root.resolve("link");
        Files.createSymbolicLink(link, real);
        Path storageDir = link.resolve("uploads");

        // 1. Root with ancestor symlink: upload and resolution should succeed
        FileStorageService storage = storage(storageDir, 1024);
        String profile = storage.storeProfileImage(image("avatar.png", "image/png", PNG), "owner@haru.test");
        Path resolved = storage.resolveStoredPath("/profile-images/" + profile);
        assertTrue(Files.exists(resolved));
        assertArrayEquals(PNG, Files.readAllBytes(resolved));

        // 2. Root itself is a symlink: must be rejected
        Path rootLink = root.resolve("root-link");
        Files.createSymbolicLink(rootLink, realUploads);
        assertThrows(IllegalStateException.class, () -> storage(rootLink, 1024));
        FileStorageService symlinkLegacyStorage = new FileStorageService();
        ReflectionTestUtils.setField(symlinkLegacyStorage, "uploadRoot", storageDir.toString());
        ReflectionTestUtils.setField(symlinkLegacyStorage, "uploadLegacyRoot", rootLink.toString());
        ReflectionTestUtils.setField(symlinkLegacyStorage, "defaultProfileImage", "default-profile.png");
        symlinkLegacyStorage.init();
        assertThrows(IllegalArgumentException.class,
                () -> symlinkLegacyStorage.resolveStoredPath("/profile-images/123e4567-e89b-12d3-a456-426614174000.png"));

        // 3. Child directory under root is a symlink: must be rejected
        Path boardDir = storageDir.resolve("board-images");
        Files.createDirectories(boardDir);
        Path linkedBoard = boardDir.resolve("30");
        Files.createSymbolicLink(linkedBoard, real);
        assertThrows(IllegalArgumentException.class,
                () -> storage.resolveStoredPath("/board-images/30/" + profile));
    }

    @Test
    void rejectsEmptyOversizedAndInvalidImagesAcrossUploadEntryPoints() {
        FileStorageService storage = storage(root, PNG.length);
        for (MockMultipartFile file : new MockMultipartFile[] {
                image("empty.png", "image/png", new byte[0]),
                image("large.png", "image/png", new byte[PNG.length + 1]),
                image("invalid.exe", "image/png", PNG)
        }) {
            assertThrows(IllegalArgumentException.class, () -> storage.storeProfileImage(file, "owner@haru.test"));
            assertThrows(IllegalArgumentException.class, () -> storage.storeFile(file, "board-images/7", file.getOriginalFilename()));
            assertThrows(IllegalArgumentException.class, () -> storage.storeBoardFile(file, 7L, "image"));
        }
    }

    @Test
    void copiesTheClasspathDefaultImageAndNeverDeletesIt() throws IOException {
        FileStorageService storage = storage(root, 1024);
        try (InputStream expected = getClass().getResourceAsStream("/static/images/default-profile.png")) {
            assertArrayEquals(expected.readAllBytes(), Files.readAllBytes(storage.getProfileImagePath(null)));
        }
        storage.deleteProfileImage("default-profile.png");
        assertTrue(Files.size(storage.getProfileImagePath(null)) > 0);
    }

    @Test
    void defaultImageCanBeCopiedFromAJarResource() throws Exception {
        String resource = "static/images/default-profile.png";
        String fixtureName = JarStorage.class.getName();
        String fixtureEntry = fixtureName.replace('.', '/') + ".class";
        Path jar = root.resolve("image-fixture.jar");
        byte[] expected;
        try (InputStream source = getClass().getResourceAsStream("/" + resource)) {
            expected = source.readAllBytes();
        }
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(resource));
            output.write(expected);
            output.closeEntry();
            output.putNextEntry(new JarEntry(fixtureEntry));
            try (InputStream source = getClass().getResourceAsStream("/" + fixtureEntry)) {
                source.transferTo(output);
            }
            output.closeEntry();
        }
        // The subclass lives in the JAR, so getClass().getResourceAsStream uses its JAR resource.
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, getClass().getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!fixtureName.equals(name)) return super.loadClass(name, resolve);
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) loaded = findClass(name);
                if (resolve) resolveClass(loaded);
                return loaded;
            }
            @Override public URL getResource(String name) {
                return resource.equals(name) ? findResource(name) : super.getResource(name);
            }
        }) {
            assertTrue(loader.getResource(resource).toString().startsWith("jar:"));
            FileStorageService storage = (FileStorageService) loader.loadClass(fixtureName).getConstructor().newInstance();
            ReflectionTestUtils.setField(storage, "uploadRoot", root.resolve("jar-storage").toString());
            ReflectionTestUtils.setField(storage, "defaultProfileImage", "default-profile.png");
            storage.init();
            assertArrayEquals(expected, Files.readAllBytes(storage.getProfileImagePath(null)));
        }
    }

    public static class JarStorage extends FileStorageService { }

    private FileStorageService storage(Path root, long maxSize) {
        FileStorageService storage = new FileStorageService();
        ReflectionTestUtils.setField(storage, "uploadRoot", root.toString());
        ReflectionTestUtils.setField(storage, "defaultProfileImage", "default-profile.png");
        ReflectionTestUtils.setField(storage, "maxFileSize", maxSize);
        storage.init();
        return storage;
    }

    private MockMultipartFile image(String name, String type, byte[] content) {
        return new MockMultipartFile("file", name, type, content);
    }

    private MultipartFile sizeLyingImage() {
        byte[] content = new byte[PNG.length + 1];
        System.arraycopy(PNG, 0, content, 0, PNG.length);
        return new MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return "avatar.png"; }
            @Override public String getContentType() { return "image/png"; }
            @Override public boolean isEmpty() { return false; }
            @Override public long getSize() { return 1; }
            @Override public byte[] getBytes() { return content; }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
            @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), content); }
        };
    }
}
