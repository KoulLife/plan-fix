import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import App from "@/App";

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
