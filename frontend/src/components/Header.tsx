import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useCompactMode } from "../context/CompactModeContext";
import { RevisionBadge } from "./RevisionBadge";
import styles from "./Header.module.css";

interface HeaderProps {
  onSearchClick?: () => void;
  onRevisionClick?: () => void;
}

export function Header({ onSearchClick, onRevisionClick }: HeaderProps) {
  const { user, logout } = useAuth();
  const { compact, toggleCompact } = useCompactMode();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  const copyToken = () => {
    const token = localStorage.getItem("auth_token");
    if (token) {
      navigator.clipboard.writeText(token);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  const closeMenu = () => setMenuOpen(false);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
    };
    if (userMenuOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [userMenuOpen]);

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.brand}>Home</Link>
      {onRevisionClick && <RevisionBadge onClick={onRevisionClick} />}
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
              <div className={styles.userMenu} ref={userMenuRef}>
                <button
                  className={styles.userMenuTrigger}
                  onClick={() => setUserMenuOpen(!userMenuOpen)}
                  aria-expanded={userMenuOpen}
                >
                  {user.username}
                  <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points={userMenuOpen ? "2,7 5,3 8,7" : "2,3 5,7 8,3"} />
                  </svg>
                </button>
                {userMenuOpen && (
                  <div className={styles.userMenuDropdown}>
                    <button onClick={() => { copyToken(); setUserMenuOpen(false); }} className={styles.userMenuDropdownItem}>
                      {copied ? "Copied!" : "Copy token"}
                    </button>
                    <button onClick={() => { handleLogout(); setUserMenuOpen(false); }} className={styles.userMenuDropdownItem}>
                      Logout
                    </button>
                  </div>
                )}
              </div>
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
          aria-expanded={menuOpen}
          aria-controls="mobile-menu"
        >
          {menuOpen ? "×" : "☰"}
        </button>
      </nav>
      <nav id="mobile-menu" className={`${styles.mobileMenu} ${menuOpen ? styles.mobileMenuOpen : ""}`} role="menu">
        <button onClick={() => { toggleCompact(); closeMenu(); }} className={styles.mobileMenuItem}>
          {compact ? "Show flavor text" : "Hide flavor text"}
        </button>
        <Link to="/glossary" className={styles.mobileMenuItem} onClick={closeMenu}>Glossary</Link>
        {user ? (
          <>
            <Link to="/admin" className={styles.mobileMenuItem} onClick={closeMenu}>Admin</Link>
            <span className={styles.mobileMenuItem} style={{ opacity: 0.6 }}>{user.username}</span>
            <button onClick={() => { copyToken(); closeMenu(); }} className={styles.mobileMenuItem}>{copied ? "Copied!" : "Copy token"}</button>
            <button onClick={() => { handleLogout(); closeMenu(); }} className={styles.mobileMenuItem}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/login" className={styles.mobileMenuItem} onClick={closeMenu}>Login</Link>
            <Link to="/register" className={styles.mobileMenuItem} onClick={closeMenu}>Register</Link>
          </>
        )}
      </nav>
    </header>
  );
}
