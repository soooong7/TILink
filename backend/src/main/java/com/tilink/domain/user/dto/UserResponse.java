package com.tilink.domain.user.dto;

import com.tilink.domain.user.User;
import java.time.LocalDateTime;

// 사용자 정보 응답. 비밀번호는 어떤 경우에도 포함하지 않는다.
public record UserResponse(String id, String email, String name, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
