package com.tilink.til;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tilink.domain.material.Material;
import com.tilink.domain.material.MaterialRepository;
import com.tilink.domain.subject.Subject;
import com.tilink.domain.subject.SubjectRepository;
import com.tilink.domain.til.Til;
import com.tilink.domain.til.TilRepository;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import com.tilink.global.ai.AiServiceClient;
import com.tilink.global.ai.dto.AiSimilarTils;
import com.tilink.global.ai.dto.AiTilDraft;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import com.tilink.global.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * TIL 저장·조회·수정·삭제와 관련 학습·복습 알림 통합 테스트.
 *
 * <p>ai-service 는 {@code @MockitoBean} 으로 대체한다. 실제로 띄우면 테스트가 외부 서비스와
 * OpenAI 크레딧에 의존하게 되고, 임베딩 값이 매번 달라져 결과를 단언할 수 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TilIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TilRepository tilRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private AiServiceClient aiServiceClient;

    private User owner;
    private String ownerToken;
    private User stranger;
    private String strangerToken;
    private Material material;

    @BeforeEach
    void setUp() {
        owner = givenUser();
        ownerToken = jwtTokenProvider.createAccessToken(owner.getId(), owner.getEmail());
        stranger = givenUser();
        strangerToken = jwtTokenProvider.createAccessToken(stranger.getId(), stranger.getEmail());
        material = givenMaterial(owner, givenSubject("백엔드"));

        given(aiServiceClient.createEmbedding(anyString())).willReturn(embedding(0.1f));
    }

    // ==========================================
    // TIL 초안
    // ==========================================

    @Test
    void 자료로_TIL_초안을_생성하면_저장하지_않고_그대로_반환한다() throws Exception {
        given(aiServiceClient.generateTilDraft(material.getId())).willReturn(draft());

        mockMvc.perform(post("/api/materials/{id}/til-draft", material.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring AI 개요"))
                .andExpect(jsonPath("$.suggestedTags[0]").value("Spring AI"))
                .andExpect(jsonPath("$.keyConcepts[0].name").value("ChatClient"))
                .andExpect(jsonPath("$.outline[0]").value("ChatClient API"))
                .andExpect(jsonPath("$.sections[0].bodyMarkdown").value("### 사용법\n설명"))
                .andExpect(jsonPath("$.contentMarkdown").value("## 오늘 배운 내용\n\n오늘 배운 내용"))
                .andExpect(jsonPath("$.documentMarkdown").value("# Spring AI 개요\n\n## 목차\n\n- ChatClient API"))
                .andExpect(jsonPath("$.usedChunkCount").value(35));

        // 초안은 저장되지 않는다. 사용자가 고친 뒤 저장 API 로 보내는 흐름이다.
        // (개발 DB 를 공유하므로 전체 개수가 아니라 이 테스트가 만든 자료 기준으로 확인한다)
        assertThat(tilRepository.findByMaterialId(material.getId())).isEmpty();
    }

    @Test
    void 남의_자료로_초안을_요청하면_404_를_반환한다() throws Exception {
        mockMvc.perform(post("/api/materials/{id}/til-draft", material.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(strangerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATERIAL_NOT_FOUND"));
    }

    @Test
    void AI_서비스가_실패하면_초안_요청은_502_를_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.AI_SERVICE_ERROR))
                .given(aiServiceClient).generateTilDraft(anyString());

        mockMvc.perform(post("/api/materials/{id}/til-draft", material.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_ERROR"));
    }

    // ==========================================
    // 저장 / 조회 / 수정 / 삭제
    // ==========================================

    @Test
    void TIL_을_저장하면_본문_임베딩과_태그가_함께_저장된다() throws Exception {
        String body = createTil("Spring AI 정리", "본문", List.of("Spring AI", "RAG"));

        String id = JsonPath.read(body, "$.id");
        assertThat((String) JsonPath.read(body, "$.document")).startsWith("# Spring AI 정리");
        assertThat((Boolean) JsonPath.read(body, "$.embeddingReady")).isTrue();
        assertThat((List<String>) JsonPath.read(body, "$.tags")).containsExactly("Spring AI", "RAG");
        assertThat(tilRepository.findById(id).orElseThrow().getEmbedding()).isNotNull();
    }

    @Test
    void 임베딩_생성에_실패해도_TIL_저장은_성공한다() throws Exception {
        // 사용자가 작성한 본문을 AI 서비스 장애 때문에 잃으면 안 된다.
        willThrow(new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT))
                .given(aiServiceClient).createEmbedding(anyString());

        String body = createTil("임베딩 없는 TIL", "본문", List.of());

        assertThat((Boolean) JsonPath.read(body, "$.embeddingReady")).isFalse();
        assertThat(tilRepository.findById((String) JsonPath.read(body, "$.id")).orElseThrow().getEmbedding()).isNull();
    }

    @Test
    void 같은_태그를_쓰는_TIL_두_개를_저장해도_태그는_재사용된다() throws Exception {
        createTil("첫 번째", "본문", List.of("Docker"));
        createTil("두 번째", "본문", List.of("Docker", "Kafka"));

        mockMvc.perform(get("/api/tils").param("tag", "Docker")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void TIL_상세를_조회하면_최종_열람_일시가_갱신된다() throws Exception {
        String id = JsonPath.read(createTil("조회 테스트", "본문", List.of()), "$.id");
        assertThat(tilRepository.findById(id).orElseThrow().getLastReviewedAt()).isNull();

        mockMvc.perform(get("/api/tils/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("조회 테스트"))
                .andExpect(jsonPath("$.material.subjectName").value("백엔드"));

        assertThat(tilRepository.findById(id).orElseThrow().getLastReviewedAt()).isNotNull();
    }

    @Test
    void 남의_TIL_은_조회할_수_없다() throws Exception {
        String id = JsonPath.read(createTil("비공개", "본문", List.of()), "$.id");

        mockMvc.perform(get("/api/tils/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(strangerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIL_NOT_FOUND"));
    }

    @Test
    void 본문을_수정하면_임베딩을_다시_만들고_제목만_고치면_만들지_않는다() throws Exception {
        String id = JsonPath.read(createTil("원본", "원본 본문", List.of()), "$.id");

        patchTil(id, """
                {"title": "제목만 변경"}
                """);
        // 저장 시 1회. 제목만 바뀌었으므로 추가 호출은 없다.
        org.mockito.Mockito.verify(aiServiceClient, org.mockito.Mockito.times(1)).createEmbedding(anyString());

        patchTil(id, """
                {"content": "바뀐 본문"}
                """);
        org.mockito.Mockito.verify(aiServiceClient, org.mockito.Mockito.times(2)).createEmbedding(anyString());
    }

    @Test
    void 태그를_빈_배열로_수정하면_모두_제거된다() throws Exception {
        String id = JsonPath.read(createTil("태그 정리", "본문", List.of("Docker", "Kafka")), "$.id");

        patchTil(id, """
                {"tags": []}
                """);

        mockMvc.perform(get("/api/tils/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void TIL_을_삭제하면_목록에서_사라진다() throws Exception {
        String id = JsonPath.read(createTil("삭제 대상", "본문", List.of("Docker")), "$.id");

        mockMvc.perform(delete("/api/tils/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tils").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==========================================
    // 관련 학습
    // ==========================================

    @Test
    void 관련_학습은_태그가_겹치는_TIL_을_더_위로_올린다() throws Exception {
        String baseId = JsonPath.read(createTil("Spring AI", "본문", List.of("Spring AI", "RAG")), "$.id");
        String taggedId = JsonPath.read(createTil("RAG 파이프라인", "본문", List.of("RAG")), "$.id");
        String plainId = JsonPath.read(createTil("전혀 다른 글", "본문", List.of("Excel")), "$.id");

        // 벡터 유사도만 보면 plain 이 더 높지만, 태그가 겹치는 tagged 가 최종 1위여야 한다.
        given(aiServiceClient.findSimilarTils(any())).willReturn(new AiSimilarTils.Response(
                List.of(
                        new AiSimilarTils.SimilarTil(plainId, "전혀 다른 글", LocalDateTime.now(), 0.42),
                        new AiSimilarTils.SimilarTil(taggedId, "RAG 파이프라인", LocalDateTime.now(), 0.40)),
                2));

        mockMvc.perform(get("/api/tils/{id}/related", baseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatedTils[0].id").value(taggedId))
                .andExpect(jsonPath("$.relatedTils[0].matchedTags[0]").value("RAG"))
                // similarityScore 는 벡터 유사도 원본을 그대로 노출한다 (기획서 9장 형식).
                .andExpect(jsonPath("$.relatedTils[0].similarityScore").value(0.40))
                .andExpect(jsonPath("$.relatedTils[1].id").value(plainId))
                .andExpect(jsonPath("$.relatedTils[1].matchedTags.length()").value(0));
    }

    @Test
    void 임베딩이_없는_TIL_의_관련_학습은_409_를_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.AI_SERVICE_ERROR))
                .given(aiServiceClient).createEmbedding(anyString());
        String id = JsonPath.read(createTil("임베딩 없음", "본문", List.of()), "$.id");

        mockMvc.perform(get("/api/tils/{id}/related", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIL_EMBEDDING_NOT_READY"));
    }

    // ==========================================
    // 복습 알림
    // ==========================================

    @Test
    void 방금_작성한_TIL_은_복습_대상이_아니다() throws Exception {
        createTil("오늘 쓴 글", "본문", List.of());

        mockMvc.perform(get("/api/tils/needs-review").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 열람한_지_7일이_지난_TIL_은_복습_대상이다() throws Exception {
        String id = JsonPath.read(createTil("오래된 글", "본문", List.of()), "$.id");
        Til til = tilRepository.findById(id).orElseThrow();
        til.markReviewed(LocalDateTime.now().minusDays(10));
        tilRepository.flush();

        mockMvc.perform(get("/api/tils/needs-review").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("오래된 글"))
                .andExpect(jsonPath("$[0].daysSinceReview").value(10))
                .andExpect(jsonPath("$[0].neverReviewed").value(false));
    }

    // ==========================================
    // 헬퍼
    // ==========================================

    private String createTil(String title, String content, List<String> tags) throws Exception {
        String body = jsonMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("materialId", material.getId());
            put("title", title);
            put("content", content);
            put("document", "# " + title + "\n\n## 목차\n\n- 섹션");
            put("tags", tags);
        }});

        return mockMvc.perform(json(post("/api/tils"), ownerToken, body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void patchTil(String id, String body) throws Exception {
        mockMvc.perform(json(patch("/api/tils/{id}", id), ownerToken, body))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String token, String body) {
        return builder.header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private float[] embedding(float value) {
        float[] embedding = new float[1536];
        java.util.Arrays.fill(embedding, value);
        return embedding;
    }

    private AiTilDraft draft() {
        return new AiTilDraft(
                material.getId(), "Spring AI 개요",
                "오늘 배운 내용", List.of(new AiTilDraft.KeyConcept("ChatClient", "설명")), "실습",
                List.of("ChatClient API"),
                List.of(new AiTilDraft.Section("ChatClient API", "### 사용법\n설명")),
                List.of("새로 안 점"), List.of("어려웠던 점"), "회고",
                List.of("Spring AI", "RAG"),
                "## 오늘 배운 내용\n\n오늘 배운 내용",
                "# Spring AI 개요\n\n## 목차\n\n- ChatClient API",
                35, 35, "gpt-5-mini");
    }

    private User givenUser() {
        return userRepository.save(User.builder()
                .email("til-" + UUID.randomUUID() + "@tilink.dev")
                .password("{noop}password")
                .name("테스터")
                .build());
    }

    private Subject givenSubject(String name) {
        return subjectRepository.findByName(name)
                .orElseGet(() -> subjectRepository.save(Subject.builder().name(name).build()));
    }

    private Material givenMaterial(User user, Subject subject) {
        return materialRepository.save(Material.builder()
                .user(user)
                .subject(subject)
                .title("Spring AI 교안")
                .fileUrl(user.getId() + "/2026/09/" + UUID.randomUUID() + ".pdf")
                .originalFileName("spring-ai.pdf")
                .build());
    }
}
