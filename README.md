# PlanFix

강원도 18개 시·군을 중심으로 여행지를 탐색하고, 문제가 생긴 여행 일정을 AI로 다시 구성하는
강원도 특화 여행 일정 복구 서비스입니다.
현재 저장소에서는 프론트엔드 UI와 사용자 흐름을 우선 개발하고 있으며, 인증과 여행 데이터는
추후 백엔드 API와 연결할 수 있도록 화면과 서비스 계층을 분리했습니다.

## 주요 기능

### 인증 및 계정

- 이메일 로그인과 PlanFix 테마의 입력 오류 안내
- 카카오 계정 로그인·정보 제공 동의 UI
- 이름, 생년월일, 이메일, 비밀번호를 입력하는 회원가입 UI
- 한글 이름, 날짜 형식, 이메일 중복 확인, 비밀번호 형식·일치 여부 검증
- 로그인 후 여행 지구본 로딩 화면을 거쳐 메인 화면으로 이동

### 여행 홈 및 지역 탐색

- 강원도 주간 날씨, 여행 안내 카드, 인기 숙소 UI
- 강원도 18개 시·군을 선택할 수 있는 인터랙티브 지도
- 지역별 여행 설명, 여행 키워드, 대표 명소와 대표 먹거리 안내
- 마우스·키보드·터치 조작과 선택 지역 강조 효과 지원

### 디자인 및 반응형 UI

- Figma 가이드와 tweakcn `Cosmic Night` 테마 기반 디자인
- Tailwind CSS와 shadcn 호환 컴포넌트 구조
- 모바일 로그인·회원가입·카카오 인증·지역 선택 화면 최적화
- 모바일 지역 선택 모달을 스크롤 없이 한 화면에 표시

## 기술 스택

- React 19
- TypeScript
- Tailwind CSS
- shadcn 호환 구조
- React Router
- Motion
- Lucide React
- Jest / Testing Library

## 빠른 시작

```bash
cd frontend
npm install
npm start
```

브라우저에서 `http://localhost:3000`에 접속하면 `/login`으로 이동합니다. 백엔드 환경 변수를
설정하지 않으면 프론트엔드 데모 모드로 로그인·회원가입·카카오 UI를 확인할 수 있습니다.

백엔드 인증 서버가 준비된 경우에만 다음과 같이 환경 파일을 생성합니다.

```bash
cp .env.example .env.local
```

`.env.local`의 `REACT_APP_API_BASE_URL`을 실제 백엔드 주소로 변경한 뒤 개발 서버를 다시 시작합니다.

## 주요 화면

| 경로 | 화면 |
| --- | --- |
| `/login` | 이메일·카카오 로그인 |
| `/login/kakao` | 카카오 계정·동의 UI |
| `/signup` | 회원가입 |
| `/main` | 여행 메인·강원도 지역 선택 |
| `/login/demo` | 로그인 폼 단독 데모 |
| `/loading/demo` | 로딩 UI 모음 |

## 프로젝트 구조

```text
plan-fix/
├── frontend/       # React 프론트엔드
├── AGENTS.md       # 프로젝트 작업 규칙
├── tweakcn.md      # 색상·테마 적용 가이드
└── README.md
```

프론트엔드의 세부 구조, API 연결 방식과 검증 규칙은
[`frontend/README.md`](frontend/README.md)를 참고합니다.

## 현재 연결 상태

- 이메일 로그인과 카카오 인증 시작점은 `frontend/src/services/auth.ts`에 분리되어 있습니다.
- 백엔드 주소가 없으면 이메일 로그인은 데모 흐름으로 동작합니다.
- 회원가입, 이메일 중복 확인, 날씨, 여행지와 숙소 데이터는 현재 UI 확인용 데이터입니다.
- 실제 서비스 연결 시 API 요청은 `services/`에 추가하고 페이지는 요청 상태만 관리합니다.

## 품질 확인

```bash
cd frontend
npm run typecheck
npm test -- --watchAll=false
npm run build
```

## Git 작업 방식

기능 브랜치에서 작업하고 `feat:`, `fix:`, `chore:`, `docs:` 형식의 커밋 메시지를 사용합니다.
PR/MR로 검토를 요청하며 승인 전에는 임의로 병합하지 않습니다.
