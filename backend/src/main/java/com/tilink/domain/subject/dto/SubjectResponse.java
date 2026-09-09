package com.tilink.domain.subject.dto;

import com.tilink.domain.subject.Subject;

// 과목 응답. 학습자료 응답 안에도 중첩되어 쓰인다.
public record SubjectResponse(String id, String name) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName());
    }
}
