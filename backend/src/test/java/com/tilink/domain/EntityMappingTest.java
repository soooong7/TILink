package com.tilink.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tilink.domain.material.Material;
import com.tilink.domain.material.MaterialChunk;
import com.tilink.domain.material.MaterialChunkRepository;
import com.tilink.domain.material.MaterialRepository;
import com.tilink.domain.material.ProcessingStatus;
import com.tilink.domain.subject.Subject;
import com.tilink.domain.subject.SubjectRepository;
import com.tilink.domain.tag.Tag;
import com.tilink.domain.tag.TagRepository;
import com.tilink.domain.til.Til;
import com.tilink.domain.til.TilRepository;
import com.tilink.domain.til.TilTag;
import com.tilink.domain.til.TilTagRepository;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 엔티티 매핑 검증 통합 테스트.
 *
 * <p>실행 전에 저장소 루트에서 {@code docker-compose up -d} 로 PostgreSQL(pgvector)을 띄워야 한다.
 *
 * <p>{@code @Transactional} 이 붙어 있어 테스트가 끝나면 저장한 데이터는 모두 롤백된다.
 */
@SpringBootTest
@Transactional
class EntityMappingTest {

    private static final int EMBEDDING_DIMENSION = 1536;

    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MaterialChunkRepository materialChunkRepository;
    @Autowired private TilRepository tilRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private TilTagRepository tilTagRepository;

    @Test
    void 학습자료_청크의_임베딩이_vector_컬럼에_저장되고_그대로_조회된다() {
        Material material = givenMaterial();
        float[] embedding = sampleEmbedding(0.1f);

        MaterialChunk saved = materialChunkRepository.save(
                MaterialChunk.builder()
                        .material(material)
                        .chunkIndex(0)
                        .content("Docker 는 컨테이너 기반 가상화 도구다.")
                        .embedding(embedding)
                        .build());

        // 영속성 컨텍스트를 비워, 캐시가 아니라 DB에서 실제로 다시 읽어오게 한다.
        entityManager.flush();
        entityManager.clear();

        MaterialChunk found = materialChunkRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmbedding()).hasSize(EMBEDDING_DIMENSION);
        assertThat(found.getEmbedding()).isEqualTo(embedding);
        assertThat(found.getMaterial().getId()).isEqualTo(material.getId());
        assertThat(found.getChunkIndex()).isZero();
    }

    @Test
    void 코사인_유사도_연산자로_유사한_청크를_찾을_수_있다() {
        Material material = givenMaterial();
        float[] target = sampleEmbedding(0.1f);

        materialChunkRepository.save(MaterialChunk.builder()
                .material(material).chunkIndex(0).content("가까운 청크").embedding(target).build());
        materialChunkRepository.save(MaterialChunk.builder()
                .material(material).chunkIndex(1).content("먼 청크").embedding(oppositeEmbedding(0.1f)).build());

        entityManager.flush();
        entityManager.clear();

        // pgvector 의 <=> 는 코사인 거리(0에 가까울수록 유사)를 계산하는 연산자다.
        // JPQL 로는 표현할 수 없어 네이티브 쿼리를 쓴다.
        @SuppressWarnings("unchecked")
        List<String> contents = entityManager
                .createNativeQuery("""
                        SELECT content FROM material_chunks
                        WHERE material_id = :materialId
                        ORDER BY embedding <=> CAST(:target AS vector)
                        """)
                .setParameter("materialId", material.getId())
                .setParameter("target", toVectorLiteral(target))
                .getResultList();

        assertThat(contents).containsExactly("가까운 청크", "먼 청크");
    }

    @Test
    void TIL_은_임베딩과_최종열람일시가_비어있는_상태로_저장될_수_있다() {
        Material material = givenMaterial();

        Til saved = tilRepository.save(Til.builder()
                .user(material.getUser())
                .material(material)
                .title("Docker 기본 개념")
                .content("## 오늘 배운 내용\n- 컨테이너와 이미지의 차이")
                .build());

        entityManager.flush();
        entityManager.clear();

        Til found = tilRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmbedding()).isNull();
        assertThat(found.getLastReviewedAt()).isNull();
        // Auditing 으로 자동 입력되는 값
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void TIL_과_태그를_복합키로_연결할_수_있다() {
        Material material = givenMaterial();
        Til til = tilRepository.save(Til.builder()
                .user(material.getUser()).material(material)
                .title("Docker 기본 개념").content("본문").build());
        Tag tag = tagRepository.save(Tag.builder().name("Docker-" + UUID.randomUUID()).build());

        tilTagRepository.save(new TilTag(til, tag));

        entityManager.flush();
        entityManager.clear();

        List<TilTag> links = tilTagRepository.findByIdTilId(til.getId());
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getId().getTagId()).isEqualTo(tag.getId());
    }

    @Test
    void 학습자료는_UPLOADED_상태로_생성되고_상태를_변경할_수_있다() {
        Material material = givenMaterial();
        assertThat(material.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);

        material.changeProcessingStatus(ProcessingStatus.DONE);
        entityManager.flush();
        entityManager.clear();

        Material found = materialRepository.findById(material.getId()).orElseThrow();
        assertThat(found.getProcessingStatus()).isEqualTo(ProcessingStatus.DONE);
    }

    // ==========================================
    // 테스트 데이터 준비
    // ==========================================

    private Material givenMaterial() {
        // unique 제약이 걸린 컬럼은 테스트끼리 충돌하지 않도록 임의 값을 섞는다.
        User user = userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@tilink.test")
                .password("encoded-password")
                .name("테스트 교육생")
                .build());
        Subject subject = subjectRepository.save(
                Subject.builder().name("인프라-" + UUID.randomUUID()).build());

        return materialRepository.save(Material.builder()
                .user(user)
                .subject(subject)
                .title("Docker 컨테이너 및 Docker Compose")
                .fileUrl("/files/docker.pdf")
                .build());
    }

    /** 모든 값이 같은 1536차원 벡터 */
    private float[] sampleEmbedding(float value) {
        float[] embedding = new float[EMBEDDING_DIMENSION];
        java.util.Arrays.fill(embedding, value);
        return embedding;
    }

    /** 방향이 정반대인 벡터 (코사인 거리가 가장 먼 경우) */
    private float[] oppositeEmbedding(float value) {
        return sampleEmbedding(-value);
    }

    /** float[] 를 pgvector 리터럴 문자열 "[0.1,0.1,...]" 로 변환한다. */
    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }
}
