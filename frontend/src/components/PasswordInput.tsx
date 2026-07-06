import { useId, useState } from 'react';

interface PasswordInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  required?: boolean;
  minLength?: number;
  autoComplete?: string;
}

export default function PasswordInput({
  value,
  onChange,
  placeholder,
  required,
  minLength,
  autoComplete,
}: PasswordInputProps) {
  const [visible, setVisible] = useState(false);
  const inputId = useId();

  return (
    <div className="password-field">
      <input
        id={inputId}
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        minLength={minLength}
        autoComplete={autoComplete}
      />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((prev) => !prev)}
        aria-label={visible ? 'Hide password' : 'Show password'}
        aria-pressed={visible}
        tabIndex={-1}
      >
        {visible ? (
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path
              d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <circle cx="12" cy="12" r="3" />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path
              d="M2 12s3.9-7 10-7c2.02 0 3.76.63 5.2 1.53M22 12s-3.9 7-10 7c-2.02 0-3.76-.63-5.2-1.53"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path d="M9.5 9.7a3 3 0 0 0 4.3 4.2" strokeLinecap="round" strokeLinejoin="round" />
            <path d="M3 3l18 18" strokeLinecap="round" />
          </svg>
        )}
      </button>
    </div>
  );
}
