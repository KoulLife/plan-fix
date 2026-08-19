import { useState } from "react";

import LoginForm, {
  type LoginFormMessage,
  type LoginFormValues,
} from "@/components/ui/login-form";
import { isAuthApiConfigured, signIn, startKakaoSignIn } from "@/services/auth";

const heroImage =
  "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1600&q=85";

export default function LoginPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<LoginFormMessage | null>(null);

  const handleSubmit = async (values: LoginFormValues) => {
    setMessage(null);

    if (!isAuthApiConfigured()) {
      setMessage({
        tone: "info",
        text: "로그인 폼이 준비되었습니다. 백엔드 연결을 위해 REACT_APP_API_BASE_URL을 설정해 주세요.",
      });
      return;
    }

    setIsSubmitting(true);

    try {
      const result = await signIn(values);
      setMessage({
        tone: "success",
        text: `${result.user.name ?? result.user.email}님, 환영합니다.`,
      });
    } catch (error) {
      setMessage({
        tone: "error",
        text: error instanceof Error ? error.message : "예상하지 못한 오류가 발생했습니다.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleKakaoLogin = () => {
    if (isAuthApiConfigured()) {
      startKakaoSignIn();
      return;
    }

    setMessage({
      tone: "info",
      text: "카카오 로그인을 연결하려면 REACT_APP_API_BASE_URL을 설정해 주세요.",
    });
  };

  return (
    <main className="grid min-h-screen bg-background md:grid-cols-2">
      <section className="relative hidden min-h-screen overflow-hidden md:flex md:items-end" aria-label="PlanFix 소개">
        <img
          className="absolute inset-0 h-full w-full object-cover"
          src={heroImage}
          alt="산과 호수가 어우러진 여행지 풍경"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-violet-950 via-violet-950/35 to-slate-950/10" />
        <div className="relative z-10 max-w-xl p-10 text-white lg:p-16">
          <p className="mb-4 text-sm font-semibold uppercase tracking-[0.28em] text-violet-200">PlanFix</p>
          <h2 className="text-4xl font-semibold leading-tight lg:text-5xl">
            여행의 순간을<br />계획으로 완성하세요.
          </h2>
          <p className="mt-5 max-w-md text-base leading-7 text-violet-100/90">
            가고 싶은 곳부터 꼭 해야 할 일까지 한눈에 정리하고, 설레는 여정을 차근차근 준비해 보세요.
          </p>
        </div>
      </section>

      <section className="flex min-h-screen flex-col px-6 py-8 sm:px-10 md:px-8 lg:px-16">
        <div className="text-lg font-semibold tracking-tight text-primary md:invisible">PlanFix</div>
        <div className="flex flex-1 items-center justify-center py-10">
          <LoginForm
            isSubmitting={isSubmitting}
            message={message}
            onSubmit={handleSubmit}
            onKakaoLogin={handleKakaoLogin}
          />
        </div>
      </section>
    </main>
  );
}
