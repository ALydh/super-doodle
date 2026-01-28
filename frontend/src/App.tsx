import { BrowserRouter, Routes, Route } from "react-router-dom";
import { FactionListPage } from "./pages/FactionListPage";
import { FactionDetailPage } from "./pages/FactionDetailPage";
import { DatasheetDetailPage } from "./pages/DatasheetDetailPage";
import { ArmyBuilderPage } from "./pages/ArmyBuilderPage";
import { ArmyViewPage } from "./pages/ArmyViewPage";

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<FactionListPage />} />
        <Route path="/factions/:factionId" element={<FactionDetailPage />} />
        <Route path="/factions/:factionId/armies/new" element={<ArmyBuilderPage />} />
        <Route path="/datasheets/:datasheetId" element={<DatasheetDetailPage />} />
        <Route path="/armies/:armyId" element={<ArmyViewPage />} />
        <Route path="/armies/:armyId/edit" element={<ArmyBuilderPage />} />
      </Routes>
    </BrowserRouter>
  );
}
