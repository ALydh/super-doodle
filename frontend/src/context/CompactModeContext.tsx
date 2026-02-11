import { createContext, useState, useContext, type ReactNode } from "react";

interface CompactModeContextType {
  compact: boolean;
  toggleCompact: () => void;
}

const CompactModeContext = createContext<CompactModeContextType | null>(null);

const STORAGE_KEY = "compact_mode";

export function CompactModeProvider({ children }: { children: ReactNode }) {
  const [compact, setCompact] = useState(() => localStorage.getItem(STORAGE_KEY) === "true");

  const toggleCompact = () => {
    setCompact((prev) => {
      localStorage.setItem(STORAGE_KEY, String(!prev));
      return !prev;
    });
  };

  return (
    <CompactModeContext.Provider value={{ compact, toggleCompact }}>
      {children}
    </CompactModeContext.Provider>
  );
}

export function useCompactMode(): CompactModeContextType {
  const context = useContext(CompactModeContext);
  if (!context) {
    throw new Error("useCompactMode must be used within a CompactModeProvider");
  }
  return context;
}
