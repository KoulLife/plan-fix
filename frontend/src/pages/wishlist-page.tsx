import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  BookOpen,
  Calendar,
  ChevronRight,
  Compass,
  Eye,
  Heart,
  Loader2,
  MapPin,
  MessageSquare,
  Route,
} from "lucide-react";
import AppNav from "@/components/ui/app-nav";
import { unlikeSpot, UnauthorizedError } from "@/services/spots";
import { unlikeCourse, CourseResponse } from "@/services/course";
import { unlikeBoard, BoardDetail } from "@/services/board";
import {
  fetchLikedSpots,
  fetchLikedCourses,
  fetchLikedBoards,
  WishlistSpot,
} from "@/services/wishlist";

type TabType = "spots" | "courses" | "boards";

export default function WishlistPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TabType>("spots");

  const [spots, setSpots] = useState<WishlistSpot[]>([]);
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [boards, setBoards] = useState<BoardDetail[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError(null);

    const loadAllWishlist = async () => {
      try {
        const [spotsRes, coursesRes, boardsRes] = await Promise.all([
          fetchLikedSpots(),
          fetchLikedCourses(),
          fetchLikedBoards(),
        ]);

        if (!ignore) {
          setSpots(spotsRes);
          setCourses(coursesRes);
          setBoards(boardsRes);
        }
      } catch (err) {
        if (err instanceof UnauthorizedError) {
          alert("로그인이 필요합니다. 로그인 페이지로 이동합니다.");
          navigate("/login");
          return;
        }
        if (!ignore) {
          setError(
            err instanceof Error ? err.message : "위시리스트를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    loadAllWishlist();

    return () => {
      ignore = true;
    };
  }, [navigate]);

  const handleUnlikeSpot = async (e: React.MouseEvent, spotId: number) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      await unlikeSpot(spotId);
      setSpots((prev) => prev.filter((s) => s.spotId !== spotId));
    } catch {
      alert("좋아요 취소에 실패했습니다.");
    }
  };

  const handleUnlikeCourse = async (e: React.MouseEvent, courseId: number) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      await unlikeCourse(courseId);
      setCourses((prev) => prev.filter((c) => c.courseId !== courseId));
    } catch {
      alert("좋아요 취소에 실패했습니다.");
    }
  };

  const handleUnlikeBoard = async (e: React.MouseEvent, boardId: number) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      await unlikeBoard(boardId);
      setBoards((prev) => prev.filter((b) => b.boardId !== boardId));
    } catch {
      alert("좋아요 취소에 실패했습니다.");
    }
  };

  return (
    <div className="min-h-screen bg-muted/20 pb-28 md:pb-16 md:pt-20">
      <AppNav />

      <main className="mx-auto max-w-5xl px-4 pt-6 sm:px-6 sm:pt-8">
        {/* 상단 브레드크럼 */}
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Link to="/main" className="hover:text-foreground">
            홈
          </Link>
          <ChevronRight className="h-3.5 w-3.5" />
          <span className="font-medium text-foreground">위시리스트</span>
        </div>

        {/* 헤더 */}
        <div className="mt-3 border-b border-border pb-5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-500/10 text-rose-500">
              <Heart className="h-5 w-5 fill-rose-500" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              위시리스트
            </h1>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            내가 좋아요를 누른 여행지, 코스, 여행기 모음입니다.
          </p>
        </div>

        {/* 탭 네비게이션 */}
        <div className="mt-6 flex border-b border-border">
          <button
            type="button"
            onClick={() => setActiveTab("spots")}
            className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
              activeTab === "spots"
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <Compass className="h-4 w-4" />
            <span>여행지 / 명소</span>
            <span
              className={`rounded-full px-2 py-0.5 text-xs ${
                activeTab === "spots"
                  ? "bg-primary/15 text-primary"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              {spots.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab("courses")}
            className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
              activeTab === "courses"
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <Route className="h-4 w-4" />
            <span>여행 코스</span>
            <span
              className={`rounded-full px-2 py-0.5 text-xs ${
                activeTab === "courses"
                  ? "bg-primary/15 text-primary"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              {courses.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab("boards")}
            className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
              activeTab === "boards"
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <BookOpen className="h-4 w-4" />
            <span>여행기</span>
            <span
              className={`rounded-full px-2 py-0.5 text-xs ${
                activeTab === "boards"
                  ? "bg-primary/15 text-primary"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              {boards.length}
            </span>
          </button>
        </div>

        {/* 컨텐츠 영역 */}
        {loading ? (
          <div className="flex h-64 flex-col items-center justify-center gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-sm text-muted-foreground">
              위시리스트를 불러오는 중입니다...
            </p>
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
        ) : (
          <div className="mt-6">
            {/* 1. 여행지/스팟 탭 */}
            {activeTab === "spots" && (
              <div>
                {spots.length === 0 ? (
                  <div className="mt-6 rounded-2xl border border-dashed border-border bg-card/60 p-12 text-center shadow-sm">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/10 text-rose-500">
                      <Compass className="h-7 w-7" />
                    </div>
                    <h2 className="mt-4 text-lg font-bold text-foreground">
                      좋아요한 여행지가 없습니다.
                    </h2>
                    <p className="mt-1 text-xs text-muted-foreground">
                      강원도의 인기 명소와 숨은 핫플레이스를 둘러보고 마음에 드는 곳을 찜해보세요!
                    </p>
                    <div className="mt-6 flex justify-center">
                      <Link
                        to="/spots/popular"
                        className="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow transition-transform active:scale-95"
                      >
                        인기 장소 둘러보기
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
                    {spots.map((spot) => (
                      <Link
                        key={spot.spotId}
                        to={`/spots/${spot.spotId}`}
                        data-testid={`wishlist-spot-${spot.spotId}`}
                        className="group relative flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-sm transition-all hover:border-primary/50 hover:shadow-md active:scale-[0.99]"
                      >
                        <div className="relative aspect-[16/10] w-full overflow-hidden bg-muted">
                          {spot.thumbnail ? (
                            <img
                              src={spot.thumbnail}
                              alt={spot.title}
                              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                            />
                          ) : (
                            <div className="flex h-full w-full items-center justify-center bg-muted/80 text-muted-foreground">
                              <Compass className="h-8 w-8 stroke-[1.5]" />
                            </div>
                          )}
                          <button
                            type="button"
                            onClick={(e) => handleUnlikeSpot(e, spot.spotId)}
                            title="위시리스트에서 삭제"
                            className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-black/40 text-rose-400 backdrop-blur-sm transition-colors hover:bg-black/60 hover:text-rose-300 active:scale-90"
                          >
                            <Heart className="h-4 w-4 fill-rose-500 text-rose-500" />
                          </button>
                          <div className="absolute left-3 top-3">
                            <span className="rounded-full bg-background/90 px-2.5 py-0.5 text-xs font-semibold text-foreground backdrop-blur-sm">
                              {spot.category}
                            </span>
                          </div>
                        </div>

                        <div className="flex flex-1 flex-col justify-between p-4">
                          <div>
                            <h3 className="line-clamp-1 text-base font-bold text-foreground group-hover:text-primary">
                              {spot.title}
                            </h3>
                            <p className="mt-1 line-clamp-1 text-xs text-muted-foreground">
                              {spot.address ||
                                [spot.region, spot.sigungu].filter(Boolean).join(" ")}
                            </p>
                          </div>
                          <div className="mt-4 flex items-center justify-between border-t border-border/60 pt-2.5 text-xs text-muted-foreground">
                            <span className="flex items-center gap-1">
                              <MapPin className="h-3.5 w-3.5 text-primary" />
                              {[spot.region, spot.sigungu].filter(Boolean).join(" ")}
                            </span>
                            <span className="flex items-center gap-1 font-medium text-rose-500">
                              <Heart className="h-3.5 w-3.5 fill-rose-500 text-rose-500" />
                              {spot.likeCount}
                            </span>
                          </div>
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 2. 여행 코스 탭 */}
            {activeTab === "courses" && (
              <div>
                {courses.length === 0 ? (
                  <div className="mt-6 rounded-2xl border border-dashed border-border bg-card/60 p-12 text-center shadow-sm">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/10 text-rose-500">
                      <Route className="h-7 w-7" />
                    </div>
                    <h2 className="mt-4 text-lg font-bold text-foreground">
                      좋아요한 여행 코스가 없습니다.
                    </h2>
                    <p className="mt-1 text-xs text-muted-foreground">
                      다른 여행자들의 멋진 코스를 구경하고 마음에 드는 일정을 담아보세요!
                    </p>
                    <div className="mt-6 flex justify-center">
                      <Link
                        to="/courses"
                        className="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow transition-transform active:scale-95"
                      >
                        내 코스 보러가기
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2">
                    {courses.map((course) => {
                      const totalSpots = course.days.reduce(
                        (sum, day) => sum + day.spots.length,
                        0
                      );
                      return (
                        <Link
                          key={course.courseId}
                          to={`/courses/${course.courseId}`}
                          data-testid={`wishlist-course-${course.courseId}`}
                          className="group relative flex flex-col justify-between rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:border-primary/50 hover:shadow-md active:scale-[0.99]"
                        >
                          <div>
                            <div className="flex items-center justify-between gap-2">
                              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                                {course.days.length}일 일정
                              </span>
                              <button
                                type="button"
                                onClick={(e) => handleUnlikeCourse(e, course.courseId)}
                                title="위시리스트에서 삭제"
                                className="flex h-7 w-7 items-center justify-center rounded-full bg-rose-500/10 text-rose-500 transition-colors hover:bg-rose-500/20 active:scale-90"
                              >
                                <Heart className="h-4 w-4 fill-rose-500" />
                              </button>
                            </div>

                            <h3 className="mt-3 line-clamp-1 text-base font-bold text-foreground group-hover:text-primary">
                              {course.title}
                            </h3>

                            {course.description && (
                              <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                                {course.description}
                              </p>
                            )}
                          </div>

                          <div className="mt-5 flex items-center justify-between border-t border-border/60 pt-3 text-xs text-muted-foreground">
                            <div className="flex items-center gap-3">
                              {course.startDate && course.endDate ? (
                                <span className="flex items-center gap-1 truncate">
                                  <Calendar className="h-3.5 w-3.5 text-primary" />
                                  {course.startDate.substring(5)} ~{" "}
                                  {course.endDate.substring(5)}
                                </span>
                              ) : (
                                <span className="flex items-center gap-1">
                                  <MapPin className="h-3.5 w-3.5 text-primary" />
                                  {totalSpots}개 장소
                                </span>
                              )}
                            </div>
                            <div className="flex items-center gap-2.5">
                              <span className="flex items-center gap-1">
                                <Eye className="h-3.5 w-3.5" />
                                {course.viewCount}
                              </span>
                              <span className="flex items-center gap-1 font-medium text-rose-500">
                                <Heart className="h-3.5 w-3.5 fill-rose-500" />
                                {course.likeCount}
                              </span>
                            </div>
                          </div>
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>
            )}

            {/* 3. 여행기/게시글 탭 */}
            {activeTab === "boards" && (
              <div>
                {boards.length === 0 ? (
                  <div className="mt-6 rounded-2xl border border-dashed border-border bg-card/60 p-12 text-center shadow-sm">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/10 text-rose-500">
                      <BookOpen className="h-7 w-7" />
                    </div>
                    <h2 className="mt-4 text-lg font-bold text-foreground">
                      좋아요한 여행기가 없습니다.
                    </h2>
                    <p className="mt-1 text-xs text-muted-foreground">
                      다른 여행자들의 생생한 여행기를 읽고 유용한 정보가 담긴 글을 저장해보세요!
                    </p>
                    <div className="mt-6 flex justify-center">
                      <Link
                        to="/main"
                        className="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow transition-transform active:scale-95"
                      >
                        메인으로 가기
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
                    {boards.map((board) => (
                      <Link
                        key={board.boardId}
                        to={`/boards/${board.boardId}`}
                        data-testid={`wishlist-board-${board.boardId}`}
                        className="group relative flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-sm transition-all hover:border-primary/50 hover:shadow-md active:scale-[0.99]"
                      >
                        <div className="relative aspect-[16/10] w-full overflow-hidden bg-muted">
                          {board.thumbnail ? (
                            <img
                              src={board.thumbnail}
                              alt={board.title}
                              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                            />
                          ) : (
                            <div className="flex h-full w-full items-center justify-center bg-muted/80 text-muted-foreground">
                              <BookOpen className="h-8 w-8 stroke-[1.5]" />
                            </div>
                          )}
                          <button
                            type="button"
                            onClick={(e) => handleUnlikeBoard(e, board.boardId)}
                            title="위시리스트에서 삭제"
                            className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-black/40 text-rose-400 backdrop-blur-sm transition-colors hover:bg-black/60 hover:text-rose-300 active:scale-90"
                          >
                            <Heart className="h-4 w-4 fill-rose-500 text-rose-500" />
                          </button>
                        </div>

                        <div className="flex flex-1 flex-col justify-between p-4">
                          <div>
                            <h3 className="line-clamp-1 text-base font-bold text-foreground group-hover:text-primary">
                              {board.title}
                            </h3>
                            <p className="mt-1 line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                              {board.content.replace(/<[^>]*>?/gm, "").slice(0, 100)}
                            </p>
                          </div>

                          <div className="mt-4 flex items-center justify-between border-t border-border/60 pt-2.5 text-xs text-muted-foreground">
                            <span className="text-xs">
                              {board.createdAt.substring(0, 10)}
                            </span>
                            <div className="flex items-center gap-2">
                              <span className="flex items-center gap-1">
                                <Eye className="h-3.5 w-3.5" />
                                {board.viewCount}
                              </span>
                              <span className="flex items-center gap-1 font-medium text-rose-500">
                                <Heart className="h-3.5 w-3.5 fill-rose-500" />
                                {board.likeCount}
                              </span>
                              <span className="flex items-center gap-1">
                                <MessageSquare className="h-3.5 w-3.5" />
                                {board.commentCount}
                              </span>
                            </div>
                          </div>
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
