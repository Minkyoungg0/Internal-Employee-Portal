import { useEffect, useState, type FormEvent } from 'react';
import { Shell } from '../components/Shell';
import { api } from '../api';
import type { CurrentUser, EmployeeChange, Profile } from '../types';

const CHANGE_LABEL = { PENDING: '승인 대기', APPROVED: '승인 완료', REJECTED: '반려' } as const;

function formatDateTime(value: string | null) {
  return value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-';
}

export function ProfilePage({ user, onLogout }: { user: CurrentUser; onLogout: () => Promise<void> }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [history, setHistory] = useState<EmployeeChange[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    const [nextProfile, nextHistory] = await Promise.all([
      api<Profile>('/api/me/profile'),
      api<EmployeeChange[]>('/api/me/profile-change-requests'),
    ]);
    setProfile(nextProfile);
    setHistory(nextHistory);
  };

  useEffect(() => { load().catch(x => setError(x.message)); }, []);

  async function requestChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError('');
    setMessage('');
    try {
      await api('/api/me/profile-change-requests', {
        method: 'POST',
        body: JSON.stringify({
          lastName: String(data.get('lastName')),
          firstName: String(data.get('firstName')),
          dateOfBirth: String(data.get('dateOfBirth')) || null,
        }),
      });
      setMessage('인적사항 변경 요청을 제출했습니다. 관리자 승인 후 반영됩니다.');
      await load();
    } catch (x) {
      setError(x instanceof Error ? x.message : '변경 요청을 제출하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  }

  const pending = history.some(item => item.status === 'PENDING');

  return (
    <Shell title="내 정보" user={user} onLogout={onLogout}>
      <section className="card">
        {error && <p className="error">{error}</p>}
        {message && <p className="success">{message}</p>}
        {profile && <>
          <dl className="details">
            <dt>사번</dt><dd>{profile.employeeNumber}</dd>
            <dt>성명</dt><dd>{profile.fullName}</dd>
            <dt>생년월일</dt><dd>{profile.dateOfBirth ?? '확인되지 않음'}</dd>
            <dt>아이디</dt><dd>{profile.username}</dd>
            <dt>재직 상태</dt><dd>{profile.employmentStatus === 'ACTIVE' ? '재직' : '퇴사'}</dd>
          </dl>
          <form className="form profile-change-form" onSubmit={requestChange}>
            <h2>인적사항 변경 요청</h2>
            <p className="muted">관리자 승인 후 실제 정보에 반영됩니다.</p>
            <div className="grid">
              <label>성<input name="lastName" defaultValue={profile.lastName} maxLength={50} required /></label>
              <label>이름<input name="firstName" defaultValue={profile.firstName} maxLength={50} required /></label>
              <label>생년월일<input name="dateOfBirth" type="date" defaultValue={profile.dateOfBirth ?? ''} /></label>
            </div>
            <button disabled={submitting || pending}>{submitting ? '요청 중…' : pending ? '승인 대기 중' : '변경 요청'}</button>
          </form>
        </>}
      </section>
      <section className="card table-card">
        <h2>인적사항 변경 이력</h2>
        {history.length === 0 ? <p className="muted">변경 이력이 없습니다.</p> : <table>
          <thead><tr><th>요청 내용</th><th>상태</th><th>요청 시각</th><th>처리 시각</th></tr></thead>
          <tbody>{history.map(item => <tr key={item.id}>
            <td>{item.previous.fullName} / {item.previous.dateOfBirth ?? '미확인'} → {item.requested.fullName} / {item.requested.dateOfBirth ?? '미확인'}</td>
            <td><span className={`change-badge ${item.status.toLowerCase()}`}>{CHANGE_LABEL[item.status]}</span></td>
            <td>{formatDateTime(item.requestedAt)}</td>
            <td>{formatDateTime(item.reviewedAt)}</td>
          </tr>)}</tbody>
        </table>}
      </section>
    </Shell>
  );
}
