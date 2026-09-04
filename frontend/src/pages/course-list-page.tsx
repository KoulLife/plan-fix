import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Calendar,
  ChevronRight,
  Eye,
  Heart,
  Loader2,
  MapPin,
  MapPinPlus,
  Plus,
} from "lucide-react";
import AppNav from "@/components/ui/app-nav";
import { CourseResponse, fetchMyCourses } from "@/services/course";
import { UnauthorizedError } from "@/services/spots";

export default function CourseListPage() {
  const navigate = useNavigate();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError(null);

    const loadCourses = async () => {
      try {
        const res = await fetchMyCourses();
        if (!ignore) {
          setCourses(res);
        }
      } catch (err) {
        if (err instanceof UnauthorizedError) {
          alert("로그인이 필요합니다. 로그인 페이지로 이동합니다.");
          navigate("/login");
          return;
        }
        if (!ignore) {
          setError(err instanceof Error ? err.message : "코스 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    loadCourses();

    return () => {
      ignore = true;
    };
  }, [navigate]);

  return (
    <div className="min-h-screen bg-muted/20 pb-20">
      <AppNav />

      <main className="mx-auto max-w-4xl px-4 pt-6 sm:px-6 sm:pt-8">
        {/* 상단 브레드크럼 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <span>여행</span>
            <ChevronRight className="h-3.5 w-3.5" />
            <span className="font-medium text-foreground">내 여행 코스</span>
          </div>
          <Link
            to="/courses/create"
            className="flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow transition-transform active:scale-95"
          >
            <Plus className="h-3.5 w-3.5" />
            새 코스 만들기
          </Link>
        </div>

        {/* 헤더 */}
        <div className="mt-4 border-b border-border pb-5">
          <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            내 여행 코스
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            직접 계획하고 저장한 강원도 여행 일정 목록입니다.
          </p>
        </div>

        {loading ? (
          <div className="flex h-64 flex-col items-center justify-center gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-sm text-muted-foreground">코스 목록을 불러오는 중입니다...</p>
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
        ) : courses.length === 0 ? (
          <div className="mt-8 rounded-2xl border border-dashed border-border bg-card/60 p-12 text-center shadow-sm">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <MapPinPlus className="h-7 w-7" />
            </div>
            <h2 className="mt-4 text-lg font-bold text-foreground">
              생성한 여행 코스가 없습니다.
            </h2>
            <p className="mt-1 text-xs text-muted-foreground">
              강원도의 다양한 인기 명소와 맛집을 골라 나만의 여행 코스를 만들어보세요!
            </p>
            <div className="mt-6 flex justify-center">
              <Link
                to="/courses/create"
                className="flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow transition-transform active:scale-95"
              >
                <Plus className="h-4 w-4" />
                첫 여행 코스 만들기
              </Link>
            </div>
          </div>
        ) : (
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            {courses.map((course) => {
              const totalSpots = course.days.reduce(
                (sum, day) => sum + day.spots.length,
                0
              );
              return (
                <Link
                  key={course.courseId}
                  to={`/courses/${course.courseId}`}
                  data-testid={`course-item-${course.courseId}`}
                  className="group flex flex-col justify-between rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:border-primary/50 hover:shadow-md active:scale-[0.99]"
                >
                  <div>
                    <div className="flex items-center justify-between gap-2">
                      <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                        {course.days.length}일 일정
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {course.createdAt.substring(0, 10)}
                      </span>
                    </div>

                    <h2 className="mt-3 line-clamp-1 text-base font-bold text-foreground group-hover:text-primary">
                      {course.title}
                    </h2>

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
                          {course.startDate.substring(5)} ~ {course.endDate.substring(5)}
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
                      <span className="flex items-center gap-1">
                        <Heart className="h-3.5 w-3.5" />
                        {course.likeCount}
                      </span>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
