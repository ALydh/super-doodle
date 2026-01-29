import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <header className="app-header">
      <Link to="/" className="header-brand">Army Builder</Link>
      <nav className="header-nav">
        {user ? (
          <>
            <Link to="/admin">Admin</Link>
            <span className="header-user">{user.username}</span>
            <button onClick={handleLogout} className="btn-link">Logout</button>
          </>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </nav>
    </header>
  );
}
