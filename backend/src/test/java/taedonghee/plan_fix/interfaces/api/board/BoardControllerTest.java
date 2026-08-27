package taedonghee.plan_fix.interfaces.api.board;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import taedonghee.plan_fix.application.board.BoardApplicationService;
import taedonghee.plan_fix.application.board.BoardCommand;
import taedonghee.plan_fix.application.board.BoardResult;
import taedonghee.plan_fix.domain.board.BoardImageModel;
import taedonghee.plan_fix.domain.board.BoardStatus;
import taedonghee.plan_fix.domain.user.UserRole;
import taedonghee.plan_fix.infrastructure.security.AuthenticatedUser;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardControllerTest {

    private final BoardApplicationService boardApplicationService = mock(BoardApplicationService.class);
    private final BoardController controller = new BoardController(boardApplicationService);
    private final AuthenticatedUser principal = new AuthenticatedUser(10L, "tester", UserRole.USER);

    @Test
    void create_uses_principal_id_and_returns_created_response() {
        BoardRequest.Create request = new BoardRequest.Create(1L, "Board", "<p>Hello</p>", null,
                List.of(new BoardRequest.Image("https://example.com/one.jpg", "cover")));
        BoardCommand.Create command = new BoardCommand.Create(1L, "Board", "<p>Hello</p>", null,
                List.of(new BoardImageModel("https://example.com/one.jpg", "cover")));
        when(boardApplicationService.create(10L, command)).thenReturn(result());

        ResponseEntity<BoardResponse> response = controller.create(principal, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().boardId()).isEqualTo(1L);
        assertThat(response.getBody().images()).hasSize(1);
        verify(boardApplicationService).create(10L, command);
    }

    @Test
    void list_mine_uses_principal_id() {
        when(boardApplicationService.listMine(10L)).thenReturn(List.of(result()));

        ResponseEntity<List<BoardResponse>> response = controller.listMine(principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        verify(boardApplicationService).listMine(10L);
    }

    @Test
    void get_uses_board_id() {
        when(boardApplicationService.get(1L)).thenReturn(result());

        ResponseEntity<BoardResponse> response = controller.get(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        verify(boardApplicationService).get(1L);
    }

    @Test
    void update_uses_principal_id_and_board_id() {
        BoardRequest.Update request = new BoardRequest.Update(null, "Updated", "<p>Updated</p>", null, List.of());
        BoardCommand.Update command = new BoardCommand.Update(null, "Updated", "<p>Updated</p>", null, List.of());
        when(boardApplicationService.update(10L, 1L, command)).thenReturn(result());

        ResponseEntity<BoardResponse> response = controller.update(principal, 1L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(boardApplicationService).update(10L, 1L, command);
    }

    @Test
    void delete_uses_principal_id_and_board_id() {
        when(boardApplicationService.delete(10L, 1L)).thenReturn(result());

        ResponseEntity<BoardResponse> response = controller.delete(principal, 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(boardApplicationService).delete(10L, 1L);
    }

    private BoardResult result() {
        OffsetDateTime now = OffsetDateTime.now();
        return new BoardResult(1L, 1L, 10L, "Board", "<p>Hello</p>",
                "https://example.com/one.jpg", BoardStatus.ACTIVE, 0L, 0L, 0L,
                List.of(new BoardResult.Image("https://example.com/one.jpg", "cover", 0)),
                now, now);
    }
}
