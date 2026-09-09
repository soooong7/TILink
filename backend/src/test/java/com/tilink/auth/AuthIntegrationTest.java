package com.tilink.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tilink.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입·로그인·인증 필터 통합 테스트.
 *
 * <p>실행 전에 저장소 루트에서 {@code docker-compose up -d} 로 PostgreSQL(pgvector)을 띄워야 한다.
 * {@code @Transactional} 이 붙어 있어 저장된 계정은 테스트 종료 시 모두 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    private static final String EMAIL = "songmi@tilink.dev";
    private static final String PASSWORD = "password1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void 회원가입에_성공하면_비밀번호가_BCrypt_로_암호화되어_저장된다() throws Exception {
        mockMvc.perform(signupRequest(EMAIL, PASSWORD, "이송미"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("이송미"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                // 응답에 비밀번호가 절대 섞여 나가면 안 된다.
                .andExpect(jsonPath("$.password").doesNotExist());

        String stored = userRepository.findByEmail(EMAIL).orElseThrow().getPassword();
        assertThat(stored).isNotEqualTo(PASSWORD);
        assertThat(stored).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, stored)).isTrue();
    }

    @Test
    void 이미_가입된_이메일로_회원가입하면_409_를_반환한다() throws Exception {
        mockMvc.perform(signupRequest(EMAIL, PASSWORD, "이송미")).andExpect(status().isCreated());

        mockMvc.perform(signupRequest(EMAIL, "otherpassword", "다른사람"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void 이메일_형식이_잘못되면_400_을_반환한다() throws Exception {
        mockMvc.perform(signupRequest("not-an-email", PASSWORD, "이송미"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 비밀번호가_8자_미만이면_400_을_반환한다() throws Exception {
        mockMvc.perform(signupRequest(EMAIL, "short", "이송미"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 로그인에_성공하면_액세스_토큰을_발급한다() throws Exception {
        givenSignedUpUser();

        mockMvc.perform(loginRequest(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void 비밀번호가_틀리면_401_을_반환한다() throws Exception {
        givenSignedUpUser();

        mockMvc.perform(loginRequest(EMAIL, "wrongpassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 가입하지_않은_이메일로_로그인하면_비밀번호_오류와_같은_응답을_반환한다() throws Exception {
        mockMvc.perform(loginRequest("nobody@tilink.dev", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 발급받은_토큰으로_내_정보를_조회할_수_있다() throws Exception {
        givenSignedUpUser();
        String accessToken = loginAndExtractToken();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("이송미"));
    }

    @Test
    void 토큰_없이_보호된_엔드포인트에_접근하면_401_을_반환한다() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 위조된_토큰으로_접근하면_401_을_반환한다() throws Exception {
        givenSignedUpUser();
        String accessToken = loginAndExtractToken();
        // 서명 부분 한 글자를 바꿔 검증에 실패하도록 만든다.
        String tampered = accessToken.substring(0, accessToken.length() - 1)
                + (accessToken.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void givenSignedUpUser() throws Exception {
        mockMvc.perform(signupRequest(EMAIL, PASSWORD, "이송미")).andExpect(status().isCreated());
    }

    private String loginAndExtractToken() throws Exception {
        String body = mockMvc.perform(loginRequest(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private RequestBuilder signupRequest(
            String email, String password, String name) {
        return post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "name": "%s"}
                        """.formatted(email, password, name));
    }

    private RequestBuilder loginRequest(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }
}
