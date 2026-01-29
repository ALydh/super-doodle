import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { createInvite, listInvites } from "../api";
import type { Invite } from "../types";

export function AdminPage() {
  const { user, loading: authLoading } = useAuth();
  const [invites, setInvites] = useState<Invite[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    listInvites()
      .then(setInvites)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user]);

  const handleCreateInvite = async () => {
    setCreating(true);
    setError(null);
    try {
      const invite = await createInvite();
      setInvites([invite, ...invites]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to create invite");
    } finally {
      setCreating(false);
    }
  };

  const handleCopyCode = async (code: string) => {
    await navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  if (authLoading) return <div>Loading...</div>;

  if (!user) {
    return (
      <div>
        <p>You must be logged in to access the admin panel.</p>
        <Link to="/login">Login</Link>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <h1>Admin</h1>

      <section className="admin-section">
        <h2>Invite Codes</h2>
        <button onClick={handleCreateInvite} disabled={creating} className="btn-create-invite">
          {creating ? "Creating..." : "Create Invite"}
        </button>

        {error && <div className="error-message">{error}</div>}

        {loading ? (
          <p>Loading invites...</p>
        ) : invites.length === 0 ? (
          <p>No invites created yet.</p>
        ) : (
          <table className="invites-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Created</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {invites.map((invite) => (
                <tr key={invite.code} className={invite.used ? "invite-used" : ""}>
                  <td className="invite-code">{invite.code}</td>
                  <td>{new Date(invite.createdAt).toLocaleDateString()}</td>
                  <td>{invite.used ? "Used" : "Available"}</td>
                  <td>
                    {!invite.used && (
                      <button
                        onClick={() => handleCopyCode(invite.code)}
                        className="btn-copy"
                      >
                        {copiedCode === invite.code ? "Copied!" : "Copy"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
