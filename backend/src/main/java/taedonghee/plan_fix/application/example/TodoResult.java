package taedonghee.plan_fix.application.example;

import taedonghee.plan_fix.domain.example.TodoModel;

import java.time.LocalDateTime;

/** [application] application -> interfaces 결과값. domain 객체를 그대로 노출하지 않는다. */
public record TodoResult(Long id, String title, boolean completed, LocalDateTime createdAt) {

    public static TodoResult from(TodoModel todo) {
        return new TodoResult(todo.getId(), todo.getTitle(), todo.isCompleted(), todo.getCreatedAt());
    }
}
