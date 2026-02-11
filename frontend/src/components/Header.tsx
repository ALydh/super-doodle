import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useCompactMode } from "../context/CompactModeContext";
import styles from "./Header.module.css";

export function Header() {
  const { user, logout } = useAuth();
  const { compact, toggleCompact } = useCompactMode();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.brand}>Home</Link>
      <nav className={styles.nav}>
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
      </nav>
    </header>
  );
}
