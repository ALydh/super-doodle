import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuth } from "../context/useAuth";
import { Spinner } from "./Spinner";

interface Props {
  children: ReactNode;
}

export function PublicOnlyRoute({ children }: Props) {
  const { user, loading } = useAuth();

  if (loading) {
    return <Spinner />;
  }

  if (user) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
