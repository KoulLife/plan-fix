import { useEffect, useState } from "react";
import { Loader2, MapPin, Search, Sparkles, X } from "lucide-react";

import { sigunguCodeByRegion, type GangwonRegion } from "@/components/ui/gangwon-region-map";
import {
  fetchAiCourseDraft,
  type AiCourseCompanion,
  type AiCourseDraft,
  type AiCourseTheme,
} from "@/services/ai-course";
import { searchSpots, UnauthorizedError, type PopularSpot } from "@/services/spots";

const GANGWON_REGION_CODE = "51";

const COMPANION_OPTIONS: { value: AiCourseCompanion; label: string }[] = [
  { value: "SOLO", label: "혼자" },
  { value: "COUPLE", label: "연인" },
  { value: "FRIENDS", label: "친구" },
  { value: "FAMILY", label: "가족(아이 동반)" },
];

const THEME_OPTIONS: { value: AiCourseTheme; label: string }[] = [
  { value: "HEALING", label: "힐링·자연" },
  { value: "FOOD", label: "맛집 탐방" },
  { value: "CAFE", label: "카페 투어" },
  { value: "ACTIVITY", label: "액티비티" },
  { value: "CULTURE", label: "문화·역사" },
];

const REGION_OPTIONS = Object.keys(sigunguCodeByRegion) as GangwonRegion[];

type AiCourseModalProps = {
  open: boolean;
  /** 코스 생성 화면에서 이미 고른 기간. 여기서 다시 묻지 않는다. */
  startDate: string;
  endDate: string;
  onClose: () => void;
  onApply: (draft: AiCourseDraft) => void;
};

