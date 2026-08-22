# PlanFix Frontend

PlanFix 프론트엔드는 Create React App과 CRACO 위에 TypeScript, Tailwind CSS, shadcn 호환 구조를
구성한 React 애플리케이션입니다.

## 실행 방법

프로젝트 루트에서 다음 명령을 실행합니다.

```bash
cd frontend
npm install
npm start
```

개발 서버가 준비되면 `http://localhost:3000`으로 접속합니다. 루트 주소는 자동으로 `/login`으로
이동합니다. 개발 서버를 실행한 터미널을 닫거나 `Ctrl+C`를 누르면 접속도 종료됩니다.

백엔드 없이 UI만 확인할 때는 `.env.local`을 만들지 않아도 됩니다. 백엔드 인증 API가 준비된
경우에만 다음 명령으로 환경 파일을 생성하고 서버 주소를 수정합니다.

```bash
cp .env.example .env.local
```

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
```

환경 변수를 변경한 뒤에는 `npm start`를 다시 실행해야 합니다. `REACT_APP_*` 값은 브라우저
번들에 포함되므로 비밀번호나 API 비밀 키를 저장하면 안 됩니다.

## 구현 기능

### 로그인 및 인증

- 이메일과 비밀번호를 사용하는 로그인 폼
- 빈 값·이메일 형식을 검사하는 PlanFix 테마의 오류 문구
- 로그인 요청 중 점프 로더와 로그인 완료 후 여행 지구본 전환 화면
- 카카오 계정 로그인, QR 로그인 안내, 필수·선택 정보 제공 동의 UI
- 백엔드 주소가 있으면 카카오 인증 URL로 이동하고, 없으면 카카오 UI 데모를 표시

### 회원가입 및 입력 검증

- 이름, 생년월일, 이메일, 비밀번호, 비밀번호 확인 입력
- 공백 없는 완성형 한글 이름 2~7자 검증
- 생년월일 `yyyy-mm-dd` 자동 형식화와 실제 날짜 검증
- 이메일 형식 검사와 중복 확인 버튼
- 영문·숫자·대문자를 포함한 8~20자 비밀번호와 비밀번호 일치 검증
- 모바일 화면에서 입력 폼 전체를 한 화면에 맞춘 레이아웃

### 여행 홈 및 강원도 지역 선택

- 강원도 주간 날씨, 여행 안내, 숙소 카드와 하단 내비게이션 UI
- 강원도 18개 시·군 경계와 지역명이 표시된 SVG 지도
- 지역별 설명, 여행 키워드, 대표 명소와 대표 먹거리 안내
- 마우스 호버 시 지역이 가볍게 떠오르는 입체 효과와 선택 상태 표시
- 키보드 `Enter`·`Space`, 마우스 클릭, 모바일 터치 지원
- 모바일 지역 선택 모달에서 지도·지역 정보·선택 버튼을 스크롤 없이 표시

### 디자인 시스템 및 접근성

- `src/styles/globals.css`의 CSS 변수와 tweakcn `Cosmic Night` 테마 사용
- Figma에서 정한 보라색 계열을 최우선으로 적용
- 기존 폰트와 radius 설정 유지
- 대화상자, 입력 오류, 상태 안내에 ARIA 속성과 키보드 조작 적용
- Lucide React 아이콘과 Motion 애니메이션 사용

## 화면 경로

| 경로 | 용도 | 현재 데이터 |
| --- | --- | --- |
| `/login` | 이메일·카카오 로그인 | 데모 또는 인증 API |
| `/login/kakao` | 카카오 계정·동의 UI | UI 데모 |
| `/signup` | 회원가입·이메일 중복 확인 | UI 데모 |
| `/main` | 여행 홈·지역 선택 지도 | UI 데모 |
| `/login/demo` | 로그인 폼 단독 확인 | UI 데모 |
| `/loading/demo` | 로딩 컴포넌트 모음 | UI 데모 |

정의되지 않은 경로는 `/login`으로 이동합니다.

## 폴더 구조

```text
src/
├── assets/             # 지도 경로 데이터와 라이선스
├── components/ui/      # 재사용 가능한 shadcn 스타일 UI
├── pages/              # URL 단위 화면 조합과 요청 상태
├── services/           # 백엔드 API와 외부 인증 연결
├── lib/                # 공통 유틸리티
└── styles/globals.css  # Tailwind 지시문과 전역 테마 변수
```

CRA는 `src` 아래를 애플리케이션 소스로 사용하므로 실제 UI 컴포넌트 경로는
`src/components/ui`입니다. 코드에서는 `@/components/ui` 별칭으로 가져옵니다.

`components.json`에도 shadcn 기본 별칭이 설정되어 있어 다음과 같이 컴포넌트를 추가할 수 있습니다.

```bash
npx shadcn@latest add button
```

공통 UI를 `src/components/ui`에 두면 페이지 조합과 표현 컴포넌트가 섞이지 않고, shadcn CLI가
추가하는 파일 경로도 일관되게 유지됩니다.

## 백엔드 연결

### 현재 연결된 인증 계약

- 이메일 로그인: `POST {REACT_APP_API_BASE_URL}/auth/login`
- 요청 JSON: `{ "email": string, "password": string, "rememberMe": boolean }`
- 성공 JSON: `{ "user": { "id": string, "email": string, "name"?: string } }`
- 카카오 로그인 시작: `GET {REACT_APP_API_BASE_URL}/auth/kakao`
- 쿠키 세션: `credentials: "include"` 사용

프론트엔드와 백엔드의 origin이 다르면 백엔드 CORS에서 프론트엔드 origin과 credentials를
허용해야 합니다.

### 추후 API 연결 대상

- 회원가입 제출과 이메일 중복 확인
- 카카오 OAuth 인가 코드 처리 결과
- 지역별 날씨, 관광 정보, 숙소와 위시리스트
- 사용자 프로필과 여행 일정

API 함수는 `src/services`에 추가하고, 컴포넌트에는 요청 함수를 prop으로 전달합니다. 이렇게 하면
UI 컴포넌트가 백엔드 구현에 직접 종속되지 않습니다.

## 데이터와 에셋

- 메인 화면의 날씨·여행 카드·숙소는 현재 고정된 UI 데모 데이터입니다.
- 지역 안내는 강원도 18개 시·군별 정적 데이터이며 추후 API 응답으로 교체할 수 있습니다.
- 지도 경로 데이터의 출처와 라이선스는 `src/assets/gangwon-map-LICENSE.txt`에서 확인합니다.
- 외부 이미지는 Unsplash URL을 사용하므로 인터넷 연결이 없으면 이미지가 표시되지 않을 수 있습니다.

## 품질 확인

```bash
npm run typecheck
npm test -- --watchAll=false
npm run build
```

현재 테스트는 로그인·카카오 인증 UI·회원가입 검증·로딩 전환·메인 화면·지역 선택과 키보드 조작을
확인합니다.

## 접속 문제 해결

### `localhost:3000`에 접속되지 않을 때

1. `frontend` 폴더에서 `npm start`가 실행 중인지 확인합니다.
2. 터미널에 `Compiled successfully!`가 표시될 때까지 기다립니다.
3. 실행 터미널을 닫지 않은 상태에서 `http://localhost:3000`으로 접속합니다.

### 3000번 포트를 이미 사용 중일 때

기존 PlanFix 개발 서버가 실행 중인지 먼저 확인합니다. 다른 포트로 실행할 경우 터미널에 표시되는
주소로 접속합니다. 팀원이 동일한 주소로 확인해야 한다면 기존 서버를 종료한 뒤 `npm start`를 다시
실행합니다.
