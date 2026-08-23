import { useCallback, useState } from "react";
import {
  ArrowRight,
  ChevronDown,
  CloudSun,
  Heart,
  Info,
  Luggage,
  MessageSquare,
  Search,
  Sun,
  UserRound,
} from "lucide-react";

import GangwonRegionMap, {
  type GangwonRegion,
} from "@/components/ui/gangwon-region-map";

const weatherDays = [
  { date: "11.12", day: "수", low: 23, high: 28, icon: Sun },
  { date: "11.13", day: "목", low: 22, high: 27, icon: CloudSun },
  { date: "11.14", day: "금", low: 22, high: 28, icon: Sun },
  { date: "11.15", day: "토", low: 22, high: 28, icon: Sun },
  { date: "11.16", day: "일", low: 24, high: 28, icon: CloudSun },
];

const guideCards = [
  {
    id: "course",
    getTitle: (region: string) => `${region} 필수\n관광 코스`,
    image:
      "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=900&q=85",
    alt: "해 질 무렵의 바다와 해변",
  },
  {
    id: "food",
    getTitle: (region: string) => `건강한\n${region} 음식`,
    image:
      "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=900&q=85",
    alt: "채소와 면이 담긴 따뜻한 음식",
  },
  {
    id: "place",
    getTitle: () => "요즘 떠오르는\n인기 명소",
    image:
      "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=900&q=85",
    alt: "초록빛 나무가 울창한 숲길",
  },
];

const stays = [
  {
    id: "ocean-view",
    getTitle: (region: string) => `오션뷰 ${region} 스테이`,
    dates: "8월 14일~16일",
    price: "₩172,535",
    rating: "4.9",
    image:
      "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=85",
    alt: "밝은 외관과 발코니가 있는 숙소",
  },
  {
    id: "mood-stay",
    getTitle: (region: string) => `감성 스테이 ${region}`,
    dates: "8월 21일~23일",
    price: "₩146,745",
    rating: "4.9",
    image:
      "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=85",
    alt: "따뜻한 조명의 아늑한 숙소 침실",
  },
];

const navigationItems = [
  { label: "검색", icon: Search },
  { label: "메시지", icon: MessageSquare },
  { label: "여행", icon: Luggage, active: true },
  { label: "위시리스트", icon: Heart },
  { label: "프로필", icon: UserRound },
];

