import { Navigate, Route, Routes } from "react-router-dom";

import LoginFormDemo from "@/components/ui/demo";
import UniqueLoaderDemo from "@/components/ui/unique-loader-demo";
import LoginPage from "@/pages/login-page";
import MainPage from "@/pages/main-page";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/login/demo" element={<LoginFormDemo />} />
      <Route path="/loading/demo" element={<UniqueLoaderDemo />} />
      <Route path="/main" element={<MainPage />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
