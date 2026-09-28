import { useId, useState, type InputHTMLAttributes } from 'react';

type PasswordInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>;

export function PasswordInput({ id, disabled, ...props }: PasswordInputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const [visible, setVisible] = useState(false);
  const action = visible ? '비밀번호 숨기기' : '비밀번호 보기';

  return (
    <span className="password-input">
      <input {...props} id={inputId} disabled={disabled} type={visible ? 'text' : 'password'} />
      <button
        type="button"
        className="password-toggle"
        aria-label={action}
        aria-controls={inputId}
        title={action}
        disabled={disabled}
        onClick={() => setVisible(value => !value)}
      >
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor"
          strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
          <circle cx="12" cy="12" r="3" />
          {visible && <path d="m3 3 18 18" />}
        </svg>
      </button>
    </span>
  );
}
