import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import App from "@/App";
import LoginForm from "@/components/ui/login-form";
import SignupForm from "@/components/ui/signup-form";

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
  expect(screen.getByRole("button", { name: "카카오 로그인" })).toBeInTheDocument();
});

test("opens the Kakao account UI preview from the login screen", () => {
  render(
    <MemoryRouter
      initialEntries={["/login"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  fireEvent.click(screen.getByRole("button", { name: "카카오 로그인" }));

  expect(screen.getByRole("heading", { name: "카카오계정 로그인" })).toBeInTheDocument();
  expect(screen.queryByText("UI 미리보기 화면입니다.", { exact: false })).not.toBeInTheDocument();
  expect(screen.getByLabelText("카카오계정")).toHaveAttribute(
    "placeholder",
    "카카오메일 아이디, 이메일, 전화번호",
  );
});

test("shows the Kakao consent UI after the account preview", () => {
  render(
    <MemoryRouter
      initialEntries={["/login/kakao"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  fireEvent.change(screen.getByLabelText("카카오계정"), {
    target: { value: "preview@kakao.com" },
  });
  fireEvent.change(screen.getByLabelText("카카오계정 비밀번호"), {
    target: { value: "preview-password" },
  });
  fireEvent.submit(screen.getByRole("form", { name: "카카오계정 로그인" }));

  expect(screen.getByRole("heading", { name: "PlanFix에 로그인" })).toBeInTheDocument();
  const requiredConsent = screen.getByLabelText(
    /^\[필수\] 카카오 개인정보 제3자 제공 동의/,
  );
  expect(requiredConsent).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "동의하고 계속하기" })).toBeDisabled();

  fireEvent.click(requiredConsent);
  fireEvent.click(screen.getByRole("button", { name: "동의하고 계속하기" }));

  expect(screen.getByRole("status")).toHaveTextContent(
    "실제 연동에서는 카카오가 인가 코드를 PlanFix 백엔드로 전달합니다.",
  );
});

test("moves from the login screen to the signup screen", () => {
  render(
    <MemoryRouter
      initialEntries={["/login"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>,
  );

  fireEvent.click(screen.getByRole("link", { name: "회원가입" }));

  expect(screen.getByRole("heading", { name: "회원가입" })).toBeInTheDocument();
  expect(screen.getByLabelText("이름")).toBeInTheDocument();
  expect(screen.getByLabelText("생년월일")).toBeInTheDocument();
  expect(screen.getByLabelText("이메일")).toBeInTheDocument();
  expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
  expect(screen.getByLabelText("비밀번호 확인")).toBeInTheDocument();
  expect(screen.getByLabelText("이름")).toHaveAttribute("spellcheck", "false");
  expect(screen.getByLabelText("이름")).toHaveAttribute("maxlength", "7");
});

test.each([
  ["한글 자음만 입력한 이름", "ㄴㄷㅇ"],
  ["띄어쓰기가 포함된 이름", "김 태용"],
  ["영문 이름", "Kim"],
  ["한글과 영문이 섞인 이름", "김Tae용"],
  ["한 글자 이름", "김"],
  ["여덟 글자 이름", "김가나다라마바사"],
])("shows a warning after leaving a %s", (_caseName, invalidName) => {
  render(<SignupForm />);

  const nameInput = screen.getByLabelText("이름");
  fireEvent.change(nameInput, {
    target: { value: invalidName },
  });
  fireEvent.blur(nameInput);

  expect(screen.getByRole("alert")).toHaveTextContent(
    "공백 없이 완성된 한글 2~7자로 입력해 주세요.",
  );
});

test("does not show warnings when leaving empty signup fields", () => {
  render(<SignupForm />);

  ["이름", "생년월일", "이메일", "비밀번호", "비밀번호 확인"].forEach((label) => {
    fireEvent.blur(screen.getByLabelText(label));
  });

  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

test("keeps format warnings after leaving invalid signup fields", () => {
  render(<SignupForm />);

  const birthDateInput = screen.getByLabelText("생년월일");
  fireEvent.change(birthDateInput, { target: { value: "20261340" } });
  fireEvent.blur(birthDateInput);

  const emailInput = screen.getByLabelText("이메일");
  fireEvent.change(emailInput, { target: { value: "planfix@" } });
  fireEvent.blur(emailInput);

  const passwordInput = screen.getByLabelText("비밀번호");
  fireEvent.change(passwordInput, { target: { value: "password1" } });
  fireEvent.blur(passwordInput);

  const passwordConfirmationInput = screen.getByLabelText("비밀번호 확인");
  fireEvent.change(passwordConfirmationInput, { target: { value: "Password2" } });
  fireEvent.blur(passwordConfirmationInput);

  expect(screen.getByText(
    "생년월일을 yyyy-mm-dd 형식에 맞게 입력해 주세요.",
  )).toBeInTheDocument();
  expect(screen.getByText("올바른 이메일 형식을 입력해 주세요.")).toBeInTheDocument();
  expect(screen.getByText(
    "영문·숫자를 조합하고 대문자를 1개 이상 포함해 8~20자로 입력해 주세요.",
  )).toBeInTheDocument();
  expect(screen.getByText("비밀번호가 일치하지 않습니다.")).toBeInTheDocument();
});

test("formats the signup birth date as yyyy-mm-dd", () => {
  render(<SignupForm />);

  const birthDateInput = screen.getByLabelText("생년월일");
  fireEvent.change(birthDateInput, { target: { value: "19900101" } });

  expect(birthDateInput).toHaveValue("1990-01-01");
  expect(birthDateInput).toHaveAttribute("maxlength", "10");
});

test("rejects an invalid signup birth date", () => {
  render(<SignupForm />);

  fireEvent.change(screen.getByLabelText("이름"), {
    target: { value: "김태용" },
  });
  fireEvent.change(screen.getByLabelText("생년월일"), {
    target: { value: "19901340" },
  });
  fireEvent.submit(screen.getByRole("form", { name: "회원가입 정보" }));

  expect(screen.getByRole("alert")).toHaveTextContent(
    "생년월일을 yyyy-mm-dd 형식에 맞게 입력해 주세요.",
  );
});

test("validates matching passwords on the signup form", async () => {
  const handleSubmit = jest.fn();
  render(
    <SignupForm
      onSubmit={handleSubmit}
      onCheckEmailAvailability={async () => ({
        available: true,
        message: "사용 가능한 이메일입니다.",
      })}
    />,
  );

  fireEvent.change(screen.getByLabelText("이름"), {
    target: { value: "김태용" },
  });

  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "new@planfix.kr" },
  });
  fireEvent.click(screen.getByRole("button", { name: "중복 확인" }));
  expect(await screen.findByText("사용 가능한 이메일입니다.")).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("비밀번호"), {
    target: { value: "Password1" },
  });
  fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
    target: { value: "Password2" },
  });
  fireEvent.submit(screen.getByRole("form", { name: "회원가입 정보" }));

  expect(screen.getByRole("alert")).toHaveTextContent("비밀번호가 일치하지 않습니다.");
  expect(handleSubmit).not.toHaveBeenCalled();
});

test("validates the signup password format", async () => {
  const handleSubmit = jest.fn();
  render(
    <SignupForm
      onSubmit={handleSubmit}
      onCheckEmailAvailability={async () => ({
        available: true,
        message: "사용 가능한 이메일입니다.",
      })}
    />,
  );

  fireEvent.change(screen.getByLabelText("이름"), {
    target: { value: "김태용" },
  });

  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "new@planfix.kr" },
  });
  fireEvent.click(screen.getByRole("button", { name: "중복 확인" }));
  expect(await screen.findByText("사용 가능한 이메일입니다.")).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("비밀번호"), {
    target: { value: "password1" },
  });
  fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
    target: { value: "password1" },
  });
  fireEvent.submit(screen.getByRole("form", { name: "회원가입 정보" }));

  expect(screen.getByRole("alert")).toHaveTextContent(
    "영문·숫자를 조합하고 대문자를 1개 이상 포함해 8~20자로 입력해 주세요.",
  );
  expect(handleSubmit).not.toHaveBeenCalled();
});

