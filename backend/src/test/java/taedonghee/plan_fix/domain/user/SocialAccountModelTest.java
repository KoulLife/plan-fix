package taedonghee.plan_fix.domain.user;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialAccountModelTest {

    @Test
    void 소셜_계정은_userId와_provider_식별자로_생성된다() {
        SocialAccountModel account = SocialAccountModel.create(1L, SocialProvider.KAKAO, "4321", "a@b.com");

        assertThat(account.getUserId()).isEqualTo(1L);
        assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(account.getProviderUserId()).isEqualTo("4321");
        assertThat(account.getProviderEmail()).isEqualTo("a@b.com");
    }

    @Test
    void provider_이메일은_없어도_된다() {
        assertThat(SocialAccountModel.create(1L, SocialProvider.KAKAO, "4321", null).getProviderEmail()).isNull();
    }

    @Test
    void userId가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> SocialAccountModel.create(null, SocialProvider.KAKAO, "4321", null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void provider_식별자가_비어_있으면_생성할_수_없다() {
        assertThatThrownBy(() -> SocialAccountModel.create(1L, SocialProvider.KAKAO, " ", null))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("providerUserId");
    }
}
