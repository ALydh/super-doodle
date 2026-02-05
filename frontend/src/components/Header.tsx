import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import styles from "./Header.module.css";

export function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.brand}>Home</Link>
      <nav className={styles.nav}>
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
