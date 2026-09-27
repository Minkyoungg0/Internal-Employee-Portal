import { useEffect, useState } from 'react';
import { Shell } from '../components/Shell';
import { api } from '../api';
import type { CurrentUser, Profile } from '../types';

export function ProfilePage({ user, onLogout }: { user: CurrentUser; onLogout: () => Promise<void> }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api<Profile>('/api/me/profile')
      .then(setProfile)
      .catch(x => setError(x.message));
  }, []);

  return (
    <Shell title="내 정보" user={user} onLogout={onLogout}>
      <section className="card">
        {error && <p className="error">{error}</p>}
        {profile && (
          <dl className="details">
            <dt>사번</dt>
            <dd>{profile.employeeNumber}</dd>
            <dt>성명</dt>
            <dd>{profile.fullName}</dd>
            <dt>생년월일</dt>
            <dd>{profile.dateOfBirth ?? '확인되지 않음'}</dd>
            <dt>아이디</dt>
            <dd>{profile.username}</dd>
            <dt>재직 상태</dt>
            <dd>{profile.employmentStatus === 'ACTIVE' ? '재직' : '퇴사'}</dd>
          </dl>
        )}
        <p className="muted">개인정보 변경이 필요하면 관리자에게 요청해 주세요. 비밀번호는 직접 변경할 수 있습니다.</p>
      </section>
    </Shell>
  );
}
