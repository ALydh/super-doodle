import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";

export function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <header className="app-header">
      <Link to="/" className="header-brand">Home</Link>
      <nav className="header-nav">
        {user ? (
          <>
            <Link to="/admin">Admin</Link>
            <span className="header-separator">·</span>
            <span className="header-user">{user.username}</span>
            <span className="header-separator">·</span>
            <button onClick={handleLogout} className="header-logout">Logout</button>
          </>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <span className="header-separator">·</span>
            <Link to="/register">Register</Link>
          </>
        )}
      </nav>
    </header>
  );
}
