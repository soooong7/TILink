package com.tilink.domain.user;

import com.tilink.domain.user.dto.SignupRequest;
import com.tilink.domain.user.dto.UserResponse;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 교육생 계정 서비스.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입. 비밀번호는 BCrypt 로 해싱해서 저장한다.
     *
     * <p>이메일 중복은 여기서 먼저 확인하지만, users.email 에 UNIQUE 제약이 걸려 있어
     * 동시 요청이 겹치면 DB 제약이 최종 방어선이 된다.
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build());

        return UserResponse.from(user);
    }

    // 인증된 사용자 본인 정보 조회.
    public UserResponse getById(String userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
