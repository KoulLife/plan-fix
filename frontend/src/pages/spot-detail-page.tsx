import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ChevronLeft, Heart } from "lucide-react";

import { LoaderFour } from "@/components/ui/unique-loader-components";
import AppNav from "@/components/ui/app-nav";
import {
  fetchSpotDetail,
  likeSpot,
  unlikeSpot,
  UnauthorizedError,
  type SpotDetail,
  type SpotTourInfo,
} from "@/services/spots";

const FALLBACK_SPOT_IMAGE =
  "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=900&q=85";

// [라벨, info 필드]. tel~restInfo는 공통, 나머지 셋은 음식점만 값이 있어 있을 때만 보인다.
const TOUR_INFO_FIELDS: { label: string; key: keyof SpotTourInfo }[] = [
  { label: "전화", key: "tel" },
  { label: "주차", key: "parkInfo" },
  { label: "이용시간", key: "timeInfo" },
  { label: "쉬는날", key: "restInfo" },
  { label: "대표메뉴", key: "firstMenu" },
  { label: "취급메뉴", key: "treatMenu" },
  { label: "인허가번호", key: "lcnsno" },
];

export default function SpotDetailPage() {
  const { spotId } = useParams<{ spotId: string }>();
  const navigate = useNavigate();
  // undefined = 로딩 중, null = 없음(404) 또는 에러
  const [spot, setSpot] = useState<SpotDetail | null | undefined>(undefined);

  // 이 API는 호출할 때마다 조회수를 늘린다. React.StrictMode는 개발 모드에서 effect를
  // 마운트→클린업→재마운트로 일부러 두 번 실행하는데, 이때 매번 fetch를 새로 호출하면
  // 같은 화면 진입에 조회수가 2씩 올라간다. 같은 spotId로 이미 나가 있는 요청이 있으면
  // 새로 호출하지 않고 그 결과를 재사용해서, 실제 네트워크 호출이 spotId당 한 번만 나가게 한다.
  const inFlightRequest = useRef<{ spotId: string; promise: Promise<SpotDetail | null> } | null>(null);

  useEffect(() => {
    const currentSpotId = spotId ?? "";
    let cancelled = false;
    setSpot(undefined);

    let request = inFlightRequest.current;
    if (!request || request.spotId !== currentSpotId) {
      request = { spotId: currentSpotId, promise: fetchSpotDetail(currentSpotId) };
      inFlightRequest.current = request;
    }
    const { promise } = request;

    promise
      .then((result) => {
        if (!cancelled) {
          setSpot(result);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSpot(null);
        }
      })
      .finally(() => {
        if (inFlightRequest.current === request) {
          inFlightRequest.current = null;
        }
      });

    return () => {
      cancelled = true;
    };
  }, [spotId]);

  const [isTogglingLike, setIsTogglingLike] = useState(false);

  const goBack = () => navigate(-1);

  const toggleLike = async () => {
    if (!spot || isTogglingLike) {
      return;
    }

    setIsTogglingLike(true);
    try {
      const result = spot.isLiked ? await unlikeSpot(spot.spotId) : await likeSpot(spot.spotId);
      setSpot({ ...spot, isLiked: result.liked, likeCount: result.likeCount });
    } catch (error) {
      if (error instanceof UnauthorizedError) {
        navigate("/login");
      }
      // 그 외 실패는 화면을 바꾸지 않고 그대로 둔다 — 사용자가 다시 눌러보면 된다.
    } finally {
      setIsTogglingLike(false);
    }
  };

  return (
    <div className="min-h-screen bg-background pb-28 text-foreground md:pb-16 md:pt-16">
      <AppNav />

      <header className="mx-auto flex max-w-3xl items-center px-5 py-4 sm:px-8">
        <button
          type="button"
          onClick={goBack}
          className="flex h-10 w-10 items-center justify-center rounded-full bg-muted transition-colors hover:bg-primary/10 hover:text-primary"
          aria-label="뒤로 가기"
        >
          <ChevronLeft className="h-5 w-5" aria-hidden="true" />
        </button>
      </header>

      {spot === undefined ? (
        <div className="flex justify-center py-24">
          <LoaderFour text="장소 정보를 불러오는 중..." />
        </div>
      ) : spot === null ? (
        <div className="flex flex-col items-center gap-4 px-5 py-24 text-center">
          <p className="text-base text-muted-foreground">존재하지 않는 장소예요.</p>
          <button
            type="button"
            onClick={goBack}
            className="rounded-full bg-muted px-5 py-3 text-sm font-semibold transition-colors hover:bg-primary/10 hover:text-primary"
          >
            돌아가기
          </button>
        </div>
      ) : (
        <main className="mx-auto max-w-3xl px-5 pb-16 sm:px-8">
          <div className="relative h-64 overflow-hidden rounded-lg sm:h-96">
            <img
              className="h-full w-full object-cover"
              src={spot.thumbnail ?? FALLBACK_SPOT_IMAGE}
              alt={spot.title}
            />
            <button
              type="button"
              onClick={toggleLike}
              disabled={isTogglingLike}
              className={`absolute right-4 top-4 drop-shadow-md transition-transform hover:scale-105 disabled:opacity-60 ${
                spot.isLiked ? "text-red-500" : "text-white"
              }`}
              aria-pressed={spot.isLiked}
              aria-label={spot.isLiked ? `${spot.title} 좋아요 취소` : `${spot.title} 좋아요`}
            >
              <Heart
                className="h-8 w-8"
                strokeWidth={1.7}
                fill={spot.isLiked ? "currentColor" : "none"}
                aria-hidden="true"
              />
            </button>
          </div>

          <span className="mt-5 inline-block rounded-full bg-muted px-3 py-1.5 text-xs font-medium sm:text-sm">
            {spot.category}
          </span>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight sm:text-3xl">{spot.title}</h1>
          {spot.address ? (
            <p className="mt-2 text-sm text-muted-foreground sm:text-base">{spot.address}</p>
          ) : null}

          {spot.description ? (
            <p className="mt-6 whitespace-pre-line text-sm leading-relaxed sm:text-base">
              {spot.description}
            </p>
          ) : null}

          <div className="mt-6 flex items-center gap-4 text-sm text-muted-foreground sm:text-base">
            <span>좋아요 {spot.likeCount}</span>
            <span>조회수 {spot.viewCount}</span>
          </div>

          {spot.images.length > 0 ? (
            <div className="mt-8 flex gap-3 overflow-x-auto pb-2">
              {spot.images.map((image, index) => (
                <img
                  key={image}
                  className="h-28 w-40 shrink-0 rounded-lg object-cover sm:h-36 sm:w-52"
                  src={image}
                  alt={`${spot.title} 사진 ${index + 1}`}
                />
              ))}
            </div>
          ) : null}

          {spot.info ? (
            <div className="mt-8 border-t pt-6">
              <h2 className="text-lg font-semibold sm:text-xl">이용 안내</h2>
              <dl className="mt-3 space-y-2 text-sm sm:text-base">
                {TOUR_INFO_FIELDS.filter(({ key }) => spot.info?.[key]).map(({ label, key }) => (
                  <div key={key} className="flex gap-3">
                    <dt className="w-20 shrink-0 text-muted-foreground">{label}</dt>
                    <dd>{spot.info?.[key]}</dd>
                  </div>
                ))}
              </dl>
            </div>
          ) : null}
        </main>
      )}
    </div>
  );
}
