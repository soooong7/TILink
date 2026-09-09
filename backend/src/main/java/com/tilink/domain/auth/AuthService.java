package com.tilink.domain.auth;

import com.tilink.domain.auth.dto.LoginRequest;
import com.tilink.domain.auth.dto.TokenResponse;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import com.tilink.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 인증 서비스.
 *
 * <p>사용자 유형이 교육생 하나뿐이라 {@code AuthenticationManager}/{@code UserDetailsService}
 * 를 거치지 않고 리포지토리와 인코더로 직접 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getEmail());
        return TokenResponse.of(accessToken, tokenProvider.getExpiresInSeconds());
    }
}
