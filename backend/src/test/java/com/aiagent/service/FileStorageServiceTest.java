package com.aiagent.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileStorageService 单元测试
 * 测试文件上传、删除、列表等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文件存储服务测试")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws IOException {
        fileStorageService = new FileStorageService();
        // 使用反射设置 storagePath 和初始化
        try {
            var field = FileStorageService.class.getDeclaredField("storagePath");
            field.setAccessible(true);
            field.set(fileStorageService, tempDir.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        fileStorageService.init();
    }

    @Test
    @DisplayName("上传文件 - 成功")
    void testUploadFile() throws IOException {
        // 准备测试文件
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        FileStorageService.FileInfo result = fileStorageService.upload(file, "uploads");

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("test.txt", result.getOriginalName());
        assertTrue(result.getPath().contains("test.txt"));
        assertEquals("text/plain", result.getContentType());
        assertEquals(13, result.getSize());
        assertNotNull(result.getUploadedAt());
    }

    @Test
    @DisplayName("上传文件 - 空文件抛出异常")
    void testUploadFile_EmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.upload(file, null)
        );
    }

    @Test
    @DisplayName("上传文件 - null文件抛出异常")
    void testUploadFile_NullFile() {
        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.upload(null, null)
        );
    }

    @Test
    @DisplayName("上传文件 - 不支持的文件类型抛出异常")
    void testUploadFile_UnsupportedType() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "executable content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.upload(file, null)
        );
    }

    @Test
    @DisplayName("上传文件 - 文件过大抛出异常")
    void testUploadFile_TooLarge() {
        // 创建超过50MB的文件内容
        byte[] largeContent = new byte[51 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.upload(file, null)
        );
    }

    @Test
    @DisplayName("上传文件 - 无子目录成功")
    void testUploadFile_NoSubDir() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                "{}".getBytes()
        );

        FileStorageService.FileInfo result = fileStorageService.upload(file, null);

        assertNotNull(result);
        assertEquals("test.json", result.getOriginalName());
    }

    @Test
    @DisplayName("删除文件 - 成功")
    void testDeleteFile() throws IOException {
        // 先上传文件
        MultipartFile file = new MockMultipartFile(
                "file",
                "to_delete.txt",
                "text/plain",
                "delete me".getBytes()
        );

        FileStorageService.FileInfo uploaded = fileStorageService.upload(file, "temp");

        // 删除文件
        assertDoesNotThrow(() -> fileStorageService.delete(uploaded.getPath()));
    }

    @Test
    @DisplayName("删除文件 - 路径为空抛出异常")
    void testDeleteFile_EmptyPath() {
        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.delete("")
        );
    }

    @Test
    @DisplayName("删除文件 - null路径抛出异常")
    void testDeleteFile_NullPath() {
        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.delete(null)
        );
    }

    @Test
    @DisplayName("删除文件 - 路径遍历攻击抛出异常")
    void testDeleteFile_PathTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.delete("../../etc/passwd")
        );
    }

    @Test
    @DisplayName("删除文件 - 文件不存在抛出异常")
    void testDeleteFile_NotFound() {
        assertThrows(RuntimeException.class, () ->
                fileStorageService.delete("nonexistent/file.txt")
        );
    }

    @Test
    @DisplayName("列出文件 - 成功")
    void testListFiles() throws IOException {
        // 上传几个文件
        MultipartFile file1 = new MockMultipartFile(
                "file1", "doc1.txt", "text/plain", "content1".getBytes()
        );
        MultipartFile file2 = new MockMultipartFile(
                "file2", "doc2.txt", "text/plain", "content2".getBytes()
        );

        fileStorageService.upload(file1, "list_test");
        fileStorageService.upload(file2, "list_test");

        List<FileStorageService.FileInfo> files = fileStorageService.listFiles("list_test");

        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    @DisplayName("列出文件 - 不存在的目录返回空列表")
    void testListFiles_EmptyDir() {
        List<FileStorageService.FileInfo> files = fileStorageService.listFiles("nonexistent_dir");

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    @DisplayName("列出文件 - 路径遍历攻击抛出异常")
    void testListFiles_PathTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.listFiles("../../etc")
        );
    }
}
