import { Globe2, LoaderCircle, LockKeyhole, Mail } from "lucide-react";
import { FormEvent, useState } from "react";

import { cn } from "@/lib/utils";

export type LoginFormValues = {
  email: string;
  password: string;
  rememberMe: boolean;
};

export type LoginFormMessage = {
  tone: "error" | "info" | "success";
  text: string;
};

type LoginFormProps = {
  className?: string;
  isSubmitting?: boolean;
  message?: LoginFormMessage | null;
  onSubmit?: (values: LoginFormValues) => void | Promise<void>;
  onGoogleLogin?: () => void;
  forgotPasswordHref?: string;
  signUpHref?: string;
};

const fieldClassName =
  "flex h-12 w-full items-center gap-3 overflow-hidden rounded-full border border-input bg-background px-5 transition-colors focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/15";

export default function LoginForm({
  className,
  isSubmitting = false,
  message,
  onSubmit,
  onGoogleLogin,
  forgotPasswordHref = "/forgot-password",
  signUpHref = "/signup",
}: LoginFormProps) {
  const [values, setValues] = useState<LoginFormValues>({
    email: "",
    password: "",
    rememberMe: false,
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit?.(values);
  };

  return (
    <form
      className={cn("flex w-full max-w-sm flex-col items-center", className)}
      onSubmit={handleSubmit}
    >
      <h1 className="text-4xl font-semibold tracking-tight text-foreground">Sign in</h1>
      <p className="mt-3 text-center text-sm text-muted-foreground">
        Welcome back! Please sign in to continue
      </p>

      <button
        type="button"
        className="mt-8 flex h-12 w-full items-center justify-center gap-2 rounded-full bg-muted text-sm font-medium text-foreground transition-colors hover:bg-muted/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
        onClick={onGoogleLogin}
        disabled={isSubmitting}
      >
        <Globe2 aria-hidden="true" className="h-5 w-5 text-primary" />
        <span>Continue with Google</span>
      </button>

      <div className="my-5 flex w-full items-center gap-4" aria-hidden="true">
        <div className="h-px flex-1 bg-border" />
        <p className="whitespace-nowrap text-sm text-muted-foreground">or sign in with email</p>
        <div className="h-px flex-1 bg-border" />
      </div>

      <label className={fieldClassName}>
        <span className="sr-only">Email address</span>
        <Mail aria-hidden="true" className="h-4 w-4 shrink-0 text-muted-foreground" />
        <input
          type="email"
          name="email"
          value={values.email}
          onChange={(event) => setValues((current) => ({ ...current, email: event.target.value }))}
          placeholder="Email address"
          autoComplete="email"
          className="h-full w-full bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
          disabled={isSubmitting}
          required
        />
      </label>

      <label className={cn(fieldClassName, "mt-4")}>
        <span className="sr-only">Password</span>
        <LockKeyhole aria-hidden="true" className="h-4 w-4 shrink-0 text-muted-foreground" />
        <input
          type="password"
          name="password"
          value={values.password}
          onChange={(event) => setValues((current) => ({ ...current, password: event.target.value }))}
          placeholder="Password"
          autoComplete="current-password"
          className="h-full w-full bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
          disabled={isSubmitting}
          required
        />
      </label>

      <div className="mt-6 flex w-full items-center justify-between text-muted-foreground">
        <label className="flex cursor-pointer items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={values.rememberMe}
            onChange={(event) =>
              setValues((current) => ({ ...current, rememberMe: event.target.checked }))
            }
            className="h-4 w-4 accent-primary"
            disabled={isSubmitting}
          />
          Remember me
        </label>
        <a className="text-sm underline-offset-4 hover:text-primary hover:underline" href={forgotPasswordHref}>
          Forgot password?
        </a>
      </div>

      <div className="mt-4 min-h-5 w-full" aria-live="polite">
        {message ? (
          <p
            className={cn(
              "text-sm",
              message.tone === "error" && "text-destructive",
              message.tone === "info" && "text-muted-foreground",
              message.tone === "success" && "text-emerald-600",
            )}
          >
            {message.text}
          </p>
        ) : null}
      </div>

      <button
        type="submit"
        className="mt-2 flex h-11 w-full items-center justify-center gap-2 rounded-full bg-primary font-medium text-primary-foreground transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isSubmitting}
      >
        {isSubmitting ? <LoaderCircle aria-hidden="true" className="h-4 w-4 animate-spin" /> : null}
        {isSubmitting ? "Signing in..." : "Login"}
      </button>

      <p className="mt-4 text-sm text-muted-foreground">
        Don&apos;t have an account?{" "}
        <a className="font-medium text-primary hover:underline" href={signUpHref}>
          Sign up
        </a>
      </p>
    </form>
  );
}