export default function MainPage() {
  const [selectedRegion, setSelectedRegion] = useState<GangwonRegion | null>(null);
  const [isRegionMapOpen, setIsRegionMapOpen] = useState(false);
  const locationName = selectedRegion ?? "강원도";
  const locationLabel = selectedRegion
    ? `강원도 / ${selectedRegion}`
    : "강원도 / 지역 선택";

  const closeRegionMap = useCallback(() => setIsRegionMapOpen(false), []);
  const selectRegion = useCallback((region: GangwonRegion) => {
    setSelectedRegion(region);
    setIsRegionMapOpen(false);
  }, []);

  return (
    <div className="min-h-screen bg-background pb-28 text-foreground">
      <main>
        <section className="relative overflow-hidden bg-gradient-to-b from-primary/15 via-primary/5 to-background">
          <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-[70%] rounded-full bg-background/35 blur-2xl" />
          <div className="pointer-events-none absolute -left-20 top-24 h-36 w-[85%] rounded-full bg-primary/5 blur-2xl" />

          <div className="relative mx-auto max-w-6xl px-5 pb-8 pt-10 sm:px-8 lg:px-10 lg:pt-14">
            <button
              type="button"
              onClick={() => setIsRegionMapOpen(true)}
              className="flex items-center gap-3 text-3xl font-semibold tracking-tight sm:text-4xl"
              aria-label={`여행 지역 선택: ${locationLabel}`}
              aria-haspopup="dialog"
              aria-expanded={isRegionMapOpen}
            >
              {locationLabel}
              <ChevronDown className="h-6 w-6 stroke-[3]" aria-hidden="true" />
            </button>

            <section
              className="mt-8 rounded-lg border bg-background/95 px-3 py-6 shadow-panel sm:px-6 lg:px-8"
              aria-labelledby="weather-title"
            >
              <h1 id="weather-title" className="sr-only">
                {locationName} 주간 날씨
              </h1>
              <div className="grid grid-cols-5">
                {weatherDays.map((weather, index) => {
                  const WeatherIcon = weather.icon;
                  const isCloudy = weather.icon === CloudSun;

                  return (
                    <article
                      key={weather.date}
                      className={`min-w-0 px-1 text-center sm:px-5 ${
                        index > 0 ? "border-l" : ""
                      }`}
                    >
                      <h2 className="whitespace-nowrap text-xs font-semibold sm:text-lg">
                        {weather.date} <span className="text-muted-foreground">({weather.day})</span>
                      </h2>
                      <WeatherIcon
                        className={`mx-auto mt-4 h-7 w-7 sm:mt-5 sm:h-10 sm:w-10 ${
                          isCloudy ? "fill-amber-300/80 text-muted-foreground/40" : "fill-amber-400 text-amber-400"
                        }`}
                        strokeWidth={1.5}
                        aria-hidden="true"
                      />
                      <p className="mt-3 whitespace-nowrap text-xs font-semibold sm:mt-4 sm:text-lg">
                        {weather.low}° / {weather.high}°
                      </p>
                      <p className="mt-2 text-sm font-semibold text-blue-500 sm:text-base">0%</p>
                    </article>
                  );
                })}
              </div>

              <div className="mt-7 flex items-center justify-between gap-4 border-t pt-5">
                <p className="flex items-center gap-2 text-sm text-muted-foreground sm:text-base">
                  제공&nbsp; WWO
                  <Info className="h-4 w-4" aria-hidden="true" />
                </p>
                <button
                  type="button"
                  className="flex items-center gap-2 rounded-full bg-muted px-5 py-3 text-sm font-semibold sm:text-base"
                >
                  더보기
                  <ChevronDown className="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
            </section>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-5 py-8 sm:px-8 lg:px-10 lg:py-12">
          <div>
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">{locationName}에서 뭐 하지?</h2>
            <p className="mt-2 text-base text-muted-foreground sm:text-lg">
              {locationName} 여행이 처음인 사람들을 위한 안내서
            </p>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-2 sm:gap-4">
            {guideCards.map((card) => {
              const title = card.getTitle(locationName);

              return (
                <article
                  key={card.id}
                  className="group relative h-44 overflow-hidden rounded-lg sm:h-72"
                >
                <img
                  className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  src={card.image}
                  alt={card.alt}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/5 to-black/5" />
                <button
                  type="button"
                  className="absolute right-2 top-2 text-white transition-transform hover:scale-105 sm:right-4 sm:top-4"
                  aria-label={`${title.replace("\n", " ")} 위시리스트에 추가`}
                >
                  <Heart className="h-7 w-7 sm:h-9 sm:w-9" strokeWidth={1.7} aria-hidden="true" />
                </button>
                <h3 className="absolute bottom-3 left-3 whitespace-pre-line text-sm font-medium leading-relaxed text-white sm:bottom-5 sm:left-5 sm:text-2xl">
                  {title}
                </h3>
              </article>
              );
            })}
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-5 pb-12 sm:px-8 lg:px-10">
          <div className="flex items-center justify-between gap-4">
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">{locationName}의 인기 숙소</h2>
            <button
              type="button"
              className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-muted transition-colors hover:bg-primary/10 hover:text-primary"
              aria-label="인기 숙소 더보기"
            >
              <ArrowRight className="h-6 w-6" aria-hidden="true" />
            </button>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-3 sm:gap-5">
            {stays.map((stay) => {
              const title = stay.getTitle(locationName);

              return (
                <article
                  key={stay.id}
                  className="overflow-hidden rounded-lg border bg-background shadow-panel"
                >
                <div className="relative h-40 sm:h-64">
                  <img className="h-full w-full object-cover" src={stay.image} alt={stay.alt} />
                  <span className="absolute left-3 top-3 rounded-full bg-background/95 px-3 py-1.5 text-xs font-medium shadow sm:left-4 sm:top-4 sm:px-4 sm:py-2 sm:text-sm">
                    게스트 선호
                  </span>
                  <button
                    type="button"
                    className="absolute right-3 top-3 text-white drop-shadow-md transition-transform hover:scale-105 sm:right-4 sm:top-4"
                    aria-label={`${title} 위시리스트에 추가`}
                  >
                    <Heart className="h-7 w-7 sm:h-9 sm:w-9" strokeWidth={1.7} aria-hidden="true" />
                  </button>
                </div>
                <div className="p-3 sm:p-5">
                  <h3 className="text-sm font-semibold sm:text-lg">{title}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{stay.dates}</p>
                  <p className="mt-3 text-base font-semibold text-primary sm:text-lg">
                    {stay.price} <span className="text-sm font-normal text-muted-foreground">· ★ {stay.rating}</span>
                  </p>
                </div>
              </article>
              );
            })}
          </div>
        </section>
      </main>

      <nav
        className="fixed inset-x-0 bottom-0 z-40 border-t bg-background/95 shadow-[0_-8px_32px_hsl(var(--foreground)/0.08)] backdrop-blur-md"
        aria-label="하단 메뉴"
      >
        <div className="mx-auto grid h-20 max-w-3xl grid-cols-5 px-2 sm:h-24">
          {navigationItems.map((item) => {
            const Icon = item.icon;

            return (
              <button
                key={item.label}
                type="button"
                className={`relative flex flex-col items-center justify-center gap-1 text-xs transition-colors sm:text-sm ${
                  item.active ? "font-semibold text-primary" : "text-muted-foreground hover:text-primary"
                }`}
                aria-current={item.active ? "page" : undefined}
              >
                {item.active ? <span className="absolute inset-y-2 aspect-square rounded-full bg-primary/10" /> : null}
                <Icon className="relative h-6 w-6 sm:h-7 sm:w-7" strokeWidth={1.8} aria-hidden="true" />
                <span className="relative">{item.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      <GangwonRegionMap
        open={isRegionMapOpen}
        selectedRegion={selectedRegion}
        onClose={closeRegionMap}
        onSelect={selectRegion}
      />
    </div>
  );
}
