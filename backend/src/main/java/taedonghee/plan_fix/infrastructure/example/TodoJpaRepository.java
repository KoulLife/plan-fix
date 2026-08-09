package taedonghee.plan_fix.infrastructure.example;

import org.springframework.data.jpa.repository.JpaRepository;

/** [infrastructure] Spring Data JPA 저장소. infrastructure 내부에서만 사용된다. */
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, Long> {
}
