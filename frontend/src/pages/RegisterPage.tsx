import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";

const USERNAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

function validateUsername(username: string): string | null {
  const trimmed = username.trim();
  if (!trimmed) return "Username is required";
  if (trimmed.length < 3) return "Username must be at least 3 characters";
  if (trimmed.length > 50) return "Username cannot exceed 50 characters";
  if (!USERNAME_PATTERN.test(trimmed)) return "Username can only contain letters, numbers, underscores, and hyphens";
  return null;
}

function validatePassword(password: string): string | null {
  if (!password) return "Password is required";
  if (password.length < 6) return "Password must be at least 6 characters";
  return null;
}

export function RegisterPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ username?: string; password?: string }>({});
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { register } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const usernameError = validateUsername(username);
    const passwordError = validatePassword(password);
    setFieldErrors({ username: usernameError ?? undefined, password: passwordError ?? undefined });

    if (usernameError || passwordError) return;

    setLoading(true);
    try {
      await register(username.trim(), password, inviteCode || undefined);
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <h1>Register</h1>
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <div className="error-message">{error}</div>}
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
          {fieldErrors.username && <div className="field-error">{fieldErrors.username}</div>}
        </div>
        <div>
          <label>
            Password:
            <input
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setFieldErrors(prev => ({ ...prev, password: undefined })); }}
              required
              autoComplete="new-password"
              aria-invalid={!!fieldErrors.password}
            />
          </label>
          {fieldErrors.password && <div className="field-error">{fieldErrors.password}</div>}
        </div>
        <div>
          <label>
            Invite Code (required unless first user):
            <input
              type="text"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value)}
              autoComplete="off"
            />
          </label>
        </div>
        <button type="submit" disabled={loading}>
          {loading ? "Registering..." : "Register"}
        </button>
      </form>
      <p>
        Already have an account? <Link to="/login">Login</Link>
      </p>
    </div>
  );
}
