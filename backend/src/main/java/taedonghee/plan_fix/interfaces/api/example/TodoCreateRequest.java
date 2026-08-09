package taedonghee.plan_fix.interfaces.api.example;

/** [interfaces] HTTP 요청 바디 전용 DTO. application의 Command와 분리한다. */
public record TodoCreateRequest(String title) {
}
