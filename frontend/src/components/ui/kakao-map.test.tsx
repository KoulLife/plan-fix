import { render, screen } from "@testing-library/react";

import KakaoMap from "@/components/ui/kakao-map";

const mutableEnv = import.meta.env as unknown as Record<string, string | undefined>;

function setKakaoKey(value: string | undefined) {
  if (value === undefined) {
    delete mutableEnv.VITE_KAKAO_JS_KEY;
  } else {
    mutableEnv.VITE_KAKAO_JS_KEY = value;
  }
}

describe("KakaoMap", () => {
  const originalKey = import.meta.env.VITE_KAKAO_JS_KEY;

  afterEach(() => {
    setKakaoKey(originalKey);
  });

  test("키가 설정되지 않으면 지도 대신 안내 문구를 보여준다", () => {
    setKakaoKey(undefined);

    render(
      <KakaoMap
        spots={[{ spotId: 1, title: "경포해변", latitude: 37.8, longitude: 128.9 }]}
      />,
    );

    expect(screen.getByRole("status")).toHaveTextContent(
      "지도 키(VITE_KAKAO_JS_KEY)가 설정되지 않아 지도를 표시할 수 없어요.",
    );
    // 키가 없을 땐 지도 컨테이너 자체를 그리지 않는다
    expect(screen.queryByRole("img", { name: "장소 위치 지도" })).not.toBeInTheDocument();
  });

  test("좌표가 없는 장소가 있으면 몇 곳이 빠졌는지 알려준다", () => {
    setKakaoKey("test-kakao-js-key");

    render(
      <KakaoMap
        spots={[
          { spotId: 1, title: "경포해변", latitude: 37.8, longitude: 128.9 },
          { spotId: 2, title: "좌표없음1", latitude: null, longitude: null },
          { spotId: 3, title: "좌표없음2" },
        ]}
      />,
    );

    expect(screen.getByText("좌표 정보가 없는 장소 2곳은 지도에 표시되지 않았어요.")).toBeInTheDocument();
  });
});
