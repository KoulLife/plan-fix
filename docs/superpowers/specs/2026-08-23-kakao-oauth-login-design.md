# 카카오 OAuth 로그인

작성일: 2026-08-23

## 배경

현재 인증은 자체 로그인 하나뿐이다. `AuthController`가 `POST /api/v1/auth/login`으로
loginId/password를 받아 `AuthResponse`에 `accessToken`을 담아 내려준다.

소셜 로그인의 흔적은 이미 있다. `UserSocialAccountJpaEntity`가
`(provider, provider_user_id)` 유니크 제약까지 갖춘 채로 존재하지만, 도메인 모델도
Repository 포트도 없어 아무도 쓰지 않는다. 프론트에도 `services/auth.ts`의
`startKakaoSignIn()`과 카카오 로그인 화면을 흉내 낸 목업 페이지가 있다.

그리고 프론트와 백엔드가 서로 다른 인증 방식을 전제하고 있다. 백엔드는 토큰을 응답
본문으로 주는데, 프론트의 `LoginResponse`에는 토큰 필드가 없고 `credentials: "include"`를
쓴다. 즉 프론트는 쿠키 세션을 기대한다. 자체 로그인조차 지금 상태로는 세션이 유지되지 않는다.

## 목표

- 카카오 계정으로 로그인해 PlanFix 세션을 얻을 수 있다.
- 자체 로그인과 카카오 로그인이 **같은 세션 메커니즘**(HttpOnly 쿠키)을 쓴다.
- 이메일로 이미 가입한 사용자가 같은 이메일의 카카오로 들어오면 계정이 하나로 이어진다.
- 인가 코드가 유출되어도 토큰으로 교환되지 않는다(PKCE).
- 사용자 필드의 역할이 명확히 갈린다(로그인 아이디 / 닉네임 / 실명).

## 비목표

- **refresh token.** access token 유효기간을 늘려 대응한다. 강제 로그아웃이 필요해지는
  시점에 별도로 다룬다.
- **구글·네이버 등 다른 provider.** 카카오만 붙인다. `SocialProvider` enum이 확장 지점을
  남겨두지만, 지금 일반화하지 않는다.
- **가입 완성 온보딩 화면.** 소셜 가입자의 `name`(실명)은 null로 두고, 실제로 필요해지는
  기능이 생길 때 받는다.
- **카카오 계정 연결 해제(unlink) 및 회원 탈퇴 연동.**
- **CSRF 토큰.** `SameSite=Lax`로 방어한다. 아래 "미해결" 참고.

## 사용자 필드 정리

`username`을 "일반 사용자명"으로 바꾸려 했으나, `UserCredentialModel.loginId`가 이미
영문 소문자+숫자 6~20자의 로그인 아이디라 역할이 정면으로 겹친다. 세 필드를 이렇게 가른다.

| 필드 | 위치 | 규칙 | 자체 가입 | 소셜 가입 |
|---|---|---|---|---|
| `loginId` | `user_credentials` | 영문 소문자+숫자 6~20자 | 필수 | 없음 |
| `username` | `users` | 닉네임. 2~30자, 앞뒤 공백·연속 공백·개행 불가. 문자 종류는 제한하지 않음. **유니크** | `loginId`로 초기화 | 카카오 닉네임 |
| `name` | `users` | 한글 2~30자 | 필수 | null |

`username`의 한글 전용 제약(구 실명 검증)은 `name`으로 옮긴다.

`username`에 문자 종류 제한을 두지 않는 이유: 카카오 닉네임은 한글·영문·숫자·이모지가
모두 올 수 있고, 이걸 거르면 소셜 가입이 실패하거나 원래 닉네임을 잃는다. 대신 공백
관련 규칙만 두어 화면 깨짐과 눈속임(앞뒤 공백만 다른 동명 계정)을 막는다.

프론트 회원가입 폼(`signup-form.tsx`)은 이미 `name`을 받고 있고 **닉네임 입력란이 없다**.
자체 가입에 입력란을 새로 추가하는 대신 `username`을 `loginId` 값으로 초기화한다.
`loginId`가 유니크하므로 대부분 그대로 통과하고, 카카오 닉네임과 충돌하는 드문 경우만
숫자 접미로 처리된다. 사용자는 이후 마이페이지에서 바꾼다.

