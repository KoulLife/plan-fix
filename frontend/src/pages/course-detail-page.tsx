import { useEffect, useState, type ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  Calendar,
  ChevronRight,
  Eye,
  Globe,
  Heart,
  Loader2,
  Lock,
  MapPin,
  Pencil,
  Plus,
  Trash2,
  UserPlus,
  Users,
  X,
} from "lucide-react";
import AppNav from "@/components/ui/app-nav";
import { CourseResponse, createCourseInvite, deleteCourse, fetchCourse, fetchCourseMembers, fetchPendingCourseInvites, cancelCourseInvite, removeCourseMember, updateCourseMemberRole, type CourseInviteRole, type CourseMember, type PendingCourseInvite } from "@/services/course";
import { UnauthorizedError } from "@/services/spots";

export default function CourseDetailPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const navigate = useNavigate();

  const [course, setCourse] = useState<CourseResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [creatingInvite, setCreatingInvite] = useState(false);
  const [inviteDialogOpen, setInviteDialogOpen] = useState(false);
  const [inviteRole, setInviteRole] = useState<CourseInviteRole>("EDITOR");
  const [inviteToast, setInviteToast] = useState<{ message: string; inviteUrl?: string } | null>(null);
  const [membersDialogOpen, setMembersDialogOpen] = useState(false);
  const [showMembersTable, setShowMembersTable] = useState(false);
  const [members, setMembers] = useState<CourseMember[]>([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [pendingInvites, setPendingInvites] = useState<PendingCourseInvite[]>([]);

  const handleDelete = async () => {
    if (!courseId) return;
    if (!window.confirm("정말 이 여행 코스를 삭제하시겠습니까?")) {
      return;
    }

    setDeleting(true);
    try {
      await deleteCourse(courseId);
      navigate("/courses", { replace: true });
    } catch (err) {
      if (err instanceof UnauthorizedError) {
        alert("로그인이 필요합니다. 로그인 페이지로 이동합니다.");
        navigate("/login");
        return;
      }
      alert(err instanceof Error ? err.message : "코스 삭제에 실패했습니다.");
      setDeleting(false);
    }
  };

  const handleInvite = async () => {
    if (!courseId || creatingInvite) return;
    setCreatingInvite(true);
    try {
      const invite = await createCourseInvite(courseId, inviteRole);
      await navigator.clipboard?.writeText(invite.inviteUrl);
      setInviteDialogOpen(false);
      setInviteToast({
        message: `${inviteRole === "EDITOR" ? "편집" : "읽기"} 권한 초대 링크를 복사했습니다.`,
        inviteUrl: invite.inviteUrl,
      });
    } catch (err) {
      if (err instanceof UnauthorizedError) { navigate("/login"); return; }
      setInviteToast({ message: err instanceof Error ? err.message : "초대 링크를 만들지 못했습니다." });
    } finally { setCreatingInvite(false); }
  };

  const openMembers = async () => {
    if (!courseId || membersLoading) return;
    setInviteDialogOpen(false);
    setMembersDialogOpen(false); setShowMembersTable(true); setMembersLoading(true);
    try {
      const [memberResult, pendingResult] = await Promise.all([fetchCourseMembers(courseId), fetchPendingCourseInvites(courseId)]);
      setMembers(memberResult); setPendingInvites(pendingResult);
    } catch (err) { setInviteToast({ message: err instanceof Error ? err.message : "멤버 목록을 불러오지 못했습니다." }); }
    finally { setMembersLoading(false); }
  };

  useEffect(() => {
    if (!courseId) return;

    let ignore = false;
    setLoading(true);
    setError(null);

    const loadCourse = async () => {
      try {
        const res = await fetchCourse(courseId);
        if (!ignore) {
          setCourse(res);
        }
      } catch (err) {
        if (err instanceof UnauthorizedError) {
          alert("로그인이 필요합니다. 로그인 페이지로 이동합니다.");
          navigate("/login");
          return;
        }
        if (!ignore) {
          setError(err instanceof Error ? err.message : "코스를 불러오지 못했습니다.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    loadCourse();

    return () => {
      ignore = true;
    };
  }, [courseId, navigate]);

  useEffect(() => {
    if (!courseId || course?.isOwner === false) return;
    fetchCourseMembers(courseId).then(setMembers).catch(() => undefined);
  }, [courseId, course?.isOwner]);

  return (
    <div className="min-h-screen bg-muted/20 pb-20">
      <AppNav />

      <main className="mx-auto max-w-4xl px-4 pt-6 sm:px-6 sm:pt-8">
        {/* 상단 브레드크럼 */}
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Link to="/courses" className="hover:text-foreground">
            내 여행 코스
          </Link>
          <ChevronRight className="h-3.5 w-3.5" />
          <span className="font-medium text-foreground">코스 상세</span>
        </div>

        {loading ? (
          <div className="flex h-64 flex-col items-center justify-center gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-sm text-muted-foreground">여행 코스를 불러오는 중입니다...</p>
          </div>
        ) : error ? (
          <div className="mt-8 rounded-2xl border border-destructive/30 bg-card p-8 text-center shadow-sm">
            <p className="text-base font-semibold text-destructive">{error}</p>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="mt-4 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
            >
              다시 시도
            </button>
          </div>
        ) : !course ? (
          <div className="mt-8 rounded-2xl border border-border bg-card p-10 text-center shadow-sm">
            <MapPin className="mx-auto h-10 w-10 text-muted-foreground/40" />
            <h2 className="mt-4 text-lg font-bold text-foreground">
              존재하지 않거나 삭제된 코스입니다.
            </h2>
            <p className="mt-1 text-xs text-muted-foreground">
              요청하신 코스 정보를 찾을 수 없습니다.
            </p>
            <div className="mt-6 flex justify-center gap-3">
              <Link
                to="/courses"
                className="rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
              >
                코스 목록으로
              </Link>
              <Link
                to="/courses/create"
                className="rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground"
              >
                새 코스 만들기
              </Link>
            </div>
          </div>
        ) : (
          <div className="mt-4 space-y-6">
            {/* 코스 헤더 카드 */}
            <div className="rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
                    {course.days.length}일 코스
                  </span>
                  <span
                    className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium ${
                      course.visibility === "PUBLIC"
                        ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {course.visibility === "PUBLIC" ? (
                      <>
                        <Globe className="h-3 w-3" />
                        <span>전체 공개</span>
                      </>
                    ) : (
                      <>
                        <Lock className="h-3 w-3" />
                        <span>비공개</span>
                      </>
                    )}
                  </span>
                </div>

                {course.isOwner !== false && (
                  <div className="flex items-center gap-2">
                    <button type="button" onClick={() => { setInviteToast(null); setInviteDialogOpen(true); }} className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-colors hover:opacity-90">
                      <UserPlus className="h-3.5 w-3.5" /> <span>친구 초대</span>
                    </button>
                    <button type="button" onClick={() => void openMembers()} className="inline-flex items-center gap-1.5 rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-medium hover:bg-muted"><Users className="h-3.5 w-3.5" />멤버 관리</button>
                    <Link
                      to={`/courses/${course.courseId}/edit`}
                      className="inline-flex items-center gap-1.5 rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
                    >
                      <Pencil className="h-3.5 w-3.5 text-muted-foreground" />
                      <span>코스 수정</span>
                    </Link>
                    <button
                      type="button"
                      onClick={handleDelete}
                      disabled={deleting}
                      className="inline-flex items-center gap-1.5 rounded-xl border border-destructive/30 bg-background px-3 py-1.5 text-xs font-medium text-destructive transition-colors hover:bg-destructive/10 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      <span>{deleting ? "삭제 중..." : "코스 삭제"}</span>
                    </button>
                  </div>
                )}
              </div>

              <h1 className="mt-3 text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
                {course.title}
              </h1>

              {course.description && (
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground sm:text-base">
                  {course.description}
                </p>
              )}

              {course.isOwner !== false && <div className="mt-5 flex items-center justify-between rounded-xl border border-border bg-muted/20 px-4 py-3"><div className="flex min-w-0 items-center gap-2"><Users className="h-4 w-4 shrink-0 text-primary" /><span className="text-sm font-semibold">참여 멤버</span><div className="flex -space-x-2">{members.filter((member) => member.role !== "OWNER").slice(0, 4).map((member) => <span key={member.userId} title={member.username} className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-background bg-primary/15 text-[10px] font-bold text-primary">{(member.username || "?").slice(0, 1)}</span>)}{members.filter((member) => member.role !== "OWNER").length > 4 && <span className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-background bg-muted text-[10px] font-bold">+{members.filter((member) => member.role !== "OWNER").length - 4}</span>}</div><span className="text-xs text-muted-foreground">{members.filter((member) => member.role !== "OWNER").length}명</span></div><button type="button" onClick={() => void openMembers()} className="shrink-0 text-xs font-semibold text-primary hover:underline">전체 보기</button></div>}

              <div className="mt-6 flex flex-wrap items-center gap-y-2 gap-x-6 border-t border-border pt-4 text-xs text-muted-foreground">
                {course.startDate && course.endDate && (
                  <div className="flex items-center gap-1.5">
                    <Calendar className="h-4 w-4 text-primary" />
                    <span>
                      {course.startDate} ~ {course.endDate} ({course.days.length}일)
                    </span>
                  </div>
                )}
                <div className="flex items-center gap-1.5">
                  <MapPin className="h-4 w-4 text-primary" />
                  <span>
                    총{" "}
                    <strong className="text-foreground">
                      {course.days.reduce((sum, d) => sum + d.spots.length, 0)}
                    </strong>
                    개 장소
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="flex items-center gap-1">
                    <Eye className="h-3.5 w-3.5" />
                    {course.viewCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <Heart className="h-3.5 w-3.5" />
                    {course.likeCount}
                  </span>
                </div>
              </div>
            </div>

            {/* Day별 일정 리스트 */}
            <div className="space-y-6">
              {course.days.map((day) => (
                <section
                  key={day.dayNumber}
                  data-testid={`day-detail-${day.dayNumber}`}
                  className="overflow-hidden rounded-2xl border border-border bg-card shadow-sm"
                >
                  {/* Day 헤더 */}
                  <div className="flex items-center justify-between border-b border-border bg-muted/30 px-5 py-3.5 sm:px-6">
                    <div className="flex items-center gap-3">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary font-bold text-xs text-primary-foreground">
                        D{day.dayNumber}
                      </span>
                      <h2 className="text-base font-bold text-foreground">
                        Day {day.dayNumber}
                      </h2>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {day.spots.length}개 장소
                    </span>
                  </div>

                  {/* Day 스팟 목록 */}
                  <div className="p-4 sm:p-6">
                    {day.spots.length === 0 ? (
                      <div className="rounded-xl border border-dashed border-border py-8 text-center text-xs text-muted-foreground">
                        아직 계획이 없어요.
                      </div>
                    ) : (
                      <div className="space-y-3">
                        {day.spots.map((spot, idx) => (
                          <div
                            key={spot.spotId}
                            className="flex flex-col gap-3 rounded-xl border border-border bg-background p-3.5 shadow-sm transition-colors hover:border-primary/40 sm:flex-row sm:items-center sm:gap-4 sm:p-4"
                          >
                            <div className="flex items-center gap-3">
                              <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[11px] font-bold text-primary">
                                {idx + 1}
                              </span>
                              <div className="h-14 w-14 shrink-0 overflow-hidden rounded-lg bg-muted">
                                {spot.thumbnail ? (
                                  <img
                                    src={spot.thumbnail}
                                    alt={spot.title}
                                    className="h-full w-full object-cover"
                                  />
                                ) : (
                                  <div className="flex h-full w-full items-center justify-center text-muted-foreground">
                                    <MapPin className="h-5 w-5" />
                                  </div>
                                )}
                              </div>
                            </div>

                            <div className="min-w-0 flex-1">
                              <div className="flex items-center gap-2">
                                <Link
                                  to={`/spots/${spot.spotId}`}
                                  className="truncate text-sm font-bold text-foreground hover:text-primary"
                                >
                                  {spot.title}
                                </Link>
                                <span className="shrink-0 rounded-md bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                                  {spot.category}
                                </span>
                              </div>
                              <p className="mt-0.5 truncate text-xs text-muted-foreground">
                                {spot.address ||
                                  [spot.region, spot.sigungu].filter(Boolean).join(" ") ||
                                  "강원특별자치도"}
                              </p>
                              {spot.memo && (
                                <p className="mt-2 rounded-lg bg-muted/50 px-2.5 py-1.5 text-xs text-muted-foreground">
                                  💬 {spot.memo}
                                </p>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </section>
              ))}
            </div>

            {/* 하단 액션 버튼 */}
            <div className="flex justify-end gap-3 pt-4">
              <Link
                to="/courses"
                className="rounded-xl border border-border bg-background px-5 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
              >
                코스 목록
              </Link>
              <Link
                to="/courses/create"
                className="flex items-center gap-1.5 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow"
              >
                <Plus className="h-4 w-4" />
                새 코스 만들기
              </Link>
            </div>
          </div>
        )}
      </main>
      {inviteToast && <div className="fixed inset-x-4 top-20 z-[60] mx-auto max-w-md rounded-2xl border border-primary/20 bg-background p-4 shadow-2xl" role="status" aria-live="polite">
        <div className="flex items-start gap-3"><div className="min-w-0 flex-1"><p className="text-sm font-semibold text-foreground">초대 링크 준비 완료</p><p className="mt-1 text-sm text-muted-foreground">{inviteToast.message}</p><p className="mt-0.5 text-sm text-muted-foreground">카카오톡으로 바로 공유할까요?</p></div><button type="button" onClick={() => setInviteToast(null)} aria-label="알림 닫기" className="text-muted-foreground hover:text-foreground"><X className="h-4 w-4" /></button></div>
        {inviteToast.inviteUrl && <div className="mt-3 flex justify-end gap-2"><button type="button" onClick={() => setInviteToast(null)} className="rounded-lg border border-border px-3 py-2 text-xs font-medium">확인</button><button type="button" onClick={() => { window.location.href = "kakaotalk://"; }} className="rounded-lg bg-[#FEE500] px-3 py-2 text-xs font-semibold text-[#191919]">카카오톡으로 이동</button></div>}
      </div>}
      {showMembersTable && <div className="fixed inset-0 z-[55] flex items-center justify-center bg-foreground/40 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowMembersTable(false); }}><section role="dialog" aria-modal="true" className="w-full max-w-xl rounded-2xl border border-border bg-background p-6 shadow-2xl"><div className="flex items-center justify-between"><h2 className="text-lg font-bold">참여 중인 멤버</h2><button type="button" onClick={() => setShowMembersTable(false)} aria-label="닫기"><X className="h-5 w-5" /></button></div>{membersLoading ? <div className="flex h-24 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div> : <div className="mt-4 overflow-hidden rounded-xl border border-border"><table className="w-full text-left text-sm"><thead className="bg-muted/50 text-xs text-muted-foreground"><tr><th className="px-4 py-3">이름</th><th className="px-4 py-3">닉네임</th><th className="px-4 py-3">권한</th><th className="px-4 py-3">관리</th></tr></thead><tbody className="divide-y divide-border">{members.filter((m) => m.role !== "OWNER").map((member) => <tr key={member.userId}><td className="px-4 py-3">{member.name || "-"}</td><td className="px-4 py-3">{member.username || "-"}</td><td className="px-4 py-3"><select value={member.role} onChange={(e) => void updateCourseMemberRole(courseId!, member.userId, e.target.value as CourseInviteRole).then(() => setMembers((list) => list.map((m) => m.userId === member.userId ? { ...m, role: e.target.value as CourseMember["role"] } : m)))} className="rounded-lg border border-border px-2 py-1 text-xs"><option value="VIEWER">읽기 권한</option><option value="EDITOR">편집 권한</option></select></td><td className="px-4 py-3"><button type="button" onClick={() => void removeCourseMember(courseId!, member.userId).then(() => setMembers((list) => list.filter((m) => m.userId !== member.userId)))} className="text-destructive" aria-label="멤버 삭제"><Trash2 className="h-4 w-4" /></button></td></tr>)}</tbody></table>{members.filter((m) => m.role !== "OWNER").length === 0 && <p className="p-5 text-center text-sm text-muted-foreground">참여 중인 멤버가 없습니다.</p>}</div>}</section></div>}
      {membersDialogOpen && <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/40 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setMembersDialogOpen(false); }}><section role="dialog" aria-modal="true" aria-labelledby="members-title" className="w-full max-w-xl rounded-2xl border border-border bg-background p-6 shadow-2xl"><div className="flex items-center justify-between"><h2 id="members-title" className="text-lg font-bold">멤버 및 초대 관리</h2><button type="button" onClick={() => setMembersDialogOpen(false)} aria-label="닫기"><X className="h-5 w-5" /></button></div>{membersLoading ? <div className="flex h-32 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div> : <div className="mt-5 space-y-6"><div className="overflow-hidden rounded-xl border border-border"><table className="w-full text-left text-sm"><thead className="bg-muted/50 text-xs text-muted-foreground"><tr><th className="px-4 py-3 font-semibold">이름</th><th className="px-4 py-3 font-semibold">닉네임</th><th className="px-4 py-3 font-semibold">권한</th></tr></thead><tbody className="divide-y divide-border">{(members.filter((member) => member.role !== "OWNER").length ? members.filter((member) => member.role !== "OWNER") : [{ userId: -1, name: "김민수", username: "minsu", role: "VIEWER", joinedAt: "" }]).map((member) => <tr key={member.userId}><td className="px-4 py-3">{member.name || "-"}</td><td className="px-4 py-3 font-medium">{member.username || "-"}</td><td className="px-4 py-3">{member.userId === -1 ? <span className="text-xs text-muted-foreground">샘플</span> : <select value={member.role} onChange={(e) => void updateCourseMemberRole(courseId!, member.userId, e.target.value as CourseInviteRole).then(() => setMembers((current) => current.map((item) => item.userId === member.userId ? { ...item, role: e.target.value as CourseMember["role"] } : item)))} className="rounded-lg border border-border bg-background px-2 py-1 text-xs"><option value="VIEWER">읽기 권한</option><option value="EDITOR">편집 권한</option></select>}</td></tr>)}</tbody></table></div><MemberGroup title="승인 대기 초대" empty="승인을 기다리는 초대가 없습니다.">{pendingInvites.map((invite) => <div key={invite.token} className="flex items-center justify-between rounded-lg border border-border px-3 py-2 text-sm"><span>{invite.role === "EDITOR" ? "편집 권한" : "읽기 권한"}</span><button type="button" onClick={() => void cancelCourseInvite(courseId!, invite.token).then(() => setPendingInvites((current) => current.filter((item) => item.token !== invite.token)))} className="text-xs font-semibold text-destructive">초대 취소</button></div>)}</MemberGroup></div>}</section></div>}
      {inviteDialogOpen && <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/40 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setInviteDialogOpen(false); }}>
        <section role="dialog" aria-modal="true" aria-labelledby="invite-title" className="w-full max-w-md rounded-2xl border border-border bg-background p-6 shadow-2xl">
          <div className="flex items-center justify-between"><h2 id="invite-title" className="text-lg font-bold">친구 초대</h2><button type="button" onClick={() => setInviteDialogOpen(false)} aria-label="닫기"><X className="h-5 w-5" /></button></div>
          <p className="mt-2 text-sm text-muted-foreground">초대받은 친구에게 어떤 권한을 줄까요?</p>
          <div className="mt-5 grid gap-3">
            {([['EDITOR', '편집 권한', '일정과 메모를 함께 수정할 수 있어요.'], ['VIEWER', '읽기 권한', '코스를 보기만 할 수 있어요.']] as const).map(([role, label, description]) => <button key={role} type="button" onClick={() => setInviteRole(role)} className={`rounded-xl border p-4 text-left transition-colors ${inviteRole === role ? 'border-primary bg-primary/10' : 'border-border hover:bg-muted'}`}><span className="font-semibold">{label}</span><span className="mt-1 block text-xs text-muted-foreground">{description}</span></button>)}
          </div>
          <div className="mt-6 flex justify-end gap-2"><button type="button" onClick={() => setInviteDialogOpen(false)} className="rounded-xl border px-4 py-2 text-sm">취소</button><button type="button" onClick={() => void handleInvite()} disabled={creatingInvite} className="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50">{creatingInvite ? '링크 생성 중...' : '초대 링크 만들기'}</button></div>
        </section>
      </div>}
    </div>
  );
}

function MemberGroup({ title, empty, children }: { title: string; empty: string; children: ReactNode }) {
  return <div><div className="flex items-center justify-between"><h3 className="text-sm font-bold">{title}</h3></div><div className="mt-2 space-y-2">{children || <p className="rounded-lg border border-dashed border-border p-3 text-xs text-muted-foreground">{empty}</p>}</div></div>;
}

function MemberRow({ label, role }: { label: string; role: string }) {
  return <div className="flex items-center justify-between rounded-lg border border-border bg-muted/20 px-3 py-2.5"><span className="text-sm">{label}</span><span className="rounded-full bg-primary/10 px-2.5 py-1 text-[11px] font-semibold text-primary">{role}</span></div>;
}
