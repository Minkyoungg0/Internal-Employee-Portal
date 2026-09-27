import { Fragment, FormEvent, ReactNode, useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';

type Role = 'EMPLOYEE' | 'ADMIN';
type CurrentUser = { accountId:number; employeeId:number; employeeNumber:string; username:string; role:Role; passwordChangeRequired:boolean };
type CsrfToken = { headerName:string; parameterName:string; token:string };
type ApiError = { code?:string; message?:string };
type Profile = { id:number; employeeNumber:string; lastName:string; firstName:string; fullName:string; dateOfBirth:string|null; employmentStatus:string; username:string };
type Employee = { id:number; employeeNumber:string; fullName:string; dateOfBirth:string|null; employmentStatus:'ACTIVE'|'TERMINATED' };
type EmployeeDetail = Employee & { lastName:string; firstName:string; terminationDate:string|null; terminatedAt:string|null; account:{username:string;enabled:boolean;passwordChangeRequired:boolean}|null };
type Check = { trackingActive:boolean; trackingStopReason:string|null; id:number; externalCheckId:string|null; submittedFirstName:string; submittedLastName:string; submittedDateOfBirth:string; status:string; result:{criminalRecord:boolean|null;educationVerified:boolean|null;employmentVerified:boolean|null;creditScore:string|null}|null; requestedAt:string; completedAt:string|null; lastCheckedAt:string|null };

async function readError(response:Response, fallback:string) { const body=await response.json().catch(()=>({})) as ApiError; return body.message??fallback; }
async function fetchCsrf() { const r=await fetch('/api/auth/csrf',{credentials:'same-origin'}); if(!r.ok) throw new Error('보안 토큰을 발급하지 못했습니다.'); return r.json() as Promise<CsrfToken>; }
async function api<T>(url:string, options:RequestInit={}, csrf?:CsrfToken|null):Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type','application/json');
  if (options.method && options.method !== 'GET') { const token=csrf??await fetchCsrf(); headers.set(token.headerName,token.token); }
  const response=await fetch(url,{...options,headers,credentials:'same-origin'});
  if(!response.ok) throw new Error(await readError(response,'요청을 처리하지 못했습니다.'));
  return response.status===204 ? undefined as T : response.json() as Promise<T>;
}

