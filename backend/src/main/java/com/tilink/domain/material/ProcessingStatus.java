package com.tilink.domain.material;

/**
 * 학습자료의 AI 분석 처리 상태.
 *
 * <p>문자열 대신 enum 을 쓰면 오타로 잘못된 상태값이 저장되는 것을 컴파일 시점에 막을 수 있다.
 */
public enum ProcessingStatus {

    /** 업로드됨 - 파일 저장 완료, 아직 분석 요청 전 */
    UPLOADED,

    /** 분석중 - FastAPI 에 처리를 요청한 상태 */
    PROCESSING,

    /** 완료 - 파싱·청킹·임베딩까지 모두 끝난 상태 */
    DONE
}
