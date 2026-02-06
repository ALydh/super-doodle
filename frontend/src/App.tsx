import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { Header } from "./components/Header";
import { FactionListPage } from "./pages/FactionListPage";
import { FactionDetailPage } from "./pages/FactionDetailPage";
import { DatasheetDetailPage } from "./pages/DatasheetDetailPage";
import { ArmyBuilderPage } from "./pages/ArmyBuilderPage";
import { ArmyViewPage } from "./pages/ArmyViewPage";
import { InventoryPage } from "./pages/InventoryPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { AdminPage } from "./pages/AdminPage";

export function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
          <Header />
          <Routes>
            <Route path="/" element={<FactionListPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/admin" element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />
            <Route path="/factions/:factionId" element={<FactionDetailPage />} />
            <Route path="/factions/:factionId/armies/new" element={<ProtectedRoute><ArmyBuilderPage /></ProtectedRoute>} />
            <Route path="/factions/:factionId/inventory" element={<ProtectedRoute><InventoryPage /></ProtectedRoute>} />
            <Route path="/datasheets/:datasheetId" element={<DatasheetDetailPage />} />
            <Route path="/armies/:armyId" element={<ArmyViewPage />} />
            <Route path="/armies/:armyId/edit" element={<ProtectedRoute><ArmyBuilderPage /></ProtectedRoute>} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  );
}
