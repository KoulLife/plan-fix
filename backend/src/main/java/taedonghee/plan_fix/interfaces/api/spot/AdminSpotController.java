package taedonghee.plan_fix.interfaces.api.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taedonghee.plan_fix.application.spot.SpotApplicationService;

/**
 * 관리자 Spot API HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/admin/spots")
@RequiredArgsConstructor
public class AdminSpotController {

    private final SpotApplicationService spotApplicationService;

    /**
     * 관리자 Spot 생성 API
     */
    @PostMapping
    public ResponseEntity<SpotResponse> create(@RequestBody SpotRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SpotResponse.from(spotApplicationService.create(request.toCommand())));
    }

    /**
     * 관리자 Spot 수정 API
     */
    @PatchMapping("/{spotId}")
    public ResponseEntity<SpotResponse> update(
            @PathVariable Long spotId,
            @RequestBody SpotRequest.Update request
    ) {
        return ResponseEntity.ok(SpotResponse.from(spotApplicationService.update(spotId, request.toCommand())));
    }

    /**
     * 관리자 Spot 삭제 API
     */
    @DeleteMapping("/{spotId}")
    public ResponseEntity<SpotResponse> delete(@PathVariable Long spotId) {
        return ResponseEntity.ok(SpotResponse.from(spotApplicationService.delete(spotId)));
    }
}
