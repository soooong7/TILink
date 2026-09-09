package com.tilink.domain.subject;

import com.tilink.domain.subject.dto.SubjectResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 과목/기술 카테고리 조회 서비스. 등록·수정은 관리자 API 단계에서 추가한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    // 선택 목록으로 쓰이므로 이름 오름차순으로 내려준다.
    public List<SubjectResponse> findAll() {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .map(SubjectResponse::from)
                .toList();
    }
}
