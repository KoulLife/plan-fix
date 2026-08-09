package taedonghee.plan_fix.domain.example;

import java.util.List;
import java.util.Optional;

/**
 * [domain] 저장소 포트. domain은 이 인터페이스만 알고, 구현(JPA 등)은 infrastructure가 담당한다.
 */
public interface TodoRepository {

    TodoModel save(TodoModel todo);

    Optional<TodoModel> findById(Long id);

    List<TodoModel> findAll();
}
