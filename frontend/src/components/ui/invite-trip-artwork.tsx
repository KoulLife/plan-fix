import { Flag, MapPin, Navigation, Sparkles } from "lucide-react";

/** 여행 초대 화면에 공통으로 사용하는 장식. 실제 일정 정보를 나타내지 않는다. */
export default function InviteTripArtwork({ compact = false }: { compact?: boolean }) {
  return (
    <div aria-hidden="true" className={`pointer-events-none relative mx-auto w-full ${compact ? "h-24 max-w-[240px]" : "h-32 max-w-[300px]"}`}>
      <div className="absolute inset-x-10 inset-y-0 rounded-full bg-primary/10 blur-2xl" />
      <svg viewBox="0 0 300 128" fill="none" className="absolute inset-0 h-full w-full text-primary/30">
        <path d="M40 89C76 120 102 28 151 49S213 109 264 43" stroke="currentColor" strokeWidth="2" strokeDasharray="4 7" strokeLinecap="round" />
        <circle cx="40" cy="89" r="4" fill="currentColor" />
        <circle cx="264" cy="43" r="4" fill="currentColor" />
      </svg>
      <div className="absolute left-[14%] top-[24%] flex h-14 w-14 -rotate-12 items-center justify-center rounded-2xl border border-primary/10 bg-background shadow-[0_8px_20px_-8px_hsl(var(--primary)/0.3)]">
        <MapPin className="h-7 w-7 text-primary" strokeWidth={1.7} />
      </div>
      <div className="absolute left-[44%] top-[2%] flex h-16 w-16 rotate-6 items-center justify-center rounded-[20px] bg-primary text-primary-foreground shadow-[0_12px_28px_-12px_hsl(var(--primary)/0.65)]">
        <Navigation className="h-7 w-7" strokeWidth={1.6} />
        <span className="absolute -bottom-1.5 -left-1.5 flex h-6 w-6 items-center justify-center rounded-full border-[3px] border-background bg-primary/15 text-primary"><Sparkles className="h-3 w-3" /></span>
      </div>
      <div className="absolute right-[7%] top-[47%] flex h-11 w-11 rotate-12 items-center justify-center rounded-2xl border border-primary/10 bg-background shadow-[0_8px_20px_-8px_hsl(var(--primary)/0.25)]">
        <Flag className="h-5 w-5 text-primary" strokeWidth={1.7} />
      </div>
      <Sparkles className="absolute right-[13%] top-[4%] h-4 w-4 text-primary/50" strokeWidth={1.5} />
      <span className="absolute bottom-[9%] left-[39%] h-1.5 w-1.5 rounded-full bg-primary/30" />
    </div>
  );
}