function LoginPage({user,csrf,onLogin,onCsrfChange}:{user:CurrentUser|null;csrf:CsrfToken|null;onLogin:(u:CurrentUser)=>void;onCsrfChange:(token:CsrfToken)=>void}) {
  const [error,setError]=useState(''); const [busy,setBusy]=useState(false); const navigate=useNavigate();
  if(user) return <Navigate to={user.passwordChangeRequired?'/change-password':user.role==='ADMIN'?'/admin/employees':'/me'} replace/>;
  async function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();setBusy(true);setError('');try{const f=new FormData(e.currentTarget);const token=csrf??await fetchCsrf();const body=new URLSearchParams({username:String(f.get('username')),password:String(f.get('password'))});const r=await fetch('/api/auth/login',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8',[token.headerName]:token.token},body});if(!r.ok)throw new Error(await readError(r,'로그인하지 못했습니다.'));const u=await r.json() as CurrentUser;onCsrfChange(await fetchCsrf());onLogin(u);navigate(u.passwordChangeRequired?'/change-password':u.role==='ADMIN'?'/admin/employees':'/me',{replace:true});}catch(x){setError(x instanceof Error?x.message:'로그인하지 못했습니다.')}finally{setBusy(false)}}
  return <main className="centered"><form className="card form" onSubmit={submit}><p className="eyebrow">BIT COMPUTER</p><h1>직원 포털</h1><label>아이디<input name="username" autoComplete="username" required/></label><label>비밀번호<input name="password" type="password" autoComplete="current-password" required/></label>{error&&<p className="error">{error}</p>}<button disabled={busy}>{busy?'로그인 중…':'로그인'}</button></form></main>;
}

function ChangePasswordPage({user,csrf,onChanged}:{user:CurrentUser;csrf:CsrfToken|null;onChanged:()=>void}) {
  const [error,setError]=useState(''); const navigate=useNavigate();
  async function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();setError('');const f=new FormData(e.currentTarget);const next=String(f.get('newPassword'));if(next!==String(f.get('confirmPassword'))){setError('새 비밀번호 확인이 일치하지 않습니다.');return;}try{await api('/api/me/password',{method:'PUT',body:JSON.stringify({currentPassword:f.get('currentPassword'),newPassword:next})},csrf);onChanged();navigate(user.role==='ADMIN'?'/admin/employees':'/me',{replace:true});}catch(x){setError(x instanceof Error?x.message:'비밀번호를 변경하지 못했습니다.')}}
  return <main className="centered"><form className="card form" onSubmit={submit}><p className="eyebrow">PASSWORD</p><h1>{user.passwordChangeRequired?'초기 비밀번호 변경':'비밀번호 변경'}</h1><label>현재 비밀번호<input name="currentPassword" type="password" required/></label><label>새 비밀번호<input name="newPassword" type="password" minLength={8} required/></label><label>새 비밀번호 확인<input name="confirmPassword" type="password" minLength={8} required/></label>{error&&<p className="error">{error}</p>}<button>변경하기</button></form></main>;
}

function Shell({title,user,onLogout,children}:{title:string;user:CurrentUser;onLogout:()=>Promise<void>;children:ReactNode}){return <div className="app-shell"><aside><strong>Employee Portal</strong><p className="account">{user.employeeNumber} · {user.username}</p><nav><Link to="/me">내 정보</Link><Link to="/change-password">비밀번호 변경</Link>{user.role==='ADMIN'&&<Link to="/admin/employees">직원 관리</Link>}</nav><button className="logout" onClick={()=>void onLogout()}>로그아웃</button></aside><main className="content"><h1>{title}</h1>{children}</main></div>}

function ProfilePage({user,onLogout}:{user:CurrentUser;onLogout:()=>Promise<void>}){const [profile,setProfile]=useState<Profile|null>(null);const [error,setError]=useState('');useEffect(()=>{api<Profile>('/api/me/profile').then(setProfile).catch(x=>setError(x.message))},[]);return <Shell title="내 정보" user={user} onLogout={onLogout}><section className="card">{error&&<p className="error">{error}</p>}{profile&&<dl className="details"><dt>사번</dt><dd>{profile.employeeNumber}</dd><dt>성명</dt><dd>{profile.fullName}</dd><dt>생년월일</dt><dd>{profile.dateOfBirth??'확인되지 않음'}</dd><dt>아이디</dt><dd>{profile.username}</dd><dt>재직 상태</dt><dd>{profile.employmentStatus==='ACTIVE'?'재직':'퇴사'}</dd></dl>}<p className="muted">개인정보 변경이 필요하면 관리자에게 요청해 주세요. 비밀번호는 직접 변경할 수 있습니다.</p></section></Shell>}

function EmployeeCreateForm({csrf,onCreated}:{csrf:CsrfToken|null;onCreated:()=>void}){const [error,setError]=useState('');async function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();const form=e.currentTarget;const f=new FormData(form);const date=String(f.get('dateOfBirth')??'');const payload={employeeNumber:String(f.get('employeeNumber')),lastName:String(f.get('lastName')),firstName:String(f.get('firstName')),dateOfBirth:date||null,username:String(f.get('username')),initialPassword:String(f.get('initialPassword'))};try{await api('/api/admin/employees',{method:'POST',body:JSON.stringify(payload)},csrf);form.reset();setError('');onCreated()}catch(x){setError(x instanceof Error?x.message:'생성하지 못했습니다.')}}return <form className="card form compact" onSubmit={submit}><h2>직원 계정 생성</h2><div className="grid"><label>사번<input name="employeeNumber" placeholder="EMP-011" required/></label><label>성<input name="lastName" required/></label><label>이름<input name="firstName" required/></label><label>생년월일<input name="dateOfBirth" type="date"/></label><label>로그인 아이디<input name="username" required/></label><label>초기 비밀번호<input name="initialPassword" type="password" minLength={8} required/></label></div>{error&&<p className="error">{error}</p>}<button>생성</button></form>}

