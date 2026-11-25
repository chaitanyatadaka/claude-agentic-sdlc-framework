// Readable error state with an optional Retry action. Shows the correlation ID only when one is present.
export default function ErrorMessage({ message, correlationId, onRetry }) {
  return (
    <div className="error-state" role="alert">
      <p className="error-text">{message}</p>
      {correlationId && <p className="small muted">Reference: {correlationId}</p>}
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}
