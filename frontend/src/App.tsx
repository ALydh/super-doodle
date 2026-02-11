import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { CompactModeProvider, useCompactMode } from "./context/CompactModeContext";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { Header } from "./components/Header";
import { FactionListPage } from "./pages/FactionListPage";
import { FactionDetailPage } from "./pages/FactionDetailPage";
import { ArmyBuilderPage } from "./pages/ArmyBuilderPage";
import { ArmyViewPage } from "./pages/ArmyViewPage";
import { InventoryPage } from "./pages/InventoryPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { AdminPage } from "./pages/AdminPage";

function AppShell() {
  const { compact } = useCompactMode();
  return (
    <div data-compact={compact || undefined}>
      <Header />
      <Routes>
        <Route path="/" element={<FactionListPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/admin" element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />
        <Route path="/factions/:factionId" element={<FactionDetailPage />} />
        <Route path="/factions/:factionId/armies/new" element={<ProtectedRoute><ArmyBuilderPage /></ProtectedRoute>} />
        <Route path="/factions/:factionId/inventory" element={<ProtectedRoute><InventoryPage /></ProtectedRoute>} />
        <Route path="/armies/:armyId" element={<ArmyViewPage />} />
        <Route path="/armies/:armyId/edit" element={<ProtectedRoute><ArmyBuilderPage /></ProtectedRoute>} />
      </Routes>
    </div>
  );
}

export function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <CompactModeProvider>
          <BrowserRouter>
            <AppShell />
          </BrowserRouter>
        </CompactModeProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}