function EmployeesPage({user,csrf,onLogout}:{user:CurrentUser;csrf:CsrfToken|null;onLogout:()=>Promise<void>}){const [items,setItems]=useState<Employee[]>([]);const [selected,setSelected]=useState<number|null>(null);const load=()=>api<Employee[]>('/api/admin/employees').then(setItems);useEffect(()=>{void load()},[]);return <Shell title="직원 관리" user={user} onLogout={onLogout}><EmployeeCreateForm csrf={csrf} onCreated={()=>void load()}/><section className="card table-card"><h2>전체 직원</h2><table><thead><tr><th>사번</th><th>성명</th><th>생년월일</th><th>상태</th></tr></thead><tbody>{items.map(e=>{const isSelected=selected===e.id;return <Fragment key={e.id}><tr className={isSelected?'selected-row':undefined} onClick={()=>setSelected(isSelected?null:e.id)}><td>{e.employeeNumber}</td><td>{e.fullName}</td><td>{e.dateOfBirth??'확인되지 않음'}</td><td>{e.employmentStatus==='ACTIVE'?'재직':'퇴사'}</td></tr>{isSelected&&<tr className="panel-row"><td colSpan={4}><EmployeePanel id={e.id} csrf={csrf} onChanged={()=>void load()}/></td></tr>}</Fragment>})}</tbody></table></section></Shell>}

function EmployeePanel({id,csrf,onChanged}:{id:number;csrf:CsrfToken|null;onChanged:()=>void}){const [employee,setEmployee]=useState<EmployeeDetail|null>(null);const [checks,setChecks]=useState<Check[]>([]);const [error,setError]=useState('');const [submitting,setSubmitting]=useState(false);const load=()=>Promise.all([api<EmployeeDetail>(`/api/admin/employees/${id}`),api<Check[]>(`/api/admin/employees/${id}/background-checks`)]).then(([e,c])=>{setEmployee(e);setChecks(c);setError('')}).catch(x=>setError(x.message));useEffect(()=>{void load()},[id]);async function terminate(){const date=window.prompt('실제 퇴사일을 YYYY-MM-DD 형식으로 입력하세요.');if(!date)return;try{await api(`/api/admin/employees/${id}/termination`,{method:'POST',body:JSON.stringify({terminationDate:date})},csrf);await load();onChanged()}catch(x){setError(x instanceof Error?x.message:'퇴사 처리하지 못했습니다.')}}async function start(){
  if(submitting)return;
  setSubmitting(true);
  setError('');
  try {
    const created=await api<Check>(`/api/admin/employees/${id}/background-checks`,{method:'POST'},csrf);
    setChecks(previous=>[created,...previous.filter(check=>check.id!==created.id)]);
  } catch(x) {
    setError(x instanceof Error?x.message:'검사를 시작하지 못했습니다.');
  } finally {
    setSubmitting(false);
  }
}return <div className="panel">{error&&<p className="error">{error}</p>}{employee&&<><div className="row"><div><h2>{employee.fullName}</h2><p>{employee.employeeNumber} · {employee.dateOfBirth??'생년월일 미확인'} · {employee.account?.username}</p></div>{employee.employmentStatus==='ACTIVE'&&<button className="danger" onClick={()=>void terminate()}>퇴사 처리</button>}</div>{employee.terminationDate&&<p>실제 퇴사일 {employee.terminationDate} · 처리 시각 {employee.terminatedAt}</p>}<div className="row"><h3>Background Check</h3><button disabled={submitting} aria-busy={submitting} onClick={()=>void start()}>{submitting?'접수 중…':'검사 시작'}</button></div>{checks.length===0?<p className="muted">검사 이력이 없습니다.</p>:checks.map(c=><CheckResult key={c.id} employeeId={id} initial={c} csrf={csrf}/>)}</>}</div>}

