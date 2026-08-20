package taedonghee.plan_fix.application.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taedonghee.plan_fix.domain.spot.SpotModel;
import taedonghee.plan_fix.domain.spot.SpotRepository;
import taedonghee.plan_fix.support.error.CoreException;
import taedonghee.plan_fix.support.error.ErrorType;

/**
 * Spot Application Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotApplicationService {

    private final SpotRepository spotRepository;

    /**
     * Spot 생성 처리
     */
    @Transactional
    public SpotResult create(SpotCommand.Create command) {
        SpotModel spot = SpotModel.create(
                command.title(),
                command.category(),
                command.region(),
                command.sigungu(),
                command.address(),
                command.latitude(),
                command.longitude(),
                command.thumbnail(),
                command.description()
        );
        return SpotResult.from(spotRepository.save(spot));
    }

    /**
     * Spot 수정 처리
     */
    @Transactional
    public SpotResult update(Long spotId, SpotCommand.Update command) {
        SpotModel spot = getOrThrow(spotId);
        SpotModel updatedSpot = spot.update(
                command.title(),
                command.category(),
                command.region(),
                command.sigungu(),
                command.address(),
                command.latitude(),
                command.longitude(),
                command.thumbnail(),
                command.description()
        );
        return SpotResult.from(spotRepository.save(updatedSpot));
    }

    /**
     * Spot 삭제 상태 변경 처리
     */
    @Transactional
    public SpotResult delete(Long spotId) {
        return SpotResult.from(spotRepository.save(getOrThrow(spotId).delete()));
    }

    /**
     * Spot 조회 실패 예외 처리
     */
    private SpotModel getOrThrow(Long spotId) {
        return spotRepository.findBySpotId(spotId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Spot not found. spotId=" + spotId));
    }
}
