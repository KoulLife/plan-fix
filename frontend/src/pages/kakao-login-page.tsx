import { ArrowLeft, Info, QrCode } from "lucide-react";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";

type KakaoStep = "account" | "consent";

const kakaoYellow = "#FEE500";
const kakaoText = "#191919";

export default function KakaoLoginPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<KakaoStep>("account");
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [rememberAccount, setRememberAccount] = useState(false);
  const [requiredConsent, setRequiredConsent] = useState(false);
  const [optionalConsent, setOptionalConsent] = useState(false);
  const [demoMessage, setDemoMessage] = useState<string | null>(null);

  const handleAccountSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!account || !password) return;

    setPassword("");
    setDemoMessage(null);
    setStep("consent");
  };

  const handleAllConsent = (checked: boolean) => {
    setRequiredConsent(checked);
    setOptionalConsent(checked);
  };

  const handleConsentSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!requiredConsent) return;

    setDemoMessage(
      "UI 확인이 완료되었습니다. 실제 연동에서는 카카오가 인가 코드를 PlanFix 백엔드로 전달합니다.",
    );
  };

  return (
    <main
      className="relative h-dvh overflow-hidden bg-white px-5 py-4 text-[#191919] focus-within:overflow-y-auto sm:px-8 sm:py-8"
      style={{ fontFamily: "Arial, 'Noto Sans KR', sans-serif" }}
    >
      <button
        type="button"
        className="absolute left-5 top-5 flex h-10 items-center gap-2 rounded-lg px-3 text-sm text-neutral-600 transition-colors hover:bg-neutral-100 hover:text-neutral-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-neutral-400 sm:left-8 sm:top-8"
        onClick={() => navigate("/login")}
      >
        <ArrowLeft aria-hidden="true" className="h-4 w-4" />
        <span className="hidden sm:inline">PlanFix 로그인으로 돌아가기</span>
        <span className="sm:hidden">돌아가기</span>
      </button>

      <section className="mx-auto flex h-full min-h-0 w-full max-w-[420px] flex-col justify-center pt-10 sm:pt-6">
        <p className="text-center text-4xl font-black leading-none tracking-[-0.08em] sm:text-[42px]">
          kakao
        </p>

        {step === "account" ? (
          <section className="mt-8 sm:mt-12" aria-labelledby="kakao-account-heading">
            <h1 id="kakao-account-heading" className="sr-only">
              카카오계정 로그인
            </h1>

            <form aria-label="카카오계정 로그인" onSubmit={handleAccountSubmit}>
              <label className="block">
                <span className="sr-only">카카오계정</span>
                <input
                  type="text"
                  name="kakaoAccount"
                  value={account}
                  onChange={(event) => setAccount(event.target.value)}
                  placeholder="카카오메일 아이디, 이메일, 전화번호"
                  autoComplete="off"
                  className="h-12 w-full border-0 border-b border-neutral-300 bg-transparent px-1 text-base outline-none transition-colors placeholder:text-neutral-400 focus:border-neutral-800 sm:h-14"
                  required
                />
              </label>

              <label className="mt-2 block">
                <span className="sr-only">카카오계정 비밀번호</span>
                <input
                  type="password"
                  name="kakaoPassword"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="비밀번호"
                  autoComplete="off"
                  className="h-12 w-full border-0 border-b border-neutral-300 bg-transparent px-1 text-base outline-none transition-colors placeholder:text-neutral-400 focus:border-neutral-800 sm:h-14"
                  required
                />
              </label>

              <label className="mt-4 flex cursor-pointer items-center gap-2 text-sm text-neutral-600 sm:mt-5">
                <input
                  type="checkbox"
                  checked={rememberAccount}
                  onChange={(event) => setRememberAccount(event.target.checked)}
                  className="h-4 w-4 accent-[#FEE500]"
                />
                간편로그인 정보 저장
                <Info aria-label="공용 기기에서는 선택하지 마세요" className="h-4 w-4" />
              </label>

              <button
                type="submit"
                className="mt-6 h-12 w-full rounded-xl text-base font-medium transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#FEE500] focus-visible:ring-offset-2 sm:mt-10"
                style={{ backgroundColor: kakaoYellow, color: kakaoText }}
              >
                로그인
              </button>

              <div className="my-3 flex items-center gap-4 text-xs text-neutral-400 sm:my-5" aria-hidden="true">
                <div className="h-px flex-1 bg-neutral-200" />
                또는
                <div className="h-px flex-1 bg-neutral-200" />
              </div>

              <button
                type="button"
                className="flex h-12 w-full items-center justify-center gap-2 rounded-xl border border-neutral-300 bg-white text-base font-medium transition-colors hover:bg-neutral-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-neutral-400"
                onClick={() => setDemoMessage("QR코드 로그인은 실제 카카오 인증 화면에서 제공됩니다.")}
              >
                <QrCode aria-hidden="true" className="h-5 w-5" />
                QR코드 로그인
              </button>
            </form>

            {demoMessage ? (
              <p className="mt-4 text-center text-sm text-neutral-600" role="status">
                {demoMessage}
              </p>
            ) : null}

            <nav className="mt-5 flex items-center justify-center divide-x divide-neutral-300 text-sm text-neutral-600 sm:mt-8" aria-label="카카오계정 도움말">
              <button type="button" className="px-3 hover:underline">회원가입</button>
              <button type="button" className="px-3 hover:underline">계정 찾기</button>
              <button type="button" className="px-3 hover:underline">비밀번호 찾기</button>
            </nav>
          </section>
        ) : (
          <section className="mt-5 sm:mt-10" aria-labelledby="kakao-consent-heading">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-xl font-bold text-primary-foreground shadow-sm sm:h-16 sm:w-16 sm:rounded-2xl sm:text-2xl">
              P
            </div>
            <h1 id="kakao-consent-heading" className="mt-3 text-center text-xl font-semibold tracking-tight sm:mt-5 sm:text-2xl">
              PlanFix에 로그인
            </h1>
            <p className="mt-2 text-center text-sm leading-6 text-neutral-600">
              PlanFix에서 카카오계정으로 로그인합니다.
            </p>

            <form className="mt-4 sm:mt-8" aria-label="카카오 정보 제공 동의" onSubmit={handleConsentSubmit}>
              <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-neutral-300 px-4 py-3 font-semibold sm:py-4">
                <input
                  type="checkbox"
                  checked={requiredConsent && optionalConsent}
                  onChange={(event) => handleAllConsent(event.target.checked)}
                  className="h-5 w-5 accent-[#FEE500]"
                />
                모두 동의하기
              </label>

              <div className="mt-3 divide-y divide-neutral-200 border-y border-neutral-200 sm:mt-4">
                <label className="flex cursor-pointer items-start gap-3 py-3 sm:py-4">
                  <input
                    type="checkbox"
                    checked={requiredConsent}
                    onChange={(event) => setRequiredConsent(event.target.checked)}
                    className="mt-0.5 h-5 w-5 accent-[#FEE500]"
                  />
                  <span>
                    <span className="block text-sm font-medium">[필수] 카카오 개인정보 제3자 제공 동의</span>
                    <span className="mt-1 block text-xs leading-5 text-neutral-500">
                      카카오계정(이메일), 프로필 정보(닉네임)
                    </span>
                  </span>
                </label>

                <label className="flex cursor-pointer items-start gap-3 py-3 sm:py-4">
                  <input
                    type="checkbox"
                    checked={optionalConsent}
                    onChange={(event) => setOptionalConsent(event.target.checked)}
                    className="mt-0.5 h-5 w-5 accent-[#FEE500]"
                  />
                  <span>
                    <span className="block text-sm font-medium">[선택] 프로필 사진 제공 동의</span>
                    <span className="mt-1 block text-xs leading-5 text-neutral-500">
                      여행 프로필을 꾸미는 데 사용합니다.
                    </span>
                  </span>
                </label>
              </div>

              <p className="mt-3 text-xs leading-5 text-neutral-500 sm:mt-5">
                실제 동의항목은 카카오디벨로퍼스 앱 설정에 따라 달라집니다.
              </p>

              <button
                type="submit"
                className="mt-4 h-11 w-full rounded-xl text-sm font-medium transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#FEE500] focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:bg-neutral-200 disabled:text-neutral-400 sm:mt-7 sm:h-12 sm:text-base"
                style={requiredConsent ? { backgroundColor: kakaoYellow, color: kakaoText } : undefined}
                disabled={!requiredConsent}
              >
                동의하고 계속하기
              </button>

              <button
                type="button"
                className="mt-1 h-10 w-full rounded-xl text-sm text-neutral-600 hover:bg-neutral-50 sm:mt-3 sm:h-11"
                onClick={() => {
                  setDemoMessage(null);
                  setStep("account");
                }}
              >
                취소
              </button>
            </form>

            {demoMessage ? (
              <p className="mt-2 rounded-xl bg-amber-50 px-4 py-2 text-xs leading-5 text-amber-900 sm:mt-4 sm:py-3 sm:text-sm sm:leading-6" role="status">
                {demoMessage}
              </p>
            ) : null}
          </section>
        )}

        <footer className="mt-6 text-center text-xs text-neutral-500 sm:mt-14">
          <nav className="flex flex-wrap justify-center gap-x-4 gap-y-2" aria-label="카카오 정책">
            <span>이용약관</span>
            <strong className="font-semibold text-neutral-700">개인정보 처리방침</strong>
            <span>고객센터</span>
          </nav>
          <p className="mt-3">© Kakao Corp.</p>
        </footer>
      </section>
    </main>
  );
}