export default function AiCourseModal({ open, startDate, endDate, onClose, onApply }: AiCourseModalProps) {
  const [region, setRegion] = useState<GangwonRegion | null>(null);
  const [companion, setCompanion] = useState<AiCourseCompanion>("COUPLE");
  const [themes, setThemes] = useState<AiCourseTheme[]>([]);
  const [anchors, setAnchors] = useState<PopularSpot[]>([]);

  const [anchorKeyword, setAnchorKeyword] = useState("");
  const [anchorResults, setAnchorResults] = useState<PopularSpot[]>([]);
  const [anchorSearching, setAnchorSearching] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return undefined;

    setRegion(null);
    setCompanion("COUPLE");
    setThemes([]);
    setAnchors([]);
    setAnchorKeyword("");
    setAnchorResults([]);
    setError(null);

    const previousOverflow = document.body.style.overflow;
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open, onClose]);

  // 고정 장소 검색 (300ms 디바운스)
  useEffect(() => {
    if (!open) return undefined;
    const keyword = anchorKeyword.trim();
    if (!keyword) {
      setAnchorResults([]);
      return undefined;
    }

    let ignore = false;
    setAnchorSearching(true);
    const timer = setTimeout(() => {
      searchSpots({ keyword, size: 5, region: GANGWON_REGION_CODE })
        .then((res) => {
          if (!ignore) setAnchorResults(res.items ?? []);
        })
        .catch(() => {
          if (!ignore) setAnchorResults([]);
        })
        .finally(() => {
          if (!ignore) setAnchorSearching(false);
        });
    }, 300);

    return () => {
      ignore = true;
      clearTimeout(timer);
    };
  }, [anchorKeyword, open]);

  if (!open) return null;

  const toggleTheme = (theme: AiCourseTheme) => {
    setThemes((prev) => (prev.includes(theme) ? prev.filter((t) => t !== theme) : [...prev, theme]));
  };

  const addAnchor = (spot: PopularSpot) => {
    setAnchors((prev) => (prev.some((s) => s.spotId === spot.spotId) ? prev : [...prev, spot]));
    setAnchorKeyword("");
    setAnchorResults([]);
  };

  const handleSubmit = async () => {
    if (!region || submitting) return;

    setSubmitting(true);
    setError(null);
    try {
      const draft = await fetchAiCourseDraft({
        region: GANGWON_REGION_CODE,
        sigungu: sigunguCodeByRegion[region],
        startDate,
        endDate,
        themes,
        companion,
        anchorSpotIds: anchors.map((a) => a.spotId),
      });
      onApply(draft);
    } catch (err) {
      if (err instanceof UnauthorizedError) {
        setError("로그인이 필요한 기능이에요.");
      } else {
        setError(err instanceof Error ? err.message : "AI 코스를 만들지 못했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-foreground/30 backdrop-blur-[3px] sm:items-center sm:p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="ai-course-title"
        className="relative flex h-[88vh] w-full max-w-lg flex-col overflow-hidden rounded-t-3xl border border-border bg-background shadow-2xl sm:h-auto sm:max-h-[88vh] sm:rounded-2xl"
      >
        <header className="flex items-start justify-between border-b border-border px-5 py-4">
          <div>
            <div className="flex items-center gap-2">
              <Sparkles className="h-4.5 w-4.5 text-primary" aria-hidden="true" />
              <h2 id="ai-course-title" className="text-lg font-bold text-foreground">
                AI에게 코스 맡기기
              </h2>
            </div>
            <p className="mt-0.5 text-xs text-muted-foreground">
              몇 가지만 알려주시면 {startDate.replace(/-/g, ".")} ~ {endDate.replace(/-/g, ".")} 일정에 맞춰 짜드려요.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="창 닫기"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <X className="h-4.5 w-4.5" aria-hidden="true" />
          </button>
        </header>

        <div className="min-h-0 flex-1 space-y-5 overflow-y-auto px-5 py-4">
          {/* 지역 */}
          <section>
            <h3 className="text-sm font-semibold text-foreground">
              어디로 떠나시나요? <span className="text-destructive">*</span>
            </h3>
            <div role="group" aria-label="지역 선택" className="mt-2 flex flex-wrap gap-1.5">
              {REGION_OPTIONS.map((name) => (
                <button
                  key={name}
                  type="button"
                  onClick={() => setRegion(name)}
                  aria-pressed={region === name}
                  className={`rounded-full border px-3 py-1 text-xs font-semibold transition-colors ${
                    region === name
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border bg-background text-foreground hover:bg-muted"
                  }`}
                >
                  {name}
                </button>
              ))}
            </div>
          </section>

          {/* 동행 */}
          <section>
            <h3 className="text-sm font-semibold text-foreground">누구와 가시나요?</h3>
            <div role="group" aria-label="동행 선택" className="mt-2 flex flex-wrap gap-1.5">
              {COMPANION_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => setCompanion(option.value)}
                  aria-pressed={companion === option.value}
                  className={`rounded-full border px-3 py-1 text-xs font-semibold transition-colors ${
                    companion === option.value
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border bg-background text-foreground hover:bg-muted"
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </section>

          {/* 테마 */}
          <section>
            <h3 className="text-sm font-semibold text-foreground">
              어떤 여행을 원하세요? <span className="font-normal text-muted-foreground">(복수 선택)</span>
            </h3>
            <div role="group" aria-label="테마 선택" className="mt-2 flex flex-wrap gap-1.5">
              {THEME_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => toggleTheme(option.value)}
                  aria-pressed={themes.includes(option.value)}
                  className={`rounded-full border px-3 py-1 text-xs font-semibold transition-colors ${
                    themes.includes(option.value)
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border bg-background text-foreground hover:bg-muted"
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </section>

          {/* 고정 장소 */}
          <section>
            <h3 className="text-sm font-semibold text-foreground">
              꼭 가고 싶은 곳이 있나요? <span className="font-normal text-muted-foreground">(선택)</span>
            </h3>
            <p className="mt-0.5 text-xs text-muted-foreground">
              여기 넣은 곳은 반드시 코스에 들어가고, 그 주변으로 일정이 짜여요.
            </p>

            {anchors.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1.5">
                {anchors.map((anchor) => (
                  <span
                    key={anchor.spotId}
                    className="flex items-center gap-1 rounded-full bg-primary/10 py-1 pl-3 pr-1 text-xs font-semibold text-primary"
                  >
                    {anchor.title}
                    <button
                      type="button"
                      onClick={() => setAnchors((prev) => prev.filter((s) => s.spotId !== anchor.spotId))}
                      aria-label={`${anchor.title} 고정 해제`}
                      className="flex h-5 w-5 items-center justify-center rounded-full transition-colors hover:bg-primary/20"
                    >
                      <X className="h-3 w-3" aria-hidden="true" />
                    </button>
                  </span>
                ))}
              </div>
            )}

            <div className="relative mt-2">
              <Search
                className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground"
                aria-hidden="true"
              />
              <input
                type="text"
                value={anchorKeyword}
                onChange={(e) => setAnchorKeyword(e.target.value)}
                placeholder="장소 이름으로 검색 (예: 경포해변)"
                aria-label="고정할 장소 검색"
                className="w-full rounded-xl border border-input bg-background py-2 pl-9 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            {anchorSearching && (
              <p className="mt-2 text-xs text-muted-foreground">검색 중...</p>
            )}

            {anchorResults.length > 0 && (
              <ul className="mt-2 space-y-1">
                {anchorResults.map((spot) => (
                  <li key={spot.spotId}>
                    <button
                      type="button"
                      onClick={() => addAnchor(spot)}
                      className="flex w-full items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-left text-xs transition-colors hover:border-primary/50 hover:bg-muted/40"
                    >
                      <MapPin className="h-3.5 w-3.5 shrink-0 text-muted-foreground" aria-hidden="true" />
                      <span className="truncate font-medium text-foreground">{spot.title}</span>
                      <span className="ml-auto shrink-0 text-[11px] text-muted-foreground">{spot.category}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {error && (
            <p role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-xs font-medium text-destructive">
              {error}
            </p>
          )}
        </div>

        <footer className="flex shrink-0 items-center justify-end gap-2 border-t border-border px-5 py-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl px-4 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!region || submitting}
            className="flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:pointer-events-none disabled:opacity-40"
          >
            {submitting ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                코스 짜는 중...
              </>
            ) : (
              <>
                <Sparkles className="h-4 w-4" aria-hidden="true" />
                코스 만들기
              </>
            )}
          </button>
        </footer>
      </section>
    </div>
  );
}
