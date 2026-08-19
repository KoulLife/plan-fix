import { useState } from "react";

import LoginForm, {
  type LoginFormMessage,
  type LoginFormValues,
} from "@/components/ui/login-form";
import { isAuthApiConfigured, signIn, startGoogleSignIn } from "@/services/auth";

const heroImage =
  "https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=1600&q=85";

export default function LoginPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<LoginFormMessage | null>(null);

  const handleSubmit = async (values: LoginFormValues) => {
    setMessage(null);

    if (!isAuthApiConfigured()) {
      setMessage({
        tone: "info",
        text: "The form is ready. Set REACT_APP_API_BASE_URL to connect the backend.",
      });
      return;
    }

    setIsSubmitting(true);

    try {
      const result = await signIn(values);
      setMessage({
        tone: "success",
        text: `Welcome${result.user.name ? `, ${result.user.name}` : ""}.`,
      });
    } catch (error) {
      setMessage({
        tone: "error",
        text: error instanceof Error ? error.message : "An unexpected error occurred.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGoogleLogin = () => {
    if (isAuthApiConfigured()) {
      startGoogleSignIn();
      return;
    }

    setMessage({
      tone: "info",
      text: "Set REACT_APP_API_BASE_URL to connect the Google OAuth endpoint.",
    });
  };

  return (
    <main className="grid min-h-screen bg-background md:grid-cols-2">
      <section className="relative hidden min-h-screen overflow-hidden md:flex md:items-end" aria-label="PlanFix introduction">
        <img
          className="absolute inset-0 h-full w-full object-cover"
          src={heroImage}
          alt="A bright workspace with a notebook and laptop"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-violet-950 via-violet-950/35 to-slate-950/10" />
        <div className="relative z-10 max-w-xl p-10 text-white lg:p-16">
          <p className="mb-4 text-sm font-semibold uppercase tracking-[0.28em] text-violet-200">PlanFix</p>
          <h2 className="text-4xl font-semibold leading-tight lg:text-5xl">
            Turn every plan into a clear next step.
          </h2>
          <p className="mt-5 max-w-md text-base leading-7 text-violet-100/90">
            Organize priorities, stay focused, and keep your team moving in the same direction.
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
            onGoogleLogin={handleGoogleLogin}
          />
        </div>
      </section>
    </main>
  );
}
