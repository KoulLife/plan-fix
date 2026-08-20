package taedonghee.plan_fix.infrastructure.spot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [infrastructure] Spot의 JPA 매핑 전용 엔티티. domain.spot.SpotModel과 별개로 둔다.
 * JPA가 요구하는 기본 생성자/PK 전략 같은 영속성 관심사가 domain을 오염시키지 않게 하기 위함.
 */
@Entity
@Table(name = "spots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "spot_id")
	private Long spotId;

	@Builder
	private SpotJpaEntity(Long spotId) {
		this.spotId = spotId;
	}
}
