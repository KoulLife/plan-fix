import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CloudSun,
  Heart,
  Info,
  MessageSquare,
  Sun,
} from "lucide-react";

import AppNav from "@/components/ui/app-nav";
import GangwonRegionMap, {
  sigunguCodeByRegion,
  type GangwonRegion,
} from "@/components/ui/gangwon-region-map";
import {
  fetchPopularBoards,
  type BoardItem,
} from "@/services/board";
import {
  fetchPopularSpots,
  likeSpot,
  unlikeSpot,
  UnauthorizedError,
  type PopularSpot,
} from "@/services/spots";

// 강원도 전체가 시도코드 "51"(강원특별자치도) 하나뿐이라 상수로 둔다.
const GANGWON_REGION_CODE = "51";
const FALLBACK_SPOT_IMAGE =
  "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=900&q=85";

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

export default function MainPage() {
  const navigate = useNavigate();
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

  const carouselRef = useRef<HTMLDivElement>(null);
  const [popularSpots, setPopularSpots] = useState<PopularSpot[] | null>(null);
  const [popularSpotsError, setPopularSpotsError] = useState(false);
  const [likedSpots, setLikedSpots] = useState<Record<number, boolean>>({});
  const [loadingSpots, setLoadingSpots] = useState<Record<number, boolean>>({});
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);

  const boardCarouselRef = useRef<HTMLDivElement>(null);
  const [popularBoards, setPopularBoards] = useState<BoardItem[] | null>(null);
  const [popularBoardsError, setPopularBoardsError] = useState(false);
  const [canBoardScrollLeft, setCanBoardScrollLeft] = useState(false);
  const [canBoardScrollRight, setCanBoardScrollRight] = useState(false);

  const updateScrollButtons = useCallback(() => {
    const el = carouselRef.current;
    if (!el) return;
    const { scrollLeft, scrollWidth, clientWidth } = el;
    setCanScrollLeft(scrollLeft > 1);
    setCanScrollRight(scrollLeft + clientWidth < scrollWidth - 1);
  }, []);

  const updateBoardScrollButtons = useCallback(() => {
    const el = boardCarouselRef.current;
    if (!el) return;
    const { scrollLeft, scrollWidth, clientWidth } = el;
    setCanBoardScrollLeft(scrollLeft > 1);
    setCanBoardScrollRight(scrollLeft + clientWidth < scrollWidth - 1);
  }, []);

  useEffect(() => {
    let ignore = false;

    setPopularSpots(null);
    setPopularSpotsError(false);

    fetchPopularSpots({
      region: selectedRegion ? GANGWON_REGION_CODE : undefined,
      sigungu: selectedRegion ? sigunguCodeByRegion[selectedRegion] : undefined,
      size: 6,
    })
      .then((res) => {
        if (!ignore) {
          setPopularSpots(res.items);
        }
      })
      .catch(() => {
        if (!ignore) {
          setPopularSpotsError(true);
        }
      });

    return () => {
      ignore = true;
    };
  }, [selectedRegion]);

  useEffect(() => {
    let ignore = false;

    setPopularBoards(null);
    setPopularBoardsError(false);

    fetchPopularBoards({ size: 6 })
      .then((res) => {
        if (!ignore) {
          setPopularBoards(res.items);
        }
      })
      .catch(() => {
        if (!ignore) {
          setPopularBoardsError(true);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    updateScrollButtons();
    const handleResize = () => updateScrollButtons();
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [popularSpots, updateScrollButtons]);

  useEffect(() => {
    updateBoardScrollButtons();
    const handleResize = () => updateBoardScrollButtons();
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [popularBoards, updateBoardScrollButtons]);

  const handleScrollLeft = () => {
    if (!carouselRef.current) return;
    const scrollAmount = Math.max(200, Math.floor(carouselRef.current.clientWidth * 0.6));
    if (typeof carouselRef.current.scrollBy === "function") {
      carouselRef.current.scrollBy({ left: -scrollAmount, behavior: "smooth" });
    } else {
      carouselRef.current.scrollLeft -= scrollAmount;
    }
  };

  const handleScrollRight = () => {
    if (!carouselRef.current) return;
    const scrollAmount = Math.max(200, Math.floor(carouselRef.current.clientWidth * 0.6));
    if (typeof carouselRef.current.scrollBy === "function") {
      carouselRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    } else {
      carouselRef.current.scrollLeft += scrollAmount;
    }
  };

  const handleBoardScrollLeft = () => {
    if (!boardCarouselRef.current) return;
    const scrollAmount = Math.max(200, Math.floor(boardCarouselRef.current.clientWidth * 0.6));
    if (typeof boardCarouselRef.current.scrollBy === "function") {
      boardCarouselRef.current.scrollBy({ left: -scrollAmount, behavior: "smooth" });
    } else {
      boardCarouselRef.current.scrollLeft -= scrollAmount;
    }
  };

  const handleBoardScrollRight = () => {
    if (!boardCarouselRef.current) return;
    const scrollAmount = Math.max(200, Math.floor(boardCarouselRef.current.clientWidth * 0.6));
    if (typeof boardCarouselRef.current.scrollBy === "function") {
      boardCarouselRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    } else {
      boardCarouselRef.current.scrollLeft += scrollAmount;
    }
  };

  const handleToggleLike = async (event: React.MouseEvent, spotId: number) => {
    event.preventDefault();
    event.stopPropagation();

    if (loadingSpots[spotId]) {
      return;
    }

    const isCurrentlyLiked = !!likedSpots[spotId];
    setLoadingSpots((prev) => ({ ...prev, [spotId]: true }));
    try {
      const result = isCurrentlyLiked ? await unlikeSpot(spotId) : await likeSpot(spotId);
      setLikedSpots((prev) => ({ ...prev, [spotId]: result.liked }));
    } catch (error) {
      if (error instanceof UnauthorizedError) {
        // 비로그인 상태일 때는 조용히 무시
      }
    } finally {
      setLoadingSpots((prev) => ({ ...prev, [spotId]: false }));
    }
  };

  return (
    <div className="min-h-screen bg-background pb-28 text-foreground md:pb-0 md:pt-16">
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
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">{locationName}의 인기 장소</h2>
            <button
              type="button"
              onClick={() => {
                if (selectedRegion) {
                  navigate(`/spots/popular?region=${encodeURIComponent(selectedRegion)}`);
                } else {
                  navigate("/spots/popular");
                }
              }}
              className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-muted transition-colors hover:bg-primary/10 hover:text-primary"
              aria-label="인기 장소 더보기"
            >
              <ArrowRight className="h-6 w-6" aria-hidden="true" />
            </button>
          </div>

          {popularSpotsError || popularSpots?.length === 0 ? (
            <p className="mt-6 text-base text-muted-foreground">표시할 인기 장소가 없어요.</p>
          ) : (
            <div className="relative mt-6">
              {canScrollLeft ? (
                <button
                  type="button"
                  onClick={handleScrollLeft}
                  className="absolute left-2 top-1/2 z-10 -translate-y-1/2 flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-full border border-border/60 bg-background/80 text-foreground shadow-md backdrop-blur-sm transition-all hover:scale-105 hover:bg-background active:scale-95 sm:left-3"
                  aria-label="이전 인기 장소 보기"
                >
                  <ChevronLeft className="h-5 w-5 sm:h-6 sm:w-6" aria-hidden="true" />
                </button>
              ) : null}

              <div
                ref={carouselRef}
                onScroll={updateScrollButtons}
                className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scrollbar-hide sm:gap-4"
              >
                {(popularSpots ?? []).map((spot) => {
                  const isLiked = !!likedSpots[spot.spotId];
                  const isLoading = !!loadingSpots[spot.spotId];

                  return (
                    <Link
                      key={spot.spotId}
                      to={`/spots/${spot.spotId}`}
                      className="block w-[42%] shrink-0 overflow-hidden rounded-lg border bg-background shadow-panel snap-start sm:w-56"
                    >
                      <div className="relative h-40 sm:h-56">
                        <img
                          className="h-full w-full object-cover"
                          src={spot.thumbnail ?? FALLBACK_SPOT_IMAGE}
                          alt={spot.title}
                        />
                        <span className="absolute left-3 top-3 rounded-full bg-background/95 px-3 py-1.5 text-xs font-medium shadow sm:left-4 sm:top-4 sm:px-4 sm:py-2 sm:text-sm">
                          {spot.category}
                        </span>
                        <button
                          type="button"
                          onClick={(event) => handleToggleLike(event, spot.spotId)}
                          disabled={isLoading}
                          className={`absolute right-3 top-3 drop-shadow-md transition-transform hover:scale-105 disabled:opacity-60 sm:right-4 sm:top-4 ${
                            isLiked ? "text-red-500" : "text-white"
                          }`}
                          aria-pressed={isLiked}
                          aria-label={isLiked ? `${spot.title} 좋아요 취소` : `${spot.title} 좋아요`}
                        >
                          <Heart
                            className="h-7 w-7 sm:h-9 sm:w-9"
                            strokeWidth={1.7}
                            fill={isLiked ? "currentColor" : "none"}
                            aria-hidden="true"
                          />
                        </button>
                      </div>
                      <div className="p-3 sm:p-4">
                        <h3 className="truncate text-sm font-semibold sm:text-base">{spot.title}</h3>
                      </div>
                    </Link>
                  );
                })}
              </div>

              {canScrollRight ? (
                <button
                  type="button"
                  onClick={handleScrollRight}
                  className="absolute right-2 top-1/2 z-10 -translate-y-1/2 flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-full border border-border/60 bg-background/80 text-foreground shadow-md backdrop-blur-sm transition-all hover:scale-105 hover:bg-background active:scale-95 sm:right-3"
                  aria-label="다음 인기 장소 보기"
                >
                  <ChevronRight className="h-5 w-5 sm:h-6 sm:w-6" aria-hidden="true" />
                </button>
              ) : null}
            </div>
          )}
        </section>

        <section className="mx-auto max-w-6xl px-5 pb-12 sm:px-8 lg:px-10">
          <div className="flex items-center justify-between gap-4">
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">게시글</h2>
          </div>

          {popularBoardsError || popularBoards?.length === 0 ? (
            <p className="mt-6 text-base text-muted-foreground">표시할 게시글이 없어요.</p>
          ) : (
            <div className="relative mt-6">
              {canBoardScrollLeft ? (
                <button
                  type="button"
                  onClick={handleBoardScrollLeft}
                  className="absolute left-2 top-1/2 z-10 -translate-y-1/2 flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-full border border-border/60 bg-background/80 text-foreground shadow-md backdrop-blur-sm transition-all hover:scale-105 hover:bg-background active:scale-95 sm:left-3"
                  aria-label="이전 게시글 보기"
                >
                  <ChevronLeft className="h-5 w-5 sm:h-6 sm:w-6" aria-hidden="true" />
                </button>
              ) : null}

              <div
                ref={boardCarouselRef}
                onScroll={updateBoardScrollButtons}
                className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scrollbar-hide sm:gap-4"
              >
                {(popularBoards ?? []).map((board) => (
                  <Link
                    key={board.boardId}
                    to={`/boards/${board.boardId}`}
                    className="group block w-[42%] shrink-0 overflow-hidden rounded-lg border bg-background shadow-panel snap-start transition-all duration-200 hover:shadow-md sm:w-56"
                  >
                    <div className="relative h-40 sm:h-56 overflow-hidden">
                      <img
                        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                        src={board.thumbnail ?? FALLBACK_SPOT_IMAGE}
                        alt={board.title}
                      />
                    </div>
                    <div className="p-3 sm:p-4">
                      <h3 className="truncate text-sm font-semibold sm:text-base">{board.title}</h3>
                      <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Heart className="h-3.5 w-3.5" aria-hidden="true" />
                          {board.likeCount}
                        </span>
                        <span className="flex items-center gap-1">
                          <MessageSquare className="h-3.5 w-3.5" aria-hidden="true" />
                          {board.commentCount}
                        </span>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>

              {canBoardScrollRight ? (
                <button
                  type="button"
                  onClick={handleBoardScrollRight}
                  className="absolute right-2 top-1/2 z-10 -translate-y-1/2 flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-full border border-border/60 bg-background/80 text-foreground shadow-md backdrop-blur-sm transition-all hover:scale-105 hover:bg-background active:scale-95 sm:right-3"
                  aria-label="다음 게시글 보기"
                >
                  <ChevronRight className="h-5 w-5 sm:h-6 sm:w-6" aria-hidden="true" />
                </button>
              ) : null}
            </div>
          )}
        </section>
      </main>

      <AppNav />

      <GangwonRegionMap
        open={isRegionMapOpen}
        selectedRegion={selectedRegion}
        onClose={closeRegionMap}
        onSelect={selectRegion}
      />
    </div>
  );
}
