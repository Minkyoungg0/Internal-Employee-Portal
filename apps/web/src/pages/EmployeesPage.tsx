import { Fragment, useEffect, useState, type FormEvent } from 'react';
import { Shell } from '../components/Shell';
import { api } from '../api';
import { usePolling } from '../hooks/usePolling';
import type {
  BackgroundCheckStatus,
  Check,
  CheckStatusLike,
  CsrfToken,
  CurrentUser,
  Employee,
  EmployeeDetail,
  EmployeeChange,
  LatestCheck,
} from '../types';

const CHANGE_LABEL = { PENDING: '승인 대기', APPROVED: '승인 완료', REJECTED: '반려' } as const;

function formatDateTime(value: string | null | undefined) {
  return value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-';
}

function resultText(value: boolean | null) {
  if (value === null) return '확인되지 않음';
  return value ? '있음' : '없음';
}

function verifiedText(value: boolean | null) {
  if (value === null) return '확인되지 않음';
  return value ? '확인 완료' : '확인 실패';
}

const STATUS_LABELS: Record<BackgroundCheckStatus, string> = {
  REQUESTING: '접수 중…',
  PENDING: '검사 진행 중',
  CLEAR: 'CLEAR',
  FLAGGED: 'FLAGGED',
  SUBMISSION_UNKNOWN: '접수 확인 필요',
  SUBMISSION_FAILED: '접수 실패',
};

function checkLabel(check: CheckStatusLike | null) {
  if (!check) return '검사 없음';
  if (check.trackingStopReason) return '조회 중단';
  if (check.trackingActive) return '검사 진행 중';
  return STATUS_LABELS[check.status] ?? check.status;
}

function EmployeeCreateForm({ csrf, onCreated }: { csrf: CsrfToken | null; onCreated: () => void }) {
  const [error, setError] = useState('');

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const f = new FormData(form);
    const date = String(f.get('dateOfBirth') ?? '');
    const payload = {
      employeeNumber: String(f.get('employeeNumber')),
      lastName: String(f.get('lastName')),
      firstName: String(f.get('firstName')),
      dateOfBirth: date || null,
      username: String(f.get('username')),
      initialPassword: String(f.get('initialPassword')),
    };
    try {
      await api('/api/admin/employees', { method: 'POST', body: JSON.stringify(payload) }, csrf);
      form.reset();
      setError('');
      onCreated();
    } catch (x) {
      setError(x instanceof Error ? x.message : '생성하지 못했습니다.');
    }
  }

  return (
    <form className="card form compact" onSubmit={submit}>
      <h2>직원 계정 생성</h2>
      <div className="grid">
        <label>
          사번
          <input name="employeeNumber" placeholder="EMP-011" required />
        </label>
        <label>
          성
          <input name="lastName" required />
        </label>
        <label>
          이름
          <input name="firstName" required />
        </label>
        <label>
          생년월일
          <input name="dateOfBirth" type="date" />
        </label>
        <label>
          로그인 아이디
          <input name="username" required />
        </label>
        <label>
          초기 비밀번호
          <input name="initialPassword" type="password" minLength={8} required />
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      <button>생성</button>
    </form>
  );
}

