package com.aiagent.integration;

import com.aiagent.security.JwtUtil;
import com.aiagent.service.FileStorageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileController 集成测试
 * 使用 @SpringBootTest + MockMvc 进行端到端 API 测试
 * 覆盖文件上传、列表、删除及安全防护
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("文件控制器集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.storage.path:/data/uploads}")
    private String storagePath;

    private String authToken;
    private Path testUploadDir;

    @BeforeEach
    void setUp() throws Exception {
        // 生成测试用 JWT Token
        authToken = "Bearer " + jwtUtil.generateToken(1L, "testuser", 100L);

        // 确保上传目录存在
        testUploadDir = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(testUploadDir);
    }

    // ==================== 文件上传测试 ====================

    @Test
    @Order(1)
    @DisplayName("上传文件 - 有效文件返回200")
    void testUpload_WithValidFile_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.txt",
                "text/plain",
                "This is a test file content for integration testing.".getBytes()
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .header("Authorization", authToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.originalName").value("test-document.txt"))
                .andExpect(jsonPath("$.data.path").isString())
                .andExpect(jsonPath("$.data.size").isNumber());
    }

    @Test
    @Order(2)
    @DisplayName("上传文件 - 超大文件返回错误")
    void testUpload_WithOversizedFile_ReturnsError() throws Exception {
        // 创建一个超过50MB限制的文件内容（使用较小的模拟数据，因为内存限制）
        // FileStorageService 中 MAX_FILE_SIZE = 50MB，这里构造一个超大字节数组来模拟
        // 实际测试中用 51MB 数据
        byte[] largeContent = new byte[51 * 1024 * 1024];
        java.util.Arrays.fill(largeContent, (byte) 'A');

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-file.txt",
                "text/plain",
                largeContent
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .header("Authorization", authToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(3)
    @DisplayName("上传文件 - 无效类型返回错误")
    void testUpload_WithInvalidType_ReturnsError() throws Exception {
        // 使用不在白名单中的 content type
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "EXE content here".getBytes()
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .header("Authorization", authToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(4)
    @DisplayName("上传文件 - 指定子目录成功")
    void testUpload_WithSubDir_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.csv",
                "text/csv",
                "id,name,value\n1,test,100".getBytes()
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .param("subDir", "reports")
                        .header("Authorization", authToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.originalName").value("report.csv"));
    }

    // ==================== 路径遍历防护测试 ====================

    @Test
    @Order(5)
    @DisplayName("上传文件 - 路径遍历攻击返回400")
    void testUpload_WithPathTraversal_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "path traversal test".getBytes()
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .param("subDir", "../../etc")
                        .header("Authorization", authToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(6)
    @DisplayName("上传文件 - 反斜杠路径遍历返回400")
    void testUpload_WithBackslashTraversal_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "backslash traversal test".getBytes()
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .param("subDir", "..\\windows\\system32")
                        .header("Authorization", authToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 文件列表测试 ====================

    @Test
    @Order(7)
    @DisplayName("获取文件列表 - 返回列表")
    void testListFiles_ReturnsList() throws Exception {
        mockMvc.perform(get("/v1/files/list")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("获取文件列表 - 指定子目录")
    void testListFiles_WithSubDir_ReturnsList() throws Exception {
        mockMvc.perform(get("/v1/files/list")
                        .param("subDir", "documents")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 文件删除测试 ====================

    @Test
    @Order(9)
    @DisplayName("删除文件 - 不存在的文件返回错误")
    void testDeleteFile_NotFound_ReturnsError() throws Exception {
        mockMvc.perform(delete("/v1/files/nonexistent-file-path.txt")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 未认证访问测试 ====================

    @Test
    @Order(10)
    @DisplayName("上传文件 - 未认证返回401")
    void testUpload_Unauthenticated_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "unauthorized upload".getBytes()
        );

        mockMvc.perform(multipart("/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("获取文件列表 - 未认证返回401")
    void testListFiles_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/v1/files/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @DisplayName("删除文件 - 未认证返回401")
    void testDeleteFile_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/v1/files/some-file.txt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
