package taedonghee.plan_fix.interfaces.api.example;

import taedonghee.plan_fix.application.example.TodoResult;

import java.time.LocalDateTime;

/** [interfaces] HTTP 응답 전용 DTO. */
public record TodoResponse(Long id, String title, boolean completed, LocalDateTime createdAt) {

    public static TodoResponse from(TodoResult result) {
        return new TodoResponse(result.id(), result.title(), result.completed(), result.createdAt());
    }
}
