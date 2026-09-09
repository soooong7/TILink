package com.tilink.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tilink.domain.material.MaterialRepository;
import com.tilink.domain.subject.Subject;
import com.tilink.domain.subject.SubjectRepository;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import com.tilink.global.security.JwtTokenProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학습자료 업로드·조회 통합 테스트.
 *
 * <p>실행 전에 저장소 루트에서 {@code docker-compose up -d} 로 PostgreSQL(pgvector)을 띄워야 한다.
 * DB 는 {@code @Transactional} 로 롤백되지만 <b>디스크에 쓴 파일은 롤백되지 않으므로</b>
 * 저장 루트를 build 아래 임시 디렉터리로 돌려놓고 클래스가 끝날 때 통째로 지운다.
 */
@SpringBootTest(properties = "storage.local.root=" + MaterialIntegrationTest.STORAGE_ROOT)
@AutoConfigureMockMvc
@Transactional
class MaterialIntegrationTest {

    static final String STORAGE_ROOT = "build/test-uploads";

    private static final byte[] PDF_BYTES = "%PDF-1.4 테스트용 더미 PDF".getBytes();

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private String ownerToken;
    private Subject infra;
    private Subject backend;

    @BeforeEach
    void setUp() {
        owner = givenUser();
        ownerToken = jwtTokenProvider.createAccessToken(owner.getId(), owner.getEmail());
        infra = givenSubject("인프라");
        backend = givenSubject("백엔드");
    }

    @AfterAll
    static void cleanUpUploadedFiles() throws Exception {
        Path root = Path.of(STORAGE_ROOT);
        if (!Files.exists(root)) {
            return;
        }
        // 디렉터리를 지우려면 안쪽 파일부터 지워야 하므로 깊은 경로부터 역순으로 순회한다.
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void PDF_를_업로드하면_자료가_저장되고_파일이_디스크에_기록된다() throws Exception {
        String body = mockMvc.perform(uploadRequest(ownerToken, infra.getId(), "Docker 정리", "docker.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Docker 정리"))
                .andExpect(jsonPath("$.originalFileName").value("docker.pdf"))
                .andExpect(jsonPath("$.subject.name").value(infra.getName()))
                // 업로드 직후 상태는 항상 UPLOADED 다. (분석 요청은 5단계 범위)
                .andExpect(jsonPath("$.processingStatus").value("UPLOADED"))
                .andReturn().getResponse().getContentAsString();

        String fileUrl = JsonPath.read(body, "$.fileUrl");
        // 저장 키는 절대경로가 아니라 "사용자ID/연/월/UUID.pdf" 형태의 상대 키다.
        assertThat(fileUrl).startsWith(owner.getId() + "/").endsWith(".pdf");
        assertThat(Files.exists(Path.of(STORAGE_ROOT).resolve(fileUrl))).isTrue();
        assertThat(Files.readAllBytes(Path.of(STORAGE_ROOT).resolve(fileUrl))).isEqualTo(PDF_BYTES);
    }

    @Test
    void 제목을_생략하면_파일명에서_확장자를_뗀_값이_제목이_된다() throws Exception {
        mockMvc.perform(uploadRequest(ownerToken, infra.getId(), null, "Kafka 기본 개념.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Kafka 기본 개념"));
    }

    @Test
    void PDF_가_아닌_파일을_업로드하면_400_을_반환한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "그냥 텍스트".getBytes());

        mockMvc.perform(multipart("/api/materials")
                        .file(file)
                        .param("subjectId", infra.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
    }

    @Test
    void 존재하지_않는_과목으로_업로드하면_404_를_반환하고_파일도_남기지_않는다() throws Exception {
        mockMvc.perform(uploadRequest(ownerToken, UUID.randomUUID().toString(), "제목", "a.pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));

        // 과목 검증이 파일 저장보다 먼저라 이 사용자의 디렉터리 자체가 만들어지지 않는다.
        assertThat(Files.exists(Path.of(STORAGE_ROOT).resolve(owner.getId()))).isFalse();
    }

    @Test
    void 토큰_없이_업로드하면_401_을_반환한다() throws Exception {
        mockMvc.perform(uploadRequest(null, infra.getId(), "제목", "a.pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 목록_조회는_내_자료만_돌려준다() throws Exception {
        upload(ownerToken, infra.getId(), "내 자료", "mine.pdf");

        User other = givenUser();
        String otherToken = jwtTokenProvider.createAccessToken(other.getId(), other.getEmail());
        upload(otherToken, infra.getId(), "남의 자료", "others.pdf");

        mockMvc.perform(get("/api/materials")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("내 자료"));
    }

    @Test
    void 목록_조회에_subjectId_를_주면_해당_과목만_돌려준다() throws Exception {
        upload(ownerToken, infra.getId(), "인프라 자료", "infra.pdf");
        upload(ownerToken, backend.getId(), "백엔드 자료", "backend.pdf");

        mockMvc.perform(get("/api/materials")
                        .param("subjectId", backend.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("백엔드 자료"));
    }

    @Test
    void 다른_사람의_자료를_상세_조회하면_404_를_반환한다() throws Exception {
        String materialId = upload(ownerToken, infra.getId(), "내 자료", "mine.pdf");

        User other = givenUser();
        String otherToken = jwtTokenProvider.createAccessToken(other.getId(), other.getEmail());

        mockMvc.perform(get("/api/materials/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATERIAL_NOT_FOUND"));

        // 주인이 조회하면 정상적으로 보인다.
        mockMvc.perform(get("/api/materials/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(materialId));

        assertThat(materialRepository.findByIdAndUserId(materialId, other.getId())).isEmpty();
    }

    @Test
    void 과목_목록은_인증된_사용자에게_이름순으로_내려간다() throws Exception {
        mockMvc.perform(get("/api/subjects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty());

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 테스트 데이터 준비
    // ==========================================

    private String upload(String token, String subjectId, String title, String fileName) throws Exception {
        String body = mockMvc.perform(uploadRequest(token, subjectId, title, fileName))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private MockMultipartHttpServletRequestBuilder uploadRequest(
            String token, String subjectId, String title, String fileName) {

        MockMultipartFile file =
                new MockMultipartFile("file", fileName, MediaType.APPLICATION_PDF_VALUE, PDF_BYTES);

        MockMultipartHttpServletRequestBuilder builder = multipart("/api/materials")
                .file(file)
                .param("subjectId", subjectId);
        if (title != null) {
            builder.param("title", title);
        }
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder;
    }

    // unique 제약이 걸린 컬럼은 테스트끼리 충돌하지 않도록 임의 값을 섞는다.
    private User givenUser() {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@tilink.test")
                .password("encoded-password")
                .name("테스트 교육생")
                .build());
    }

    private Subject givenSubject(String name) {
        return subjectRepository.save(
                Subject.builder().name(name + "-" + UUID.randomUUID()).build());
    }
}
