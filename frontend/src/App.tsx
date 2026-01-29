import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { Header } from "./components/Header";
import { FactionListPage } from "./pages/FactionListPage";
import { FactionDetailPage } from "./pages/FactionDetailPage";
import { DatasheetDetailPage } from "./pages/DatasheetDetailPage";
import { ArmyBuilderPage } from "./pages/ArmyBuilderPage";
import { ArmyViewPage } from "./pages/ArmyViewPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { AdminPage } from "./pages/AdminPage";

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Header />
        <Routes>
          <Route path="/" element={<FactionListPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/factions/:factionId" element={<FactionDetailPage />} />
          <Route path="/factions/:factionId/armies/new" element={<ArmyBuilderPage />} />
          <Route path="/datasheets/:datasheetId" element={<DatasheetDetailPage />} />
          <Route path="/armies/:armyId" element={<ArmyViewPage />} />
          <Route path="/armies/:armyId/edit" element={<ArmyBuilderPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
