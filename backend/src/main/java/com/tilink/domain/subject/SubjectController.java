package com.tilink.domain.subject;

import com.tilink.domain.subject.dto.SubjectResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 과목/기술 카테고리 API. 학습자료 업로드 시 고를 수 있는 목록을 제공한다.
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public List<SubjectResponse> findAll() {
        return subjectService.findAll();
    }
}
