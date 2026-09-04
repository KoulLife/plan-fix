import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowDown,
  ArrowUp,
  Calendar,
  ChevronRight,
  Info,
  MapPin,
  Plus,
  Trash2,
} from "lucide-react";
import AppNav from "@/components/ui/app-nav";
import SpotSearchModal from "@/components/ui/spot-search-modal";
import { createCourse } from "@/services/course";
import { PopularSpot, UnauthorizedError } from "@/services/spots";

const DRAFT_STORAGE_KEY = "planfix:course-draft";

export type DraftSpot = {
  spotId: number;
  title: string;
  category: string;
  region: string | null;
  sigungu: string | null;
  thumbnail: string | null;
  memo: string;
};

export type CourseDraft = {
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  days: DraftSpot[][];
};

function formatDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function calculateDayCount(startStr: string, endStr: string): number {
  if (!startStr || !endStr) return 1;
  const start = new Date(startStr);
  const end = new Date(endStr);
  const diffTime = end.getTime() - start.getTime();
  const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24)) + 1;
  return Math.max(1, Math.min(30, diffDays));
}

export default function CourseCreatePage() {
  const navigate = useNavigate();

  const todayStr = useMemo(() => formatDate(new Date()), []);
  const defaultEndStr = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() + 2); // 기본 2박 3일
    return formatDate(d);
  }, []);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startDate, setStartDate] = useState(todayStr);
  const [endDate, setEndDate] = useState(defaultEndStr);
  const [days, setDays] = useState<DraftSpot[][]>(() => {
    const initialDaysCount = calculateDayCount(todayStr, defaultEndStr);
    return Array.from({ length: initialDaysCount }, () => []);
  });

  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 검색 모달 상태
  const [searchModalOpen, setSearchModalOpen] = useState(false);
  const [activeDayIndex, setActiveDayIndex] = useState<number | null>(null);

  // 초기에 sessionStorage에서 복원
  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(DRAFT_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as CourseDraft;
        if (parsed.title !== undefined) setTitle(parsed.title);
        if (parsed.description !== undefined) setDescription(parsed.description);
        if (parsed.startDate) setStartDate(parsed.startDate);
        if (parsed.endDate) setEndDate(parsed.endDate);
        if (Array.isArray(parsed.days) && parsed.days.length > 0) {
          setDays(parsed.days);
        }
      }
    } catch {
      // sessionStorage 파싱 오류 무시
    }
  }, []);

  // 상태 변경 시 sessionStorage에 자동 저장
  useEffect(() => {
    try {
      const draft: CourseDraft = {
        title,
        description,
        startDate,
        endDate,
        days,
      };
      sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
    } catch {
      // sessionStorage 저장 오류 무시
    }
  }, [title, description, startDate, endDate, days]);

  // 날짜 변경 핸들러
  const handleStartDateChange = (newStart: string) => {
    setStartDate(newStart);
    let newEnd = endDate;
    if (newStart > endDate) {
      newEnd = newStart;
      setEndDate(newStart);
    }
    syncDaysDuration(newStart, newEnd);
  };

  const handleEndDateChange = (newEnd: string) => {
    if (newEnd < startDate) {
      return;
    }
    setEndDate(newEnd);
    syncDaysDuration(startDate, newEnd);
  };

  const syncDaysDuration = (start: string, end: string) => {
    const targetCount = calculateDayCount(start, end);
    if (targetCount === days.length) return;

    if (targetCount < days.length) {
      const removedDays = days.slice(targetCount);
      const hasSpotsInRemoved = removedDays.some((d) => d.length > 0);
      if (hasSpotsInRemoved) {
        const confirmed = window.confirm(
          "선택한 기간을 줄이면 삭제되는 일차의 장소 목록이 사라집니다. 계속하시겠습니까?"
        );
        if (!confirmed) {
          return;
        }
      }
      setDays(days.slice(0, targetCount));
    } else {
      const additionalCount = targetCount - days.length;
      const newDays = [...days];
      for (let i = 0; i < additionalCount; i++) {
        newDays.push([]);
      }
      setDays(newDays);
    }
  };

  // 장소 추가 모달 열기
  const handleOpenSearchModal = (dayIndex: number) => {
    setActiveDayIndex(dayIndex);
    setSearchModalOpen(true);
  };

  // 장소 추가 완료
  const handleSelectSpot = (spot: PopularSpot) => {
    if (activeDayIndex === null) return;
    const newDraftSpot: DraftSpot = {
      spotId: spot.spotId,
      title: spot.title,
      category: spot.category,
      region: spot.region,
      sigungu: spot.sigungu,
      thumbnail: spot.thumbnail,
      memo: "",
    };

    setDays((prev) => {
      const next = [...prev];
      next[activeDayIndex] = [...next[activeDayIndex], newDraftSpot];
      return next;
    });
  };

  // 장소 삭제
  const handleRemoveSpot = (dayIndex: number, spotIndex: number) => {
    setDays((prev) => {
      const next = [...prev];
      next[dayIndex] = next[dayIndex].filter((_, idx) => idx !== spotIndex);
      return next;
    });
  };

  // 같은 Day 내 순서 이동
  const handleMoveSpot = (dayIndex: number, spotIndex: number, direction: "up" | "down") => {
    setDays((prev) => {
      const next = [...prev];
      const daySpots = [...next[dayIndex]];
      const targetIndex = direction === "up" ? spotIndex - 1 : spotIndex + 1;
      if (targetIndex < 0 || targetIndex >= daySpots.length) return prev;

      const temp = daySpots[spotIndex];
      daySpots[spotIndex] = daySpots[targetIndex];
      daySpots[targetIndex] = temp;
      next[dayIndex] = daySpots;
      return next;
    });
  };

  // 다른 Day로 장소 이동
  const handleMoveSpotToDay = (
    sourceDayIndex: number,
    spotIndex: number,
    targetDayIndex: number
  ) => {
    if (sourceDayIndex === targetDayIndex) return;
    setDays((prev) => {
      const next = [...prev];
      const spot = next[sourceDayIndex][spotIndex];
      // 타겟 Day에 이미 같은 spotId가 있는지 체크
      if (next[targetDayIndex].some((s) => s.spotId === spot.spotId)) {
        alert("해당 일차에 이미 같은 장소가 추가되어 있습니다.");
        return prev;
      }
      next[sourceDayIndex] = next[sourceDayIndex].filter((_, idx) => idx !== spotIndex);
      next[targetDayIndex] = [...next[targetDayIndex], spot];
      return next;
    });
  };

  // 메모 변경
  const handleMemoChange = (dayIndex: number, spotIndex: number, memo: string) => {
    setDays((prev) => {
      const next = [...prev];
      const daySpots = [...next[dayIndex]];
      daySpots[spotIndex] = { ...daySpots[spotIndex], memo };
      next[dayIndex] = daySpots;
      return next;
    });
  };

  // 유효성 검사
  const totalSpotCount = days.reduce((sum, d) => sum + d.length, 0);
  const isValid = title.trim().length > 0 && totalSpotCount > 0;

  // 저장 요청
  const handleSaveCourse = async () => {
    if (!isValid || submitting) return;

    setSubmitting(true);
    setErrorMessage(null);

    try {
      const payload = {
        title: title.trim(),
        description: description.trim() || null,
        startDate,
        endDate,
        days: days.map((daySpots, idx) => ({
          dayNumber: idx + 1,
          spots: daySpots.map((s) => ({
            spotId: s.spotId,
            memo: s.memo.trim() || null,
          })),
        })),
      };

      const result = await createCourse(payload);
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      navigate(`/courses/${result.courseId}`, { replace: true });
    } catch (err) {
      if (err instanceof UnauthorizedError) {
        alert("로그인이 필요합니다. 로그인 페이지로 이동합니다.");
        navigate("/login");
        return;
      }
      setErrorMessage(err instanceof Error ? err.message : "코스 저장에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-muted/20 pb-20">
      <AppNav />

      <main className="mx-auto max-w-4xl px-4 pt-6 sm:px-6 sm:pt-8">
        {/* 상단 브레드크럼 */}
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <span>여행</span>
          <ChevronRight className="h-3.5 w-3.5" />
          <span className="font-medium text-foreground">직접 코스 생성</span>
        </div>

        {/* 헤더 및 저장 버튼 */}
        <div className="mt-4 flex flex-col justify-between gap-4 border-b border-border pb-5 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              나만의 여행 코스 만들기
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              여행 일정과 방문할 명소들을 Day별로 자유롭게 계획해보세요.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
            >
              취소
            </button>
            <button
              type="button"
              disabled={!isValid || submitting}
              onClick={handleSaveCourse}
              className="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow transition-transform active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? "저장 중..." : "코스 저장하기"}
            </button>
          </div>
        </div>

        {/* 에러 메시지 */}
        {errorMessage && (
          <div className="mt-4 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm font-medium text-destructive">
            {errorMessage}
          </div>
        )}

        {/* 유효성 안내 */}
        {!isValid && (
          <div className="mt-4 flex items-center gap-2 rounded-xl border border-border bg-card/60 p-3.5 text-xs text-muted-foreground">
            <Info className="h-4 w-4 shrink-0 text-primary" />
            <span>
              {title.trim().length === 0
                ? "코스 제목을 입력해주세요."
                : "최소 1개 이상의 장소를 일정에 추가해야 저장할 수 있습니다."}
            </span>
          </div>
        )}

        {/* 기본 정보 설정 카드 */}
        <div className="mt-6 rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
          <h2 className="text-base font-semibold text-foreground">여행 기본 정보</h2>

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label htmlFor="course-title" className="block text-xs font-semibold text-muted-foreground">
                코스 제목 <span className="text-destructive">*</span>
              </label>
              <input
                id="course-title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: 2박 3일 강릉 힐링 힐링 바다 여행"
                className="mt-1.5 w-full rounded-xl border border-input bg-background px-3.5 py-2.5 text-sm text-foreground shadow-sm placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                maxLength={100}
              />
            </div>

            <div className="sm:col-span-2">
              <label htmlFor="course-desc" className="block text-xs font-semibold text-muted-foreground">
                코스 소개 (선택)
              </label>
              <textarea
                id="course-desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="어떤 테마의 여행인지 간단히 메모해보세요."
                rows={2}
                className="mt-1.5 w-full rounded-xl border border-input bg-background px-3.5 py-2.5 text-sm text-foreground shadow-sm placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                maxLength={500}
              />
            </div>

            {/* 일정 선택 */}
            <div>
              <label htmlFor="course-start-date" className="block text-xs font-semibold text-muted-foreground">
                시작일
              </label>
              <div className="relative mt-1.5">
                <input
                  id="course-start-date"
                  type="date"
                  value={startDate}
                  onChange={(e) => handleStartDateChange(e.target.value)}
                  className="w-full rounded-xl border border-input bg-background px-3.5 py-2.5 text-sm text-foreground shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>

            <div>
              <label htmlFor="course-end-date" className="block text-xs font-semibold text-muted-foreground">
                종료일 ({days.length}일 일정)
              </label>
              <div className="relative mt-1.5">
                <input
                  id="course-end-date"
                  type="date"
                  min={startDate}
                  value={endDate}
                  onChange={(e) => handleEndDateChange(e.target.value)}
                  className="w-full rounded-xl border border-input bg-background px-3.5 py-2.5 text-sm text-foreground shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Day별 일정 섹션 */}
        <div className="mt-8 space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-foreground">
              상세 일정 ({days.length}일)
            </h2>
            <span className="text-xs text-muted-foreground">
              총 <span className="font-semibold text-primary">{totalSpotCount}</span>개 장소 선택됨
            </span>
          </div>

          {days.map((daySpots, dayIndex) => {
            const dayNumber = dayIndex + 1;
            return (
              <div
                key={`day-${dayNumber}`}
                data-testid={`day-card-${dayNumber}`}
                className="overflow-hidden rounded-2xl border border-border bg-card shadow-sm transition-all"
              >
                {/* Day 헤더 */}
                <div className="flex items-center justify-between border-b border-border bg-muted/30 px-5 py-3.5 sm:px-6">
                  <div className="flex items-center gap-3">
                    <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary font-bold text-xs text-primary-foreground shadow-sm">
                      D{dayNumber}
                    </span>
                    <div>
                      <h3 className="text-sm font-bold text-foreground sm:text-base">
                        Day {dayNumber}
                      </h3>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleOpenSearchModal(dayIndex)}
                    className="flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-primary/20"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    장소 추가
                  </button>
                </div>

                {/* Day 장소 목록 */}
                <div className="p-4 sm:p-6">
                  {daySpots.length === 0 ? (
                    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border py-8 text-center text-muted-foreground">
                      <Calendar className="h-7 w-7 text-muted-foreground/40" />
                      <p className="mt-2 text-xs font-medium">
                        Day {dayNumber}에 담긴 장소가 없습니다.
                      </p>
                      <button
                        type="button"
                        onClick={() => handleOpenSearchModal(dayIndex)}
                        className="mt-2.5 text-xs font-semibold text-primary hover:underline"
                      >
                        + 명소 및 맛집 검색하여 추가하기
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {daySpots.map((spot, spotIndex) => (
                        <div
                          key={`${spot.spotId}-${spotIndex}`}
                          className="flex flex-col gap-3 rounded-xl border border-border bg-background p-3.5 shadow-sm sm:flex-row sm:items-center sm:gap-4 sm:p-4"
                        >
                          {/* 번호 및 썸네일 */}
                          <div className="flex items-center gap-3">
                            <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-muted text-[11px] font-bold text-muted-foreground">
                              {spotIndex + 1}
                            </span>
                            <div className="h-12 w-12 shrink-0 overflow-hidden rounded-lg bg-muted">
                              {spot.thumbnail ? (
                                <img
                                  src={spot.thumbnail}
                                  alt={spot.title}
                                  className="h-full w-full object-cover"
                                />
                              ) : (
                                <div className="flex h-full w-full items-center justify-center text-muted-foreground">
                                  <MapPin className="h-4 w-4" />
                                </div>
                              )}
                            </div>
                          </div>

                          {/* 장소 정보 및 메모 */}
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                              <span className="truncate text-sm font-bold text-foreground">
                                {spot.title}
                              </span>
                              <span className="shrink-0 rounded-md bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                                {spot.category}
                              </span>
                            </div>
                            <input
                              type="text"
                              value={spot.memo}
                              onChange={(e) =>
                                handleMemoChange(dayIndex, spotIndex, e.target.value)
                              }
                              placeholder="메모 입력 (예: 점심 식사, 입장료 5000원)"
                              className="mt-1.5 w-full rounded-lg border border-input bg-card/40 px-2.5 py-1 text-xs text-foreground placeholder:text-muted-foreground/70 focus:border-primary focus:outline-none"
                              maxLength={200}
                            />
                          </div>

                          {/* 액션 컨트롤 */}
                          <div className="flex items-center justify-between gap-1 border-t border-border/50 pt-2 sm:border-0 sm:pt-0">
                            {/* 다른 Day로 이동 선택 */}
                            <select
                              aria-label="이동할 일차 선택"
                              value={dayIndex}
                              onChange={(e) =>
                                handleMoveSpotToDay(
                                  dayIndex,
                                  spotIndex,
                                  Number(e.target.value)
                                )
                              }
                              className="rounded-lg border border-input bg-background px-2 py-1 text-xs text-muted-foreground focus:border-primary focus:outline-none"
                            >
                              {days.map((_, targetIdx) => (
                                <option key={targetIdx} value={targetIdx}>
                                  Day {targetIdx + 1}로 이동
                                </option>
                              ))}
                            </select>

                            <div className="flex items-center gap-1">
                              {/* 위로 이동 */}
                              <button
                                type="button"
                                disabled={spotIndex === 0}
                                onClick={() => handleMoveSpot(dayIndex, spotIndex, "up")}
                                className="flex h-7 w-7 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-30"
                                aria-label="위로 이동"
                              >
                                <ArrowUp className="h-3.5 w-3.5" />
                              </button>

                              {/* 아래로 이동 */}
                              <button
                                type="button"
                                disabled={spotIndex === daySpots.length - 1}
                                onClick={() => handleMoveSpot(dayIndex, spotIndex, "down")}
                                className="flex h-7 w-7 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-30"
                                aria-label="아래로 이동"
                              >
                                <ArrowDown className="h-3.5 w-3.5" />
                              </button>

                              {/* 삭제 */}
                              <button
                                type="button"
                                onClick={() => handleRemoveSpot(dayIndex, spotIndex)}
                                className="flex h-7 w-7 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
                                aria-label="장소 삭제"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </main>

      {/* 장소 검색 모달 */}
      {activeDayIndex !== null && (
        <SpotSearchModal
          open={searchModalOpen}
          onClose={() => {
            setSearchModalOpen(false);
            setActiveDayIndex(null);
          }}
          onSelect={handleSelectSpot}
          excludedSpotIds={days[activeDayIndex]?.map((s) => s.spotId) || []}
          dayNumber={activeDayIndex + 1}
        />
      )}
    </div>
  );
}
