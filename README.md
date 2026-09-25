# Employee Management System

사내 직원 포털 과제 프로젝트입니다. 현재 세션 로그인과 역할별 접근 통제의 첫 번째 수직 기능이 구현되어 있습니다.

## 실행

`.env.example`을 참고해 `.env`를 준비한 후 실행합니다.

```sh
docker compose up --build
```

웹 애플리케이션은 `http://localhost`에서 접근합니다. 운영 HTTPS 환경에서는 `SESSION_COOKIE_SECURE=true`를 사용해야 합니다.

## 데모 계정

| 역할 | 아이디 | 비밀번호 | 연결 직원 |
|---|---|---|---|
| 직원 | `employee` | `Employee!234` | EMP-001 김민준 |
| 관리자 | `admin` | `Admin!234` | EMP-010 정하윤 |

데모 비밀번호는 로컬 과제 실행 전용입니다. DB에는 BCrypt 해시만 저장합니다. 실제 운영에서는 고정 시드 계정을 제거하고 최초 관리자 등록 또는 사내 인증 시스템으로 교체해야 합니다.

## 인증 정책

- Spring Security 메모리 세션, 유휴 만료 30분
- 계정당 동시 세션 1개. 새 로그인은 이전 세션을 만료시킴
- `HttpOnly`, `SameSite=Lax` 세션 쿠키
- 모든 상태 변경 요청에 CSRF 토큰 필요
- 보호 API 요청마다 현재 계정과 재직 상태를 DB에서 확인
- 퇴사 또는 비활성 계정의 기존 세션은 다음 보호 요청에서 폐기

API 서버가 재시작되면 사용자는 다시 로그인해야 합니다. 다중 인스턴스 또는 재시작 후 세션 유지가 필요해지면 표준 세션 API를 유지한 채 Spring Session JDBC나 Redis로 저장소를 교체합니다.

## 검증

```sh
cd apps/api && ./gradlew test
cd apps/web && npm run build
```

Background Check API 측정 결과와 정책 근거는 [MEASUREMENTS.md](MEASUREMENTS.md)를 참고합니다.
