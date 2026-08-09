package taedonghee.plan_fix.domain.example;

import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.time.LocalDateTime;

/**
 * [domain] 순수 도메인 모델 예시.
 * JPA 등 프레임워크 의존 없이, 비즈니스 규칙만 표현한다.
 */
public class TodoModel {

    private final Long id;
    private final String title;
    private final boolean completed;
    private final LocalDateTime createdAt;

    private TodoModel(Long id, String title, boolean completed, LocalDateTime createdAt) {
        if (title == null || title.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "title은 비어 있을 수 없습니다.");
        }
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public static TodoModel create(String title) {
        return new TodoModel(null, title, false, LocalDateTime.now());
    }

    public static TodoModel reconstruct(Long id, String title, boolean completed, LocalDateTime createdAt) {
        return new TodoModel(id, title, completed, createdAt);
    }

    public TodoModel complete() {
        if (completed) {
            throw new CoreException(ErrorType.CONFLICT, "이미 완료된 할 일입니다. id=" + id);
        }
        return new TodoModel(id, title, true, createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
