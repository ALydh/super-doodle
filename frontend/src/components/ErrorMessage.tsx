interface Props {
  message: string;
}

export function ErrorMessage({ message }: Props) {
  return (
    <div style={{ padding: "48px 16px", textAlign: "center" }}>
      <p style={{ color: "var(--text-secondary)", marginBottom: "16px" }}>{message}</p>
      <button
        onClick={() => window.location.reload()}
        style={{
          padding: "10px 24px",
          background: "var(--faction-primary)",
          color: "white",
          border: "none",
          borderRadius: "6px",
          cursor: "pointer",
          fontSize: "0.9rem",
        }}
      >
        Try Again
      </button>
    </div>
  );
}
