import { useState } from "react";
import { useNavigate } from "react-router-dom";

import SignupForm, {
  type EmailAvailabilityResult,
  type SignupFormMessage,
  type SignupFormValues,
} from "@/components/ui/signup-form";

const emailCheckDelay = 450;

const wait = (duration: number) =>
  new Promise<void>((resolve) => window.setTimeout(resolve, duration));

export default function SignupPage() {
  const navigate = useNavigate();
  const [message, setMessage] = useState<SignupFormMessage | null>(null);

  const handleSubmit = (values: SignupFormValues) => {
    setMessage({
      tone: "success",
      text: `${values.name}님의 입력 정보를 확인했습니다.`,
    });
  };

  const handleCheckEmailAvailability = async (
    email: string,
  ): Promise<EmailAvailabilityResult> => {
    await wait(emailCheckDelay);

    if (email.toLowerCase() === "demo@planfix.kr") {
      return {
        available: false,
        message: "이미 사용 중인 이메일입니다.",
      };
    }

    return {
      available: true,
      message: "사용 가능한 이메일입니다.",
    };
  };

  return (
    <main className="relative flex h-dvh items-center justify-center overflow-hidden bg-gradient-to-b from-primary/10 via-background to-background px-3 py-2 focus-within:overflow-y-auto sm:px-8 sm:py-4">
      <div className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-primary/15 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-32 -right-20 h-80 w-80 rounded-full bg-primary/10 blur-3xl" />
      <section className="relative max-h-[calc(100dvh-1rem)] w-full max-w-md overflow-hidden rounded-lg border bg-background/95 p-4 shadow-panel backdrop-blur-sm focus-within:max-h-none focus-within:overflow-visible sm:max-h-[calc(100dvh-2rem)] sm:p-8 lg:p-10" aria-label="PlanFix 회원가입">
        <SignupForm
          message={message}
          onSubmit={handleSubmit}
          onCheckEmailAvailability={handleCheckEmailAvailability}
          onBackToLogin={() => navigate("/login")}
        />
      </section>
    </main>
  );
}
