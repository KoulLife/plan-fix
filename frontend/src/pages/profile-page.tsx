import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronRight, Loader2, UserRound } from "lucide-react";
import AppNav from "@/components/ui/app-nav";
import { fetchMyProfile, updateMyProfile, UserProfile } from "@/services/user";

export default function ProfilePage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [form, setForm] = useState({ username: "", name: "", email: "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => { fetchMyProfile().then((p) => { setProfile(p); setForm({ username: p.username, name: p.name ?? "", email: p.email ?? "" }); }).catch(() => navigate("/login", { replace: true })).finally(() => setLoading(false)); }, [navigate]);

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setMessage("");
    try { const updated = await updateMyProfile({ username: form.username, name: form.name || null, email: form.email || null }); setProfile(updated); setMessage("프로필이 저장되었습니다."); }
    catch (error) { setMessage(error instanceof Error ? error.message : "저장에 실패했습니다."); }
    finally { setSaving(false); }
  };

  if (loading) return <div className="flex min-h-screen items-center justify-center"><Loader2 className="h-6 w-6 animate-spin" /></div>;
  return <div className="min-h-screen bg-muted/20 pb-28 md:pb-16 md:pt-20"><AppNav /><main className="mx-auto max-w-2xl px-4 pt-6 sm:px-6 sm:pt-8">
    <div className="flex items-center gap-1.5 text-xs text-muted-foreground"><Link to="/main">홈</Link><ChevronRight className="h-3.5 w-3.5" /><span className="font-medium text-foreground">프로필</span></div>
    <div className="mt-4 flex items-center gap-3"><div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary"><UserRound /></div><div><h1 className="text-2xl font-bold">내 프로필</h1><p className="text-sm text-muted-foreground">회원 정보를 확인하고 수정하세요.</p></div></div>
    <form onSubmit={submit} className="mt-8 space-y-5 rounded-2xl border bg-background p-5 shadow-sm sm:p-7">
      {[['username','닉네임',true],['name','이름',false],['email','이메일',false]].map(([key,label,required]) => <label key={key as string} className="block text-sm font-medium"><span>{label as string}</span><input required={required as boolean} value={form[key as keyof typeof form]} onChange={(e) => setForm({ ...form, [key as string]: e.target.value })} className="mt-2 w-full rounded-lg border px-3 py-2.5" /></label>)}
      {message && <p className="text-sm text-muted-foreground">{message}</p>}
      <div className="flex items-center justify-between"><Link to="/courses" className="text-sm font-medium text-primary hover:underline">내 여행 코스 보기</Link><button disabled={saving} className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground disabled:opacity-50">{saving ? "저장 중..." : "저장"}</button></div>
    </form>
  </main></div>;
}
