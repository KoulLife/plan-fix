import LoginForm from "@/components/ui/login-form";

export default function LoginFormDemo() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-6">
      <LoginForm className="rounded-2xl border bg-background px-8 py-10 shadow-panel" />
    </div>
  );
}
