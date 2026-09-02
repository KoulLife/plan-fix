import { fireEvent, render, screen } from "@testing-library/react";

import CourseSelectModal from "@/components/ui/course-select-modal";

describe("CourseSelectModal", () => {
  const mockOnClose = jest.fn();
  const mockOnSelectManual = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("does not render when open is false", () => {
    render(
      <CourseSelectModal
        open={false}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("renders modal with AI (disabled) and Manual course selection buttons when open is true", () => {
    render(
      <CourseSelectModal
        open={true}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("여행 코스 만들기")).toBeInTheDocument();
    expect(screen.getByText("AI 코스 생성")).toBeInTheDocument();
    expect(screen.getByText("준비 중")).toBeInTheDocument();
    expect(screen.getByText("직접 코스 생성")).toBeInTheDocument();
  });

  test("AI 코스 생성 버튼은 비활성화(disabled) 상태이다", () => {
    render(
      <CourseSelectModal
        open={true}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    const aiButton = screen.getByRole("button", { name: /AI 코스 생성/i });
    expect(aiButton).toBeDisabled();
  });

  test("calls onSelectManual when 직접 코스 생성 button is clicked", () => {
    render(
      <CourseSelectModal
        open={true}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    const manualButton = screen.getByRole("button", { name: /직접 코스 생성/i });
    fireEvent.click(manualButton);

    expect(mockOnSelectManual).toHaveBeenCalledTimes(1);
  });

  test("calls onClose when close button is clicked", () => {
    render(
      <CourseSelectModal
        open={true}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    const closeButton = screen.getByRole("button", { name: "코스 선택 창 닫기" });
    fireEvent.click(closeButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test("calls onClose when backdrop is clicked", () => {
    render(
      <CourseSelectModal
        open={true}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    const backdrop = screen.getByTestId("course-select-backdrop");
    fireEvent.mouseDown(backdrop);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test("calls onClose when Escape key is pressed", () => {
    render(
      <CourseSelectModal
        open={true}
        onClose={mockOnClose}
        onSelectManual={mockOnSelectManual}
      />
    );

    fireEvent.keyDown(document, { key: "Escape" });

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });
});
