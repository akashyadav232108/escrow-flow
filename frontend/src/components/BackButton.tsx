import { useNavigate } from 'react-router-dom';

export default function BackButton({ className = '' }: { className?: string }) {
  const navigate = useNavigate();

  const handleBack = () => {
    if (window.history.length > 2) {
      navigate(-1);
    } else {
      navigate('/');
    }
  };

  return (
    <button
      type="button"
      className={`back-button${className ? ` ${className}` : ''}`}
      onClick={handleBack}
      aria-label="Go back"
    >
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </button>
  );
}
