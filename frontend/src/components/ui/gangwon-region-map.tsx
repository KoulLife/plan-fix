import {
  useEffect,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
} from "react";
import { Check, MapPinned, X } from "lucide-react";

import { gangwonMapPaths } from "@/assets/gangwon-map-paths";

export type GangwonRegion =
  | "철원"
  | "화천"
  | "양구"
  | "고성"
  | "춘천"
  | "홍천"
  | "인제"
  | "속초"
  | "양양"
  | "원주"
  | "횡성"
  | "평창"
  | "강릉"
  | "영월"
  | "정선"
  | "동해"
  | "태백"
  | "삼척";

type RegionMapItem = {
  name: GangwonRegion;
  mapId: string;
  label: [number, number];
};

const regionMapItems: RegionMapItem[] = [
  { name: "철원", mapId: "철원군", label: [120, 174] },
  { name: "화천", mapId: "화천군", label: [208, 210] },
  { name: "양구", mapId: "양구군", label: [315, 195] },
  { name: "고성", mapId: "고성군", label: [446, 94] },
  { name: "춘천", mapId: "춘천시", label: [218, 330] },
  { name: "홍천", mapId: "홍천군", label: [352, 390] },
  { name: "인제", mapId: "인제군", label: [405, 240] },
  { name: "속초", mapId: "속초시", label: [493, 198] },
  { name: "양양", mapId: "양양군", label: [525, 275] },
  { name: "원주", mapId: "원주시", label: [295, 585] },
  { name: "횡성", mapId: "횡성군", label: [335, 505] },
  { name: "평창", mapId: "평창군", label: [482, 470] },
  { name: "강릉", mapId: "강릉시", label: [600, 390] },
  { name: "영월", mapId: "영월군", label: [480, 635] },
  { name: "정선", mapId: "정선군", label: [565, 535] },
  { name: "동해", mapId: "동해시", label: [685, 485] },
  { name: "태백", mapId: "태백시", label: [670, 640] },
  { name: "삼척", mapId: "삼척시", label: [725, 580] },
];

const regionByMapId = Object.fromEntries(
  regionMapItems.map((region) => [region.mapId, region.name]),
) as Record<string, GangwonRegion>;

const mapIdByRegion = Object.fromEntries(
  regionMapItems.map((region) => [region.name, region.mapId]),
) as Record<GangwonRegion, string>;

type GangwonRegionMapProps = {
  open: boolean;
  selectedRegion: GangwonRegion | null;
  onClose: () => void;
  onSelect: (region: GangwonRegion) => void;
};

const getRegionFromMapTarget = (target: EventTarget): GangwonRegion | null => {
  const element = target as Element;
  return element.tagName?.toLowerCase() === "path"
    ? regionByMapId[element.id] ?? null
    : null;
};

