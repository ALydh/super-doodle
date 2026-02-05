import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import styles from "./AuthPage.module.css";

export function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ username?: string; password?: string }>({});
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const usernameError = !username.trim() ? "Username is required" : undefined;
    const passwordError = !password ? "Password is required" : undefined;
    setFieldErrors({ username: usernameError, password: passwordError });

    if (usernameError || passwordError) return;

    setLoading(true);
    try {
      await login(username.trim(), password);
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <h1>Login</h1>
      <form onSubmit={handleSubmit} className={styles.form}>
        {error && <div className={styles.errorMessage}>{error}</div>}
        <div>
          <label>
            Username:
            <input
              type="text"
              value={username}
              onChange={(e) => { setUsername(e.target.value); setFieldErrors(prev => ({ ...prev, username: undefined })); }}
              required
              autoComplete="username"
              aria-invalid={!!fieldErrors.username}
            />
          </label>
          {fieldErrors.username && <div className={styles.fieldError}>{fieldErrors.username}</div>}
        </div>
        <div>
          <label>
            Password:
            <input
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setFieldErrors(prev => ({ ...prev, password: undefined })); }}
              required
              autoComplete="current-password"
              aria-invalid={!!fieldErrors.password}
            />
          </label>
          {fieldErrors.password && <div className={styles.fieldError}>{fieldErrors.password}</div>}
        </div>
        <button type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>
      </form>
      <p>
        Don't have an account? <Link to="/register">Register</Link>
      </p>
    </div>
  );
}
