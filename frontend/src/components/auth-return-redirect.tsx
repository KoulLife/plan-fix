import { ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";

import { clearPendingAuthReturnTo, readPendingAuthReturnTo } from "@/lib/auth-return-to";

export default function AuthReturnRedirect({ children }: { children: ReactNode }) {
  const [returnTo] = useState(readPendingAuthReturnTo);

  useEffect(() => {
    clearPendingAuthReturnTo();
  }, []);

  return returnTo ? <Navigate to={returnTo} replace /> : <>{children}</>;
}
