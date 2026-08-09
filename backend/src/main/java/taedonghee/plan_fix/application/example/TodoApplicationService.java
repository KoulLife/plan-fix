package taedonghee.plan_fix.application.example;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.example.TodoModel;
import taedonghee.plan_fix.domain.example.TodoRepository;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

import java.util.List;

/**
 * [application] 유스케이스 조율. 비즈니스 규칙 자체는 domain(Todo)에 위임하고,
 * 여기서는 domain.TodoRepository 인터페이스만 의존한다(구현은 infrastructure).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoApplicationService {

    private final TodoRepository todoRepository;

    @Transactional
    public TodoResult create(TodoCreateCommand command) {
        TodoModel saved = todoRepository.save(TodoModel.create(command.title()));
        return TodoResult.from(saved);
    }

    @Transactional
    public TodoResult complete(Long id) {
        TodoModel completed = getOrThrow(id).complete();
        return TodoResult.from(todoRepository.save(completed));
    }

    public TodoResult get(Long id) {
        return TodoResult.from(getOrThrow(id));
    }

    public List<TodoResult> getAll() {
        return todoRepository.findAll().stream()
                .map(TodoResult::from)
                .toList();
    }

    private TodoModel getOrThrow(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "할 일을 찾을 수 없습니다. id=" + id));
    }
}
