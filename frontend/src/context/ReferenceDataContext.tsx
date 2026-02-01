import { createContext, useContext, ReactNode } from "react";
import type { UnitCost, Enhancement, DatasheetLeader, Datasheet, DatasheetOption } from "../types";

export interface ReferenceDataContextType {
  costs: UnitCost[];
  enhancements: Enhancement[];
  leaders: DatasheetLeader[];
  datasheets: Datasheet[];
  options: DatasheetOption[];
}

const ReferenceDataContext = createContext<ReferenceDataContextType | null>(null);

interface ProviderProps {
  children: ReactNode;
  costs: UnitCost[];
  enhancements: Enhancement[];
  leaders: DatasheetLeader[];
  datasheets: Datasheet[];
  options: DatasheetOption[];
}

export function ReferenceDataProvider({
  children,
  costs,
  enhancements,
  leaders,
  datasheets,
  options,
}: ProviderProps) {
  return (
    <ReferenceDataContext.Provider value={{ costs, enhancements, leaders, datasheets, options }}>
      {children}
    </ReferenceDataContext.Provider>
  );
}

export function useReferenceData(): ReferenceDataContextType {
  const context = useContext(ReferenceDataContext);
  if (!context) {
    throw new Error("useReferenceData must be used within a ReferenceDataProvider");
  }
  return context;
}
