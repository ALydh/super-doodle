import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useCompactMode } from "../context/CompactModeContext";
import styles from "./Header.module.css";

interface HeaderProps {
  onSearchClick?: () => void;
}

export function Header({ onSearchClick }: HeaderProps) {
  const { user, logout } = useAuth();
  const { compact, toggleCompact } = useCompactMode();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  const closeMenu = () => setMenuOpen(false);

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.brand}>Home</Link>
      <nav className={styles.nav}>
        {onSearchClick && (
          <>
            <button onClick={onSearchClick} className={styles.compactToggle} title="Search (Ctrl+K)">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </button>
            <span className={styles.separator}>·</span>
          </>
        )}
        <span className={styles.desktopOnly}>
          <button onClick={toggleCompact} className={styles.compactToggle} title={compact ? "Show flavor text" : "Hide flavor text"}>
            {compact ? (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            )}
          </button>
          <span className={styles.separator}>·</span>
          <Link to="/glossary">Glossary</Link>
          <span className={styles.separator}>·</span>
          {user ? (
            <>
              <Link to="/admin">Admin</Link>
              <span className={styles.separator}>·</span>
              <span className={styles.user}>{user.username}</span>
              <span className={styles.separator}>·</span>
              <button onClick={handleLogout} className={styles.logout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login">Login</Link>
              <span className={styles.separator}>·</span>
              <Link to="/register">Register</Link>
            </>
          )}
        </span>
        <button
          className={styles.menuToggle}
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle menu"
        >
          {menuOpen ? "×" : "☰"}
        </button>
      </nav>
      <div className={`${styles.mobileMenu} ${menuOpen ? styles.mobileMenuOpen : ""}`}>
        <button onClick={() => { toggleCompact(); closeMenu(); }} className={styles.mobileMenuItem}>
          {compact ? "Show flavor text" : "Hide flavor text"}
        </button>
        <Link to="/glossary" className={styles.mobileMenuItem} onClick={closeMenu}>Glossary</Link>
        {user ? (
          <>
            <Link to="/admin" className={styles.mobileMenuItem} onClick={closeMenu}>Admin</Link>
            <span className={styles.mobileMenuItem} style={{ opacity: 0.6 }}>{user.username}</span>
            <button onClick={() => { handleLogout(); closeMenu(); }} className={styles.mobileMenuItem}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/login" className={styles.mobileMenuItem} onClick={closeMenu}>Login</Link>
            <Link to="/register" className={styles.mobileMenuItem} onClick={closeMenu}>Register</Link>
          </>
        )}
      </div>
    </header>
  );
}
