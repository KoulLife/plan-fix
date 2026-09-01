import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Calendar, ChevronLeft, Eye, Heart, MessageSquare } from "lucide-react";

import AppNav from "@/components/ui/app-nav";
import { LoaderFour } from "@/components/ui/unique-loader-components";
import { fetchBoardDetail, type BoardDetail } from "@/services/board";

const FALLBACK_BOARD_IMAGE =
  "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=85";

function formatDate(dateString: string): string {
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return dateString;
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
  } catch {
    return dateString;
  }
}

/**
 * 백엔드에서 XSS 위험 요소(<script>, <iframe> 등)가 이미 필터링된 안전한 본문 HTML을
 * 일반 텍스트일 때도 줄바꿈이 유지되도록 가공한다.
 */
function formatContentHtml(content: string): string {
  if (!content) return "";
  // 이미 HTML 태그가 포함되어 있다면 그대로 사용하고, 단순 텍스트면 줄바꿈을 <br />로 변환
  const hasHtmlTag = /<[a-z][\s\S]*>/i.test(content);
  if (hasHtmlTag) {
    return content;
  }
  return content.replace(/\n/g, "<br />");
}

export default function BoardDetailPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  // undefined = 로딩 중, null = 없음(404) 또는 에러
  const [board, setBoard] = useState<BoardDetail | null | undefined>(undefined);

  const inFlightRequest = useRef<{ boardId: string; promise: Promise<BoardDetail | null> } | null>(null);

  useEffect(() => {
    const currentBoardId = boardId ?? "";
    let cancelled = false;
    setBoard(undefined);

    let request = inFlightRequest.current;
    if (!request || request.boardId !== currentBoardId) {
      request = { boardId: currentBoardId, promise: fetchBoardDetail(currentBoardId) };
      inFlightRequest.current = request;
    }
    const { promise } = request;

    promise
      .then((result) => {
        if (!cancelled) {
          setBoard(result);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setBoard(null);
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
  }, [boardId]);

  const handleGoBack = () => {
    navigate("/main");
  };

  const heroImage =
    board?.thumbnail || (board?.images && board.images.length > 0 ? board.images[0].imageUrl : null) || FALLBACK_BOARD_IMAGE;

  return (
    <div className="min-h-screen bg-background pb-28 text-foreground md:pb-16 md:pt-16">
      <AppNav />

      <header className="sticky top-0 z-30 border-b bg-background/95 backdrop-blur-md md:static md:z-auto md:border-b-0 md:bg-transparent md:backdrop-blur-none">
        <div className="mx-auto flex max-w-3xl items-center px-5 py-4 sm:px-8">
          <button
            type="button"
            onClick={handleGoBack}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-muted transition-colors hover:bg-primary/10 hover:text-primary"
            aria-label="뒤로 가기"
          >
            <ChevronLeft className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </header>

      {board === undefined ? (
        <div className="flex justify-center py-24">
          <LoaderFour text="게시글을 불러오는 중..." />
        </div>
      ) : board === null ? (
        <div className="flex flex-col items-center justify-center gap-4 px-5 py-24 text-center">
          <p className="text-base text-muted-foreground">게시글을 찾을 수 없어요.</p>
          <button
            type="button"
            onClick={handleGoBack}
            className="rounded-full bg-muted px-5 py-3 text-sm font-semibold transition-colors hover:bg-primary/10 hover:text-primary"
          >
            홈으로 돌아가기
          </button>
        </div>
      ) : (
        <main className="mx-auto max-w-3xl px-5 pb-16 sm:px-8">
          <article className="overflow-hidden">
            {/* 히어로 이미지 */}
            <div className="relative aspect-[16/10] w-full overflow-hidden rounded-2xl bg-muted shadow-panel sm:aspect-[16/9]">
              <img
                src={heroImage}
                alt={board.title}
                className="h-full w-full object-cover"
              />
            </div>

            {/* 헤더 메타데이터 영역 */}
            <div className="mt-6 sm:mt-8">
              <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary sm:text-sm">
                여행 이야기
              </span>

              <h1 className="mt-3 text-2xl font-bold tracking-tight text-foreground sm:text-3xl lg:text-4xl">
                {board.title}
              </h1>

              <div className="mt-4 flex flex-wrap items-center gap-y-2 text-xs text-muted-foreground sm:text-sm">
                <span className="flex items-center gap-1.5 font-medium">
                  <Calendar className="h-4 w-4 text-muted-foreground/80" aria-hidden="true" />
                  <time dateTime={board.createdAt}>{formatDate(board.createdAt)}</time>
                </span>

                <span className="mx-2.5 select-none text-muted-foreground/40">·</span>

                <div className="flex items-center gap-3">
                  <span className="flex items-center gap-1" title="조회수">
                    <Eye className="h-4 w-4" aria-hidden="true" />
                    <span>조회 {board.viewCount.toLocaleString()}</span>
                  </span>

                  <span className="flex items-center gap-1" title="좋아요 수">
                    <Heart className="h-4 w-4 text-red-500/80" aria-hidden="true" />
                    <span>좋아요 {board.likeCount.toLocaleString()}</span>
                  </span>

                  <span className="flex items-center gap-1" title="댓글 수">
                    <MessageSquare className="h-4 w-4 text-primary/80" aria-hidden="true" />
                    <span>댓글 {board.commentCount.toLocaleString()}</span>
                  </span>
                </div>
              </div>
            </div>

            {/* 본문 콘텐츠 */}
            <div
              className="mt-8 border-t border-border/70 pt-8 text-base leading-relaxed text-foreground/90 break-words sm:text-lg sm:leading-loose
                [&_p]:mb-4 [&_p]:leading-relaxed
                [&_h1]:mt-8 [&_h1]:mb-4 [&_h1]:text-2xl [&_h1]:font-bold [&_h1]:text-foreground
                [&_h2]:mt-6 [&_h2]:mb-3 [&_h2]:text-xl [&_h2]:font-bold [&_h2]:text-foreground
                [&_h3]:mt-5 [&_h3]:mb-2 [&_h3]:text-lg [&_h3]:font-semibold [&_h3]:text-foreground
                [&_ul]:my-4 [&_ul]:list-disc [&_ul]:pl-6
                [&_ol]:my-4 [&_ol]:list-decimal [&_ol]:pl-6
                [&_li]:my-1.5
                [&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-primary/50 [&_blockquote]:bg-muted/40 [&_blockquote]:py-2 [&_blockquote]:pl-4 [&_blockquote]:italic [&_blockquote]:text-muted-foreground
                [&_a]:text-primary [&_a]:underline [&_a]:underline-offset-4 hover:[&_a]:opacity-80
                [&_img]:my-6 [&_img]:max-w-full [&_img]:rounded-xl [&_img]:shadow-sm
                [&_strong]:font-semibold [&_strong]:text-foreground"
              dangerouslySetInnerHTML={{ __html: formatContentHtml(board.content) }}
            />

            {/* 추가 사진 갤러리 (이미지가 2장 이상이거나 이미지가 존재할 때) */}
            {board.images && board.images.length > 0 && (
              <section className="mt-12 border-t border-border/70 pt-8" aria-label="게시글 사진 갤러리">
                <h2 className="text-lg font-semibold tracking-tight sm:text-xl">
                  사진 갤러리 <span className="text-sm font-normal text-muted-foreground">({board.images.length})</span>
                </h2>
                <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
                  {board.images.map((img, idx) => (
                    <div
                      key={`${img.imageUrl}-${idx}`}
                      className="group relative aspect-[4/3] overflow-hidden rounded-xl border border-border/60 bg-muted shadow-panel"
                    >
                      <img
                        src={img.imageUrl}
                        alt={img.altText || `${board.title} 사진 ${idx + 1}`}
                        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                        loading="lazy"
                      />
                    </div>
                  ))}
                </div>
              </section>
            )}
          </article>
        </main>
      )}
    </div>
  );
}
