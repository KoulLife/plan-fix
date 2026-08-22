import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import App from "@/App";
import LoginForm from "@/components/ui/login-form";

test("renders the login screen", () => {
  render(
    <MemoryRouter
      initialEntries={["/login"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  expect(screen.getByRole("heading", { name: "로그인" })).toBeInTheDocument();
});

test("renders the loading UI demo", () => {
  render(
    <MemoryRouter
      initialEntries={["/loading/demo"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  expect(screen.getByRole("heading", { name: "로딩 UI" })).toBeInTheDocument();
  expect(screen.getAllByRole("status")).toHaveLength(5);
});

test("uses the jump loader while signing in", () => {
  render(<LoginForm isSubmitting />);

  expect(screen.getByRole("status", { name: "로딩 중" })).toBeInTheDocument();
  expect(screen.getByText("로그인 중...")).toBeInTheDocument();
});

test("renders the main screen", () => {
  render(
    <MemoryRouter
      initialEntries={["/main"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  expect(screen.getByRole("heading", { name: "강원도 주간 날씨" })).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "여행 지역 선택: 강원도 / 지역 선택" }),
  ).toBeInTheDocument();
  expect(screen.getByRole("navigation", { name: "하단 메뉴" })).toBeInTheDocument();
});

test("opens the Gangwon map and applies the selected region", () => {
  render(
    <MemoryRouter
      initialEntries={["/main"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  fireEvent.click(
    screen.getByRole("button", { name: "여행 지역 선택: 강원도 / 지역 선택" }),
  );

  expect(screen.getByRole("dialog", { name: "어디로 떠나볼까요?" })).toBeInTheDocument();

  const regionNames = [
    "철원", "화천", "양구", "고성", "춘천", "홍천", "인제", "속초", "양양",
    "원주", "횡성", "평창", "강릉", "영월", "정선", "동해", "태백", "삼척",
  ];
  regionNames.forEach((region) => {
    expect(screen.getByRole("button", { name: region })).toBeInTheDocument();
  });

  const gangneungRegion = screen.getByRole("button", { name: "강릉" });
  const gangwonMap = screen.getByTestId("gangwon-boundary-map");

  fireEvent.mouseEnter(gangneungRegion);
  expect(gangwonMap).toHaveAttribute("data-active-region", "강릉");
  expect(screen.getByText("강릉", { selector: "p" })).toBeInTheDocument();

  fireEvent.mouseLeave(gangneungRegion);
  expect(gangwonMap).toHaveAttribute("data-active-region", "");

  fireEvent.click(gangneungRegion);
  fireEvent.click(screen.getByRole("button", { name: "강릉 선택하기" }));

  expect(screen.queryByRole("dialog", { name: "어디로 떠나볼까요?" })).not.toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "강릉 주간 날씨" })).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "여행 지역 선택: 강원도 / 강릉" }),
  ).toBeInTheDocument();
});

test("closes the region map without changing the initial location", () => {
  render(
    <MemoryRouter
      initialEntries={["/main"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  fireEvent.click(
    screen.getByRole("button", { name: "여행 지역 선택: 강원도 / 지역 선택" }),
  );
  fireEvent.keyDown(document, { key: "Escape" });

  expect(screen.queryByRole("dialog", { name: "어디로 떠나볼까요?" })).not.toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "강원도 주간 날씨" })).toBeInTheDocument();
});

test("moves from the demo login to the main screen", async () => {
  render(
    <MemoryRouter
      initialEntries={["/login"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  fireEvent.change(screen.getByPlaceholderText("이메일"), {
    target: { value: "demo@planfix.kr" },
  });
  fireEvent.change(screen.getByPlaceholderText("비밀번호"), {
    target: { value: "demo-password" },
  });
  fireEvent.click(screen.getByRole("button", { name: "로그인" }));

  expect(screen.getByRole("status", { name: "로딩 중" })).toBeInTheDocument();
  expect(
    await screen.findByRole("heading", { name: "강원도 주간 날씨" }, { timeout: 2000 }),
  ).toBeInTheDocument();
});