function CheckResult({employeeId, initial, csrf}:{employeeId:number;initial:Check;csrf:CsrfToken|null}) {
  const [check,setCheck]=useState(initial);
  const [retrying,setRetrying]=useState(false);
  const [revision,setRevision]=useState(0);
  async function retry(){
    if(retrying)return;
    setRetrying(true);
    setError('');
    try {
      const updated=await api<Check>(`/api/admin/employees/${employeeId}/background-checks/${initial.id}/retry`,{method:'POST'},csrf);
      setCheck(updated);
      setRevision(value=>value+1);
    } catch(e) {
      setError(e instanceof Error?e.message:'재시도 요청에 실패했습니다.');
    } finally {setRetrying(false);}
  }

  const [error,setError]=useState('');
  useEffect(()=>{
    const controller=new AbortController();
    let timer:ReturnType<typeof setTimeout>|undefined;
    async function read(){
      try {
        const result=await api<Check>(`/api/admin/employees/${employeeId}/background-checks/${initial.id}`, {signal:controller.signal});
        if(controller.signal.aborted)return;
        setCheck(result);
        setError('');
        if(result.trackingActive)timer=setTimeout(()=>void read(),5000);
      } catch(e) {
        if(!controller.signal.aborted)setError(e instanceof Error?e.message:'화면 갱신에 실패했습니다. 화면을 다시 열어 주세요.');
      }
    }
    void read();
    return ()=>{controller.abort();if(timer)clearTimeout(timer)};
  },[employeeId,initial,revision]);
  return <article className="check">
    <div><strong role="status">{check.trackingActive?'검사 진행 중':check.status}</strong>
      <span>{new Date(check.requestedAt).toLocaleString()}</span></div>
    {check.trackingStopReason&&<p className="error">자동 결과 확인이 중단되었습니다. ({check.trackingStopReason})</p>}
    {check.trackingStopReason&&!check.trackingActive&&check.externalCheckId&&<button disabled={retrying} onClick={()=>void retry()}>{retrying?'재시도 접수 중…':'재시도'}</button>}
    {error&&<p className="error">{error}</p>}
    {!check.trackingActive&&check.result&&<p>범죄 기록 {String(check.result.criminalRecord)} · 학력 확인 {String(check.result.educationVerified)} · 경력 확인 {String(check.result.employmentVerified)} · 신용 {check.result.creditScore}</p>}
  </article>;
}

function Protected({user,role,children}:{user:CurrentUser|null;role?:Role;children:ReactNode}){if(!user)return <Navigate to="/login" replace/>;if(user.passwordChangeRequired)return <Navigate to="/change-password" replace/>;if(role&&user.role!==role)return <Navigate to="/me" replace/>;return children}

export default function App(){const [user,setUser]=useState<CurrentUser|null>(null);const [csrf,setCsrf]=useState<CsrfToken|null>(null);const [loading,setLoading]=useState(true);useEffect(()=>{fetchCsrf().then(t=>{setCsrf(t);return fetch('/api/auth/me',{credentials:'same-origin'})}).then(async r=>{if(r.ok)setUser(await r.json())}).finally(()=>setLoading(false))},[]);async function logout(){try{await api('/api/auth/logout',{method:'POST'},csrf)}finally{setUser(null);setCsrf(await fetchCsrf())}}if(loading)return <main className="centered">로그인 상태를 확인하고 있습니다…</main>;return <Routes><Route path="/login" element={<LoginPage user={user} csrf={csrf} onLogin={setUser} onCsrfChange={setCsrf}/>}/><Route path="/change-password" element={user?<ChangePasswordPage user={user} csrf={csrf} onChanged={()=>setUser({...user,passwordChangeRequired:false})}/>:<Navigate to="/login"/>}/><Route path="/me" element={<Protected user={user}><ProfilePage user={user!} onLogout={logout}/></Protected>}/><Route path="/admin/employees" element={<Protected user={user} role="ADMIN"><EmployeesPage user={user!} csrf={csrf} onLogout={logout}/></Protected>}/><Route path="*" element={<Navigate to={user?user.passwordChangeRequired?'/change-password':user.role==='ADMIN'?'/admin/employees':'/me':'/login'} replace/>}/></Routes>}
