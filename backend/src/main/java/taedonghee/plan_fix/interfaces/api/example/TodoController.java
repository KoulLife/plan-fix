package taedonghee.plan_fix.interfaces.api.example;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.example.TodoCreateCommand;
import taedonghee.plan_fix.application.example.TodoResult;
import taedonghee.plan_fix.application.example.TodoApplicationService;

import java.util.List;

/**
 * [interfaces] interfaces -> application 흐름 예시.
 * 컨트롤러는 HTTP 변환만 담당하고, 처리는 application(TodoService)에 위임한다.
 * domain, infrastructure는 직접 참조하지 않는다.
 */
@RestController
@RequestMapping("/api/v1/example/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoApplicationService todoApplicationService;

    @PostMapping
    public ResponseEntity<TodoResponse> create(@RequestBody TodoCreateRequest request) {
        TodoResult result = todoApplicationService.create(new TodoCreateCommand(request.title()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoResponse.from(result));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<TodoResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(TodoResponse.from(todoApplicationService.complete(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(TodoResponse.from(todoApplicationService.get(id)));
    }

    @GetMapping
    public ResponseEntity<List<TodoResponse>> getAll() {
        List<TodoResponse> responses = todoApplicationService.getAll().stream()
                .map(TodoResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
