import { useCallback, useEffect, useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { CompactModeProvider, useCompactMode } from "./context/CompactModeContext";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { PublicOnlyRoute } from "./components/PublicOnlyRoute";
import { Header } from "./components/Header";
import { SpotlightSearch } from "./components/SpotlightSearch";
import { RevisionPanel } from "./components/RevisionPanel";
import {
  fetchFactions, fetchAllDatasheets, fetchAllStratagems, fetchAllEnhancements,
  fetchWeaponAbilities, fetchCoreAbilities, fetchAllArmies,
} from "./api";
import { FactionListPage } from "./pages/FactionListPage";
import { FactionDetailPage } from "./pages/FactionDetailPage";
import { ArmyBuilderPage } from "./pages/ArmyBuilderPage";
import { ArmyViewPage } from "./pages/ArmyViewPage";
import { InventoryPage } from "./pages/InventoryPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { AdminPage } from "./pages/AdminPage";
import { GlossaryPage } from "./pages/GlossaryPage";

function AppShell() {
  const { compact } = useCompactMode();
  const [spotlightOpen, setSpotlightOpen] = useState(false);
  const [revisionOpen, setRevisionOpen] = useState(false);
  const closeSpotlight = useCallback(() => setSpotlightOpen(false), []);
  const closeRevision = useCallback(() => setRevisionOpen(false), []);

  useEffect(() => {
    fetchFactions();
    fetchAllDatasheets();
    fetchAllStratagems();
    fetchAllEnhancements();
    fetchWeaponAbilities();
    fetchCoreAbilities();
    fetchAllArmies();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setSpotlightOpen((prev) => !prev);
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <div data-compact={compact || undefined}>
      <Header onSearchClick={() => setSpotlightOpen(true)} onRevisionClick={() => setRevisionOpen(true)} />
      <SpotlightSearch open={spotlightOpen} onClose={closeSpotlight} />
      <RevisionPanel open={revisionOpen} onClose={closeRevision} />
      <Routes>
        <Route path="/" element={<FactionListPage />} />
        <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
        <Route path="/register" element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />
        <Route path="/glossary" element={<GlossaryPage />} />
        <Route path="/admin" element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />
        <Route path="/factions/:factionId" element={<FactionDetailPage />} />
        <Route path="/factions/:factionId/armies/new" element={<ProtectedRoute><ArmyBuilderPage /></ProtectedRoute>} />
        <Route path="/factions/:factionId/inventory" element={<ProtectedRoute><InventoryPage /></ProtectedRoute>} />
        <Route path="/armies/:armyId" element={<ArmyViewPage />} />
        <Route path="/armies/:armyId/edit" element={<ProtectedRoute><ArmyViewPage /></ProtectedRoute>} />
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
