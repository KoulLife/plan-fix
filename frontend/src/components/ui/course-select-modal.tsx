import { useEffect } from "react";
import { ArrowRight, MapPinPlus, Sparkles, X } from "lucide-react";

export interface CourseSelectModalProps {
  open: boolean;
  onClose: () => void;
  onSelectAi?: () => void;
  onSelectManual: () => void;
}

export default function CourseSelectModal({
  open,
  onClose,
  onSelectAi,
  onSelectManual,
}: CourseSelectModalProps) {
  useEffect(() => {
    if (!open) return undefined;

    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      data-testid="course-select-backdrop"
      className="fixed inset-0 z-50 flex items-end justify-center bg-foreground/30 p-0 backdrop-blur-[3px] sm:items-center sm:p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="course-select-title"
        aria-describedby="course-select-desc"
        className="relative w-full max-w-lg overflow-hidden rounded-t-3xl border border-border bg-background p-6 shadow-[0_24px_64px_hsl(var(--foreground)/0.18)] transition-all sm:rounded-2xl sm:p-7"
      >
        {/* 모바일 상단 드래그 핸들 */}
        <div className="mx-auto -mt-2 mb-4 h-1.5 w-12 rounded-full bg-muted-foreground/20 sm:hidden" />

        {/* 닫기 버튼 */}
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground sm:right-5 sm:top-5"
          aria-label="코스 선택 창 닫기"
        >
          <X className="h-5 w-5" aria-hidden="true" />
        </button>

        <header className="pr-8">
          <div className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
            <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
            <span>새로운 여행 계획</span>
          </div>
          <h2
            id="course-select-title"
            className="mt-2.5 text-2xl font-bold tracking-tight text-foreground sm:text-2xl"
          >
            여행 코스 만들기
          </h2>
          <p id="course-select-desc" className="mt-1.5 text-sm text-muted-foreground">
            어떤 방식으로 여행 일정을 계획해볼까요?
          </p>
        </header>

        <div className="mt-6 grid gap-3.5 sm:gap-4">
          {/* AI 코스 생성 버튼 (준비 중) */}
          <button
            type="button"
            disabled
            className="group relative flex cursor-not-allowed items-start gap-4 rounded-xl border border-border bg-muted/30 p-4 text-left opacity-75 sm:p-5"
          >
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-muted text-muted-foreground shadow-sm">
              <Sparkles className="h-6 w-6" strokeWidth={2.2} aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="text-base font-semibold text-foreground sm:text-lg">
                  AI 코스 생성
                </span>
                <span className="rounded-full bg-amber-500/15 px-2 py-0.5 text-[11px] font-semibold text-amber-600 dark:text-amber-400">
                  준비 중
                </span>
              </div>
              <p className="mt-1 text-xs leading-relaxed text-muted-foreground sm:text-sm">
                여행 지역과 테마, 일정만 알려주시면 AI가 최적의 동선과 장소를 추천해 드려요.
              </p>
            </div>
          </button>

          {/* 직접 코스 생성 버튼 */}
          <button
            type="button"
            onClick={onSelectManual}
            className="group relative flex items-start gap-4 rounded-xl border border-primary/40 bg-gradient-to-br from-primary/[0.08] via-background to-primary/[0.02] p-4 text-left transition-all hover:border-primary hover:shadow-md active:scale-[0.99] sm:p-5"
          >
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm transition-transform duration-200 group-hover:scale-105">
              <MapPinPlus className="h-6 w-6" strokeWidth={2} aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="text-base font-semibold text-foreground group-hover:text-primary sm:text-lg">
                  직접 코스 생성
                </span>
                <span className="rounded-full bg-primary/15 px-2 py-0.5 text-[11px] font-semibold text-primary">
                  추천
                </span>
              </div>
              <p className="mt-1 text-xs leading-relaxed text-muted-foreground sm:text-sm">
                강원도의 다양한 인기 명소와 맛집을 직접 골라 Day별로 나만의 특별한 여행 코스를 설계해요.
              </p>
            </div>
            <ArrowRight
              className="mt-1 h-5 w-5 shrink-0 text-muted-foreground transition-transform duration-200 group-hover:translate-x-1 group-hover:text-primary"
              aria-hidden="true"
            />
          </button>
        </div>
      </section>
    </div>
  );
}