export function EmployeesPage({
  user,
  csrf,
  onLogout,
}: {
  user: CurrentUser;
  csrf: CsrfToken | null;
  onLogout: () => Promise<void>;
}) {
  const [selected, setSelected] = useState<number | null>(null);
  const [history, setHistory] = useState<number | null>(null);
  const [revision, setRevision] = useState(0);
  const [changeRevision, setChangeRevision] = useState(0);

  const { data: items, error } = usePolling<Employee[]>(
    signal => api<Employee[]>('/api/admin/employees', { signal }),
    { intervalMs: 5000, initialData: [], errorMessage: '목록 조회 실패' },
    [revision],
  );

  const changed = () => setRevision(value => value + 1);

  return (
    <Shell title="직원 관리" user={user} onLogout={onLogout}>
      <EmployeeCreateForm csrf={csrf} onCreated={changed} />
      <ChangeRequestManagement csrf={csrf} revision={changeRevision} onChanged={() => {
        setChangeRevision(value => value + 1);
        changed();
      }} />
      <section className="card table-card">
        <h2>전체 직원</h2>
        <p className="muted">가장 최근에 시작한 검사 상태입니다. 5초마다 자동 갱신됩니다.</p>
        {error && <p className="error">{error}</p>}
        <table>
          <thead>
            <tr>
              <th>사번</th>
              <th>성명</th>
              <th>생년월일</th>
              <th>재직 상태</th>
              <th>최신 Background Check</th>
              <th>조회</th>
            </tr>
          </thead>
          <tbody>
            {items.map(e => (
              <Fragment key={e.id}>
                <tr
                  className={selected === e.id ? 'selected-row' : undefined}
                  onClick={() => {
                    const closing = selected === e.id;
                    setSelected(closing ? null : e.id);
                    if (closing) setHistory(null);
                  }}
                >
                  <td>{e.employeeNumber}</td>
                  <td>{e.fullName}</td>
                  <td>{e.dateOfBirth ?? '확인되지 않음'}</td>
                  <td>{e.employmentStatus === 'ACTIVE' ? '재직' : '퇴사'}</td>
                  <td>
                    <span
                      className={
                        'check-badge ' +
                        (e.latestBackgroundCheck?.trackingStopReason
                          ? 'stopped'
                          : e.latestBackgroundCheck?.trackingActive
                            ? 'pending'
                            : (e.latestBackgroundCheck?.status.toLowerCase() ?? 'none'))
                      }
                    >
                      {checkLabel(e.latestBackgroundCheck)}
                    </span>
                    {e.latestBackgroundCheck && (
                      <small className="check-date">{formatDateTime(e.latestBackgroundCheck.requestedAt)}</small>
                    )}
                  </td>
                  <td>
                    <button
                      className="secondary"
                      aria-expanded={history === e.id}
                      onClick={event => {
                        event.stopPropagation();
                        setHistory(history === e.id ? null : e.id);
                      }}
                    >
                      {history === e.id ? '이력 닫기' : '검사 이력'}
                    </button>
                  </td>
                </tr>
                {selected === e.id && (
                  <tr className="panel-row">
                    <td colSpan={6}>
                      <EmployeePanel id={e.id} csrf={csrf} latest={e.latestBackgroundCheck} onChanged={changed} />
                    </td>
                  </tr>
                )}
                {history === e.id && (
                  <tr className="panel-row">
                    <td colSpan={6}>
                      <CheckHistory employeeId={e.id} csrf={csrf} latestId={e.latestBackgroundCheck?.id} />
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </section>
    </Shell>
  );
}

function EmployeePanel({
  id,
  csrf,
  latest,
  onChanged,
}: {
  id: number;
  csrf: CsrfToken | null;
  latest: LatestCheck | null;
  onChanged: () => void;
}) {
  const [employee, setEmployee] = useState<EmployeeDetail | null>(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [accepted, setAccepted] = useState<LatestCheck | null>(null);
  const [latestDetail, setLatestDetail] = useState<Check | null>(null);

  const load = () => api<EmployeeDetail>(`/api/admin/employees/${id}`).then(setEmployee).catch(e => setError(e.message));
  useEffect(() => {
    void load();
  }, [id]);

  async function terminate() {
    const date = window.prompt('실제 퇴사일을 YYYY-MM-DD 형식으로 입력하세요.');
    if (!date) return;
    try {
      await api(`/api/admin/employees/${id}/termination`, { method: 'POST', body: JSON.stringify({ terminationDate: date }) }, csrf);
      await load();
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : '퇴사 처리 실패');
    }
  }

  async function start() {
    if (submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const created = await api<Check>(`/api/admin/employees/${id}/background-checks`, { method: 'POST' }, csrf);
      setAccepted(created);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : '검사 접수 실패');
    } finally {
      setSubmitting(false);
    }
  }

  const current = accepted && (!latest || accepted.id > latest.id) ? accepted : latest;
  const checkInProgress = Boolean(current && (
    current.trackingActive || current.status === 'REQUESTING' || current.status === 'PENDING'
  ));

  useEffect(() => {
    if (!current?.id) {
      setLatestDetail(null);
      return;
    }
    const controller = new AbortController();
    api<Check>(`/api/admin/employees/${id}/background-checks/${current.id}`, { signal: controller.signal })
      .then(setLatestDetail)
      .catch(e => { if (!controller.signal.aborted) setError(e.message); });
    return () => controller.abort();
  }, [id, current?.id, current?.status, current?.trackingActive]);

  return (
    <div className="panel">
      {error && <p className="error">{error}</p>}
      {employee && (
        <>
          <div className="row">
            <div>
              <h2>{employee.fullName}</h2>
              <p>
                {employee.employeeNumber} · {employee.account?.username}
              </p>
            </div>
            {employee.employmentStatus === 'ACTIVE' && (
              <button className="danger" onClick={() => void terminate()}>
                퇴사 처리
              </button>
            )}
          </div>
          {employee.terminationDate && (
            <p>
              실제 퇴사일 {employee.terminationDate} · 처리 시각 {formatDateTime(employee.terminatedAt)}
            </p>
          )}
          <div className="row">
            <p role="status">{submitting ? '접수 중…' : checkLabel(current)}</p>
            <button disabled={submitting || checkInProgress} aria-busy={submitting} onClick={() => void start()}>
              {submitting ? '접수 중…' : '검사 시작'}
            </button>
          </div>
          {latestDetail && <CheckResult employeeId={id} initial={latestDetail} csrf={csrf} title="최신 검사 결과" />}
        </>
      )}
    </div>
  );
}

function CheckHistory({
  employeeId,
  csrf,
  latestId,
}: {
  employeeId: number;
  csrf: CsrfToken | null;
  latestId?: number;
}) {
  const [checks, setChecks] = useState<Check[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    api<Check[]>(`/api/admin/employees/${employeeId}/background-checks`, { signal: controller.signal })
      .then(result => {
        setChecks(result);
        setError('');
      })
      .catch(e => {
        if (!controller.signal.aborted) setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [employeeId, latestId]);

  return (
    <div className="panel">
      <h3>검사 이력</h3>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="muted">이력을 불러오는 중…</p>
      ) : checks.length === 0 ? (
        <p className="muted">검사 이력이 없습니다.</p>
      ) : (
        checks.map(check => <CheckResult key={check.id} employeeId={employeeId} initial={check} csrf={csrf} />)
      )}
    </div>
  );
}

function CheckResult({ employeeId, initial, csrf, title }: { employeeId: number; initial: Check; csrf: CsrfToken | null; title?: string }) {
  const [revision, setRevision] = useState(0);
  const [retrying, setRetrying] = useState(false);

  const {
    data: check,
    setData: setCheck,
    error,
    setError,
  } = usePolling<Check>(
    signal => api<Check>(`/api/admin/employees/${employeeId}/background-checks/${initial.id}`, { signal }),
    {
      intervalMs: 5000,
      initialData: initial,
      shouldContinue: result => result.trackingActive,
      errorMessage: '화면 갱신에 실패했습니다. 화면을 다시 열어 주세요.',
    },
    [employeeId, initial, revision],
  );

  async function retry() {
    if (retrying) return;
    setRetrying(true);
    setError('');
    try {
      const updated = await api<Check>(
        `/api/admin/employees/${employeeId}/background-checks/${initial.id}/retry`,
        { method: 'POST' },
        csrf,
      );
      setCheck(updated);
      setRevision(value => value + 1);
    } catch (e) {
      setError(e instanceof Error ? e.message : '재시도 요청에 실패했습니다.');
    } finally {
      setRetrying(false);
    }
  }

  return (
    <article className="check">
      {title && <h3>{title}</h3>}
      <div>
        <strong role="status">{check.trackingActive ? '검사 진행 중' : check.status}</strong>
        <span>{formatDateTime(check.requestedAt)}</span>
      </div>
      {check.trackingStopReason && <p className="error">자동 결과 확인이 중단되었습니다. ({check.trackingStopReason})</p>}
      {check.trackingStopReason && !check.trackingActive && check.externalCheckId && (
        <button disabled={retrying} onClick={() => void retry()}>
          {retrying ? '재시도 접수 중…' : '재시도'}
        </button>
      )}
      {error && <p className="error">{error}</p>}
      {!check.trackingActive && check.result && <table className="result-table"><tbody>
        <tr><th>범죄 기록</th><td>{resultText(check.result.criminalRecord)}</td></tr>
        <tr><th>학력 확인</th><td>{verifiedText(check.result.educationVerified)}</td></tr>
        <tr><th>경력 확인</th><td>{verifiedText(check.result.employmentVerified)}</td></tr>
        <tr><th>신용 점수</th><td>{check.result.creditScore ?? '확인되지 않음'}</td></tr>
        <tr><th>완료 시각</th><td>{formatDateTime(check.completedAt)}</td></tr>
      </tbody></table>}
    </article>
  );
}

function ChangeRequestManagement({ csrf, revision, onChanged }: {
  csrf: CsrfToken | null;
  revision: number;
  onChanged: () => void;
}) {
  const [items, setItems] = useState<EmployeeChange[]>([]);
  const [error, setError] = useState('');
  const [processing, setProcessing] = useState<number | null>(null);

  const load = () => api<EmployeeChange[]>('/api/admin/employee-change-requests')
    .then(result => { setItems(result); setError(''); })
    .catch(e => setError(e.message));

  useEffect(() => { void load(); }, [revision]);

  async function review(id: number, action: 'approve' | 'reject') {
    setProcessing(id);
    setError('');
    try {
      await api(`/api/admin/employee-change-requests/${id}/${action}`, { method: 'POST' }, csrf);
      await load();
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : '변경 요청을 처리하지 못했습니다.');
    } finally {
      setProcessing(null);
    }
  }

  return <section className="card table-card">
    <h2>인적사항 변경 요청</h2>
    {error && <p className="error">{error}</p>}
    {items.length === 0 ? <p className="muted">변경 요청 이력이 없습니다.</p> : <table>
      <thead><tr><th>직원</th><th>변경 전</th><th>요청 내용</th><th>상태</th><th>요청/처리 시각</th><th>처리</th></tr></thead>
      <tbody>{items.map(item => <tr key={item.id}>
        <td>{item.employeeNumber}<br />{item.employeeName}</td>
        <td>{item.previous.fullName}<br />{item.previous.dateOfBirth ?? '미확인'}</td>
        <td>{item.requested.fullName}<br />{item.requested.dateOfBirth ?? '미확인'}</td>
        <td><span className={`change-badge ${item.status.toLowerCase()}`}>{CHANGE_LABEL[item.status]}</span></td>
        <td>요청 {formatDateTime(item.requestedAt)}<br />처리 {formatDateTime(item.reviewedAt)}</td>
        <td>{item.status === 'PENDING' && <div className="button-group">
          <button disabled={processing === item.id} onClick={() => void review(item.id, 'approve')}>승인</button>
          <button className="danger" disabled={processing === item.id} onClick={() => void review(item.id, 'reject')}>반려</button>
        </div>}</td>
      </tr>)}</tbody>
    </table>}
  </section>;
}
