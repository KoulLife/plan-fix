import { Navigate, Route, Routes } from "react-router-dom";

import LoginFormDemo from "@/components/ui/demo";
import LoginPage from "@/pages/login-page";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/login/demo" element={<LoginFormDemo />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
