package taedonghee.plan_fix.domain.spot;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotLikeModelTest {

    @Test
    void userId와_spotId로_생성된다() {
        SpotLikeModel like = SpotLikeModel.create(1L, 100L);

        assertThat(like.userId()).isEqualTo(1L);
        assertThat(like.spotId()).isEqualTo(100L);
        assertThat(like.spotLikeId()).isNull();
        assertThat(like.createdAt()).isNotNull();
    }

    @Test
    void userId가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> SpotLikeModel.create(null, 100L))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void spotId가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> SpotLikeModel.create(1L, null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("spotId");
    }
}
