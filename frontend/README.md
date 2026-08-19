# PlanFix Frontend

PlanFix 프론트엔드는 Create React App 위에 TypeScript, Tailwind CSS, shadcn 호환 구조를 구성한다.

## 실행

```bash
npm install
cp .env.example .env.local
npm start
```

- 실제 로그인 화면: `http://localhost:3000/login`
- 폼 단독 데모: `http://localhost:3000/login/demo`

## 폴더 역할

```text
src/
├── components/ui/     # 화면에 종속되지 않는 shadcn 스타일 UI
├── pages/             # URL 단위 화면 조합과 요청 상태
├── services/          # 백엔드 API 및 외부 인증 연결
├── lib/               # 공통 유틸리티
└── styles/globals.css # Tailwind 지시문과 전역 테마 변수
```

CRA는 `src` 아래 파일을 애플리케이션 소스로 사용하므로, 이 프로젝트의 물리적인 UI 경로는
`src/components/ui`다. `@/components/ui` 별칭을 설정했기 때문에 코드에서는 shadcn 기본 구조와
같은 형태로 import할 수 있다. 공통 UI를 이 폴더에 두면 페이지 코드와 표현 컴포넌트가 섞이지 않고,
shadcn CLI가 생성하는 컴포넌트 경로도 일관되게 유지된다.

## 로그인 폼과 백엔드 연결

`LoginForm`은 `email`, `password`, `rememberMe`를 관리하고 `onSubmit` prop으로 값을 전달한다.
요청 중 상태와 성공/오류 메시지는 `LoginPage`가 관리하며, HTTP 통신은 `services/auth.ts`에만 둔다.

현재 연결 계약은 다음과 같다.

- 이메일 로그인: `POST {REACT_APP_API_BASE_URL}/auth/login`
- 요청 JSON: `{ "email": string, "password": string, "rememberMe": boolean }`
- 성공 JSON: `{ "user": { "id": string, "email": string, "name"?: string } }`
- 카카오 로그인 시작: `GET {REACT_APP_API_BASE_URL}/auth/kakao`
- 세션: 브라우저가 `credentials: "include"`로 HttpOnly 쿠키를 주고받는 방식 권장

백엔드 주소가 없을 때는 실제 요청 대신 연결 안내 메시지만 보여 준다. 백엔드가 준비되면 `.env.local`에
주소를 넣고 개발 서버를 다시 시작한다. 프론트엔드와 백엔드 origin이 다르면 백엔드 CORS에서 프론트엔드
origin과 credentials 사용을 허용해야 한다. `REACT_APP_*` 값은 브라우저 번들에 포함되므로 비밀 키는 넣지 않는다.

## 품질 확인

```bash
npm run typecheck
npm test -- --watchAll=false
npm run build
```

---

# Getting Started with Create React App

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Available Scripts

In the project directory, you can run:

### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.\
You may also see any lint errors in the console.

### `npm test`

Launches the test runner in the interactive watch mode.\
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### `npm run build`

Builds the app for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.

The build is minified and the filenames include the hashes.\
Your app is ready to be deployed!

See the section about [deployment](https://facebook.github.io/create-react-app/docs/deployment) for more information.

### `npm run eject`

**Note: this is a one-way operation. Once you `eject`, you can't go back!**

If you aren't satisfied with the build tool and configuration choices, you can `eject` at any time. This command will remove the single build dependency from your project.

Instead, it will copy all the configuration files and the transitive dependencies (webpack, Babel, ESLint, etc) right into your project so you have full control over them. All of the commands except `eject` will still work, but they will point to the copied scripts so you can tweak them. At this point you're on your own.

You don't have to ever use `eject`. The curated feature set is suitable for small and middle deployments, and you shouldn't feel obligated to use this feature. However we understand that this tool wouldn't be useful if you couldn't customize it when you are ready for it.

## Learn More

You can learn more in the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

To learn React, check out the [React documentation](https://reactjs.org/).

### Code Splitting

This section has moved here: [https://facebook.github.io/create-react-app/docs/code-splitting](https://facebook.github.io/create-react-app/docs/code-splitting)

### Analyzing the Bundle Size

This section has moved here: [https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size](https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size)

### Making a Progressive Web App

This section has moved here: [https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app](https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app)

### Advanced Configuration

This section has moved here: [https://facebook.github.io/create-react-app/docs/advanced-configuration](https://facebook.github.io/create-react-app/docs/advanced-configuration)

### Deployment

This section has moved here: [https://facebook.github.io/create-react-app/docs/deployment](https://facebook.github.io/create-react-app/docs/deployment)

### `npm run build` fails to minify

This section has moved here: [https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify](https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify)
