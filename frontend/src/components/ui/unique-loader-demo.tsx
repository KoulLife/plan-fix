import {
  LoaderFive,
  LoaderFour,
  LoaderOne,
  LoaderThree,
  LoaderTwo,
} from "@/components/ui/unique-loader-components";

const loaderCards = [
  {
    title: "점프 로더",
    description: "버튼이나 작은 영역에서 짧은 요청을 기다릴 때 사용합니다.",
    loader: <LoaderOne />,
  },
  {
    title: "플로우 로더",
    description: "화면 전환이나 연속 데이터를 불러오는 상태에 어울립니다.",
    loader: <LoaderTwo />,
  },
  {
    title: "에너지 로더",
    description: "일정 생성처럼 중요한 처리 과정을 강조할 때 사용합니다.",
    loader: <LoaderThree />,
  },
  {
    title: "텍스트 글리치",
    description: "현재 처리 중인 작업을 짧은 문장으로 안내합니다.",
    loader: <LoaderFour text="여행 정보를 불러오는 중..." />,
  },
  {
    title: "텍스트 웨이브",
    description: "조금 더 차분한 진행 상태 문구에 사용할 수 있습니다.",
    loader: <LoaderFive text="일정을 정리하고 있어요" />,
  },
];

export default function UniqueLoaderDemo() {
  return (
    <main className="min-h-screen bg-background px-6 py-14 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <header className="max-w-2xl">
          <p className="text-sm font-semibold tracking-[0.24em] text-primary">PLANFIX UI</p>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">로딩 UI</h1>
          <p className="mt-4 leading-7 text-muted-foreground">
            데이터 조회, 화면 전환, 여행 일정 생성 과정에 맞춰 선택할 수 있는 로더입니다.
          </p>
        </header>

        <section className="mt-10 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {loaderCards.map((item) => (
            <article
              key={item.title}
              className="flex min-h-64 flex-col rounded-lg border bg-background p-6 shadow-panel"
            >
              <div>
                <h2 className="text-lg font-semibold">{item.title}</h2>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">{item.description}</p>
              </div>
              <div className="flex flex-1 items-center justify-center py-8">{item.loader}</div>
            </article>
          ))}
        </section>
      </div>
    </main>
  );
}
