package taedonghee.plan_fix.application.auth;

import org.junit.jupiter.api.Test;
import taedonghee.plan_fix.domain.user.SocialAccountModel;
import taedonghee.plan_fix.domain.user.SocialAccountRepository;
import taedonghee.plan_fix.domain.user.SocialProvider;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;
import taedonghee.plan_fix.infrastructure.oauth.KakaoOAuthClient;
import taedonghee.plan_fix.infrastructure.oauth.KakaoUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialLoginApplicationServiceTest {

    @Test
    void 이미_연결된_소셜_계정은_기존_사용자로_로그인한다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "홍길동", "a@b.com", true));
        UserModel existing = fixture.users.save(UserModel.create("기존닉", null, "a@b.com"));
        fixture.socialAccounts.save(
                SocialAccountModel.create(existing.getUserId(), SocialProvider.KAKAO, "kakao-1", "a@b.com"));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(result.userId()).isEqualTo(existing.getUserId());
        assertThat(result.username()).isEqualTo("기존닉");
        assertThat(fixture.users.findAll()).hasSize(1);
    }

    @Test
    void 인증된_이메일이_같으면_기존_계정에_연결한다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "카카오닉", "a@b.com", true));
        UserModel existing = fixture.users.save(UserModel.create("기존닉", "홍길동", "a@b.com"));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(result.userId()).isEqualTo(existing.getUserId());
        assertThat(fixture.users.findAll()).hasSize(1);
        assertThat(fixture.socialAccounts
                .findByProviderAndProviderUserId(SocialProvider.KAKAO, "kakao-1")).isPresent();
    }

    @Test
    void 미인증_이메일은_기존_계정에_연결하지_않고_새_계정을_만든다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "카카오닉", "a@b.com", false));
        fixture.users.save(UserModel.create("기존닉", "홍길동", "a@b.com"));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(fixture.users.findAll()).hasSize(2);
        assertThat(result.username()).isEqualTo("카카오닉");
        assertThat(result.email()).isNull();
    }

    @Test
    void 신규_가입은_카카오_닉네임을_username으로_쓰고_name은_비운다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "카카오닉", "a@b.com", true));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        UserModel created = fixture.users.findByUserId(result.userId()).orElseThrow();
        assertThat(created.getUsername()).isEqualTo("카카오닉");
        assertThat(created.getName()).isNull();
        assertThat(created.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void 닉네임이_중복되면_숫자를_붙여_유니크하게_만든다() {
        Fixture fixture = new Fixture(new KakaoUser("kakao-1", "홍길동", null, false));
        fixture.users.save(UserModel.create("홍길동", null, null));
        fixture.users.save(UserModel.create("홍길동2", null, null));

        AuthResult result = fixture.service().loginWithKakao("code", "verifier");

        assertThat(result.username()).isEqualTo("홍길동3");
    }

    /**
     * 테스트 대상과 스텁 저장소를 묶은 픽스처
     */
    static class Fixture {
        final InMemoryUserRepository users = new InMemoryUserRepository();
        final InMemorySocialAccountRepository socialAccounts = new InMemorySocialAccountRepository();
        final KakaoOAuthClient kakaoOAuthClient = mock(KakaoOAuthClient.class);

        Fixture(KakaoUser kakaoUser) {
            when(kakaoOAuthClient.exchangeCodeForAccessToken(anyString(), anyString())).thenReturn("kakao-token");
            when(kakaoOAuthClient.fetchUser("kakao-token")).thenReturn(kakaoUser);
        }

        SocialLoginApplicationService service() {
            return new SocialLoginApplicationService(
                    kakaoOAuthClient, users, socialAccounts, new FixedAuthTokenProvider());
        }
    }

    static class FixedAuthTokenProvider implements AuthTokenProvider {
        @Override
        public AuthToken create(UserModel user) {
            return new AuthToken("jwt-" + user.getUserId(), "Bearer", 3600);
        }
    }

    static class InMemoryUserRepository implements UserRepository {
        private final List<UserModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public UserModel save(UserModel user) {
            UserModel stored = UserModel.reconstruct(
                    user.getUserId() == null ? ++sequence : user.getUserId(),
                    user.getUsername(), user.getName(), user.getEmail(),
                    user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
            saved.removeIf(u -> u.getUserId().equals(stored.getUserId()));
            saved.add(stored);
            return stored;
        }

        @Override
        public Optional<UserModel> findByUserId(Long userId) {
            return saved.stream().filter(u -> u.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<UserModel> findByEmail(String email) {
            return saved.stream().filter(u -> email != null && email.equals(u.getEmail())).findFirst();
        }

        @Override
        public List<UserModel> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public boolean existsByUsername(String username) {
            return saved.stream().anyMatch(u -> u.getUsername().equals(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return saved.stream().anyMatch(u -> email != null && email.equals(u.getEmail()));
        }
    }

    static class InMemorySocialAccountRepository implements SocialAccountRepository {
        private final List<SocialAccountModel> saved = new ArrayList<>();
        private long sequence = 0;

        @Override
        public Optional<SocialAccountModel> findByProviderAndProviderUserId(
                SocialProvider provider, String providerUserId) {
            return saved.stream()
                    .filter(a -> a.getProvider() == provider && a.getProviderUserId().equals(providerUserId))
                    .findFirst();
        }

        @Override
        public SocialAccountModel save(SocialAccountModel account) {
            SocialAccountModel stored = SocialAccountModel.reconstruct(
                    ++sequence, account.getUserId(), account.getProvider(),
                    account.getProviderUserId(), account.getProviderEmail(),
                    account.getCreatedAt(), account.getUpdatedAt());
            saved.add(stored);
            return stored;
        }
    }
}