`username`에 유니크를 유지하는 이유: 기존 `UK_USERS_USERNAME` 제약과 회원가입 검증이
그대로 살고, 향후 동행 초대·일정 공유에서 닉네임으로 사람을 지목할 수 있다. 유니크를
버리면 중복 데이터가 쌓인 뒤에는 되돌리기 어렵다.

카카오가 주는 `profile.nickname`은 표시명이라 유니크하지 않다. 충돌하면 뒤에 숫자를
붙인다(`홍길동` → `홍길동2` → `홍길동3`). 사용자는 이후 마이페이지에서 바꿀 수 있다.

## 인증 흐름

```
[1] 프론트: window.location.assign(`${API}/api/v1/auth/kakao`)

[2] GET /api/v1/auth/kakao
      state(랜덤 32B), code_verifier(랜덤 64B) 생성
      HttpOnly 쿠키 oauth_tx에 담음 (Max-Age 300s, SameSite=Lax)
      302 → kauth.kakao.com/oauth/authorize
              ?client_id&redirect_uri&response_type=code&state
              &code_challenge=S256(verifier)&code_challenge_method=S256
              &scope=profile_nickname,account_email

[3] 사용자가 카카오에서 로그인 + 동의

[4] GET /api/v1/auth/kakao/callback?code&state
      oauth_tx의 state와 대조 → 불일치면 에러 리다이렉트
      oauth_tx 쿠키 삭제
      POST kauth.kakao.com/oauth/token   (code + code_verifier)
      GET  kapi.kakao.com/v2/user/me     (Bearer)
      계정 조회/연결/생성
      JWT 발급 → HttpOnly 쿠키 access_token
      302 → ${FRONTEND_BASE_URL}/main

[5] 이후 요청은 쿠키로 인증
```

`state`를 쿠키에 두는 이유: `SecurityConfig`가 `SessionCreationPolicy.STATELESS`라 세션이
없고 Redis도 없다. [4]는 카카오가 브라우저를 top-level 이동시키는 GET이므로 `SameSite=Lax`
쿠키가 정상적으로 실려 온다.

Spring Security의 `oauth2-client` 대신 직접 구현하는 이유: STATELESS 환경에서는 기본
`HttpSessionOAuth2AuthorizationRequestRepository`를 쓸 수 없어 쿠키 기반 구현을 따로
만들어야 하고, 그러면 프레임워크가 주는 이점이 상당 부분 사라진다. provider가 카카오
하나뿐이라 직접 구현의 코드량도 크지 않다.

## 계정 연결

```
입력: kakaoId, nickname, email, isEmailVerified

1. SocialAccount(KAKAO, kakaoId) 조회
     있음 → 해당 userId로 로그인. 끝.

2. 없음
   2a. email 존재 && isEmailVerified == true
         users에서 email 조회 → 있으면
           SocialAccount를 만들어 기존 user에 연결하고 로그인
   2b. 그 외 → 신규 User 생성
         username = nickname (충돌 시 숫자 접미)
         name     = null
         email    = 인증된 이메일만 저장, 아니면 null
         role     = USER, status = ACTIVE
         + SocialAccount 생성
```

`isEmailVerified`를 반드시 검사한다. 이 검사가 없으면 남의 이메일을 자기 카카오 계정에
등록해둔 사람이 그 사람의 PlanFix 계정을 탈취할 수 있다.

동시 요청으로 2b가 중복 실행되면 `(provider, provider_user_id)` 유니크 제약이 두 번째를
막는다. `DataIntegrityViolationException`을 잡아 1번부터 재시도한다.

## 세션

발급은 쿠키로만 한다. 읽기는 `JwtAuthenticationFilter`가 쿠키를 우선 보고 Authorization
헤더도 fallback으로 받는다(curl·Swagger·향후 모바일 대응).

