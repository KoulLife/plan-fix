package taedonghee.plan_fix.infrastructure.example;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import taedonghee.plan_fix.domain.example.TodoModel;
import taedonghee.plan_fix.domain.example.TodoRepository;

import java.util.List;
import java.util.Optional;

/**
 * [infrastructure] domain.TodoRepository 포트의 JPA 구현체(어댑터).
 * domain <- infrastructure: domain의 인터페이스를 구현하며, JPA 엔티티 <-> domain 모델 변환을 책임진다.
 */
@Repository
@RequiredArgsConstructor
public class TodoRepositoryImpl implements TodoRepository {

    private final TodoJpaRepository todoJpaRepository;

    @Override
    public TodoModel save(TodoModel todo) {
        TodoJpaEntity saved = todoJpaRepository.save(toEntity(todo));
        return toDomain(saved);
    }

    @Override
    public Optional<TodoModel> findById(Long id) {
        return todoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TodoModel> findAll() {
        return todoJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private TodoJpaEntity toEntity(TodoModel todo) {
        return TodoJpaEntity.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .completed(todo.isCompleted())
                .createdAt(todo.getCreatedAt())
                .build();
    }

    private TodoModel toDomain(TodoJpaEntity entity) {
        return TodoModel.reconstruct(entity.getId(), entity.getTitle(), entity.isCompleted(), entity.getCreatedAt());
    }
}