export default function GangwonRegionMap({
  open,
  selectedRegion,
  onClose,
  onSelect,
}: GangwonRegionMapProps) {
  const [hoveredRegion, setHoveredRegion] = useState<GangwonRegion | null>(null);
  const [pendingRegion, setPendingRegion] = useState<GangwonRegion | null>(selectedRegion);

  useEffect(() => {
    if (!open) return undefined;

    setPendingRegion(selectedRegion);
    setHoveredRegion(null);

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
  }, [open, onClose, selectedRegion]);

  if (!open) return null;

  const activeRegion = hoveredRegion ?? pendingRegion;
  const activeMapId = activeRegion ? mapIdByRegion[activeRegion] : null;

  const handleRegionKeyDown = (
    event: ReactKeyboardEvent<SVGGElement>,
    region: GangwonRegion,
  ) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      setPendingRegion(region);
    }
  };

  const handleMapMouseMove = (event: ReactMouseEvent<SVGSVGElement>) => {
    const region = getRegionFromMapTarget(event.target);
    if (region && region !== hoveredRegion) setHoveredRegion(region);
  };

  const handleMapClick = (event: ReactMouseEvent<SVGSVGElement>) => {
    const region = getRegionFromMapTarget(event.target);
    if (region) setPendingRegion(region);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/25 p-4 backdrop-blur-[3px]"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="region-dialog-title"
        className="relative max-h-[92vh] w-full max-w-5xl overflow-y-auto rounded-lg border border-slate-200 bg-white shadow-[0_28px_80px_rgba(15,23,42,0.24)]"
      >
        <div className="sticky top-0 z-20 flex items-center justify-end bg-white/95 px-4 py-3 backdrop-blur sm:px-6">
          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900"
            aria-label="지역 선택 창 닫기"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="px-5 pb-7 sm:px-8 sm:pb-9">
          <header className="mx-auto max-w-2xl text-center">
            <h2
              id="region-dialog-title"
              className="text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl"
            >
              어디로 떠나볼까요?
            </h2>
            <p className="mt-3 text-sm leading-6 text-slate-500 sm:text-base">
              강원도 18개 시·군 중 여행할 지역을 선택해 주세요.
            </p>
          </header>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_240px] lg:items-stretch">
            <div className="relative overflow-hidden rounded-lg border border-slate-100 bg-gradient-to-br from-slate-50 via-white to-blue-50 p-3 sm:p-5">
              <style>{`
                #gangwon-boundary-map path {
                  fill: #e9eef3;
                  stroke: #cbd5e1;
                  stroke-width: 1.5px;
                  vector-effect: non-scaling-stroke;
                  cursor: pointer;
                  transition: fill 180ms ease, stroke 180ms ease, filter 180ms ease;
                }
                #gangwon-boundary-map path:hover {
                  fill: hsl(var(--primary) / 0.28);
                  stroke: hsl(var(--primary));
                }
                ${
                  activeMapId
                    ? `#gangwon-boundary-map path#${activeMapId} {
                        fill: hsl(var(--primary) / 0.72);
                        stroke: hsl(var(--primary));
                        filter: url(#selected-region-shadow);
                      }`
                    : ""
                }
              `}</style>

              <div className="relative mx-auto aspect-[800/699] w-full max-w-[640px]">
                <svg className="pointer-events-none absolute h-0 w-0" aria-hidden="true">
                  <defs>
                    <filter id="selected-region-shadow" x="-30%" y="-30%" width="160%" height="160%">
                      <feDropShadow
                        dx="0"
                        dy="5"
                        stdDeviation="7"
                        floodColor="hsl(var(--primary))"
                        floodOpacity="0.28"
                      />
                    </filter>
                  </defs>
                </svg>

                <svg
                  id="gangwon-boundary-map"
                  data-testid="gangwon-boundary-map"
                  data-active-region={activeRegion ?? ""}
                  className="absolute inset-0 h-full w-full drop-shadow-[0_16px_18px_rgba(15,23,42,0.12)]"
                  viewBox="0 0 800 699"
                  role="presentation"
                  aria-hidden="true"
                  onMouseMove={handleMapMouseMove}
                  onMouseLeave={() => setHoveredRegion(null)}
                  onClick={handleMapClick}
                >
                  <g>
                    {gangwonMapPaths.map((region) => (
                      <path key={region.id} id={region.id} d={region.d} />
                    ))}
                  </g>
                </svg>

                <svg
                  className="pointer-events-none absolute inset-0 h-full w-full"
                  viewBox="0 0 800 699"
                  role="group"
                  aria-label="강원도 18개 시군 선택 지도"
                >
                  {regionMapItems.map((region) => {
                    const isActive = activeRegion === region.name;
                    const isSelected = pendingRegion === region.name;

                    return (
                      <g
                        key={region.name}
                        role="button"
                        tabIndex={0}
                        aria-label={region.name}
                        aria-pressed={isSelected}
                        className="pointer-events-auto cursor-pointer outline-none"
                        onMouseEnter={() => setHoveredRegion(region.name)}
                        onMouseLeave={() => setHoveredRegion(null)}
                        onFocus={() => setHoveredRegion(region.name)}
                        onBlur={() => setHoveredRegion(null)}
                        onClick={() => setPendingRegion(region.name)}
                        onKeyDown={(event) => handleRegionKeyDown(event, region.name)}
                      >
                        <rect
                          x={region.label[0] - 30}
                          y={region.label[1] - 18}
                          width="60"
                          height="36"
                          rx="10"
                          fill="transparent"
                        />
                        <text
                          x={region.label[0]}
                          y={region.label[1]}
                          textAnchor="middle"
                          dominantBaseline="middle"
                          fill={isActive ? "#ffffff" : "#334155"}
                          stroke={isActive ? "hsl(var(--primary))" : "#ffffff"}
                          strokeWidth={isActive ? 7 : 6}
                          paintOrder="stroke"
                          className="pointer-events-none select-none text-[22px] font-semibold transition-colors duration-200"
                        >
                          {region.name}
                        </text>
                      </g>
                    );
                  })}
                </svg>
              </div>
            </div>

            <aside className="flex flex-col rounded-lg border border-slate-200 bg-slate-50/80 p-5 text-slate-900">
              <div className="flex h-11 w-11 items-center justify-center rounded-full bg-primary/10 text-primary">
                <MapPinned className="h-5 w-5" aria-hidden="true" />
              </div>
              <p className="mt-5 text-sm text-slate-500">선택한 지역</p>
              <p className="mt-1 text-3xl font-semibold tracking-tight">
                {activeRegion ?? "지역을 골라주세요"}
              </p>
              <p className="mt-4 text-sm leading-6 text-slate-500">
                지도에서 지역을 누르면 선택됩니다. 마우스뿐 아니라 키보드와 터치로도 이용할 수 있어요.
              </p>

              <button
                type="button"
                disabled={!pendingRegion}
                onClick={() => {
                  if (pendingRegion) onSelect(pendingRegion);
                }}
                className="mt-6 flex h-11 items-center justify-center gap-2 rounded-full bg-primary px-5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-400 lg:mt-auto"
              >
                <Check className="h-4 w-4" aria-hidden="true" />
                {pendingRegion ? `${pendingRegion} 선택하기` : "지역 선택하기"}
              </button>
            </aside>
          </div>
        </div>
      </section>
    </div>
  );
}
