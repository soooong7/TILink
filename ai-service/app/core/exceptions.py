class ProcessingError(Exception):
    """
    학습자료 처리를 계속할 수 없는 상황.

    이 예외가 나면 해당 자료는 FAILED 로 끝난다. 사용자가 원인을 알 수 있도록
    메시지에는 "무엇이 잘못됐는지"를 적는다.
    """