| 쿠키 | 용도 | 속성 |
|---|---|---|
| `access_token` | 세션 | HttpOnly, Path=/, SameSite=Lax, Secure(운영), Max-Age=토큰 유효기간 |
| `oauth_tx` | state + code_verifier | HttpOnly, Path=/api/v1/auth, SameSite=Lax, Max-Age=300 |

`POST /api/v1/auth/logout`을 추가한다. 서버에 상태가 없으므로 `Max-Age=0`으로 쿠키를
덮어쓰는 것이 전부다.

CORS 설정이 프로젝트에 하나도 없어 새로 추가한다. `allowCredentials(true)`는
`allowedOrigins("*")`와 함께 쓸 수 없으므로 프론트 origin을 명시한다.

## 엔드포인트

| 메서드 | 경로 | 변경 | 설명 |
|---|---|---|---|
| GET | `/api/v1/auth/kakao` | 신규 | 인가 URL로 302. permitAll |
| GET | `/api/v1/auth/kakao/callback` | 신규 | 콜백 처리 후 프론트로 302. permitAll |
| POST | `/api/v1/auth/login` | 변경 | body 대신 `Set-Cookie`. 응답은 `{user:{...}}` |
| POST | `/api/v1/auth/logout` | 신규 | 쿠키 만료 |
| POST | `/api/v1/users` | 변경 | 요청에 `name` 추가 |

## 에러 처리

콜백은 브라우저 리다이렉트다. `GlobalExceptionHandler`를 타서 JSON이 나가면 사용자가 날
것의 JSON을 보게 되므로, 컨트롤러가 직접 처리해 리다이렉트한다.

| 상황 | 리다이렉트 |
|---|---|
| 사용자가 동의 거부 (카카오 `error=access_denied`) | `/login?error=denied` |
| state 불일치 또는 `oauth_tx` 만료 | `/login?error=invalid_state` |
| 토큰 교환 실패 (설정 오류 등) | `/login?error=kakao_config` |
| 카카오 API 타임아웃·장애 | `/login?error=kakao_unavailable` |
| 그 외 | `/login?error=unknown` |

카카오 응답 본문은 에러 로그에만 남기고 URL에 싣지 않는다.

## 설정

비밀값은 `application-secret.yml`(gitignore 등록됨)에 두고
`spring.config.import: optional:classpath:application-secret.yml`로 읽는다. 운영은
`application-prod.yml`의 환경변수를 쓴다. `client-id`/`client-secret`을 `application.yaml`에
적지 않아 파일 간 우선순위를 따질 일이 없고, 값이 없으면 기동 시점에 드러난다.

```yaml
oauth:
  kakao:
    authorize-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
    redirect-uri: ${KAKAO_REDIRECT_URI:http://localhost:8080/api/v1/auth/kakao/callback}
app:
  frontend-base-url: ${FRONTEND_BASE_URL:http://localhost:3000}
  cookie:
    secure: ${COOKIE_SECURE:false}
    same-site: ${COOKIE_SAME_SITE:Lax}
```

카카오 디벨로퍼스에 등록할 Redirect URI는 위 `redirect-uri`와 완전히 일치해야 한다.
한 글자라도 다르면 `KOE006`이 발생한다.

## 마이그레이션

현재 `users`는 1행(`username = '장동익'`, ADMIN), `user_social_accounts`는 0행이다.

```sql
ALTER TABLE users ADD COLUMN name VARCHAR(30);
UPDATE users SET name = username WHERE name IS NULL;
```

기존 `username` 값은 한글이지만 새 `username` 규칙(자유 형식)을 위반하지 않으므로 그대로 둔다.

로컬은 `ddl-auto: update`가 컬럼을 자동 추가하지만 백필은 하지 않는다. `UserModel`은
`reconstruct` 시점에 검증하므로, 백필 전에 앱을 띄우면 기존 행을 읽다가 실패한다. 순서를
지켜야 한다. 운영은 `ddl-auto: validate`라 위 DDL을 직접 실행해야 한다.

## 신설 파일