test.each([
  ["두 글자", "가나"],
  ["일곱 글자", "김가나다라마바"],
])("accepts a %s Korean signup name and matching password", async (_nameType, signupName) => {
  const handleSubmit = jest.fn();
  render(
    <SignupForm
      onSubmit={handleSubmit}
      onCheckEmailAvailability={async () => ({
        available: true,
        message: "사용 가능한 이메일입니다.",
      })}
    />,
  );

  fireEvent.change(screen.getByLabelText("이름"), {
    target: { value: signupName },
  });

  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "new@planfix.kr" },
  });
  fireEvent.click(screen.getByRole("button", { name: "중복 확인" }));
  expect(await screen.findByText("사용 가능한 이메일입니다.")).toBeInTheDocument();

  const passwordInput = screen.getByLabelText("비밀번호");
  const passwordConfirmationInput = screen.getByLabelText("비밀번호 확인");
  fireEvent.change(passwordInput, { target: { value: "Password1!" } });
  fireEvent.change(passwordConfirmationInput, { target: { value: "Password1!" } });
  fireEvent.submit(screen.getByRole("form", { name: "회원가입 정보" }));

  expect(passwordInput).not.toHaveAttribute("pattern");
  expect(passwordConfirmationInput).not.toHaveAttribute("pattern");
  expect(handleSubmit).toHaveBeenCalledWith(
    expect.objectContaining({
      name: signupName,
      password: "Password1!",
      passwordConfirmation: "Password1!",
    }),
  );
});

test("shows the email duplication result on the signup form", async () => {
  render(
    <SignupForm
      onCheckEmailAvailability={async () => ({
        available: false,
        message: "이미 사용 중인 이메일입니다.",
      })}
    />,
  );

  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "demo@planfix.kr" },
  });
  fireEvent.click(screen.getByRole("button", { name: "중복 확인" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("이미 사용 중인 이메일입니다.");
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
  expect(screen.getByText("PlanFix", { selector: "p" })).toBeInTheDocument();

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
    await screen.findByRole("status", { name: "메인 화면을 준비하는 중" }, { timeout: 1500 }),
  ).toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "강원도 주간 날씨" })).not.toBeInTheDocument();
  expect(
    await screen.findByRole("heading", { name: "강원도 주간 날씨" }, { timeout: 3000 }),
  ).toBeInTheDocument();
});
