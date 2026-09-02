import { Navigate, Route, Routes } from "react-router-dom";

import LoginFormDemo from "@/components/ui/demo";
import UniqueLoaderDemo from "@/components/ui/unique-loader-demo";
import BoardDetailPage from "@/pages/board-detail-page";
import CourseCreatePage from "@/pages/course-create-page";
import CourseDetailPage from "@/pages/course-detail-page";
import CourseListPage from "@/pages/course-list-page";
import LoginPage from "@/pages/login-page";
import MainPage from "@/pages/main-page";
import PopularSpotsPage from "@/pages/popular-spots-page";
import SignupPage from "@/pages/signup-page";
import SpotDetailPage from "@/pages/spot-detail-page";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login/demo" element={<LoginFormDemo />} />
      <Route path="/loading/demo" element={<UniqueLoaderDemo />} />
      <Route path="/main" element={<MainPage />} />
      <Route path="/spots/popular" element={<PopularSpotsPage />} />
      <Route path="/spots/:spotId" element={<SpotDetailPage />} />
      <Route path="/boards/:boardId" element={<BoardDetailPage />} />
      <Route path="/courses" element={<CourseListPage />} />
      <Route path="/courses/create" element={<CourseCreatePage />} />
      <Route path="/courses/:courseId" element={<CourseDetailPage />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