```
domain/user/
  SocialProvider.java              enum { KAKAO }
  SocialAccountModel.java
  SocialAccountRepository.java     포트

infrastructure/user/
  SocialAccountRepositoryImpl.java 기존 UserSocialAccountJpaRepository 어댑터

infrastructure/oauth/
  KakaoOAuthProperties.java        @ConfigurationProperties("oauth.kakao")
  KakaoOAuthClient.java            인가 URL 생성 / 토큰 교환 / 사용자 조회
  KakaoUserResponse.java

infrastructure/security/
  CookieFactory.java               쿠키 속성 일원화

application/auth/
  SocialLoginApplicationService.java
```

## 테스트

- **도메인** — `UserModel`의 `username`/`name` 검증, `SocialAccountModel`
- **애플리케이션** — `SocialLoginApplicationService`의 세 분기(기존 소셜 / 이메일 연결 /
  신규 가입), 닉네임 충돌 시 숫자 증가, 미인증 이메일은 연결하지 않음
- **인프라** — `KakaoOAuthClient`를 `MockRestServiceServer`로. 실제 카카오는 호출하지 않는다
- **통합** — 콜백 엔드포인트: state 불일치 시 에러 리다이렉트, 성공 시 `Set-Cookie` + `/main`

## 구현 순서

각 단계가 독립적으로 검증 가능하도록 자른다.

| # | 단계 | 완료 판단 |
|---|---|---|
| 0 | 카카오 앱 등록, 키 설정 | 완료 |
| 1 | `username`/`name` 분리, DB 백필, 회원가입 API 변경 | 기존 유저 1행이 정상 조회되고 회원가입에 `name`이 들어감 |
| 2 | 쿠키 세션 전환, CORS, 로그아웃 | 자체 로그인이 `Set-Cookie`로 되고 프론트에서 세션 유지 |
| 3 | 소셜 도메인, `KakaoOAuthClient` | 단위 테스트 통과 |
| 4 | 인가·콜백 엔드포인트 | 브라우저로 실제 카카오 로그인 성공 |
| 5 | 프론트 연결, 목업 페이지 삭제 | 로그인 버튼 → 카카오 → `/main` 전체 흐름 |

1·2단계는 카카오와 무관한 선행 작업이다. 순서를 바꾸면 카카오를 붙이는 도중에 스키마와
세션 방식이 함께 흔들려 원인 추적이 어려워진다.

## 프론트 변경

- `services/auth.ts` — `signIn()`이 보내는 `LoginRequest`가 `{email, password}`인데 백엔드는
  `{loginId, password}`를 받는다. 여기도 맞춘다.
- `components/ui/signup-form.tsx` — 이미 `name`을 받고 있으므로 필드 추가는 없다. 다만
  제출 페이로드가 백엔드 `UserRequest.Create`와 맞는지 확인한다(`birthDate`는 백엔드에
  대응 필드가 없다. 이번 범위 밖).
- `services/auth.ts` — `startKakaoSignIn()`이 부르는 경로가 `${apiBaseUrl}/auth/kakao`인데
  백엔드는 `/api/v1/auth/kakao`다. `.env`의 `REACT_APP_API_BASE_URL`이 현재
  `http://localhost:8080/api`이므로 `/api/v1`로 맞춘다.
- `pages/kakao-login-page.tsx` 삭제 및 `App.tsx`의 `/login/kakao` 라우트 제거.
  실제 연동에서는 `kauth.kakao.com`이 그 역할을 한다.
- `/login`에서 `?error=` 쿼리를 읽어 메시지를 띄운다.

## 미해결

- **운영 배포 시 CSRF.** 프론트와 백엔드가 다른 도메인이면 `SameSite=None; Secure`가
  필요하고, 그 순간 Lax 방어가 사라져 CSRF 토큰이 필요해진다. 쿠키 속성을 환경변수로
  빼두었으므로 배포 구조가 정해질 때 다시 판단한다.
- **`application-local.yml`에 커밋된 TourAPI 키.** 이번 작업 범위 밖이지만 같은 방식으로
  `application-secret.yml`로 옮기는 것이 맞다.
