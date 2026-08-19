package taedonghee.plan_fix.domain.spot;

import java.util.List;
import java.util.Optional;

/**
 * [domain] 저장소 포트. domain은 이 인터페이스만 알고, 구현(JPA 등)은 infrastructure가 담당한다.
 */
public interface TourDataSpotRepository {

	TourDataSpotModel save(TourDataSpotModel tourDataSpot);

	/** 재수집 시 이미 있는 건인지 판단하는 기준. TourAPI의 contentId는 콘텐츠마다 고유하다. */
	Optional<TourDataSpotModel> findByContentId(Long contentId);

	List<TourDataSpotModel> findBySigungu(String sigungu);

	/** 아직 detailImage2를 시도하지 않은 건. 재실행 시 이미 끝난 건을 건너뛰기 위해 쓴다. */
	List<TourDataSpotModel> findBySigunguAndImageNotCollected(String sigungu);

	/** 아직 detailIntro2를 시도하지 않은 건. */
	List<TourDataSpotModel> findBySigunguAndInfoNotCollected(String sigungu);

	long countAll();
}
